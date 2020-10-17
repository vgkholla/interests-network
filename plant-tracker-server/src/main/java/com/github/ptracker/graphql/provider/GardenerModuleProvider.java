package com.github.ptracker.graphql.provider;

import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.graphql.GrpcNotFoundSwallower;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.GardenerCreateRequest;
import com.github.ptracker.service.GardenerDeleteRequest;
import com.github.ptracker.service.GardenerDeleteResponse;
import com.github.ptracker.service.GardenerGetRequest;
import com.github.ptracker.service.GardenerGetResponse;
import com.github.ptracker.service.GardenerGrpc;
import com.github.ptracker.service.GardenerGrpc.GardenerBlockingStub;
import com.github.ptracker.service.GardenerGrpc.GardenerFutureStub;
import com.github.ptracker.service.GardenerUpdateRequest;
import com.github.ptracker.util.IdGenerator;
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


public class GardenerModuleProvider implements GraphQLModuleProvider {
  private final ClientModule _clientModule;
  private final Module _schemaModule = new SchemaModuleImpl();

  public GardenerModuleProvider(String serverHost, int serverPort) {
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

  public static CompletableFuture<Gardener> getGardener(DataFetchingEnvironment environment, String id) {
    return ClientModule.getGardener(environment, id);
  }

  private static class ClientModule extends AbstractModule {
    private static final String GET_BY_ID_DATA_LOADER_NAME = "Gardeners";

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
      verifyDataLoaderRegistryKeysUnassigned(registry, Collections.singletonList(GET_BY_ID_DATA_LOADER_NAME));
      GrpcNotFoundSwallower<String, GardenerGetResponse> idToGardener =
          new GrpcNotFoundSwallower<>(id -> _futureStub.get(GardenerGetRequest.newBuilder().setId(id).build()));
      BatchLoader<String, Gardener> byIdLoader = ids -> {
        List<ListenableFuture<Gardener>> futures = ids.stream()
            .map(id -> Futures.transform(idToGardener.apply(id),
                response -> response != null ? response.getGardener() : null, MoreExecutors.directExecutor()))
            .collect(Collectors.toList());
        ListenableFuture<List<Gardener>> listenableFuture = Futures.allAsList(futures);
        return FutureConverter.toCompletableFuture(listenableFuture);
      };
      registry.register(GET_BY_ID_DATA_LOADER_NAME, new DataLoader<>(byIdLoader));
    }

    static CompletableFuture<Gardener> getGardener(DataFetchingEnvironment environment, String id) {
      checkNotNull(environment, "DataFetchingEnvironment cannot be null");
      checkNotNull(id, "Gardener ID cannot be null");
      return environment.<DataLoaderRegistry>getContext().<String, Gardener>getDataLoader(
          GET_BY_ID_DATA_LOADER_NAME).load(id);
    }
  }

  private static class SchemaModuleImpl extends SchemaModule {
    private static final String ID_PREFIX = "ptracker:gardener:";
    private final IdGenerator<String> _idGenerator = new RandomStringIdGenerator(ID_PREFIX);

    @Query("getGardener")
    ListenableFuture<Gardener> getGardener(GardenerGetRequest request,
        DataFetchingEnvironment dataFetchingEnvironment) {
      return FutureConverter.toListenableFuture(ClientModule.getGardener(dataFetchingEnvironment, request.getId()));
    }

    // TODO: return needs to be "empty" or "success/failure"
    @Mutation("createGardener")
    ListenableFuture<Gardener> createGardener(GardenerCreateRequest request, GardenerFutureStub client) {
      if (request.getGardener().getId() == null || request.getGardener().getId().isEmpty()) {
        String id = _idGenerator.getNextId();
        request = GardenerCreateRequest.newBuilder(request)
            .setGardener(Gardener.newBuilder(request.getGardener()).setId(id))
            .build();
      }
      GardenerCreateRequest finalRequest = request;
      return Futures.transform(client.create(request), ignored -> finalRequest.getGardener(),
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

    @SchemaModification(addField = "fertilizationEvents", onType = Gardener.class)
    ListenableFuture<List<FertilizationEvent>> gardenerToFertilizationEvents(Gardener gardener,
        DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          FertilizationEventModuleProvider.getFertilizationEventsByGardenerId(environment, gardener.getId()));
    }

    @SchemaModification(addField = "wateringEvents", onType = Gardener.class)
    ListenableFuture<List<WateringEvent>> gardenerToWateringEvents(Gardener gardener,
        DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          WateringEventModuleProvider.getWateringEventsByGardenerId(environment, gardener.getId()));
    }

    @SchemaModification(addField = "gardenPlantNoteEvents", onType = Gardener.class)
    ListenableFuture<List<NoteEvent>> gardenerToGardenPlantNoteEvents(Gardener gardener,
        DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          NoteEventModuleProvider.getNoteEventsByGardenerId(environment, gardener.getId()));
    }
  }
}
