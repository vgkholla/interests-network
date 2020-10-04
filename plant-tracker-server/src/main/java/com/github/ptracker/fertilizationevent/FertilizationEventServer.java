package com.github.ptracker.fertilizationevent;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.ModelVerifierUtils.*;


public class FertilizationEventServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "FertilizationEventsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "fertilizationEvents";

  public FertilizationEventServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION,
        ServerBuilder.forPort(port).addService(new FertilizationEventService(getCosmosResource(cosmosClient))));
  }

  private static void verifyFertilizationEvent(FertilizationEvent fertilizationEvent) {
    verifyStringFieldNotNullOrEmpty(fertilizationEvent.getId(), FertilizationEvent.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(fertilizationEvent.getGardenPlantId(), FertilizationEvent.class.getName(), "gardenPlantId");
    verifyIntNotNegative(fertilizationEvent.getQuantityMg(), FertilizationEvent.class.getName(), "quantityMg");
    verifyEventMetadata(fertilizationEvent.getMetadata(), FertilizationEvent.class.getName(), "metadata");
    // TODO quantityMg, gardenPlantId, event metadata
  }

  private static Resource<String, FertilizationEvent> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, FertilizationEvent> dataInterchange =
        new ProtoBufJsonInterchange<>(FertilizationEvent::newBuilder);
    Function<String, FertilizationEvent> valueWithIdOnlyCreator =
        key -> FertilizationEvent.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, FertilizationEvent> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME,
            dataInterchange, valueWithIdOnlyCreator, FertilizationEventServer::verifyFertilizationEvent);
    return supplier.get();
  }
}
