package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Space;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.SpaceCreateRequest;
import com.github.ptracker.service.SpaceDeleteRequest;
import com.github.ptracker.service.SpaceDeleteResponse;
import com.github.ptracker.service.SpaceGetRequest;
import com.github.ptracker.service.SpaceGetResponse;
import com.github.ptracker.service.SpaceGrpc;
import com.github.ptracker.service.SpaceGrpc.SpaceBlockingStub;
import com.github.ptracker.service.SpaceGrpc.SpaceFutureStub;
import com.github.ptracker.service.SpaceUpdateRequest;
import com.github.ptracker.util.IdGenerator;
import com.github.ptracker.util.RandomLongIdGenerator;
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


public class SpaceModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public SpaceModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<Space> getSpace(DataFetchingEnvironment environment, String id) {
    return ClientModule.getSpace(environment, id);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "spaces";

    private final String _host;
    private final int _port;

    private SpaceFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(SpaceBlockingStub.class).toInstance(SpaceGrpc.newBlockingStub(channel));
      _futureStub = SpaceGrpc.newFutureStub(channel);
      bind(SpaceFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      verifyDataLoaderRegistryKeysUnassigned(registry, Collections.singletonList(GET_BY_ID_DATA_LOADER_NAME));
      GrpcNotFoundSwallower<String, SpaceGetResponse> idToSpace =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(SpaceGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, Space> byIdLoader = ids -> {
        List<ListenableFuture<Space>> futures = ids.stream()
            .map(id -> Futures.transform(idToSpace.apply(id),
                response -> response != null ? response.getSpace() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Space>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));
    }

    static CompletableFuture<Space> getSpace(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "Space ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, Space>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:space:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getSpace")
    ListenableFuture<Space> getSpace(SpaceGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getSpace(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createSpace")
    ListenableFuture<Space> createSpace(SpaceCreateRequest request, SpaceFutureStub client) {
      if (request.getSpace().getId() == null || request.getSpace().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = SpaceCreateRequest.newBuilder(request)
            .setSpace(Space.newBuilder(request.getSpace()).setId(id))
            .build();
      }
      SpaceCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getSpace(),
          MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateSpace")
    ListenableFuture<Space> updateSpace(SpaceUpdateRequest request, SpaceFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getSpace(), MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteSpace")
    ListenableFuture<SpaceDeleteResponse> deleteSpace(SpaceDeleteRequest request, SpaceFutureStub client) {
      return client.delete(request);
    }

    @SchemaModification(addField = "gardens", onType = Space.class)
    ListenableFuture<List<Garden>> spaceToGardens(Space space, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenModuleProvider.getGardensBySpaceId(environment, space.getId()));
    }
  }
}
