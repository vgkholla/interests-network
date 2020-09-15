package com.github.inet.storage.cosmos;

import com.azure.cosmos.models.CosmosItemResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.inet.common.storage.StorageMetadata;


public class CosmosDBMetadataHandler {
  private static final String ETAG_FIELD_NAME = "_etag";

  public StorageMetadata getStorageMetadata(CosmosItemResponse<?> cosmosItemResponse) {
    StorageMetadata.Builder builder = StorageMetadata.newBuilder();
    String etag = cosmosItemResponse.getETag();
    if (etag != null && !etag.isEmpty()) {
      builder.setEtag(etag);
    }
    return builder.build();
  }

  public StorageMetadata getStorageMetadata(ObjectNode cosmosResponse) {
    StorageMetadata.Builder builder = StorageMetadata.newBuilder();
    String etag = cosmosResponse.get(ETAG_FIELD_NAME).textValue();
    if (etag != null && !etag.isEmpty()) {
      builder.setEtag(etag);
    }
    return builder.build();
  }
}
