package com.github.ptracker.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;


public abstract class AbstractGrpcServer implements StartStopService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGrpcServer.class);

  private final String _description;
  private final Server _server;

  protected AbstractGrpcServer(String description, ServerBuilder<?> serverBuilder) {
    _description = checkNotNull(description, "Description cannot be null");
    _server = serverBuilder.build();
  }

  @Override
  public void start() throws IOException {
    LOGGER.info("Starting {}", _description);
    _server.start();
    LOGGER.info("{} started, listening on {}", _description, _server.getPort());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (!AbstractGrpcServer.this.isShutdown()) {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("Shutting down " + _description + " since JVM is shutting down");
        try {
          AbstractGrpcServer.this.stop();
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
        System.err.println(_description + " shut down");
      }
    }));
  }

  @Override
  public void stop() {
    if (_server != null) {
      try {
        LOGGER.info("Stopping {}", _description);
        _server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        LOGGER.info("Stopped {}", _description);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public void shutdownNow() {
    if (_server != null) {
      _server.shutdownNow();
    }
  }

  @Override
  public boolean isShutdown() {
    return _server == null || _server.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return _server == null || _server.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return _server == null || _server.awaitTermination(timeout, unit);
  }

  @Override
  public void awaitTermination() throws InterruptedException {
    if (_server != null) {
      _server.awaitTermination();
    }
  }
}
