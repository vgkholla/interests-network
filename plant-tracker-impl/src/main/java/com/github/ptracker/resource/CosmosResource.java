package com.github.ptracker.resource;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.common.storage.StorageMetadata;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.storage.cosmos.CosmosDBMetadataHandler;
import com.github.ptracker.storage.cosmos.CosmosDBQuery;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;


public class CosmosResource<KEY_TYPE, VALUE_TYPE> implements Resource<KEY_TYPE, VALUE_TYPE> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CosmosResource.class);

  private static final CosmosQueryRequestOptions DEFAULT_QUERY_REQUEST_OPTIONS =
      new CosmosQueryRequestOptions().setQueryMetricsEnabled(true);
  private static final CosmosItemRequestOptions DEFAULT_ITEM_REQUEST_OPTIONS = new CosmosItemRequestOptions();

  private static final int CREATE_SUCCESS_STATUS_CODE = 201;
  private static final int UPSERT_SUCCESS_STATUS_CODE = 200;
  private static final int DELETE_SUCCESS_STATUS_CODE = 204;
  private static final int DELETE_NOT_FOUND_STATUS_CODE = 404;

  private final CosmosContainer _container;
  private final DataInterchange<ObjectNode, VALUE_TYPE> _dataInterchange;
  private final Function<KEY_TYPE, VALUE_TYPE> _valueWithIdOnlyCreator;
  private final Consumer<VALUE_TYPE> _valueVerifier;
  private final CosmosDBQuery _cosmosDBQuery;

  private final CosmosDBMetadataHandler _metadataHandler = new CosmosDBMetadataHandler();
  private final ResourceResponse<Optional<VALUE_TYPE>> _noMatch =
      new ResourceResponseImpl.Builder<Optional<VALUE_TYPE>>().payload(Optional.empty()).build();


  public CosmosResource(CosmosContainer container,
      DataInterchange<ObjectNode, VALUE_TYPE> dataInterchange, Function<KEY_TYPE, VALUE_TYPE> valueWithIdOnlyCreator,
      Consumer<VALUE_TYPE> valueVerifier) {
    _container = checkNotNull(container, "CosmosContainer cannot be null");
    _dataInterchange = checkNotNull(dataInterchange, "DataInterchange cannot be null");
    _valueWithIdOnlyCreator = checkNotNull(valueWithIdOnlyCreator, "valueWithIdOnlyCreator cannot be null");
    _valueVerifier = checkNotNull(valueVerifier, "valueVerifier cannot be null");
    _cosmosDBQuery = new CosmosDBQuery(container);
  }

  // TODO: Report a bug where if partition key is set, it is used across requests.
  //       createItem(item, setPartKey)
  //       updateItem(differentItem) // without setting part key
  //       fails (and same for multithreading)
  @Override
  public ResourceResponse<Optional<VALUE_TYPE>> get(KEY_TYPE key, GetRequestOptions options) {
    checkArgument(key != null, "key cannot be null");
    LOGGER.debug("Getting {}", key);
    ObjectNode node = _dataInterchange.convertBackward(_valueWithIdOnlyCreator.apply(key));
    List<ResourceResponseImpl<Optional<VALUE_TYPE>>> matchingResults =
        _cosmosDBQuery.getResults(node, DEFAULT_QUERY_REQUEST_OPTIONS)
            .stream()
            .map(item -> new ResourceResponseImpl.Builder<Optional<VALUE_TYPE>>().payload(
                Optional.of(_dataInterchange.convertForward(item)))
                .metadata(_metadataHandler.getStorageMetadata(item))
                .build())
            .collect(Collectors.toList());
    return matchingResults.isEmpty() ? _noMatch : Iterables.getOnlyElement(matchingResults);
  }

  @Override
  public ResourceResponse<Void> create(VALUE_TYPE payload, CreateRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    _valueVerifier.accept(payload);
    LOGGER.debug("Creating {}", payload);
    ObjectNode item = _dataInterchange.convertBackward(payload);


    CosmosItemResponse<ObjectNode> createResponse = _container.createItem(item, DEFAULT_ITEM_REQUEST_OPTIONS);
    ResponseStatus responseStatus = createResponse.getStatusCode() == CREATE_SUCCESS_STATUS_CODE ? ResponseStatus.OK
        : ResponseStatus.INTERNAL_ERROR;
    StorageMetadata metadata =
        ResponseStatus.OK.equals(responseStatus) ? _metadataHandler.getStorageMetadata(createResponse) : null;
    return new ResourceResponseImpl.Builder<Void>().status(responseStatus).metadata(metadata).build();
  }

  @Override
  public ResourceResponse<Void> update(VALUE_TYPE payload, UpdateRequestOptions options) {
    if (!options.shouldUpsert()) {
      // TODO: support update
      throw new UnsupportedOperationException("This resource supports upserts only");
    }
    checkNotNull(payload, "Update payload cannot be null");
    _valueVerifier.accept(payload);
    LOGGER.debug("Updating {}", payload);
    ObjectNode item = _dataInterchange.convertBackward(payload);
    CosmosItemResponse<ObjectNode> updateResponse = _container.upsertItem(item, DEFAULT_ITEM_REQUEST_OPTIONS);
    ResponseStatus responseStatus = updateResponse.getStatusCode() == UPSERT_SUCCESS_STATUS_CODE ? ResponseStatus.OK
        : ResponseStatus.INTERNAL_ERROR;
    StorageMetadata metadata =
        ResponseStatus.OK.equals(responseStatus) ? _metadataHandler.getStorageMetadata(updateResponse) : null;
    return new ResourceResponseImpl.Builder<Void>().status(responseStatus).metadata(metadata).build();
  }

  @Override
  public ResourceResponse<Void> delete(KEY_TYPE key, DeleteRequestOptions options) {
    checkArgument(key != null, "key cannot be null");
    LOGGER.debug("Deleting {}", key);
    VALUE_TYPE payload = _valueWithIdOnlyCreator.apply(key);
    ObjectNode objectNode = _dataInterchange.convertBackward(payload);
    CosmosItemResponse<Object> deleteResponse = _container.deleteItem(objectNode, DEFAULT_ITEM_REQUEST_OPTIONS);
    int statusCode = deleteResponse.getStatusCode();
    ResponseStatus responseStatus = ResponseStatus.INTERNAL_ERROR;
    if (statusCode == DELETE_SUCCESS_STATUS_CODE) {
      responseStatus = ResponseStatus.OK;
    } else if (statusCode == DELETE_NOT_FOUND_STATUS_CODE) {
      responseStatus = ResponseStatus.NOT_FOUND;
    }
    return new ResourceResponseImpl.Builder<Void>().status(responseStatus).build();
  }
}
