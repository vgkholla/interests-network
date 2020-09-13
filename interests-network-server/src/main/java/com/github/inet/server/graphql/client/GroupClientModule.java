package com.github.inet.server.graphql.client;

import com.github.inet.service.GroupGrpc;
import com.github.inet.service.GroupGrpc.GroupBlockingStub;
import com.google.inject.AbstractModule;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static com.google.common.base.Preconditions.*;


public class GroupClientModule extends AbstractModule {

  private final String _host;
  private final int _port;

  public GroupClientModule(String host, int port) {
    _host = checkNotNull(host, "Host cannot be null");
    checkArgument(port > 0, "Port should be > 0");
    _port = port;
  }

  @Override
  protected void configure() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext().build();
    bind(GroupBlockingStub.class).toInstance(GroupGrpc.newBlockingStub(channel));
  }
}
