package com.github.inet.server.graphql.schema;

import com.github.inet.entity.Group;
import com.github.inet.service.GroupCreateRequest;
import com.github.inet.service.GroupDeleteRequest;
import com.github.inet.service.GroupDeleteResponse;
import com.github.inet.service.GroupGetRequest;
import com.github.inet.service.GroupGrpc.GroupFutureStub;
import com.github.inet.service.GroupUpdateRequest;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import graphql.schema.DataFetchingEnvironment;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.DataLoaderRegistry;


class GroupSchemaModule extends SchemaModule {

  @Query("getGroup")
  ListenableFuture<Group> getGroup(GroupGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
    return FutureConverter.toListenableFuture(
        dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, Group>getDataLoader(
            "groups") // registered in DataLoaderModule
            .load(request.getId()));
  }

  // TODO: return needs to be "empty" or "success/failure"
  @Mutation("createGroup")
  ListenableFuture<Group> createGroup(GroupCreateRequest request, GroupFutureStub client) {
    return Futures.transform(client.create(request), ignored -> request.getGroup(), MoreExecutors.directExecutor());
  }

  // TODO: return needs to be "empty" or "success/failure"
  @Mutation("updateGroup")
  ListenableFuture<Group> updateGroup(GroupUpdateRequest request, GroupFutureStub client) {
    return Futures.transform(client.update(request), ignored -> request.getGroup(), MoreExecutors.directExecutor());
  }

  // TODO: return needs to be "empty" or "success/failure"
  @Mutation("deleteGroup")
  ListenableFuture<GroupDeleteResponse> deleteGroup(GroupDeleteRequest request, GroupFutureStub client) {
    return client.delete(request);
  }
}
