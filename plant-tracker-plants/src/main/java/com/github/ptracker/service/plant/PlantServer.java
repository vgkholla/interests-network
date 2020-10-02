package com.github.ptracker.service.plant;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.AbstractGrpcServer;
import io.grpc.ServerBuilder;


public class PlantServer extends AbstractGrpcServer {

  public PlantServer(int port, Resource<String, Plant> plantResource) {
    this(port, new PlantService(plantResource));
  }

  public PlantServer(int port, PlantService plantService) {
    super("PlantServer", ServerBuilder.forPort(port).addService(plantService));
  }
}
