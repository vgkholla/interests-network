package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.Account;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.AccountCreateRequest;
import com.github.ptracker.service.AccountDeleteRequest;
import com.github.ptracker.service.AccountDeleteResponse;
import com.github.ptracker.service.AccountGetRequest;
import com.github.ptracker.service.AccountGetResponse;
import com.github.ptracker.service.AccountGrpc;
import com.github.ptracker.service.AccountGrpc.AccountBlockingStub;
import com.github.ptracker.service.AccountGrpc.AccountFutureStub;
import com.github.ptracker.service.AccountUpdateRequest;
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


public class AccountModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public AccountModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<Account> getAccount(DataFetchingEnvironment environment, String id) {
    return ClientModule.getAccount(environment, id);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "accounts";

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
      verifyDataLoaderRegistryKeysUnassigned(registry, Collections.singletonList(GET_BY_ID_DATA_LOADER_NAME));
      GrpcNotFoundSwallower<String, AccountGetResponse> idToAccount =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(AccountGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, Account> byIdLoader = ids -> {
        List<ListenableFuture<Account>> futures = ids.stream()
            .map(id -> Futures.transform(idToAccount.apply(id),
                response -> response != null ? response.getAccount() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Account>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));
    }

    static CompletableFuture<Account> getAccount(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "Account ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, Account>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:account:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getAccount")
    ListenableFuture<Account> getAccount(AccountGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getAccount(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createAccount")
    ListenableFuture<Account> createAccount(AccountCreateRequest request, AccountFutureStub client) {
      if (request.getAccount().getId() == null || request.getAccount().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = AccountCreateRequest.newBuilder(request)
            .setAccount(Account.newBuilder(request.getAccount()).setId(id))
            .build();
      }
      AccountCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getAccount(),
          MoreExecutors.directExecutor());
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

    @SchemaModification(addField = "gardens", onType = Account.class)
    ListenableFuture<List<Garden>> accountToGardens(Account account, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenModuleProvider.getGardensByAccountId(environment, account.getId()));
    }
  }
}
