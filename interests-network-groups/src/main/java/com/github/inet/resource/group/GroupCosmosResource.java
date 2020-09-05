package com.github.inet.resource.group;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.github.inet.resource.CreateRequestOptions;
import com.github.inet.resource.DeleteRequestOptions;
import com.github.inet.resource.GetRequestOptions;
import com.github.inet.resource.Resource;
import com.github.inet.resource.UpsertRequestOptions;
import com.github.inet.entities.GroupProtos.Group;
import com.github.inet.resource.storage.cosmos.CosmosDBQuery;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;


public class GroupCosmosResource implements Resource<String, Group> {
  private static final CosmosQueryRequestOptions DEFAULT_QUERY_REQUEST_OPTIONS =
      new CosmosQueryRequestOptions().setQueryMetricsEnabled(true);
  private static final CosmosItemRequestOptions DEFAULT_ITEM_REQUEST_OPTIONS = new CosmosItemRequestOptions();
  private static final String ID_VARIABLE = "@id";
  private static final String GET_BY_ID_QUERY = "SELECT * FROM groups where groups.id = " + ID_VARIABLE;

  private final CosmosContainer _groupContainer;
  private final CosmosDBQuery _fetchByGroupId;

  public GroupCosmosResource(CosmosContainer groupContainer) {
    _groupContainer = checkNotNull(groupContainer, "Container cannot be null");
    _fetchByGroupId = new CosmosDBQuery(groupContainer, GET_BY_ID_QUERY);
  }

  public Optional<Group> get(String key, GetRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    List<Group> matchingGroups =
        _fetchByGroupId.getResults(DEFAULT_QUERY_REQUEST_OPTIONS, Collections.singletonMap(ID_VARIABLE, key))
            .stream()
            .map(item -> GroupUtils.convert(item.toJson()))
            .collect(Collectors.toList());
    Optional<Group> maybeGroup = Optional.empty();
    if (!matchingGroups.isEmpty()) {
      if (matchingGroups.size() > 1) {
        throw new IllegalStateException("There is more than one group with the ID " + key);
      }
      maybeGroup = Optional.of(Iterables.getOnlyElement(matchingGroups));
    }
    return maybeGroup;
  }

  @Override
  public boolean create(Group payload, CreateRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    GroupUtils.verifyGroup(payload);
    CosmosItemResponse<Group> createResponse =
        _groupContainer.createItem(payload, DEFAULT_ITEM_REQUEST_OPTIONS);
    return true;
  }

  @Override
  public boolean upsert(Group payload, UpsertRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    GroupUtils.verifyGroup(payload);
    CosmosItemResponse<Group> updateResponse =
        _groupContainer.upsertItem(payload, DEFAULT_ITEM_REQUEST_OPTIONS);
    return true;
  }

  @Override
  public boolean delete(String key, DeleteRequestOptions options) {
    checkNotNull(key, "Key to delete cannot be null");
    CosmosItemResponse<Object> deleteResponse =
        _groupContainer.deleteItem(key, new PartitionKey(key), DEFAULT_ITEM_REQUEST_OPTIONS);
    return true;
  }
}
