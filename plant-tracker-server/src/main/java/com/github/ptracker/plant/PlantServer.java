package com.github.ptracker.plant;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;


public class PlantServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "PlantsService";

  private static final String COSMOS_DB_NAME = "Plants";
  private static final String COSMOS_CONTAINER_NAME = "plants";
  private static final String COSMOS_ID_FIELD_NAME = "id";

  public PlantServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION, ServerBuilder.forPort(port).addService(new PlantService(getCosmosResource(cosmosClient))));
  }

  private static void verifyPlant(Plant plant) {
    if (plant.getId() == null || plant.getId().isEmpty()) {
      throw new IllegalArgumentException("Plant does not have an ID");
    }
    if (plant.getName() == null || plant.getName().isEmpty()) {
      throw new IllegalArgumentException("Plant does not have a name");
    }
  }

  private static Resource<String, Plant> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, Plant> dataInterchange = new ProtoBufJsonInterchange<>(Plant::newBuilder);
    Function<String, Plant> valueWithIdOnlyCreator = key -> Plant.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, Plant> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME, COSMOS_ID_FIELD_NAME,
            dataInterchange, valueWithIdOnlyCreator, PlantServer::verifyPlant);
    return supplier.get();
  }
}
