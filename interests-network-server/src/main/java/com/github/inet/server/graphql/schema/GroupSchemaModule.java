package com.github.inet.server.graphql.schema;

import com.github.inet.entity.Group;
import com.github.inet.service.GroupGetRequest;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.util.concurrent.ListenableFuture;
import graphql.schema.DataFetchingEnvironment;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.DataLoaderRegistry;


class GroupSchemaModule extends SchemaModule {

  @Query("getGroup")
  ListenableFuture<Group> getGroup(GroupGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
    return FutureConverter.toListenableFuture(
        dataFetchingEnvironment
            .<DataLoaderRegistry>getContext()
            .<String, Group>getDataLoader("groups") // registered in DataLoaderModule
            .load(request.getId()));
  }
}
