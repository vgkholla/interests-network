package com.github.inet.resource;

import java.util.Optional;


public interface Resource<K, V> {

  ResourceResponse<Optional<V>> get(K key, GetRequestOptions options);

  ResourceResponse<Void> create(V payload, CreateRequestOptions options);

  ResourceResponse<Void> update(V payload, UpdateRequestOptions options);

  ResourceResponse<Void> delete(K key, DeleteRequestOptions options);
}
