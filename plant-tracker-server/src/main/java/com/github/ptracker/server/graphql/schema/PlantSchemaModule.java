package com.github.ptracker.server.graphql.schema;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.service.PlantCreateRequest;
import com.github.ptracker.service.PlantDeleteRequest;
import com.github.ptracker.service.PlantDeleteResponse;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGrpc.PlantFutureStub;
import com.github.ptracker.service.PlantUpdateRequest;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import graphql.schema.DataFetchingEnvironment;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.DataLoaderRegistry;


class PlantSchemaModule extends SchemaModule {

  @Query("getPlant")
  ListenableFuture<Plant> getPlant(PlantGetRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
    return FutureConverter.toListenableFuture(
        dataFetchingEnvironment.<DataLoaderRegistry>getContext().<String, Plant>getDataLoader(
            "plants") // registered in DataLoaderModule
            .load(request.getId()));
  }

  // TODO: return needs to be "empty" or "success/failure"
  @Mutation("createPlant")
  ListenableFuture<Plant> createPlant(PlantCreateRequest request, PlantFutureStub client) {
    return Futures.transform(client.create(request), ignored -> request.getPlant(), MoreExecutors.directExecutor());
  }

  // TODO: return needs to be "empty" or "success/failure"
  @Mutation("updatePlant")
  ListenableFuture<Plant> updatePlant(PlantUpdateRequest request, PlantFutureStub client) {
    return Futures.transform(client.update(request), ignored -> request.getPlant(), MoreExecutors.directExecutor());
  }

  // TODO: return needs to be "empty" or "success/failure"
  @Mutation("deletePlant")
  ListenableFuture<PlantDeleteResponse> deletePlant(PlantDeleteRequest request, PlantFutureStub client) {
    return client.delete(request);
  }
}
