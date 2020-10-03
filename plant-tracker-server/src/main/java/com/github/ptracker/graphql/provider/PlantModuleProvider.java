package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.PlantCreateRequest;
import com.github.ptracker.service.PlantDeleteRequest;
import com.github.ptracker.service.PlantDeleteResponse;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGrpc;
import com.github.ptracker.service.PlantGrpc.PlantBlockingStub;
import com.github.ptracker.service.PlantGrpc.PlantFutureStub;
import com.github.ptracker.service.PlantUpdateRequest;
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


public class PlantModuleProvider implements GraphQLModuleProvider {
  private static final String PLANT_BATCH_GET_DATA_LOADER_NAME = "plants";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new PlantSchemaModule();

  public PlantModuleProvider(String plantServerHost, int plantServerPort) {
    _clientModule = new ClientModule(plantServerHost, plantServerPort);
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
      BatchLoader<String, Plant> plantBatchLoader = ids -> {
        List<ListenableFuture<Plant>> plantResponseFutures = ids.stream()
            .map(id -> Futures.transform(_futureStub.get(PlantGetRequest.newBuilder().setId(ids.get(0)).build()),
                plantGetResponse -> plantGetResponse != null ? plantGetResponse.getPlant() : null,
                MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Plant>> listenableFuture = Futures.allAsList(plantResponseFutures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(PLANT_BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(plantBatchLoader));
    }
  }

  private static class PlantSchemaModule extends SchemaModule {

    @Query("getPlant")
    ListenableFuture<Plant> getPlant(PlantGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, Plant>getDataLoader(
              PLANT_BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createPlant")
    ListenableFuture<Plant> createPlant(PlantCreateRequest request, PlantFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getPlant(), MoreExecutors.directExecutor());
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
  }
}
