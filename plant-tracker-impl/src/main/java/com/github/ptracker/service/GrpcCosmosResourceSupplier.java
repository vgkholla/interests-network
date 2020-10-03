package com.github.ptracker.service;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.resource.CosmosResource;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.storage.cosmos.CosmosDBQuery;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class GrpcCosmosResourceSupplier<KEY_TYPE, VALUE_TYPE> implements Supplier<Resource<KEY_TYPE, VALUE_TYPE>> {
  private static final String ID_VARIABLE = "@id";

  private final Resource<KEY_TYPE, VALUE_TYPE> _resource;

  public GrpcCosmosResourceSupplier(CosmosClient cosmosClient, String dbName, String containerName, String idFieldName,
      DataInterchange<ObjectNode, VALUE_TYPE> dataInterchange, Function<KEY_TYPE, VALUE_TYPE> valueWithIdOnlyCreator,
      Consumer<VALUE_TYPE> valueVerifier) {
    CosmosContainer container = cosmosClient.getDatabase(dbName).getContainer(containerName);
    String query =
        "SELECT * FROM " + containerName + " where " + containerName + "." + idFieldName + " = " + ID_VARIABLE;
    CosmosDBQuery<KEY_TYPE> cosmosDBQuery = new CosmosDBQuery<>(container, query, ID_VARIABLE);
    _resource = new CosmosResource<>(container, cosmosDBQuery, dataInterchange, valueWithIdOnlyCreator, valueVerifier);
  }

  @Override
  public Resource<KEY_TYPE, VALUE_TYPE> get() {
    return _resource;
  }
}
