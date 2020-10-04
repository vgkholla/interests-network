package com.github.ptracker.storage.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.*;


public class CosmosDBQuery {
  private static final String SELECT_KEYWORDS = "SELECT * FROM";
  private static final String FILTER_KEYWORDS = "WHERE";
  private static final String CONTAINER_FIELD_NAME_SEPARATOR = ".";

  private final CosmosContainer _container;
  private final String _queryPrefix;


  public CosmosDBQuery(CosmosContainer container) {
    _container = checkNotNull(container, "CosmosContainer cannot be null");
    _queryPrefix = SELECT_KEYWORDS + " " + container.getId() + " " + FILTER_KEYWORDS;
  }

  public CosmosPagedIterable<ObjectNode> getResults(ObjectNode objectNode, CosmosQueryRequestOptions options) {
    SqlQuerySpec querySpec = new SqlQuerySpec(getQuery(objectNode), Collections.emptyList());
    return _container.queryItems(querySpec, options, ObjectNode.class);
  }

  private String getQuery(ObjectNode objectNode) {
    StringBuilder queryBuilder = new StringBuilder(_queryPrefix);
    Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      JsonNode value = field.getValue();
      if (value.isValueNode()) {
        queryBuilder.append(" ")
            .append(getFilterField(field.getKey()))
            .append(" = ");
        if (value.isNumber()) {
          queryBuilder.append(value.asLong()); // TODO: can fail
        } else if (value.isBoolean()) {
          queryBuilder.append(value.asBoolean());
        } else if (value.isTextual()) {
          queryBuilder.append("\"").append(value.asText()).append("\"");
        } else {
          throw new IllegalArgumentException("Cannot handle value of type " + value.getNodeType());
        }
      }
    }
    return queryBuilder.toString();
  }

  private String getFilterField(String fieldName) {
    return _container.getId() + CONTAINER_FIELD_NAME_SEPARATOR + fieldName;
  }
}
