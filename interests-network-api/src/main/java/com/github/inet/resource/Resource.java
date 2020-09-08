package com.github.inet.resource;

import java.util.Optional;


public interface Resource<K, V> {

  ResourceResponse<Optional<V>> get(K key, GetRequestOptions options);

  ResourceResponse<Boolean> create(V payload, CreateRequestOptions options);

  ResourceResponse<Boolean> update(V payload, UpdateRequestOptions options);

  ResourceResponse<DeleteStatus> delete(K key, DeleteRequestOptions options);
}
