package com.github.ptracker.resource;

import com.github.ptracker.common.storage.StorageMetadata;


public class QueryRequestOptionsImpl implements QueryRequestOptions {
  private final GetRequestOptions _getRequestOptions;

  private QueryRequestOptionsImpl(GetRequestOptions getRequestOptions) {
    _getRequestOptions = getRequestOptions;
  }

  @Override
  public StorageMetadata getMetadata() {
    return _getRequestOptions == null ? null : _getRequestOptions.getMetadata();
  }

  public static class Builder {
    private GetRequestOptions _getRequestOptions = null;

    public Builder getRequestOptions(GetRequestOptions getRequestOptions) {
      _getRequestOptions = getRequestOptions;
      return this;
    }

    public QueryRequestOptionsImpl build() {
      return new QueryRequestOptionsImpl(_getRequestOptions);
    }
  }
}
