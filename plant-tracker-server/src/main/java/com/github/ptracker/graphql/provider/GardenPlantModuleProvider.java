package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.GardenPlantCreateRequest;
import com.github.ptracker.service.GardenPlantDeleteRequest;
import com.github.ptracker.service.GardenPlantDeleteResponse;
import com.github.ptracker.service.GardenPlantGetRequest;
import com.github.ptracker.service.GardenPlantGetResponse;
import com.github.ptracker.service.GardenPlantGrpc;
import com.github.ptracker.service.GardenPlantGrpc.GardenPlantBlockingStub;
import com.github.ptracker.service.GardenPlantGrpc.GardenPlantFutureStub;
import com.github.ptracker.service.GardenPlantQueryRequest;
import com.github.ptracker.service.GardenPlantQueryResponse;
import com.github.ptracker.service.GardenPlantUpdateRequest;
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


public class GardenPlantModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public GardenPlantModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<GardenPlant> getGardenPlant(DataFetchingEnvironment environment, String id) {
    return ClientModule.getGardenPlant(environment, id);
  }

  public static CompletableFuture<List<GardenPlant>> getGardenPlantsByGardenId(DataFetchingEnvironment environment,
      String gardenId) {
    return ClientModule.getGardenPlantsByGardenId(environment, gardenId);
  }

  public static CompletableFuture<List<GardenPlant>> getGardenPlantsByPlantId(DataFetchingEnvironment environment,
      String plantId) {
    return ClientModule.getGardenPlantsByPlantId(environment, plantId);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "gardenPlants";
    private static final String GET_BY_GARDEN_ID_DATA_LOADER_NAME = "gardenPlantsByGardenId";
    private static final String GET_BY_PLANT_ID_DATA_LOADER_NAME = "gardenPlantsByPlantId";

    private final String _host;
    private final int _port;

    private GardenPlantFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(GardenPlantBlockingStub.class).toInstance(GardenPlantGrpc.newBlockingStub(channel));
      _futureStub = GardenPlantGrpc.newFutureStub(channel);
      bind(GardenPlantFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry,
          ImmutableList.of(GET_BY_ID_DATA_LOADER_NAME, GET_BY_GARDEN_ID_DATA_LOADER_NAME,
              GET_BY_PLANT_ID_DATA_LOADER_NAME));

      // by id
      GrpcNotFoundSwallower<String, GardenPlantGetResponse> idToGardenPlant =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(GardenPlantGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, GardenPlant> byIdLoader = ids -> {
        List<ListenableFuture<GardenPlant>> futures = ids.stream()
            .map(id -> Futures.transform(idToGardenPlant.apply(id),
                response -> response != null ? response.getGardenPlant() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<GardenPlant>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));

      // by garden id
      GrpcNotFoundSwallower<String, GardenPlantQueryResponse> gardenIdToGardenPlant = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(GardenPlantQueryRequest.newBuilder()
              .setTemplate(GardenPlant.newBuilder().setGardenId(id).build())
              .build()));
      BatchLoader<String, List<GardenPlant>> byGardenIdLoader = ids -> {
        List<ListenableFuture<List<GardenPlant>>> futures = ids.stream()
            .map(id -> Futures.transform(gardenIdToGardenPlant.apply(id),
                response -> response != null ? response.getGardenPlantList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<GardenPlant>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_GARDEN_ID_DATA_LOADER_NAME, new DataLoader<>(byGardenIdLoader));

      // by plant id
      GrpcNotFoundSwallower<String, GardenPlantQueryResponse> plantIdToGardenPlant = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(GardenPlantQueryRequest.newBuilder()
              .setTemplate(GardenPlant.newBuilder().setPlantId(id).build())
              .build()));
      BatchLoader<String, List<GardenPlant>> byPlantIdLoader = ids -> {
        List<ListenableFuture<List<GardenPlant>>> futures = ids.stream()
            .map(id -> Futures.transform(plantIdToGardenPlant.apply(id),
                response -> response != null ? response.getGardenPlantList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<GardenPlant>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_PLANT_ID_DATA_LOADER_NAME, new DataLoader<>(byPlantIdLoader));
    }

    static CompletableFuture<GardenPlant> getGardenPlant(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "Garden Plant ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, GardenPlant>getDataLoader(GET_BY_ID_DATA_LOADER_NAME)
          .load(id);
    }

    static CompletableFuture<List<GardenPlant>> getGardenPlantsByGardenId(DataFetchingEnvironment environment,
        String gardenId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(gardenId, "Garden ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<GardenPlant>>getDataLoader(
          GET_BY_GARDEN_ID_DATA_LOADER_NAME).load(gardenId);
    }

    static CompletableFuture<List<GardenPlant>> getGardenPlantsByPlantId(DataFetchingEnvironment environment,
        String plantId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(plantId, "Plant ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<GardenPlant>>getDataLoader(
          GET_BY_PLANT_ID_DATA_LOADER_NAME).load(plantId);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:gardenplant:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getGardenPlant")
    ListenableFuture<GardenPlant> getGardenPlant(GardenPlantGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getGardenPlant(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createGardenPlant")
    ListenableFuture<GardenPlant> createGardenPlant(GardenPlantCreateRequest request, GardenPlantFutureStub client) {
      if (request.getGardenPlant().getId() == null || request.getGardenPlant().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = GardenPlantCreateRequest.newBuilder(request)
            .setGardenPlant(GardenPlant.newBuilder(request.getGardenPlant()).setId(id))
            .build();
      }
      GardenPlantCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getGardenPlant(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateGardenPlant")
    ListenableFuture<GardenPlant> updateGardenPlant(GardenPlantUpdateRequest request, GardenPlantFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getGardenPlant(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteGardenPlant")
    ListenableFuture<GardenPlantDeleteResponse> deleteGardenPlant(GardenPlantDeleteRequest request,
        GardenPlantFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "garden", onType = GardenPlant.class)
    ListenableFuture<Garden> gardenPlantToGarden(GardenPlant gardenPlant, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(GardenModuleProvider.getGarden(environment, gardenPlant.getGardenId()));
    }

    @SchemaModification(addField = "plant", onType = GardenPlant.class)
    ListenableFuture<Plant> gardenPlantToPlant(GardenPlant gardenPlant, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(PlantModuleProvider.getPlant(environment, gardenPlant.getPlantId()));
    }

    @SchemaModification(addField = "fertilizationEvents", onType = GardenPlant.class)
    ListenableFuture<List<FertilizationEvent>> gardenPlantToFertilizationEvents(GardenPlant gardenPlant,
        DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          FertilizationEventModuleProvider.getFertilizationEventsByGardenPlantId(environment, gardenPlant.getId()));
    }

    @SchemaModification(addField = "wateringEvents", onType = GardenPlant.class)
    ListenableFuture<List<WateringEvent>> gardenPlantToWateringEvents(GardenPlant gardenPlant,
        DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          WateringEventModuleProvider.getWateringEventsByGardenPlantId(environment, gardenPlant.getId()));
    }

    @SchemaModification(addField = "noteEvents", onType = GardenPlant.class)
    ListenableFuture<List<NoteEvent>> gardenPlantToNoteEvents(GardenPlant gardenPlant,
        DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          NoteEventModuleProvider.getNoteEventsByGardenPlantId(environment, gardenPlant.getId()));
    }
  }
}
