package com.github.inet.resource;

import com.github.inet.resource.common.MetadataProtos.Metadata;

import static com.google.common.base.Preconditions.*;

public class UpsertRequestOptionsImpl implements UpsertRequestOptions {

  private final Metadata _metadata;

  public UpsertRequestOptionsImpl(Metadata metadata) {
    _metadata = checkNotNull(metadata, "Metadata cannot be null");
  }

  @Override
  public Metadata getMetadata() {
    return _metadata;
  }
}
