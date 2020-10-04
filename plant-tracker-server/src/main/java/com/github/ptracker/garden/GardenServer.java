package com.github.ptracker.garden;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.VerifierUtils.*;


public class GardenServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "GardensService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "gardens";

  public GardenServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION, ServerBuilder.forPort(port).addService(new GardenService(getCosmosResource(cosmosClient))));
  }

  private static void verifyGarden(Garden garden) {
    verifyStringFieldNotNullOrEmpty(garden.getId(), Garden.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(garden.getName(), Garden.class.getName(), "name");
    verifyStringFieldNotNullOrEmpty(garden.getAccountId(), Garden.class.getName(), "accountId");
  }

  private static Resource<String, Garden> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, Garden> dataInterchange = new ProtoBufJsonInterchange<>(Garden::newBuilder);
    Function<String, Garden> valueWithIdOnlyCreator = key -> Garden.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, Garden> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME,
            dataInterchange, valueWithIdOnlyCreator, GardenServer::verifyGarden);
    return supplier.get();
  }
}
