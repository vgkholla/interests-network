package com.github.inet.resource;

public interface UpdateRequestOptions extends ExistingItemRequestOptions {

  boolean shouldUpsert();
}
