package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Gardener;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.GardenerCreateRequest;
import com.github.ptracker.service.GardenerDeleteRequest;
import com.github.ptracker.service.GardenerDeleteResponse;
import com.github.ptracker.service.GardenerGetRequest;
import com.github.ptracker.service.GardenerGrpc;
import com.github.ptracker.service.GardenerGrpc.GardenerBlockingStub;
import com.github.ptracker.service.GardenerGrpc.GardenerFutureStub;
import com.github.ptracker.service.GardenerUpdateRequest;
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


public class GardenerModuleProvider implements GraphQLModuleProvider {
  private static final String BATCH_GET_DATA_LOADER_NAME = "Gardeners";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public GardenerModuleProvider(String serverHost, int serverPort) {
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

    private GardenerFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(GardenerBlockingStub.class).toInstance(GardenerGrpc.newBlockingStub(channel));
      _futureStub = GardenerGrpc.newFutureStub(channel);
      bind(GardenerFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      BatchLoader<String, Gardener> batchLoader = ids -> {
        List<ListenableFuture<Gardener>> futures = ids.stream()
            .map(id -> Futures.transform(_futureStub.get(GardenerGetRequest.newBuilder().setId(ids.get(0)).build()),
                response -> response != null ? response.getGardener() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Gardener>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getGardener")
    ListenableFuture<Gardener> getGardener(GardenerGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, Gardener>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createGardener")
    ListenableFuture<Gardener> createGardener(GardenerCreateRequest request, GardenerFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getGardener(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateGardener")
    ListenableFuture<Gardener> updateGardener(GardenerUpdateRequest request, GardenerFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getGardener(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteGardener")
    ListenableFuture<GardenerDeleteResponse> deleteGardener(GardenerDeleteRequest request, GardenerFutureStub client) {
      return client.delete(request);
    }
  }
}
