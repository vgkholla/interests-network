package com.github.inet.resource;

import java.util.Optional;


public interface Resource<K, V> {

  Optional<V> get(K key, GetRequestOptions options);

  boolean create(V payload, CreateRequestOptions options);

  boolean upsert(V payload, UpsertRequestOptions options);

  boolean delete(K key, DeleteRequestOptions options);
}
