package com.github.ptracker.resource;

import com.github.ptracker.common.storage.StorageMetadata;


public class GetRequestOptionsImpl implements GetRequestOptions {

  private final StorageMetadata _metadata;

  private GetRequestOptionsImpl(StorageMetadata metadata) {
    _metadata = metadata;
  }

  @Override
  public StorageMetadata getMetadata() {
    return _metadata;
  }

  public static class Builder {
    private StorageMetadata _metadata = null;

    public Builder metadata(StorageMetadata metadata) {
      _metadata = metadata;
      return this;
    }

    public GetRequestOptionsImpl build() {
      return new GetRequestOptionsImpl(_metadata);
    }
  }
}
