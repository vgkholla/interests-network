package com.github.ptracker.app;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.*;


public class ApolloClientCallback<RESPONSE_TYPE, OUTPUT_TYPE> extends ApolloCall.Callback<RESPONSE_TYPE> {
  private final Function<RESPONSE_TYPE, OUTPUT_TYPE> _responseToOutput;
  private final CompletableFuture<OUTPUT_TYPE> _future = new CompletableFuture<>();

  public ApolloClientCallback(Function<RESPONSE_TYPE, OUTPUT_TYPE> responseToOutput) {
    _responseToOutput = checkNotNull(responseToOutput, "responseToOutput function cannot be null");
  }

  @Override
  public void onResponse(@NotNull Response<RESPONSE_TYPE> response) {
    if (response.hasErrors()) {
      _future.completeExceptionally(new IllegalStateException("Encountered errors: " + response.getErrors()));
    } else if (response.getData() == null) {
      _future.complete(null);
    } else {
      _future.complete(_responseToOutput.apply(response.getData()));
    }
  }

  @Override
  public void onFailure(@NotNull ApolloException e) {
    _future.completeExceptionally(e);
  }

  public OUTPUT_TYPE getNonNullOrThrow() {
    return get().orElseThrow(() -> new IllegalStateException("Data is null"));
  }

  public Optional<OUTPUT_TYPE> get() {
    try {
      return Optional.ofNullable(_future.get());
    } catch (ExecutionException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public OUTPUT_TYPE getNonNullOrThrow(int timeout, TimeUnit timeUnit) {
    return get(timeout, timeUnit).orElseThrow(() -> new IllegalStateException("Data is null"));
  }

  public Optional<OUTPUT_TYPE> get(int timeout, TimeUnit timeUnit) {
    try {
      return Optional.ofNullable(_future.get(timeout, timeUnit));
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      throw new IllegalStateException(e);
    }
  }
}
