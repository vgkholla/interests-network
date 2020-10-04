package com.github.ptracker.resource;

import io.grpc.StatusRuntimeException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;


public class GrpcResource<KEY_TYPE, VALUE_TYPE> implements Resource<KEY_TYPE, VALUE_TYPE> {

  private final GrpcClient<KEY_TYPE, VALUE_TYPE> _grpcClient;

  public GrpcResource(GrpcClient<KEY_TYPE, VALUE_TYPE> grpcClient) {
    _grpcClient = checkNotNull(grpcClient, "GrpcClient cannot be null");
  }

  @Override
  public ResourceResponse<VALUE_TYPE> get(KEY_TYPE key, GetRequestOptions options) {
    checkNotNull(key, "key cannot be null");
    VALUE_TYPE value = null;
    ResponseStatus status = ResponseStatus.OK;
    try {
      value = _grpcClient.get(key, options);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<VALUE_TYPE>().status(status).payload(value).build();
  }

  @Override
  public List<ResourceResponse<VALUE_TYPE>> query(VALUE_TYPE template, QueryRequestOptions options) {
    checkNotNull(template, "template cannot be null");
    List<VALUE_TYPE> values = Collections.emptyList();
    ResponseStatus status = ResponseStatus.OK;
    try {
      values = _grpcClient.query(template, options);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    ResponseStatus finalStatus = status;
    return values.stream()
        .map(value -> new ResourceResponseImpl.Builder<VALUE_TYPE>().status(finalStatus).payload(value).build())
        .collect(Collectors.toList());
  }

  @Override
  public ResourceResponse<Void> create(VALUE_TYPE payload, CreateRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    ResponseStatus status = ResponseStatus.OK;
    try {
      _grpcClient.create(payload, options);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  @Override
  public ResourceResponse<Void> update(VALUE_TYPE payload, UpdateRequestOptions options) {
    checkNotNull(payload, "Update payload cannot be null");
    ResponseStatus status = ResponseStatus.OK;
    try {
      _grpcClient.update(payload, options);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  @Override
  public ResourceResponse<Void> delete(KEY_TYPE key, DeleteRequestOptions options) {
    checkNotNull(key, "key cannot be null");
    ResponseStatus status = ResponseStatus.OK;
    try {
      _grpcClient.delete(key, options);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  public interface GrpcClient<KEY_TYPE, VALUE_TYPE> {

    VALUE_TYPE get(KEY_TYPE key, GetRequestOptions options);

    List<VALUE_TYPE> query(VALUE_TYPE template, QueryRequestOptions options);

    void create(VALUE_TYPE payload, CreateRequestOptions options);

    void update(VALUE_TYPE payload, UpdateRequestOptions options);

    void delete(KEY_TYPE key, DeleteRequestOptions options);
  }
}
