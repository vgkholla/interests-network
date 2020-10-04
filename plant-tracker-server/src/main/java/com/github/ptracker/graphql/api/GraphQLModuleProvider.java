package com.github.ptracker.graphql.api;

import com.google.inject.Module;
import java.util.Optional;
import org.dataloader.DataLoaderRegistry;


public interface GraphQLModuleProvider {

  Optional<Module> getClientModule();

  Optional<Module> getSchemaModule();

  void registerDataLoaders(DataLoaderRegistry registry);
}
