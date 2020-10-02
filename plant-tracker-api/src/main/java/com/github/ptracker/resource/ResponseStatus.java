package com.github.ptracker.resource;

import io.grpc.Status;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.*;


public enum ResponseStatus {
  OK(Status.OK),

  NOT_FOUND(Status.NOT_FOUND),

  INTERNAL_ERROR(Status.INTERNAL);

  private static Map<Status, ResponseStatus> STATUS_TO_RESPONSE_STATUS = null;

  private final Status _grpcStatus;

  ResponseStatus(Status grpcStatus) {
    _grpcStatus = checkNotNull(grpcStatus, "grpc status cannot be null");
  }

  public static ResponseStatus fromGrpcStatus(Status grpcStatus) {
    if (STATUS_TO_RESPONSE_STATUS == null) {
      STATUS_TO_RESPONSE_STATUS = new HashMap<>();
      for (ResponseStatus status : ResponseStatus.values()) {
        STATUS_TO_RESPONSE_STATUS.put(status.getGrpcStatus(), status);
      }
    }
    return STATUS_TO_RESPONSE_STATUS.get(grpcStatus);
  }

  public Status getGrpcStatus() {
    return _grpcStatus;
  }
}
