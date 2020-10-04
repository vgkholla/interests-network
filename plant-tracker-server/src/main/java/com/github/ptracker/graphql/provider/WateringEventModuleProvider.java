package com.github.ptracker.graphql.provider;

import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.WateringEventCreateRequest;
import com.github.ptracker.service.WateringEventDeleteRequest;
import com.github.ptracker.service.WateringEventDeleteResponse;
import com.github.ptracker.service.WateringEventGetRequest;
import com.github.ptracker.service.WateringEventGetResponse;
import com.github.ptracker.service.WateringEventGrpc;
import com.github.ptracker.service.WateringEventGrpc.WateringEventBlockingStub;
import com.github.ptracker.service.WateringEventGrpc.WateringEventFutureStub;
import com.github.ptracker.service.WateringEventQueryRequest;
import com.github.ptracker.service.WateringEventQueryResponse;
import com.github.ptracker.service.WateringEventUpdateRequest;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import static com.github.ptracker.graphql.GraphQLVerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class WateringEventModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public WateringEventModuleProvider(String serverHost, int serverPort) {
    _clientModule = new ClientModule(serverHost, serverPort);
  }

  @Override
  public Optional<Module> getClientModule() {
    return Optional.of(_clientModule);
  }

  @Override
  public Optional<Module> getSchemaModule() {
    return Optional.of(_schemaModule);
  }

  @Override
  public void registerDataLoaders(DataLoaderRegistry registry) {
    _clientModule.registerDataLoaders(registry);
  }

  public static CompletableFuture<List<WateringEvent>> getWateringEventByGardenPlantId(
      DataFetchingEnvironment environment, String gardenPlantId) {
    return WateringEventModuleProvider.ClientModule.getWateringEventByGardenPlantId(environment, gardenPlantId);
  }

  public static CompletableFuture<List<WateringEvent>> getWateringEventByGardenerId(DataFetchingEnvironment environment,
      String gardenerId) {
    checkNotNull(environment, "DataFetchingEnvironment cannot be null");
    checkNotNull(gardenerId, "WateringEvent ID cannot be null");
    return WateringEventModuleProvider.ClientModule.getWateringEventByGardenPlantId(environment, gardenerId);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "wateringEvents";
    private static final String GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME = "wateringEventsByGardenPlantId";
    private static final String GET_BY_GARDENER_ID_DATA_LOADER_NAME = "wateringEventsByGardenerId";

    private final String _host;
    private final int _port;

    private WateringEventFutureStub _futureStub;

    ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(WateringEventBlockingStub.class).toInstance(WateringEventGrpc.newBlockingStub(channel));
      _futureStub = WateringEventGrpc.newFutureStub(channel);
      bind(WateringEventFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry,
          ImmutableList.of(GET_BY_ID_DATA_LOADER_NAME, GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME,
              GET_BY_GARDENER_ID_DATA_LOADER_NAME));

      // by id
      GrpcNotFoundSwallower<String, WateringEventGetResponse> idToWateringEvent =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(WateringEventGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, WateringEvent> byIdLoader = ids -> {
        List<ListenableFuture<WateringEvent>> futures = ids.stream()
            .map(id -> Futures.transform(idToWateringEvent.apply(id),
                response -> response != null ? response.getWateringEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<WateringEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));

      // by garden plant id
      GrpcNotFoundSwallower<String, WateringEventQueryResponse> gardenPlantIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(WateringEventQueryRequest.newBuilder()
              .setTemplate(WateringEvent.newBuilder().setGardenPlantId(id).build())
              .build()));
      BatchLoader<String, List<WateringEvent>> byGardenPlantIdLoader = ids -> {
        List<ListenableFuture<List<WateringEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenPlantIdToEvent.apply(id),
                response -> response != null ? response.getWateringEventList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<WateringEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenPlantIdLoader));

      // by gardener id
      GrpcNotFoundSwallower<String, WateringEventQueryResponse> gardenerIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(WateringEventQueryRequest.newBuilder()
              .setTemplate(
                  WateringEvent.newBuilder().setMetadata(EventMetadata.newBuilder().setGardenerId(id).build()).build())
              .build()));
      BatchLoader<String, List<WateringEvent>> byGardenerIdLoader = ids -> {
        List<ListenableFuture<List<WateringEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenerIdToEvent.apply(id),
                response -> response != null ? response.getWateringEventList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<WateringEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDENER_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenerIdLoader));
    }

    static CompletableFuture<WateringEvent> getWateringEvent(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "WateringEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, WateringEvent>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }

    static CompletableFuture<List<WateringEvent>> getWateringEventByGardenPlantId(DataFetchingEnvironment environment,
        String gardenPlantId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenPlantId, "WateringEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<WateringEvent>>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(gardenPlantId);
    }

    static CompletableFuture<List<WateringEvent>> getWateringEventByGardenerId(DataFetchingEnvironment environment,
        String gardenerId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenerId, "WateringEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<WateringEvent>>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(gardenerId);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getWateringEvent")
    ListenableFuture<WateringEvent> getWateringEvent(WateringEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          ClientModule.getWateringEvent(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createWateringEvent")
    ListenableFuture<WateringEvent> createWateringEvent(WateringEventCreateRequest request,
        WateringEventFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getWateringEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateWateringEvent")
    ListenableFuture<WateringEvent> updateWateringEvent(WateringEventUpdateRequest request,
        WateringEventFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getWateringEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteWateringEvent")
    ListenableFuture<WateringEventDeleteResponse> deleteWateringEvent(WateringEventDeleteRequest request,
        WateringEventFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "gardenPlant", onType = WateringEvent.class)
    ListenableFuture<GardenPlant> eventToGardenPlant(WateringEvent event, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenPlantModuleProvider.getGardenPlant(environment, event.getGardenPlantId()));
    }
  }
}
