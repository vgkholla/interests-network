package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.OtherEvent;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.OtherEventCreateRequest;
import com.github.ptracker.service.OtherEventDeleteRequest;
import com.github.ptracker.service.OtherEventDeleteResponse;
import com.github.ptracker.service.OtherEventGetRequest;
import com.github.ptracker.service.OtherEventGrpc;
import com.github.ptracker.service.OtherEventGrpc.OtherEventBlockingStub;
import com.github.ptracker.service.OtherEventGrpc.OtherEventFutureStub;
import com.github.ptracker.service.OtherEventUpdateRequest;
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


public class OtherEventModuleProvider implements GraphQLModuleProvider {
  private static final String BATCH_GET_DATA_LOADER_NAME = "otherEvents";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public OtherEventModuleProvider(String serverHost, int serverPort) {
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
      BatchLoader<String, OtherEvent> batchLoader = ids -> {
        List<ListenableFuture<OtherEvent>> futures = ids.stream()
            .map(id -> Futures.transform(_futureStub.get(OtherEventGetRequest.newBuilder().setId(ids.get(0)).build()),
                response -> response != null ? response.getOtherEvent() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<OtherEvent>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getOtherEvent")
    ListenableFuture<OtherEvent> getOtherEvent(OtherEventGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, OtherEvent>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createOtherEvent")
    ListenableFuture<OtherEvent> createOtherEvent(OtherEventCreateRequest request, OtherEventFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getOtherEvent(),
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
  }
}
