package com.github.ptracker.space;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.Space;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.ModelVerifierUtils.*;


public class SpaceServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "SpacesService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "spaces";

  public SpaceServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION,
        ServerBuilder.forPort(port).addService(new SpaceService(getCosmosResource(cosmosClient))));
  }

  private static void verifySpace(Space space) {
    verifyStringFieldNotNullOrEmpty(space.getId(), Space.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(space.getName(), Space.class.getName(), "name");
  }

  private static Resource<String, Space> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, Space> dataInterchange = new ProtoBufJsonInterchange<>(Space::newBuilder);
    Function<String, Space> valueWithIdOnlyCreator = key -> Space.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, Space> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME, dataInterchange,
            valueWithIdOnlyCreator, SpaceServer::verifySpace);
    return supplier.get();
  }
}
