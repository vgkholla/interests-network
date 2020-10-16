package com.github.ptracker.graphql.provider;

import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.FertilizationEventCreateRequest;
import com.github.ptracker.service.FertilizationEventDeleteRequest;
import com.github.ptracker.service.FertilizationEventDeleteResponse;
import com.github.ptracker.service.FertilizationEventGetRequest;
import com.github.ptracker.service.FertilizationEventGetResponse;
import com.github.ptracker.service.FertilizationEventGrpc;
import com.github.ptracker.service.FertilizationEventGrpc.FertilizationEventBlockingStub;
import com.github.ptracker.service.FertilizationEventGrpc.FertilizationEventFutureStub;
import com.github.ptracker.service.FertilizationEventQueryRequest;
import com.github.ptracker.service.FertilizationEventQueryResponse;
import com.github.ptracker.service.FertilizationEventUpdateRequest;
import com.github.ptracker.util.IdGenerator;
import com.github.ptracker.util.RandomStringIdGenerator;
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


public class FertilizationEventModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public FertilizationEventModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<List<FertilizationEvent>> getFertilizationEventsByGardenPlantId(
      DataFetchingEnvironment environment, String gardenPlantId) {
    return ClientModule.getFertilizationEventsByGardenPlantId(environment, gardenPlantId);
  }

  public static CompletableFuture<List<FertilizationEvent>> getFertilizationEventsByGardenerId(
      DataFetchingEnvironment environment, String gardenerId) {
    checkNotNull(environment, "DataFetchingEnvironment cannot be null");
    checkNotNull(gardenerId, "FertilizationEvent ID cannot be null");
    return ClientModule.getFertilizationEventsByGardenerId(environment, gardenerId);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "fertilizationEvents";
    private static final String GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME = "fertilizationEventsByGardenPlantId";
    private static final String GET_BY_GARDENER_ID_DATA_LOADER_NAME = "fertilizationEventsByGardenerId";

    private final String _host;
    private final int _port;

    private FertilizationEventFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(FertilizationEventBlockingStub.class).toInstance(FertilizationEventGrpc.newBlockingStub(channel));
      _futureStub = FertilizationEventGrpc.newFutureStub(channel);
      bind(FertilizationEventFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry,
          ImmutableList.of(GET_BY_ID_DATA_LOADER_NAME, GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME,
              GET_BY_GARDENER_ID_DATA_LOADER_NAME));

      // by id
      GrpcNotFoundSwallower<String, FertilizationEventGetResponse> idToFertilizationEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.get(FertilizationEventGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, FertilizationEvent> byIdLoader = ids -> {
        List<ListenableFuture<FertilizationEvent>> futures = ids.stream()
            .map(id -> Futures.transform(idToFertilizationEvent.apply(id),
                response -> response != null ? response.getFertilizationEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<FertilizationEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));

      // by garden plant id
      GrpcNotFoundSwallower<String, FertilizationEventQueryResponse> gardenPlantIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(FertilizationEventQueryRequest.newBuilder()
              .setTemplate(FertilizationEvent.newBuilder().setGardenPlantId(id).build())
              .build()));
      BatchLoader<String, List<FertilizationEvent>> byGardenPlantIdLoader = ids -> {
        List<ListenableFuture<List<FertilizationEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenPlantIdToEvent.apply(id),
                response -> response != null ? response.getFertilizationEventList() : null,
                MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<FertilizationEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenPlantIdLoader));

      // by gardener id
      GrpcNotFoundSwallower<String, FertilizationEventQueryResponse> gardenerIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(FertilizationEventQueryRequest.newBuilder()
              .setTemplate(FertilizationEvent.newBuilder()
                  .setMetadata(EventMetadata.newBuilder().setGardenerId(id).build())
                  .build())
              .build()));
      BatchLoader<String, List<FertilizationEvent>> byGardenerIdLoader = ids -> {
        List<ListenableFuture<List<FertilizationEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenerIdToEvent.apply(id),
                response -> response != null ? response.getFertilizationEventList() : null,
                MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<FertilizationEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDENER_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenerIdLoader));
    }

    static CompletableFuture<FertilizationEvent> getFertilizationEvent(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "FertilizationEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, FertilizationEvent>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }

    static CompletableFuture<List<FertilizationEvent>> getFertilizationEventsByGardenPlantId(
        DataFetchingEnvironment environment, String gardenPlantId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenPlantId, "FertilizationEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<FertilizationEvent>>getDataLoader(
          GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME).load(gardenPlantId);
    }

    static CompletableFuture<List<FertilizationEvent>> getFertilizationEventsByGardenerId(
        DataFetchingEnvironment environment, String gardenerId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenerId, "FertilizationEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<FertilizationEvent>>getDataLoader(
          GET_BY_GARDENER_ID_DATA_LOADER_NAME).load(gardenerId);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:fertilizationevent:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getFertilizationEvent")
    ListenableFuture<FertilizationEvent> getFertilizationEvent(FertilizationEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          ClientModule.getFertilizationEvent(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createFertilizationEvent")
    ListenableFuture<FertilizationEvent> createFertilizationEvent(FertilizationEventCreateRequest request,
        FertilizationEventFutureStub client) {
      if (request.getFertilizationEvent().getId() == null || request.getFertilizationEvent().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = FertilizationEventCreateRequest.newBuilder(request)
            .setFertilizationEvent(FertilizationEvent.newBuilder(request.getFertilizationEvent()).setId(id))
            .build();
      }
      FertilizationEventCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getFertilizationEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateFertilizationEvent")
    ListenableFuture<FertilizationEvent> updateFertilizationEvent(FertilizationEventUpdateRequest request,
        FertilizationEventFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getFertilizationEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteFertilizationEvent")
    ListenableFuture<FertilizationEventDeleteResponse> deleteFertilizationEvent(FertilizationEventDeleteRequest request,
        FertilizationEventFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "gardenPlant", onType = FertilizationEvent.class)
    ListenableFuture<GardenPlant> eventToGardenPlant(FertilizationEvent event, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenPlantModuleProvider.getGardenPlant(environment, event.getGardenPlantId()));
    }
  }
}
