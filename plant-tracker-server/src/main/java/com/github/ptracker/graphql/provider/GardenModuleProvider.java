package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Account;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.GardenCreateRequest;
import com.github.ptracker.service.GardenDeleteRequest;
import com.github.ptracker.service.GardenDeleteResponse;
import com.github.ptracker.service.GardenGetRequest;
import com.github.ptracker.service.GardenGrpc;
import com.github.ptracker.service.GardenGrpc.GardenBlockingStub;
import com.github.ptracker.service.GardenGrpc.GardenFutureStub;
import com.github.ptracker.service.GardenUpdateRequest;
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
import java.util.List;
import java.util.stream.Collectors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import static com.google.common.base.Preconditions.*;


public class GardenModuleProvider implements GraphQLModuleProvider {
  private static final String BATCH_GET_DATA_LOADER_NAME = "gardens";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public GardenModuleProvider(String serverHost, int serverPort) {
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

    private GardenFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(GardenBlockingStub.class).toInstance(GardenGrpc.newBlockingStub(channel));
      _futureStub = GardenGrpc.newFutureStub(channel);
      bind(GardenFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      BatchLoader<String, Garden> batchLoader = ids -> {
        List<ListenableFuture<Garden>> futures = ids.stream()
            .map(id -> Futures.transform(_futureStub.get(GardenGetRequest.newBuilder().setId(ids.get(0)).build()),
                response -> response != null ? response.getGarden() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Garden>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getGarden")
    ListenableFuture<Garden> getGarden(GardenGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, Garden>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createGarden")
    ListenableFuture<Garden> createGarden(GardenCreateRequest request, GardenFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getGarden(), MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateGarden")
    ListenableFuture<Garden> updateGarden(GardenUpdateRequest request, GardenFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getGarden(), MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteGarden")
    ListenableFuture<GardenDeleteResponse> deleteGarden(GardenDeleteRequest request, GardenFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "account", onType = Garden.class)
    ListenableFuture<Account> gardenToAccount(Garden garden, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          environment
              .<DataLoaderRegistry>getContext()
              .<String, Account>getDataLoader(AccountModuleProvider.BATCH_GET_DATA_LOADER_NAME)
              .load(garden.getAccountId()));
    }
  }
}
