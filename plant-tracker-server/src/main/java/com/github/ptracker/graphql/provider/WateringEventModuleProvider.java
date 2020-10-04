package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.WateringEventCreateRequest;
import com.github.ptracker.service.WateringEventDeleteRequest;
import com.github.ptracker.service.WateringEventDeleteResponse;
import com.github.ptracker.service.WateringEventGetRequest;
import com.github.ptracker.service.WateringEventGrpc;
import com.github.ptracker.service.WateringEventGrpc.WateringEventBlockingStub;
import com.github.ptracker.service.WateringEventGrpc.WateringEventFutureStub;
import com.github.ptracker.service.WateringEventUpdateRequest;
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


public class WateringEventModuleProvider implements GraphQLModuleProvider {
  private static final String BATCH_GET_DATA_LOADER_NAME = "wateringEvents";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public WateringEventModuleProvider(String serverHost, int serverPort) {
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

    private WateringEventFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(WateringEventBlockingStub.class).toInstance(WateringEventGrpc.newBlockingStub(channel));
      _futureStub = WateringEventGrpc.newFutureStub(channel);
      bind(WateringEventFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      BatchLoader<String, WateringEvent> batchLoader = ids -> {
        List<ListenableFuture<WateringEvent>> futures = ids.stream()
            .map(
                id -> Futures.transform(_futureStub.get(WateringEventGetRequest.newBuilder().setId(ids.get(0)).build()),
                    response -> response != null ? response.getWateringEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<WateringEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getWateringEvent")
    ListenableFuture<WateringEvent> getWateringEvent(WateringEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, WateringEvent>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createWateringEvent")
    ListenableFuture<WateringEvent> createWateringEvent(WateringEventCreateRequest request,
        WateringEventFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getWateringEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateWateringEvent")
    ListenableFuture<WateringEvent> updateWateringEvent(WateringEventUpdateRequest request,
        WateringEventFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getWateringEvent(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteWateringEvent")
    ListenableFuture<WateringEventDeleteResponse> deleteWateringEvent(WateringEventDeleteRequest request,
        WateringEventFutureStub client) {
      return client.delete(request);
    }
  }
}
