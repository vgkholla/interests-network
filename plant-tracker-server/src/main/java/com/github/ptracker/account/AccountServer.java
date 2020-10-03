package com.github.ptracker.account;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ptracker.entity.Account;
import com.github.ptracker.interchange.DataInterchange;
import com.github.ptracker.interchange.ProtoBufJsonInterchange;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.service.GrpcCosmosResourceSupplier;
import com.github.ptracker.service.GrpcServer;
import io.grpc.ServerBuilder;
import java.util.function.Function;

import static com.github.ptracker.VerifierUtils.*;


public class AccountServer extends GrpcServer {
  private static final String SERVICE_DESCRIPTION = "AccountsService";

  private static final String COSMOS_DB_NAME = "PlantsTracker";
  private static final String COSMOS_CONTAINER_NAME = "accounts";
  private static final String COSMOS_ID_FIELD_NAME = "id";

  public AccountServer(int port, CosmosClient cosmosClient) {
    super(SERVICE_DESCRIPTION, ServerBuilder.forPort(port).addService(new AccountService(getCosmosResource(cosmosClient))));
  }

  private static void verifyAccount(Account account) {
    verifyStringFieldNotNullOrEmpty(account.getId(), Account.class.getName(), "id");
    verifyStringFieldNotNullOrEmpty(account.getName(), Account.class.getName(), "name");
  }

  private static Resource<String, Account> getCosmosResource(CosmosClient cosmosClient) {
    DataInterchange<ObjectNode, Account> dataInterchange = new ProtoBufJsonInterchange<>(Account::newBuilder);
    Function<String, Account> valueWithIdOnlyCreator = key -> Account.newBuilder().setId(key).build();
    GrpcCosmosResourceSupplier<String, Account> supplier =
        new GrpcCosmosResourceSupplier<>(cosmosClient, COSMOS_DB_NAME, COSMOS_CONTAINER_NAME, COSMOS_ID_FIELD_NAME,
            dataInterchange, valueWithIdOnlyCreator, AccountServer::verifyAccount);
    return supplier.get();
  }
}
