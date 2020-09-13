package com.github.inet.resource;

import com.github.inet.common.storage.StorageMetadata;


public class UpdateRequestOptionsImpl implements UpdateRequestOptions {

  private final StorageMetadata _metadata;
  private final boolean _shouldUpsert;

  private UpdateRequestOptionsImpl(StorageMetadata metadata, boolean shouldUpsert) {
    _metadata = metadata;
    _shouldUpsert = shouldUpsert;
  }

  @Override
  public StorageMetadata getMetadata() {
    return _metadata;
  }

  @Override
  public boolean shouldUpsert() {
    return _shouldUpsert;
  }

  public static class Builder {
    private StorageMetadata _metadata = null;
    private boolean _shouldUpsert = false;

    public Builder metadata(StorageMetadata metadata) {
      _metadata = metadata;
      return this;
    }

    public Builder shouldUpsert(boolean shouldUpsert) {
      _shouldUpsert = shouldUpsert;
      return this;
    }

    public UpdateRequestOptionsImpl build() {
      return new UpdateRequestOptionsImpl(_metadata, _shouldUpsert);
    }
  }
}
