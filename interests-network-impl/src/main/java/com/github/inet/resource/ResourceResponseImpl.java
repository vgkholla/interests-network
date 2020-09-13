package com.github.inet.resource;

import com.github.inet.common.storage.StorageMetadata;

import static com.google.common.base.Preconditions.*;


public class ResourceResponseImpl<T> implements ResourceResponse<T> {
  private final ResponseStatus _responseStatus;
  private final T _payload;
  private final StorageMetadata _metadata;

  private ResourceResponseImpl(ResponseStatus responseStatus, T payload, StorageMetadata metadata) {
    _responseStatus = responseStatus;
    _payload = payload;
    _metadata = metadata;
  }

  @Override
  public ResponseStatus getStatus() {
    return _responseStatus;
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
    private ResponseStatus _responseStatus = ResponseStatus.OK;
    private T _payload = null;
    private StorageMetadata _metadata = null;

    public Builder<T> status(ResponseStatus responseStatus) {
      _responseStatus = checkNotNull(responseStatus, "ResponseStatus cannot be null");
      return this;
    }

    public Builder<T> payload(T payload) {
      _payload = payload;
      return this;
    }

    public Builder<T> metadata(StorageMetadata metadata) {
      _metadata = metadata;
      return this;
    }

    public ResourceResponseImpl<T> build() {
      return new ResourceResponseImpl<>(_responseStatus, _payload, _metadata);
    }
  }
}
