package com.github.inet.server.graphql.schema;

import com.google.inject.AbstractModule;
import org.dataloader.DataLoaderRegistry;


public class OverallSchemaModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new GroupSchemaModule());
  }
}
