package com.github.ptracker.server.graphql.client;

import com.github.ptracker.service.PlantGrpc;
import com.github.ptracker.service.PlantGrpc.PlantBlockingStub;
import com.github.ptracker.service.PlantGrpc.PlantFutureStub;
import com.google.inject.AbstractModule;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static com.google.common.base.Preconditions.*;


public class PlantClientModule extends AbstractModule {

  private final String _host;
  private final int _port;

  public PlantClientModule(String host, int port) {
    _host = checkNotNull(host, "Host cannot be null");
    checkArgument(port > 0, "Port should be > 0");
    _port = port;
  }

  @Override
  protected void configure() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
    bind(PlantBlockingStub.class).toInstance(PlantGrpc.newBlockingStub(channel));
    bind(PlantFutureStub.class).toInstance(PlantGrpc.newFutureStub(channel));
  }
}
