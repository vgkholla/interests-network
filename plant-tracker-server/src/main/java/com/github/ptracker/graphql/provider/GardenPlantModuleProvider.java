package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.GardenPlantCreateRequest;
import com.github.ptracker.service.GardenPlantDeleteRequest;
import com.github.ptracker.service.GardenPlantDeleteResponse;
import com.github.ptracker.service.GardenPlantGetRequest;
import com.github.ptracker.service.GardenPlantGrpc;
import com.github.ptracker.service.GardenPlantGrpc.GardenPlantBlockingStub;
import com.github.ptracker.service.GardenPlantGrpc.GardenPlantFutureStub;
import com.github.ptracker.service.GardenPlantUpdateRequest;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.stream.Collectors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import static com.google.common.base.Preconditions.*;


public class GardenPlantModuleProvider implements GraphQLModuleProvider {
  private static final String BATCH_GET_DATA_LOADER_NAME = "gardenPlants";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public GardenPlantModuleProvider(String serverHost, int serverPort) {
    _clientModule = new ClientModule(serverHost, serverPort);
  }

  @Override
  public Module getClientModule() {
    return _clientModule;
  }

  @Override
  public Module getSchemaModule() {
    return _schemaModule;
  }

  @Override
  public void registerDataLoaders(DataLoaderRegistry registry) {
    _clientModule.registerDataLoaders(registry);
  }

  private static class ClientModule extends AbstractModule {
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
      BatchLoader<String, GardenPlant> batchLoader = ids -> {
        List<ListenableFuture<GardenPlant>> futures = ids.stream()
            .map(id -> Futures.transform(_futureStub.get(GardenPlantGetRequest.newBuilder().setId(ids.get(0)).build()),
                response -> response != null ? response.getGardenPlant() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<GardenPlant>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getGardenPlant")
    ListenableFuture<GardenPlant> getGardenPlant(GardenPlantGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, GardenPlant>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createGardenPlant")
    ListenableFuture<GardenPlant> createGardenPlant(GardenPlantCreateRequest request, GardenPlantFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getGardenPlant(),
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
  }
}
