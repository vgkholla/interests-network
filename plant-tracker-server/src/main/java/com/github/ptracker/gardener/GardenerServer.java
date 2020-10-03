package com.github.ptracker.gardener;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.VerifierUtils.*;


public class GardenerServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "GardenersService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "gardeners";
  private static final String COSMOS_ID_FIELD_NAME = "id";

  public GardenerServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION, ServerBuilder.forPort(port).addService(new GardenerService(getCosmosResource(cosmosClient))));
  }

  private static void verifyGardener(Gardener gardener) {
    verifyStringFieldNotNullOrEmpty(gardener.getId(), Gardener.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(gardener.getFirstName(), Gardener.class.getName(), "firstName");
    verifyStringFieldNotNullOrEmpty(gardener.getLastName(), Gardener.class.getName(), "lastName");
  }

  private static Resource<String, Gardener> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, Gardener> dataInterchange = new ProtoBufJsonInterchange<>(Gardener::newBuilder);
    Function<String, Gardener> valueWithIdOnlyCreator = key -> Gardener.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, Gardener> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME, COSMOS_ID_FIELD_NAME,
            dataInterchange, valueWithIdOnlyCreator, GardenerServer::verifyGardener);
    return supplier.get();
  }
}
