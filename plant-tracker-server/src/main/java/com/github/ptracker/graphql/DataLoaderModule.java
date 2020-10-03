package com.github.ptracker.graphql;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
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

import static com.google.common.base.Preconditions.*;


class DataLoaderModule extends AbstractModule {
  private final GraphQLModuleProvider _graphQLModuleProvider;

  public DataLoaderModule(GraphQLModuleProvider graphQLModuleProvider) {
    _graphQLModuleProvider = checkNotNull(graphQLModuleProvider, "GraphQLModuleProvider cannot be null");
  }

  @Provides
  DataLoaderRegistry dataLoaderRegistry() {
    DataLoaderRegistry registry = new DataLoaderRegistry();
    _graphQLModuleProvider.registerDataLoaders(registry);
    return registry;
  }
}
