package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Account;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.GardenCreateRequest;
import com.github.ptracker.service.GardenDeleteRequest;
import com.github.ptracker.service.GardenDeleteResponse;
import com.github.ptracker.service.GardenGetRequest;
import com.github.ptracker.service.GardenGetResponse;
import com.github.ptracker.service.GardenGrpc;
import com.github.ptracker.service.GardenGrpc.GardenBlockingStub;
import com.github.ptracker.service.GardenGrpc.GardenFutureStub;
import com.github.ptracker.service.GardenQueryRequest;
import com.github.ptracker.service.GardenQueryResponse;
import com.github.ptracker.service.GardenUpdateRequest;
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


public class GardenModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public GardenModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<Garden> getGarden(DataFetchingEnvironment environment, String id) {
    return ClientModule.getGarden(environment, id);
  }

  public static CompletableFuture<List<Garden>> getGardensByAccountId(DataFetchingEnvironment environment,
      String accountId) {
    return ClientModule.getGardensByAccountId(environment, accountId);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "gardens";
    private static final String GET_BY_ACCOUNT_ID_DATA_LOADER_NAME = "gardensByAccountId";

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
      verifyDataLoaderRegistryKeysUnassigned(registry,
          ImmutableList.of(GET_BY_ID_DATA_LOADER_NAME, GET_BY_ACCOUNT_ID_DATA_LOADER_NAME));
      // by id
      GrpcNotFoundSwallower<String, GardenGetResponse> idToGarden =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(GardenGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, Garden> byIdLoader = ids -> {
        List<ListenableFuture<Garden>> futures = ids.stream()
            .map(id -> Futures.transform(idToGarden.apply(id),
                response -> response != null ? response.getGarden() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Garden>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));

      // by account id
      GrpcNotFoundSwallower<String, GardenQueryResponse> accountIdToGarden = new GrpcNotFoundSwallower<>(
          id -> _futureStub.query(
              GardenQueryRequest.newBuilder().setTemplate(Garden.newBuilder().setAccountId(id).build()).build()));
      BatchLoader<String, List<Garden>> byAccountIdLoader = ids -> {
        List<ListenableFuture<List<Garden>>> futures = ids.stream()
            .map(id -> Futures.transform(accountIdToGarden.apply(id),
                response -> response != null ? response.getGardenList() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<List<Garden>>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ACCOUNT_ID_DATA_LOADER_NAME, new DataLoader<>(byAccountIdLoader));
    }

    static CompletableFuture<Garden> getGarden(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "Garden ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, Garden>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }

    static CompletableFuture<List<Garden>> getGardensByAccountId(DataFetchingEnvironment environment,
        String accountId) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(accountId, "Account ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, List<Garden>>getDataLoader(
          GET_BY_ACCOUNT_ID_DATA_LOADER_NAME).load(accountId);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getGarden")
    ListenableFuture<Garden> getGarden(GardenGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getGarden(dataFetchingEnvironment, request.getId()));
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
      return FutureConverter.toListenableFuture(AccountModuleProvider.getAccount(environment, garden.getAccountId()));
    }

    @SchemaModification(addField = "gardenPlants", onType = Garden.class)
    ListenableFuture<List<GardenPlant>> gardenToGardenPlants(Garden garden, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenPlantModuleProvider.getGardenPlantsByGardenId(environment, garden.getId()));
    }
  }
}
