package com.github.ptracker.graphql;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGrpc.PlantFutureStub;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.List;
import java.util.stream.Collectors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;


class DataLoaderModule extends AbstractModule {

  @Provides
  DataLoaderRegistry dataLoaderRegistry(PlantFutureStub plantService) {
    DataLoaderRegistry registry = new DataLoaderRegistry();

    // TODO: this is awkward and means that this this class will need changes whenever a new data loader is needed.
    //     : Instead there should be a mechanism for individual schema modules to register data loaders

    // "plants" in PlantSchemaModule
    BatchLoader<String, Plant> plantBatchLoader = ids -> {
      List<ListenableFuture<Plant>> plantResponseFutures = ids.stream()
          .map(id -> Futures.transform(plantService.get(PlantGetRequest.newBuilder().setId(ids.get(0)).build()),
              plantGetResponse -> plantGetResponse != null ? plantGetResponse.getPlant() : null,
              MoreExecutors.directExecutor()))
          .collect(Collectors.toList());
      ListenableFuture<List<Plant>> listenableFuture = Futures.allAsList(plantResponseFutures);
      return FutureConverter.toCompletableFuture(listenableFuture);
    };
    registry.register("plants", new DataLoader<>(plantBatchLoader));

    return registry;
  }
}
