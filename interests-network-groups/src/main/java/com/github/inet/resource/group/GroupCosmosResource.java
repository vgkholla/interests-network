package com.github.inet.resource.group;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.inet.common.protobuf.ProtoBufJsonInterchange;
import com.github.inet.common.storage.StorageMetadata;
import com.github.inet.entity.Group;
import com.github.inet.resource.CreateRequestOptions;
import com.github.inet.resource.DeleteRequestOptions;
import com.github.inet.resource.GetRequestOptions;
import com.github.inet.resource.Resource;
import com.github.inet.resource.ResourceResponse;
import com.github.inet.resource.ResourceResponseImpl;
import com.github.inet.resource.ResponseStatus;
import com.github.inet.resource.UpdateRequestOptions;
import com.github.inet.storage.cosmos.CosmosDBMetadataHandler;
import com.github.inet.storage.cosmos.CosmosDBQuery;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;


public class GroupCosmosResource implements Resource<String, Group> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupCosmosResource.class);

  private static final String GROUP_DATABASE_NAME = "Groups";
  private static final String GROUP_CONTAINER_NAME = "groups";

  private static final CosmosQueryRequestOptions DEFAULT_QUERY_REQUEST_OPTIONS =
      new CosmosQueryRequestOptions().setQueryMetricsEnabled(true);
  private static final CosmosItemRequestOptions DEFAULT_ITEM_REQUEST_OPTIONS = new CosmosItemRequestOptions();

  private static final ResourceResponse<Optional<Group>> NO_MATCHING_GROUPS =
      new ResourceResponseImpl.Builder<Optional<Group>>().payload(Optional.empty()).build();

  private static final int CREATE_SUCCESS_STATUS_CODE = 201;
  private static final int UPSERT_SUCCESS_STATUS_CODE = 200;
  private static final int DELETE_SUCCESS_STATUS_CODE = 204;
  private static final int DELETE_NOT_FOUND_STATUS_CODE = 404;

  private static final String ID_VARIABLE = "@id";
  private static final String GET_BY_ID_QUERY = "SELECT * FROM groups where groups.id = " + ID_VARIABLE;

  private final CosmosClient _client;
  private final ProtoBufJsonInterchange<Group, Group.Builder> _protoBufJsonInterchange =
      new ProtoBufJsonInterchange<>(Group::newBuilder);
  private final CosmosDBMetadataHandler _metadataHandler = new CosmosDBMetadataHandler();

  public GroupCosmosResource(CosmosClient client) {
    _client = checkNotNull(client, "Client cannot be null");
  }

  // TODO: Report a bug where if partition key is set, it is used across requests.
  //       createItem(item, setPartKey)
  //       updateItem(differentItem) // without setting part key
  //       fails (and same for multithreading)
  public ResourceResponse<Optional<Group>> get(String key, GetRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    LOGGER.debug("Getting {}", key);
    CosmosDBQuery query = new CosmosDBQuery(getContainer(), GET_BY_ID_QUERY);
    List<ResourceResponseImpl<Optional<Group>>> matchingGroups =
        query.getResults(DEFAULT_QUERY_REQUEST_OPTIONS, Collections.singletonMap(ID_VARIABLE, key))
            .stream()
            .map(item -> new ResourceResponseImpl.Builder<Optional<Group>>().payload(
                Optional.of(_protoBufJsonInterchange.convert(item)))
                .metadata(_metadataHandler.getStorageMetadata(item))
                .build())
            .collect(Collectors.toList());
    return matchingGroups.isEmpty() ? NO_MATCHING_GROUPS : Iterables.getOnlyElement(matchingGroups);
  }

  @Override
  public ResourceResponse<Void> create(Group payload, CreateRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    GroupUtils.verifyGroup(payload);
    LOGGER.debug("Creating {}", payload);
    ObjectNode item = _protoBufJsonInterchange.convert(payload);
    CosmosItemResponse<ObjectNode> createResponse = getContainer().createItem(item, DEFAULT_ITEM_REQUEST_OPTIONS);
    ResponseStatus responseStatus = createResponse.getStatusCode() == CREATE_SUCCESS_STATUS_CODE ? ResponseStatus.OK
        : ResponseStatus.INTERNAL_ERROR;
    StorageMetadata metadata =
        ResponseStatus.OK.equals(responseStatus) ? _metadataHandler.getStorageMetadata(createResponse) : null;
    return new ResourceResponseImpl.Builder<Void>().status(responseStatus).metadata(metadata).build();
  }

  @Override
  public ResourceResponse<Void> update(Group payload, UpdateRequestOptions options) {
    if (!options.shouldUpsert()) {
      // TODO: support update
      throw new UnsupportedOperationException("This resource supports upserts only");
    }
    checkNotNull(payload, "Update payload cannot be null");
    GroupUtils.verifyGroup(payload);
    LOGGER.debug("Updating {}", payload);

    ObjectNode item = _protoBufJsonInterchange.convert(payload);
    CosmosItemResponse<ObjectNode> updateResponse = getContainer().upsertItem(item, DEFAULT_ITEM_REQUEST_OPTIONS);
    ResponseStatus responseStatus = updateResponse.getStatusCode() == UPSERT_SUCCESS_STATUS_CODE ? ResponseStatus.OK
        : ResponseStatus.INTERNAL_ERROR;
    StorageMetadata metadata =
        ResponseStatus.OK.equals(responseStatus) ? _metadataHandler.getStorageMetadata(updateResponse) : null;
    return new ResourceResponseImpl.Builder<Void>().status(responseStatus).metadata(metadata).build();
  }

  @Override
  public ResourceResponse<Void> delete(String key, DeleteRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    LOGGER.debug("Deleting {}", key);
    ObjectNode objectNode = _protoBufJsonInterchange.convert(Group.newBuilder().setId(key).build());
    CosmosItemResponse<Object> deleteResponse = getContainer().deleteItem(objectNode, DEFAULT_ITEM_REQUEST_OPTIONS);
    int statusCode = deleteResponse.getStatusCode();
    ResponseStatus responseStatus = ResponseStatus.INTERNAL_ERROR;
    if (statusCode == DELETE_SUCCESS_STATUS_CODE) {
      responseStatus = ResponseStatus.OK;
    } else if (statusCode == DELETE_NOT_FOUND_STATUS_CODE) {
      responseStatus = ResponseStatus.NOT_FOUND;
    }
    return new ResourceResponseImpl.Builder<Void>().status(responseStatus).build();
  }

  private CosmosContainer getContainer() {
    return _client.getDatabase(GROUP_DATABASE_NAME).getContainer(GROUP_CONTAINER_NAME);
  }
}
