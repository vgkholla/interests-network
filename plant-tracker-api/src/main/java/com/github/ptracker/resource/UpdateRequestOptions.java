package com.github.ptracker.resource;

public interface UpdateRequestOptions extends ExistingItemRequestOptions {

  boolean shouldUpsert();
}
