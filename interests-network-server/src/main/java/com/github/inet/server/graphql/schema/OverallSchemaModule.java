package com.github.inet.server.graphql.schema;

import com.google.inject.AbstractModule;


public class OverallSchemaModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new GroupSchemaModule());
  }
}
