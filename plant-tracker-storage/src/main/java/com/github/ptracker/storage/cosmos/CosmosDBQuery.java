package com.github.ptracker.storage.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.*;


public class CosmosDBQuery<KEY_TYPE> {
  private final CosmosContainer _container;
  private final String _query;
  private final String _idVariableKey;

  public CosmosDBQuery(CosmosContainer container, String query, String idVariableKey) {
    _container = checkNotNull(container, "CosmosContainer cannot be null");
    _query = checkNotNull(query, "Query string cannot be null");
    checkArgument(!query.isEmpty(), "Query string cannot be empty");
    _idVariableKey = checkNotNull(idVariableKey, "ID variable key cannot be null");
    checkArgument(!idVariableKey.isEmpty(), "ID variable key cannot be empty");
  }

  public CosmosPagedIterable<ObjectNode> getResults(CosmosQueryRequestOptions options, KEY_TYPE idVariableValue) {
    List<SqlParameter> paramList = Collections.singletonList(new SqlParameter(_idVariableKey, idVariableValue));
    SqlQuerySpec querySpec = new SqlQuerySpec(_query, paramList);
    return _container.queryItems(querySpec, options, ObjectNode.class);
  }
}
