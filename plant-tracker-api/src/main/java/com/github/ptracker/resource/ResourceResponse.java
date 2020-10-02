package com.github.ptracker.resource;

import com.github.ptracker.common.storage.StorageMetadata;


public interface ResourceResponse<T> {

  ResponseStatus getStatus();

  T getPayload();

  StorageMetadata getStorageMetadata();
}
