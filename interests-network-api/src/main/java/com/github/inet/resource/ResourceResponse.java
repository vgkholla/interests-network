package com.github.inet.resource;

import com.github.inet.storage.StorageMetadataProtos;


public interface ResourceResponse<T> {

  T getPayload();

  StorageMetadataProtos.StorageMetadata getStorageMetadata();
}
