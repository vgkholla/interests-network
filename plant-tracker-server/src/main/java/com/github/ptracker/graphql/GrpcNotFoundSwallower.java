package com.github.ptracker.graphql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.function.Function;

import static com.google.common.base.Preconditions.*;


public class GrpcNotFoundSwallower<INPUT_TYPE, OUTPUT_TYPE> implements Function<INPUT_TYPE, ListenableFuture<OUTPUT_TYPE>> {
  private final Function<INPUT_TYPE, ListenableFuture<OUTPUT_TYPE>> _throwingFunction;
  private final OUTPUT_TYPE _defaultValue;

  public GrpcNotFoundSwallower(Function<INPUT_TYPE, ListenableFuture<OUTPUT_TYPE>> throwingFunction) {
    this(throwingFunction, null);
  }

  public GrpcNotFoundSwallower(Function<INPUT_TYPE, ListenableFuture<OUTPUT_TYPE>> throwingFunction, OUTPUT_TYPE defaultValue) {
    _throwingFunction = checkNotNull(throwingFunction, "Function cannot be null");
    _defaultValue = defaultValue;
  }

  @Override
  public ListenableFuture<OUTPUT_TYPE> apply(INPUT_TYPE input) {
    ListenableFuture<OUTPUT_TYPE> output;
    try {
      output = Futures.catching(_throwingFunction.apply(input), StatusRuntimeException.class, this::fallback,
          MoreExecutors.directExecutor());
    } catch (StatusRuntimeException e) {
      output = Futures.immediateFuture(fallback(e));
    }
    return output;
  }

  private OUTPUT_TYPE fallback(StatusRuntimeException e) {
    if (!e.getStatus().getCode().equals(Status.Code.NOT_FOUND)) {
      throw e;
    }
    return _defaultValue;
  }
}
