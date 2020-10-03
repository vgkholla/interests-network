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

import static com.github.ptracker.VerifierUtils.*;


public class PlantServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "PlantsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "plants";
  private static final String COSMOS_ID_FIELD_NAME = "id";

  public PlantServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION, ServerBuilder.forPort(port).addService(new PlantService(getCosmosResource(cosmosClient))));
  }

  private static void verifyPlant(Plant plant) {
    verifyStringFieldNotNullOrEmpty(plant.getId(), Plant.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(plant.getName(), Plant.class.getName(), "name");
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
