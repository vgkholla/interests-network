package com.github.inet.server.graphql;

import com.github.inet.entity.Group;
import com.github.inet.service.GroupGetRequest;
import com.github.inet.service.GroupGetResponse;
import com.github.inet.service.GroupGrpc.GroupFutureStub;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;


class DataLoaderModule extends AbstractModule {

  @Provides
  DataLoaderRegistry dataLoaderRegistry(GroupFutureStub groupService) {
    DataLoaderRegistry registry = new DataLoaderRegistry();

    // TODO: this is awkward and means that this this class will need changes whenever a new data loader is needed.
    //     : Instead there should be a mechanism for individual schema modules to register data loaders

    // "groups" in GroupSchemaModule
    BatchLoader<String, Group> groupBatchLoader = ids -> {
      List<ListenableFuture<Group>> groupResponseFutures = ids.stream()
          .map(id -> Futures.transform(groupService.get(GroupGetRequest.newBuilder().setId(ids.get(0)).build()),
              groupGetResponse -> groupGetResponse != null ? groupGetResponse.getGroup() : null,
              MoreExecutors.directExecutor()))
          .collect(Collectors.toList());
      ListenableFuture<List<Group>> listenableFuture = Futures.allAsList(groupResponseFutures);
      return FutureConverter.toCompletableFuture(listenableFuture);
    };
    registry.register("groups", new DataLoader<>(groupBatchLoader));

    return registry;
  }
}
