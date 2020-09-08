package com.github.inet.resource;

import com.github.inet.storage.StorageMetadataProtos;
import com.github.inet.storage.StorageMetadataProtos.StorageMetadata;


public interface ExistingItemRequestOptions {

  StorageMetadata getMetadata();
}
