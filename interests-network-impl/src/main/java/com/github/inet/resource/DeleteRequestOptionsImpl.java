package com.github.inet.resource;

import com.github.inet.common.storage.StorageMetadata;

public class DeleteRequestOptionsImpl implements DeleteRequestOptions {
  private final StorageMetadata _metadata;

  private DeleteRequestOptionsImpl(StorageMetadata metadata) {
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

    public DeleteRequestOptions build() {
      return new DeleteRequestOptionsImpl(_metadata);
    }
  }
}
