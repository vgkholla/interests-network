package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.FertilizationEventCreateRequest;
import com.github.ptracker.service.FertilizationEventDeleteRequest;
import com.github.ptracker.service.FertilizationEventDeleteResponse;
import com.github.ptracker.service.FertilizationEventGetRequest;
import com.github.ptracker.service.FertilizationEventGrpc;
import com.github.ptracker.service.FertilizationEventGrpc.FertilizationEventBlockingStub;
import com.github.ptracker.service.FertilizationEventGrpc.FertilizationEventFutureStub;
import com.github.ptracker.service.FertilizationEventUpdateRequest;
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


public class FertilizationEventModuleProvider implements GraphQLModuleProvider {
  private static final String BATCH_GET_DATA_LOADER_NAME = "fertilizationEvents";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public FertilizationEventModuleProvider(String serverHost, int serverPort) {
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
      BatchLoader<String, FertilizationEvent> batchLoader = ids -> {
        List<ListenableFuture<FertilizationEvent>> futures = ids.stream()
            .map(id -> Futures.transform(
                _futureStub.get(FertilizationEventGetRequest.newBuilder().setId(ids.get(0)).build()),
                response -> response != null ? response.getFertilizationEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<FertilizationEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getFertilizationEvent")
    ListenableFuture<FertilizationEvent> getFertilizationEvent(FertilizationEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, FertilizationEvent>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createFertilizationEvent")
    ListenableFuture<FertilizationEvent> createFertilizationEvent(FertilizationEventCreateRequest request,
        FertilizationEventFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getFertilizationEvent(),
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
  }
}
