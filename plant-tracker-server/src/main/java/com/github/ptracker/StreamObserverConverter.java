package com.github.ptracker;

import io.grpc.stub.StreamObserver;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;


public class StreamObserverConverter<GET_RESPONSE_TYPE, QUERY_RESPONSE_TYPE>
    implements StreamObserver<QUERY_RESPONSE_TYPE> {
  private final StreamObserver<GET_RESPONSE_TYPE> _streamObserver;
  private final Consumer<QUERY_RESPONSE_TYPE> _queryResponseConsumer;

  public StreamObserverConverter(StreamObserver<GET_RESPONSE_TYPE> streamObserver,
      Consumer<QUERY_RESPONSE_TYPE> queryResponseConsumer) {
    _streamObserver = checkNotNull(streamObserver, "StreamObserver cannot be null");
    _queryResponseConsumer = checkNotNull(queryResponseConsumer, "queryResponseConsumer cannot be null");
  }

  @Override
  public void onNext(QUERY_RESPONSE_TYPE value) {
    _queryResponseConsumer.accept(value);
  }

  @Override
  public void onError(Throwable t) {
    _streamObserver.onError(t);
  }

  @Override
  public void onCompleted() {
    _streamObserver.onCompleted();
  }
}
