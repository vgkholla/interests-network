package com.github.ptracker.graphql.provider;

import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.OtherEvent;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.OtherEventCreateRequest;
import com.github.ptracker.service.OtherEventDeleteRequest;
import com.github.ptracker.service.OtherEventDeleteResponse;
import com.github.ptracker.service.OtherEventGetRequest;
import com.github.ptracker.service.OtherEventGetResponse;
import com.github.ptracker.service.OtherEventGrpc;
import com.github.ptracker.service.OtherEventGrpc.OtherEventBlockingStub;
import com.github.ptracker.service.OtherEventGrpc.OtherEventFutureStub;
import com.github.ptracker.service.OtherEventQueryRequest;
import com.github.ptracker.service.OtherEventQueryResponse;
import com.github.ptracker.service.OtherEventUpdateRequest;
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


public class OtherEventModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public OtherEventModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<List<OtherEvent>> getOtherEventByGardenPlantId(DataFetchingEnvironment environment,
      String gardenPlantId) {
    return OtherEventModuleProvider.ClientModule.getOtherEventByGardenPlantId(environment, gardenPlantId);
  }

  public static CompletableFuture<List<OtherEvent>> getOtherEventByGardenerId(DataFetchingEnvironment environment,
      String gardenerId) {
    checkNotNull(environment, "DataFetchingEnvironment cannot be null");
    checkNotNull(gardenerId, "OtherEvent ID cannot be null");
    return OtherEventModuleProvider.ClientModule.getOtherEventByGardenPlantId(environment, gardenerId);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "otherEvents";
    private static final String GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME = "otherEventsByGardenPlantId";
    private static final String GET_BY_GARDENER_ID_DATA_LOADER_NAME = "otherEventsByGardenerId";

    private final String _host;
    private final int _port;

    private OtherEventFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(OtherEventBlockingStub.class).toInstance(OtherEventGrpc.newBlockingStub(channel));
      _futureStub = OtherEventGrpc.newFutureStub(channel);
      bind(OtherEventFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry,
          ImmutableList.of(GET_BY_ID_DATA_LOADER_NAME, GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME,
              GET_BY_GARDENER_ID_DATA_LOADER_NAME));

      // by id
      GrpcNotFoundSwallower<String, OtherEventGetResponse> idToOtherEvent =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(OtherEventGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, OtherEvent> byIdLoader = ids -> {
        List<ListenableFuture<OtherEvent>> futures = ids.stream()
            .map(id -> Futures.transform(idToOtherEvent.apply(id),
                response -> response != null ? response.getOtherEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<OtherEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));

      // by garden plant id
      GrpcNotFoundSwallower<String, OtherEventQueryResponse> gardenPlantIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(OtherEventQueryRequest.newBuilder()
              .setTemplate(OtherEvent.newBuilder().setGardenPlantId(id).build())
              .build()));
      BatchLoader<String, List<OtherEvent>> byGardenPlantIdLoader = ids -> {
        List<ListenableFuture<List<OtherEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenPlantIdToEvent.apply(id),
                response -> response != null ? response.getOtherEventList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<OtherEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenPlantIdLoader));

      // by gardener id
      GrpcNotFoundSwallower<String, OtherEventQueryResponse> gardenerIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(OtherEventQueryRequest.newBuilder()
              .setTemplate(
                  OtherEvent.newBuilder().setMetadata(EventMetadata.newBuilder().setGardenerId(id).build()).build())
              .build()));
      BatchLoader<String, List<OtherEvent>> byGardenerIdLoader = ids -> {
        List<ListenableFuture<List<OtherEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenerIdToEvent.apply(id),
                response -> response != null ? response.getOtherEventList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<OtherEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDENER_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenerIdLoader));
    }

    static CompletableFuture<OtherEvent> getOtherEvent(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "OtherEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, OtherEvent>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }

    static CompletableFuture<List<OtherEvent>> getOtherEventByGardenPlantId(DataFetchingEnvironment environment,
        String gardenPlantId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenPlantId, "OtherEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<OtherEvent>>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(gardenPlantId);
    }

    static CompletableFuture<List<OtherEvent>> getOtherEventByGardenerId(DataFetchingEnvironment environment,
        String gardenerId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenerId, "OtherEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<OtherEvent>>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(gardenerId);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:account:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getOtherEvent")
    ListenableFuture<OtherEvent> getOtherEvent(OtherEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getOtherEvent(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createOtherEvent")
    ListenableFuture<OtherEvent> createOtherEvent(OtherEventCreateRequest request, OtherEventFutureStub client) {
      if (request.getOtherEvent().getId() == null || request.getOtherEvent().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = OtherEventCreateRequest.newBuilder(request)
            .setOtherEvent(OtherEvent.newBuilder(request.getOtherEvent()).setId(id))
            .build();
      }
      OtherEventCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getOtherEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateOtherEvent")
    ListenableFuture<OtherEvent> updateOtherEvent(OtherEventUpdateRequest request, OtherEventFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getOtherEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteOtherEvent")
    ListenableFuture<OtherEventDeleteResponse> deleteOtherEvent(OtherEventDeleteRequest request,
        OtherEventFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "gardenPlant", onType = OtherEvent.class)
    ListenableFuture<GardenPlant> eventToGardenPlant(OtherEvent event, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenPlantModuleProvider.getGardenPlant(environment, event.getGardenPlantId()));
    }
  }
}
