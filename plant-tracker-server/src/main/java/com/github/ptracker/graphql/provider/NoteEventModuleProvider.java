package com.github.ptracker.graphql.provider;

import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.NoteEventCreateRequest;
import com.github.ptracker.service.NoteEventDeleteRequest;
import com.github.ptracker.service.NoteEventDeleteResponse;
import com.github.ptracker.service.NoteEventGetRequest;
import com.github.ptracker.service.NoteEventGetResponse;
import com.github.ptracker.service.NoteEventGrpc;
import com.github.ptracker.service.NoteEventGrpc.NoteEventBlockingStub;
import com.github.ptracker.service.NoteEventGrpc.NoteEventFutureStub;
import com.github.ptracker.service.NoteEventQueryRequest;
import com.github.ptracker.service.NoteEventQueryResponse;
import com.github.ptracker.service.NoteEventUpdateRequest;
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


public class NoteEventModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public NoteEventModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<List<NoteEvent>> getNoteEventsByGardenPlantId(DataFetchingEnvironment environment,
      String gardenPlantId) {
    return NoteEventModuleProvider.ClientModule.getNoteEventsByGardenPlantId(environment, gardenPlantId);
  }

  public static CompletableFuture<List<NoteEvent>> getNoteEventsByGardenerId(DataFetchingEnvironment environment,
      String gardenerId) {
    checkNotNull(environment, "DataFetchingEnvironment cannot be null");
    checkNotNull(gardenerId, "NoteEvent ID cannot be null");
    return NoteEventModuleProvider.ClientModule.getNoteEventsByGardenerId(environment, gardenerId);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "noteEvents";
    private static final String GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME = "noteEventsByGardenPlantId";
    private static final String GET_BY_GARDENER_ID_DATA_LOADER_NAME = "noteEventsByGardenerId";

    private final String _host;
    private final int _port;

    private NoteEventFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(NoteEventBlockingStub.class).toInstance(NoteEventGrpc.newBlockingStub(channel));
      _futureStub = NoteEventGrpc.newFutureStub(channel);
      bind(NoteEventFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry,
          ImmutableList.of(GET_BY_ID_DATA_LOADER_NAME, GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME,
              GET_BY_GARDENER_ID_DATA_LOADER_NAME));

      // by id
      GrpcNotFoundSwallower<String, NoteEventGetResponse> idToNoteEvent =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(NoteEventGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, NoteEvent> byIdLoader = ids -> {
        List<ListenableFuture<NoteEvent>> futures = ids.stream()
            .map(id -> Futures.transform(idToNoteEvent.apply(id),
                response -> response != null ? response.getNoteEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<NoteEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));

      // by garden plant id
      GrpcNotFoundSwallower<String, NoteEventQueryResponse> gardenPlantIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(NoteEventQueryRequest.newBuilder()
              .setTemplate(NoteEvent.newBuilder().setGardenPlantId(id).build())
              .build()));
      BatchLoader<String, List<NoteEvent>> byGardenPlantIdLoader = ids -> {
        List<ListenableFuture<List<NoteEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenPlantIdToEvent.apply(id),
                response -> response != null ? response.getNoteEventList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<NoteEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenPlantIdLoader));

      // by gardener id
      GrpcNotFoundSwallower<String, NoteEventQueryResponse> gardenerIdToEvent = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(NoteEventQueryRequest.newBuilder()
              .setTemplate(
                  NoteEvent.newBuilder().setMetadata(EventMetadata.newBuilder().setGardenerId(id).build()).build())
              .build()));
      BatchLoader<String, List<NoteEvent>> byGardenerIdLoader = ids -> {
        List<ListenableFuture<List<NoteEvent>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenerIdToEvent.apply(id),
                response -> response != null ? response.getNoteEventList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<NoteEvent>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDENER_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenerIdLoader));
    }

    static CompletableFuture<NoteEvent> getNoteEvent(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "NoteEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, NoteEvent>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }

    static CompletableFuture<List<NoteEvent>> getNoteEventsByGardenPlantId(DataFetchingEnvironment environment,
        String gardenPlantId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenPlantId, "NoteEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<NoteEvent>>getDataLoader(
          GET_BY_GARDEN_PLANT_ID_DATA_LOADER_NAME).load(gardenPlantId);
    }

    static CompletableFuture<List<NoteEvent>> getNoteEventsByGardenerId(DataFetchingEnvironment environment,
        String gardenerId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenerId, "NoteEvent ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<NoteEvent>>getDataLoader(
          GET_BY_GARDENER_ID_DATA_LOADER_NAME).load(gardenerId);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:noteevent:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getNoteEvent")
    ListenableFuture<NoteEvent> getNoteEvent(NoteEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getNoteEvent(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createNoteEvent")
    ListenableFuture<NoteEvent> createNoteEvent(NoteEventCreateRequest request, NoteEventFutureStub client) {
      if (request.getNoteEvent().getId() == null || request.getNoteEvent().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = NoteEventCreateRequest.newBuilder(request)
            .setNoteEvent(NoteEvent.newBuilder(request.getNoteEvent()).setId(id))
            .build();
      }
      NoteEventCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getNoteEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateNoteEvent")
    ListenableFuture<NoteEvent> updateNoteEvent(NoteEventUpdateRequest request, NoteEventFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getNoteEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteNoteEvent")
    ListenableFuture<NoteEventDeleteResponse> deleteNoteEvent(NoteEventDeleteRequest request,
        NoteEventFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "gardenPlant", onType = NoteEvent.class)
    ListenableFuture<GardenPlant> eventToGardenPlant(NoteEvent event, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenPlantModuleProvider.getGardenPlant(environment, event.getGardenPlantId()));
    }
  }
}
