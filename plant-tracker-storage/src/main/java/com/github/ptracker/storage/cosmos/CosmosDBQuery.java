package com.github.ptracker.storage.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;


public class CosmosDBQuery {
  private final CosmosContainer _container;
  private final String _query;

  public CosmosDBQuery(CosmosContainer container, String query) {
    _container = checkNotNull(container, "CosmosContainer cannot be null");
    _query = checkNotNull(query, "Query string cannot be null");
    checkArgument(!query.isEmpty(), "Query string cannot be empty");
  }

  public CosmosPagedIterable<ObjectNode> getResults(CosmosQueryRequestOptions options,
      Map<String, Object> variableValues) {
    List<SqlParameter> paramList = variableValues.entrySet()
        .stream()
        .map(entry -> new SqlParameter(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    SqlQuerySpec querySpec = new SqlQuerySpec(_query, paramList);
    return _container.queryItems(querySpec, options, ObjectNode.class);
  }
}
