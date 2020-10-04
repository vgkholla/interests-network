package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.PlantCreateRequest;
import com.github.ptracker.service.PlantDeleteRequest;
import com.github.ptracker.service.PlantDeleteResponse;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGetResponse;
import com.github.ptracker.service.PlantGrpc;
import com.github.ptracker.service.PlantGrpc.PlantBlockingStub;
import com.github.ptracker.service.PlantGrpc.PlantFutureStub;
import com.github.ptracker.service.PlantUpdateRequest;
import com.github.ptracker.util.IdGenerator;
import com.github.ptracker.util.RandomStringIdGenerator;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Collections;
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


public class PlantModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public PlantModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<Plant> getPlant(DataFetchingEnvironment environment, String id) {
    return ClientModule.getPlant(environment, id);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "plants";

    private final String _host;
    private final int _port;

    private PlantFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(PlantBlockingStub.class).toInstance(PlantGrpc.newBlockingStub(channel));
      _futureStub = PlantGrpc.newFutureStub(channel);
      bind(PlantFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry, Collections.singletonList(GET_BY_ID_DATA_LOADER_NAME));
      GrpcNotFoundSwallower<String, PlantGetResponse> idToPlant =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(PlantGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, Plant> byIdLoader = ids -> {
        List<ListenableFuture<Plant>> futures = ids.stream()
            .map(id -> Futures.transform(idToPlant.apply(id), response -> response != null ? response.getPlant() : null,
                MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Plant>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));
    }

    static CompletableFuture<Plant> getPlant(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "Plant ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, Plant>getDataLoader(GET_BY_ID_DATA_LOADER_NAME).load(
          id);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:account:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getPlant")
    ListenableFuture<Plant> getPlant(PlantGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getPlant(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createPlant")
    ListenableFuture<Plant> createPlant(PlantCreateRequest request, PlantFutureStub client) {
      if (request.getPlant().getId() == null || request.getPlant().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request =
            PlantCreateRequest.newBuilder(request).setPlant(Plant.newBuilder(request.getPlant()).setId(id)).build();
      }
      PlantCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getPlant(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updatePlant")
    ListenableFuture<Plant> updatePlant(PlantUpdateRequest request, PlantFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getPlant(), MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deletePlant")
    ListenableFuture<PlantDeleteResponse> deletePlant(PlantDeleteRequest request, PlantFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "gardenPlants", onType = Plant.class)
    ListenableFuture<List<GardenPlant>> plantToGardenPlants(Plant plant, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenPlantModuleProvider.getGardenPlantsByPlantId(environment, plant.getId()));
    }
  }
}
