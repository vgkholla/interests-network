package com.github.inet.resource;

import com.github.inet.storage.StorageMetadataProtos.StorageMetadata;


public class ResourceResponseImpl<T> implements ResourceResponse<T> {
  private final T _payload;
  private final StorageMetadata _metadata;

  private ResourceResponseImpl(T payload, StorageMetadata metadata) {
    _payload = payload;
    _metadata = metadata;
  }

  @Override
  public T getPayload() {
    return _payload;
  }

  @Override
  public StorageMetadata getStorageMetadata() {
    return _metadata;
  }

  public static class Builder<T> {
    private T _payload = null;
    private StorageMetadata _metadata = null;

    public Builder<T> payload(T payload) {
      _payload = payload;
      return this;
    }

    public Builder<T> metadata(StorageMetadata metadata) {
      _metadata = metadata;
      return this;
    }

    public ResourceResponseImpl<T> build() {
      return new ResourceResponseImpl<>(_payload, _metadata);
    }
  }
}
