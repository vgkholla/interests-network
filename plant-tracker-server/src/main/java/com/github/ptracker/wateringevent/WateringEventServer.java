package com.github.ptracker.wateringevent;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.ModelVerifierUtils.*;


public class WateringEventServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "WateringEventsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "wateringEvents";

  public WateringEventServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION,
        ServerBuilder.forPort(port).addService(new WateringEventService(getCosmosResource(cosmosClient))));
  }

  private static void verifyWateringEvent(WateringEvent wateringEvent) {
    verifyStringFieldNotNullOrEmpty(wateringEvent.getId(), WateringEvent.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(wateringEvent.getGardenPlantId(), WateringEvent.class.getName(), "gardenPlantId");
    verifyIntNotNegative(wateringEvent.getQuantityMl(), WateringEvent.class.getName(), "quantityMl");
    verifyEventMetadata(wateringEvent.getMetadata(), WateringEvent.class.getName(), "metadata");
  }

  private static Resource<String, WateringEvent> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, WateringEvent> dataInterchange =
        new ProtoBufJsonInterchange<>(WateringEvent::newBuilder);
    Function<String, WateringEvent> valueWithIdOnlyCreator = key -> WateringEvent.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, WateringEvent> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME,
            dataInterchange, valueWithIdOnlyCreator, WateringEventServer::verifyWateringEvent);
    return supplier.get();
  }
}
