package com.github.ptracker.server.graphql.schema;

import com.google.inject.AbstractModule;


public class OverallSchemaModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new PlantSchemaModule());
  }
}
