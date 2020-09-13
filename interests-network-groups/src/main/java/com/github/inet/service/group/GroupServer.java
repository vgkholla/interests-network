package com.github.inet.service.group;

import com.github.inet.entity.Group;
import com.github.inet.resource.Resource;
import com.github.inet.service.AbstractGrpcServer;
import io.grpc.ServerBuilder;


public class GroupServer extends AbstractGrpcServer {

  public GroupServer(int port, Resource<String, Group> groupResource) {
    this(port, new GroupService(groupResource));
  }

  public GroupServer(int port, GroupService groupService) {
    super("GroupServer", ServerBuilder.forPort(port).addService(groupService));
  }
}
