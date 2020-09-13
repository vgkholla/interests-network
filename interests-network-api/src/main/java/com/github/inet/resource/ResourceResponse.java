package com.github.inet.resource;

import com.github.inet.common.storage.StorageMetadata;


public interface ResourceResponse<T> {

  ResponseStatus getStatus();

  T getPayload();

  StorageMetadata getStorageMetadata();
}
