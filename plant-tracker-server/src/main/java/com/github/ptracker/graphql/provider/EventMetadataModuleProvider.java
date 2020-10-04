package com.github.ptracker.graphql.provider;

import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Module;
import graphql.schema.DataFetchingEnvironment;
import java.util.Optional;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.DataLoaderRegistry;


public class EventMetadataModuleProvider implements GraphQLModuleProvider {
  private final SchemaModuleImpl _schemaModule = new SchemaModuleImpl();

  @Override
  public Optional<Module> getClientModule() {
    return Optional.empty();
  }

  @Override
  public Optional<Module> getSchemaModule() {
    return Optional.of(_schemaModule);
  }

  @Override
  public void registerDataLoaders(DataLoaderRegistry registry) {

  }

  private static class SchemaModuleImpl extends SchemaModule {

    @SchemaModification(addField = "gardener", onType = EventMetadata.class)
    ListenableFuture<Gardener> eventToGardener(EventMetadata metadata, DataFetchingEnvironment environment) {
      return FutureConverter.toListenableFuture(
          GardenerModuleProvider.getGardener(environment, metadata.getGardenerId()));
    }
  }
}
