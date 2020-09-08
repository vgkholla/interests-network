package com.github.inet.storage.cosmos;

import com.azure.cosmos.implementation.CosmosItemProperties;
import com.azure.cosmos.models.CosmosItemResponse;
import com.github.inet.storage.StorageMetadataProtos.StorageMetadata;


public class CosmosDBMetadataHandler {

  public StorageMetadata getStorageMetadata(CosmosItemResponse<?> cosmosItemResponse) {
    StorageMetadata.Builder builder = StorageMetadata.newBuilder();
    String etag = cosmosItemResponse.getETag();
    if (etag != null && !etag.isEmpty()) {
      builder.setEtag(etag);
    }
    return builder.build();
  }

  public StorageMetadata getStorageMetadata(CosmosItemProperties cosmosItemProperties){
    StorageMetadata.Builder builder = StorageMetadata.newBuilder();
    String etag = cosmosItemProperties.getETag();
    if (etag != null && !etag.isEmpty()) {
      builder.setEtag(etag);
    }
    return builder.build();
  }
}
