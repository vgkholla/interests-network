package com.github.ptracker.noteevent;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.ModelVerifierUtils.*;


public class NoteEventServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "NoteEventsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "noteEvents";

  public NoteEventServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION,
        ServerBuilder.forPort(port).addService(new NoteEventService(getCosmosResource(cosmosClient))));
  }

  private static void verifyNoteEvent(NoteEvent noteEvent) {
    verifyStringFieldNotNullOrEmpty(noteEvent.getId(), NoteEvent.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(noteEvent.getDescription(), NoteEvent.class.getName(), "description");
    verifyStringFieldNotNullOrEmpty(noteEvent.getGardenPlantId(), NoteEvent.class.getName(), "gardenPlantId");
    verifyEventMetadata(noteEvent.getMetadata(), NoteEvent.class.getName(), "metadata");
  }

  private static Resource<String, NoteEvent> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, NoteEvent> dataInterchange =
        new ProtoBufJsonInterchange<>(NoteEvent::newBuilder);
    Function<String, NoteEvent> valueWithIdOnlyCreator = key -> NoteEvent.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, NoteEvent> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME,
            dataInterchange, valueWithIdOnlyCreator, NoteEventServer::verifyNoteEvent);
    return supplier.get();
  }
}
