package com.github.ptracker.graphql;

import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
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
