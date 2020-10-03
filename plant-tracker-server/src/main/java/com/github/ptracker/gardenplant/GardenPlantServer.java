package com.github.ptracker.gardenplant;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.VerifierUtils.*;


public class GardenPlantServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "GardenPlantsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "gardenPlants";
  private static final String COSMOS_ID_FIELD_NAME = "id";

  public GardenPlantServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION,
        ServerBuilder.forPort(port).addService(new GardenPlantService(getCosmosResource(cosmosClient))));
  }

  private static void verifyGardenPlant(GardenPlant gardenPlant) {
    verifyStringFieldNotNullOrEmpty(gardenPlant.getId(), GardenPlant.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(gardenPlant.getName(), GardenPlant.class.getName(), "name");
    verifyStringFieldNotNullOrEmpty(gardenPlant.getGardenId(), GardenPlant.class.getName(), "gardenId");
    verifyStringFieldNotNullOrEmpty(gardenPlant.getPlantId(), GardenPlant.class.getName(), "plantId");
  }

  private static Resource<String, GardenPlant> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, GardenPlant> dataInterchange = new ProtoBufJsonInterchange<>(GardenPlant::newBuilder);
    Function<String, GardenPlant> valueWithIdOnlyCreator = key -> GardenPlant.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, GardenPlant> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME, COSMOS_ID_FIELD_NAME,
            dataInterchange, valueWithIdOnlyCreator, GardenPlantServer::verifyGardenPlant);
    return supplier.get();
  }
}
