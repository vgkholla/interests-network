package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Account;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.AccountCreateRequest;
import com.github.ptracker.service.AccountDeleteRequest;
import com.github.ptracker.service.AccountDeleteResponse;
import com.github.ptracker.service.AccountGetRequest;
import com.github.ptracker.service.AccountGrpc;
import com.github.ptracker.service.AccountGrpc.AccountBlockingStub;
import com.github.ptracker.service.AccountGrpc.AccountFutureStub;
import com.github.ptracker.service.AccountUpdateRequest;
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


public class AccountModuleProvider implements GraphQLModuleProvider {
  public static final String BATCH_GET_DATA_LOADER_NAME = "accounts";
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public AccountModuleProvider(String serverHost, int serverPort) {
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

    private AccountFutureStub _futureStub;

    public ClientModule(String host, int port) {
      _host = checkNotNull(host, "Host cannot be null");
      checkArgument(port > 0, "Port should be > 0");
      _port = port;
    }

    @Override
    protected void configure() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
      bind(AccountBlockingStub.class).toInstance(AccountGrpc.newBlockingStub(channel));
      _futureStub = AccountGrpc.newFutureStub(channel);
      bind(AccountFutureStub.class).toInstance(_futureStub);
    }

    void registerDataLoaders(DataLoaderRegistry registry) {
      BatchLoader<String, Account> batchLoader = ids -> {
        List<ListenableFuture<Account>> futures = ids.stream()
            .map(id -> Futures.transform(_futureStub.get(AccountGetRequest.newBuilder().setId(ids.get(0)).build()),
                response -> response != null ? response.getAccount() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Account>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(BATCH_GET_DATA_LOADER_NAME, new DataLoader<>(batchLoader));
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {

    @Query("getAccount")
    ListenableFuture<Account> getAccount(AccountGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(
          dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, Account>getDataLoader(
              BATCH_GET_DATA_LOADER_NAME).load(request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createAccount")
    ListenableFuture<Account> createAccount(AccountCreateRequest request, AccountFutureStub client) {
      return Futures.transform(client.create(request), ignored -> request.getAccount(), MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("updateAccount")
    ListenableFuture<Account> updateAccount(AccountUpdateRequest request, AccountFutureStub client) {
      return Futures.transform(client.update(request), ignored -> request.getAccount(), MoreExecutors.directExecutor());
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("deleteAccount")
    ListenableFuture<AccountDeleteResponse> deleteAccount(AccountDeleteRequest request, AccountFutureStub client) {
      return client.delete(request);
    }
  }
}
