package com.github.inet.server;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.github.inet.client.group.GroupClient;
import com.github.inet.common.storage.StorageMetadata;
import com.github.inet.entity.Group;
import com.github.inet.resource.CreateRequestOptionsImpl;
import com.github.inet.resource.DeleteRequestOptionsImpl;
import com.github.inet.resource.GetRequestOptionsImpl;
import com.github.inet.resource.Resource;
import com.github.inet.resource.ResourceResponse;
import com.github.inet.resource.ResponseStatus;
import com.github.inet.resource.UpdateRequestOptionsImpl;
import com.github.inet.resource.group.GroupCosmosResource;
import com.github.inet.server.graphql.GraphQLServer;
import com.github.inet.server.graphql.client.GroupClientModule;
import com.github.inet.server.graphql.client.OverallClientModule;
import com.github.inet.server.graphql.schema.OverallSchemaModule;
import com.github.inet.service.StartStopService;
import com.github.inet.service.group.GroupServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;


public class InetDemo implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(InetDemo.class);

  // Options
  private static final String OPT_COSMOS_DB_ACCOUNT_ENDPOINT = "cosmosDBAccountEndpoint";
  private static final String OPT_COSMOS_DB_ACCOUNT_KEY = "cosmosDBAccountKey";
  private static final String OPT_COSMOS_DB_PREFERRED_REGIONS = "cosmosDBPreferredRegions";

  // ports
  private static final int GRAPHQL_SERVER_PORT = 8080;
  private static final int GROUP_SERVICE_PORT = 30000;

  // cosmos testing
  private static final boolean COSMOS_TESTING = false;

  private final CosmosClient _cosmosClient;
  private final Resource<String, Group> _groupResource;
  private final List<StartStopService> _services;

  public Resource<String, Group> getGroupResource() {
    return _groupResource;
  }

  public void awaitServicesTermination() throws InterruptedException {
    for (StartStopService service : _services) {
      service.awaitTermination();
    }
  }

  @Override
  public void close() {
    _services.forEach(StartStopService::stop);
    _cosmosClient.close();
  }

  private CosmosClient createCosmosClient(CosmosDBConfiguration configuration) {
    checkNotNull(configuration, "CosmosDBConfiguration cannot be null");
    checkArgument(
        configuration.getCosmosDBAccountEndpoint() != null && !configuration.getCosmosDBAccountEndpoint().isEmpty(),
        "Need a CosmosDB account endpoint");
    checkArgument(configuration.getCosmosDBAccountKey() != null && !configuration.getCosmosDBAccountKey().isEmpty(),
        "Need a CosmosDB account key");
    checkArgument(configuration.getPreferredRegionsList() != null && !configuration.getPreferredRegionsList().isEmpty(),
        "Need CosmosDB preferred regions");
    return new CosmosClientBuilder().endpoint(configuration.getCosmosDBAccountEndpoint())
        .key(configuration.getCosmosDBAccountKey())
        .preferredRegions(configuration.getPreferredRegionsList())
        .consistencyLevel(ConsistencyLevel.SESSION)
        .buildClient();
  }

  private List<StartStopService> createServices() {
    List<StartStopService> services = new ArrayList<>();

    // groups backend
    Resource<String, Group> groupResource = new GroupCosmosResource(_cosmosClient);
    services.add(new GroupServer(GROUP_SERVICE_PORT, groupResource));

    if (!COSMOS_TESTING) {
      // graphql server
      OverallSchemaModule schemaModule = new OverallSchemaModule();
      // TODO: maybe the schema module should return the client module(s)?
      //     : then the server would only take the schema module, get the client module and from there, the data loaders
      OverallClientModule clientModule =
          new OverallClientModule(Collections.singleton(new GroupClientModule("localhost", GROUP_SERVICE_PORT)));
      services.add(new GraphQLServer(GRAPHQL_SERVER_PORT, clientModule, schemaModule));
    }

    return services;
  }

  private InetDemo(InetServerInitializationParams params) throws IOException {
    checkNotNull(params, "Initialization params cannot be null");
    _cosmosClient = createCosmosClient(params.getCosmosDBConfiguration());
    _services = createServices();

    for (StartStopService service : _services) {
      service.start();
    }

    _groupResource = new GroupClient("localhost", GROUP_SERVICE_PORT);
  }

  public static void main(String[] args) throws Exception {
    int numThreads = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    try {
      try (InetDemo inetDemo = new InetDemo(getInitParams(args))) {
        if (COSMOS_TESTING) {
          List<Callable<Void>> callables =
              IntStream.range(0, numThreads).boxed().map(ignored -> (Callable<Void>) () -> {
                groupsCRUDDemo(inetDemo.getGroupResource());
                return null;
              }).collect(Collectors.toList());
          List<Future<Void>> futures = executorService.invokeAll(callables);
          for (Future<Void> future : futures) {
            future.get();
          }
        } else {
          inetDemo.awaitServicesTermination();
        }
      }
    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  private static InetServerInitializationParams getInitParams(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    getCosmosDBOptions().getOptions().forEach(options::addOption);
    CommandLine commandLine = parser.parse(options, args);

    InetServerInitializationParams.Builder builder = InetServerInitializationParams.newBuilder();
    builder.setCosmosDBConfiguration(getCosmosDBConfiguration(commandLine));
    return builder.build();
  }

  private static Options getCosmosDBOptions() {
    Options options = new Options();
    options.addOption(Option.builder()
        .longOpt(OPT_COSMOS_DB_ACCOUNT_ENDPOINT)
        .desc("Cosmos DB Account endpoint")
        .required()
        .hasArg()
        .argName("COSMOS_DB_ACCOUNT_ENDPOINT")
        .build());
    options.addOption(Option.builder()
        .longOpt(OPT_COSMOS_DB_ACCOUNT_KEY)
        .desc("Cosmos DB Account key")
        .required()
        .hasArg()
        .argName("COSMOS_DB_ACCOUNT_KEY")
        .build());
    options.addOption(Option.builder()
        .longOpt(OPT_COSMOS_DB_PREFERRED_REGIONS)
        .desc("Comma separated list of preferred regions")
        .required()
        .hasArg()
        .argName("COSMOS_DB_ACCOUNT_PREFERRED_REGION1,COSMOS_DB_ACCOUNT_PREFERRED_REGION2")
        .valueSeparator(',')
        .build());
    return options;
  }

  private static CosmosDBConfiguration getCosmosDBConfiguration(CommandLine commandLine) {
    CosmosDBConfiguration.Builder builder = CosmosDBConfiguration.newBuilder();
    builder.setCosmosDBAccountEndpoint(commandLine.getOptionValue(OPT_COSMOS_DB_ACCOUNT_ENDPOINT));
    builder.setCosmosDBAccountKey(commandLine.getOptionValue(OPT_COSMOS_DB_ACCOUNT_KEY));
    for (String region : commandLine.getOptionValues(OPT_COSMOS_DB_PREFERRED_REGIONS)) {
      builder.addPreferredRegions(region);
    }
    return builder.build();
  }

  private static void groupsCRUDDemo(Resource<String, Group> groupResource) {
    long id = UUID.randomUUID().getLeastSignificantBits();
    String groupId = "inet:group:" + id;

    // create a group
    Group group = Group.newBuilder().setId(groupId).setName("Demo").build();
    LOGGER.info("Creating group [\n{}]", group);
    ResourceResponse<Void> createResponse = groupResource.create(group, new CreateRequestOptionsImpl());
    if (ResponseStatus.OK.equals(createResponse.getStatus())) {
      LOGGER.info("Group created !");
    } else {
      throw new IllegalStateException("Could not create group");
    }

    StorageMetadata metadata = createResponse.getStorageMetadata();
    // get the group
    LOGGER.info("Fetching created group");
    ResourceResponse<Optional<Group>> getAfterCreateResponse =
        groupResource.get(groupId, new GetRequestOptionsImpl.Builder().metadata(metadata).build());
    getAfterCreateResponse.getPayload().map(payload -> {
      LOGGER.info("Fetched group [\n{}]", getAfterCreateResponse.getPayload().orElse(null));
      return payload;
    }).orElseThrow(() -> new IllegalStateException("Could not fetch group"));

    // update the group
    metadata = getAfterCreateResponse.getStorageMetadata();
    group = Group.newBuilder(group).setName("DemoUpdated").build();
    LOGGER.info("Updating group to [\n{}]", group);
    ResourceResponse<Void> updateResponse = groupResource.update(group,
        new UpdateRequestOptionsImpl.Builder().metadata(metadata).shouldUpsert(true).build());
    if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
      LOGGER.info("Group updated !");
    } else {
      throw new IllegalStateException("Could not update group");
    }

    // get the group
    metadata = updateResponse.getStorageMetadata();
    LOGGER.info("Fetching updated group");
    ResourceResponse<Optional<Group>> getAfterUpdateResponse =
        groupResource.get(groupId, new GetRequestOptionsImpl.Builder().metadata(metadata).build());
    getAfterUpdateResponse.getPayload().map(payload -> {
      LOGGER.info("Fetched group [\n{}]", getAfterUpdateResponse.getPayload().orElse(null));
      return payload;
    }).orElseThrow(() -> new IllegalStateException("Could not fetch group"));

    // delete the group
    metadata = getAfterUpdateResponse.getStorageMetadata();
    LOGGER.info("Deleting group");
    ResourceResponse<Void> deleteResponse =
        groupResource.delete(groupId, new DeleteRequestOptionsImpl.Builder().metadata(metadata).build());
    if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
      LOGGER.info("Group deleted !");
    } else {
      throw new IllegalStateException("Could not delete group. Received status: " + deleteResponse.getPayload());
    }
  }
}
