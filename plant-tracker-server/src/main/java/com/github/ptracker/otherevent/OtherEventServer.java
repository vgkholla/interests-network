package com.github.ptracker.otherevent;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.OtherEvent;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.ModelVerifierUtils.*;


public class OtherEventServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "OtherEventsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "otherEvents";

  public OtherEventServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION,
        ServerBuilder.forPort(port).addService(new OtherEventService(getCosmosResource(cosmosClient))));
  }

  private static void verifyOtherEvent(OtherEvent otherEvent) {
    verifyStringFieldNotNullOrEmpty(otherEvent.getId(), OtherEvent.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(otherEvent.getDescription(), OtherEvent.class.getName(), "description");
    verifyStringFieldNotNullOrEmpty(otherEvent.getGardenPlantId(), OtherEvent.class.getName(), "gardenPlantId");
    verifyEventMetadata(otherEvent.getMetadata(), OtherEvent.class.getName(), "metadata");
  }

  private static Resource<String, OtherEvent> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, OtherEvent> dataInterchange =
        new ProtoBufJsonInterchange<>(OtherEvent::newBuilder);
    Function<String, OtherEvent> valueWithIdOnlyCreator = key -> OtherEvent.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, OtherEvent> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME,
            dataInterchange, valueWithIdOnlyCreator, OtherEventServer::verifyOtherEvent);
    return supplier.get();
  }
}
