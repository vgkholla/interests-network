package com.github.ptracker.graphql.api;

import com.google.inject.Module;
import org.dataloader.DataLoaderRegistry;


public interface GraphQLModuleProvider {

  Module getClientModule();

  Module getSchemaModule();

  void registerDataLoaders(DataLoaderRegistry registry);
}
