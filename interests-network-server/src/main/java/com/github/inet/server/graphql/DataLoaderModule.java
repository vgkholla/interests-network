package com.github.inet.server.graphql;

import com.github.inet.entity.Group;
import com.github.inet.service.GroupGetRequest;
import com.github.inet.service.GroupGetResponse;
import com.github.inet.service.GroupGrpc.GroupBlockingStub;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;


class DataLoaderModule extends AbstractModule {

  @Provides
  DataLoaderRegistry dataLoaderRegistry(GroupBlockingStub groupService) {
    DataLoaderRegistry registry = new DataLoaderRegistry();

    // TODO: this is awkward and means that this this class will need changes whenever a new data loader is needed.
    //     : Instead there should be a mechanism for individual schema modules to register data loaders

    // "groups" in GroupSchemaModule
    BatchLoader<String, Group> groupBatchLoader = ids -> CompletableFuture.completedFuture(ids.stream()
        .map(id -> groupService.get(GroupGetRequest.newBuilder().setId(ids.get(0)).build()))
        .map(GroupGetResponse::getGroup)
        .collect(Collectors.toList()));
    registry.register("groups", new DataLoader<>(groupBatchLoader));

    return registry;
  }
}
