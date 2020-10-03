package com.github.ptracker.resource;

import io.grpc.StatusRuntimeException;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class GrpcResource<KEY_TYPE, VALUE_TYPE> implements Resource<KEY_TYPE, VALUE_TYPE> {

  private final GrpcClient<KEY_TYPE, VALUE_TYPE> _grpcClient;

  public GrpcResource(GrpcClient<KEY_TYPE, VALUE_TYPE> grpcClient) {
    _grpcClient = checkNotNull(grpcClient, "GrpcClient cannot be null");
  }

  @Override
  public ResourceResponse<Optional<VALUE_TYPE>> get(KEY_TYPE key, GetRequestOptions options) {
    checkArgument(key != null, "keyu cannot be null");
    Optional<VALUE_TYPE> value = Optional.empty();
    ResponseStatus status = ResponseStatus.OK;
    try {
      value = Optional.of(_grpcClient.get(key, options));
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Optional<VALUE_TYPE>>().status(status).payload(value).build();
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
    checkArgument(key != null, "keyu cannot be null");
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

    void create(VALUE_TYPE payload, CreateRequestOptions options);

    void update(VALUE_TYPE payload, UpdateRequestOptions options);

    void delete(KEY_TYPE key, DeleteRequestOptions options);
  }
}
