package com.github.ptracker.resource;

import java.util.Optional;


public interface Resource<KEY_TYPE, VALUE_TYPE> {

  ResourceResponse<Optional<VALUE_TYPE>> get(KEY_TYPE key, GetRequestOptions options);

  ResourceResponse<Void> create(VALUE_TYPE payload, CreateRequestOptions options);

  ResourceResponse<Void> update(VALUE_TYPE payload, UpdateRequestOptions options);

  ResourceResponse<Void> delete(KEY_TYPE key, DeleteRequestOptions options);
}
