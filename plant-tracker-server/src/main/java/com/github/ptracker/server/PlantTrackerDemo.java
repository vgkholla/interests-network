package com.github.ptracker.server;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.github.ptracker.account.AccountServer;
import com.github.ptracker.common.storage.StorageMetadata;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.fertilizationevent.FertilizationEventServer;
import com.github.ptracker.garden.GardenServer;
import com.github.ptracker.gardener.GardenerServer;
import com.github.ptracker.gardenplant.GardenPlantServer;
import com.github.ptracker.graphql.GraphQLServer;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.graphql.provider.AccountModuleProvider;
import com.github.ptracker.graphql.provider.EventMetadataModuleProvider;
import com.github.ptracker.graphql.provider.FertilizationEventModuleProvider;
import com.github.ptracker.graphql.provider.FullGraphProvider;
import com.github.ptracker.graphql.provider.GardenModuleProvider;
import com.github.ptracker.graphql.provider.GardenPlantModuleProvider;
import com.github.ptracker.graphql.provider.GardenerModuleProvider;
import com.github.ptracker.graphql.provider.OtherEventModuleProvider;
import com.github.ptracker.graphql.provider.PlantModuleProvider;
import com.github.ptracker.graphql.provider.WateringEventModuleProvider;
import com.github.ptracker.otherevent.OtherEventServer;
import com.github.ptracker.plant.PlantClient;
import com.github.ptracker.plant.PlantServer;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.StartStopService;
import com.github.ptracker.wateringevent.WateringEventServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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


public class PlantTrackerDemo implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlantTrackerDemo.class);

  // Options
  private static final String OPT_COSMOS_DB_ACCOUNT_ENDPOINT = "cosmosDBAccountEndpoint";
  private static final String OPT_COSMOS_DB_ACCOUNT_KEY = "cosmosDBAccountKey";
  private static final String OPT_COSMOS_DB_PREFERRED_REGIONS = "cosmosDBPreferredRegions";
  private static final String OPT_GQL_SERVER_STATIC_RESOURCES_PATH = "graphQLServerStaticResourcesPath";

  // ports
  private static final int GRAPHQL_SERVER_PORT = 8080;
  private static final int ACCOUNT_SERVICE_PORT = 30000;
  private static final int FERTILIZATION_EVENT_SERVICE_PORT = 30001;
  private static final int GARDEN_SERVICE_PORT = 30002;
  private static final int GARDENER_SERVICE_PORT = 30003;
  private static final int GARDEN_PLANT_SERVICE_PORT = 30004;
  private static final int OTHER_EVENT_SERVICE_PORT = 30005;
  private static final int PLANT_SERVICE_PORT = 30006;
  private static final int WATERING_EVENT_SERVICE_PORT = 30007;

  // cosmos testing
  private static final boolean COSMOS_TESTING = false;

  private final CosmosClient _cosmosClient;
  private final Resource<String, Plant> _plantResource;
  private final List<StartStopService> _services;

  public Resource<String, Plant> getPlantResource() {
    return _plantResource;
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
    checkArgument(configuration.getAccountEndpoint() != null && !configuration.getAccountEndpoint().isEmpty(),
        "Need a CosmosDB account endpoint");
    checkArgument(configuration.getAccountKey() != null && !configuration.getAccountKey().isEmpty(),
        "Need a CosmosDB account key");
    checkArgument(configuration.getPreferredRegionsList() != null && !configuration.getPreferredRegionsList().isEmpty(),
        "Need CosmosDB preferred regions");
    return new CosmosClientBuilder().endpoint(configuration.getAccountEndpoint())
        .key(configuration.getAccountKey())
        .preferredRegions(configuration.getPreferredRegionsList())
        .consistencyLevel(ConsistencyLevel.SESSION)
        .buildClient();
  }

  private List<StartStopService> createServices(GraphQLServerConfiguration graphQLServerConfiguration) {
    List<StartStopService> services = new ArrayList<>();

    // servers
    services.add(new AccountServer(ACCOUNT_SERVICE_PORT, _cosmosClient));
    services.add(new FertilizationEventServer(FERTILIZATION_EVENT_SERVICE_PORT, _cosmosClient));
    services.add(new GardenServer(GARDEN_SERVICE_PORT, _cosmosClient));
    services.add(new GardenerServer(GARDENER_SERVICE_PORT, _cosmosClient));
    services.add(new GardenPlantServer(GARDEN_PLANT_SERVICE_PORT, _cosmosClient));
    services.add(new OtherEventServer(OTHER_EVENT_SERVICE_PORT, _cosmosClient));
    services.add(new PlantServer(PLANT_SERVICE_PORT, _cosmosClient));
    services.add(new WateringEventServer(WATERING_EVENT_SERVICE_PORT, _cosmosClient));

    if (!COSMOS_TESTING) {
      // GraphQL module providers
      List<GraphQLModuleProvider> moduleProviders = new ArrayList<>();

      // entitites
      moduleProviders.add(new AccountModuleProvider("localhost", ACCOUNT_SERVICE_PORT));
      moduleProviders.add(new FertilizationEventModuleProvider("localhost", FERTILIZATION_EVENT_SERVICE_PORT));
      moduleProviders.add(new GardenModuleProvider("localhost", GARDEN_SERVICE_PORT));
      moduleProviders.add(new GardenerModuleProvider("localhost", GARDENER_SERVICE_PORT));
      moduleProviders.add(new GardenPlantModuleProvider("localhost", GARDEN_PLANT_SERVICE_PORT));
      moduleProviders.add(new OtherEventModuleProvider("localhost", OTHER_EVENT_SERVICE_PORT));
      moduleProviders.add(new PlantModuleProvider("localhost", PLANT_SERVICE_PORT));
      moduleProviders.add(new WateringEventModuleProvider("localhost", WATERING_EVENT_SERVICE_PORT));

      // common models
      moduleProviders.add(new EventMetadataModuleProvider());

      // graphql server
      GraphQLModuleProvider fullGraphProvider = new FullGraphProvider(moduleProviders);
      services.add(new GraphQLServer(GRAPHQL_SERVER_PORT, fullGraphProvider,
          graphQLServerConfiguration.getStaticResourcesPath()));
    }

    return services;
  }

  private PlantTrackerDemo(PlantTrackerServerInitializationParams params) throws IOException {
    checkNotNull(params, "Initialization params cannot be null");
    _cosmosClient = createCosmosClient(params.getCosmosDBConfiguration());
    _services = createServices(params.getGraphQLServerConfiguration());

    for (StartStopService service : _services) {
      service.start();
    }

    _plantResource = new GrpcResource<>(new PlantClient("localhost", PLANT_SERVICE_PORT));
  }

  public static void main(String[] args) throws Exception {
    int numThreads = 1;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    try {
      try (PlantTrackerDemo ptrackerDemo = new PlantTrackerDemo(getInitParams(args))) {
        if (COSMOS_TESTING) {
          List<Callable<Void>> callables =
              IntStream.range(0, numThreads).boxed().map(ignored -> (Callable<Void>) () -> {
                plantsCRUDDemo(ptrackerDemo.getPlantResource());
                return null;
              }).collect(Collectors.toList());
          List<Future<Void>> futures = executorService.invokeAll(callables);
          for (Future<Void> future : futures) {
            future.get();
          }
        } else {
          ptrackerDemo.awaitServicesTermination();
        }
      }
    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  private static PlantTrackerServerInitializationParams getInitParams(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    getCosmosDBOptions().getOptions().forEach(options::addOption);
    getGQLServerOptions().getOptions().forEach(options::addOption);
    CommandLine commandLine = parser.parse(options, args);

    PlantTrackerServerInitializationParams.Builder builder = PlantTrackerServerInitializationParams.newBuilder();
    builder.setCosmosDBConfiguration(getCosmosDBConfiguration(commandLine));
    builder.setGraphQLServerConfiguration(getGraphQLServerConfiguration(commandLine));
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
    builder.setAccountEndpoint(commandLine.getOptionValue(OPT_COSMOS_DB_ACCOUNT_ENDPOINT));
    builder.setAccountKey(commandLine.getOptionValue(OPT_COSMOS_DB_ACCOUNT_KEY));
    for (String region : commandLine.getOptionValues(OPT_COSMOS_DB_PREFERRED_REGIONS)) {
      builder.addPreferredRegions(region);
    }
    return builder.build();
  }

  private static Options getGQLServerOptions() {
    Options options = new Options();
    options.addOption(Option.builder()
        .longOpt(OPT_GQL_SERVER_STATIC_RESOURCES_PATH)
        .desc("Path to static resources that the GraphQL server will serve")
        .required(false)
        .hasArg()
        .argName("GQL_SERVER_STATIC_RESOURCES_PATH")
        .build());
    return options;
  }

  private static GraphQLServerConfiguration getGraphQLServerConfiguration(CommandLine commandLine) {
    GraphQLServerConfiguration.Builder builder = GraphQLServerConfiguration.newBuilder();
    String staticResourcesPath = commandLine.getOptionValue(OPT_GQL_SERVER_STATIC_RESOURCES_PATH, "src/main/resources");
    builder.setStaticResourcesPath(staticResourcesPath);
    return builder.build();
  }

  private static void plantsCRUDDemo(Resource<String, Plant> plantResource) {
    long id = UUID.randomUUID().getLeastSignificantBits();
    String plantId = "ptracker:plant:" + id;

    // create a plant
    Plant plant = Plant.newBuilder().setId(plantId).setName("Demo").build();
    LOGGER.info("Creating plant [\n{}]", plant);
    ResourceResponse<Void> createResponse = plantResource.create(plant, new CreateRequestOptionsImpl());
    if (ResponseStatus.OK.equals(createResponse.getStatus())) {
      LOGGER.info("Plant created !");
    } else {
      throw new IllegalStateException("Could not create plant");
    }

    StorageMetadata metadata = createResponse.getStorageMetadata();
    // get the plant
    LOGGER.info("Fetching created plant");
    ResourceResponse<Plant> getAfterCreateResponse =
        plantResource.get(plantId, new GetRequestOptionsImpl.Builder().metadata(metadata).build());
    if (getAfterCreateResponse.getStatus().equals(ResponseStatus.NOT_FOUND)) {
      throw new IllegalStateException("Could not fetch plant");
    }
    LOGGER.info("Fetched plant [\n{}]", getAfterCreateResponse.getPayload());

    // update the plant
    metadata = getAfterCreateResponse.getStorageMetadata();
    plant = Plant.newBuilder(plant).setName("DemoUpdated").build();
    LOGGER.info("Updating plant to [\n{}]", plant);
    ResourceResponse<Void> updateResponse = plantResource.update(plant,
        new UpdateRequestOptionsImpl.Builder().metadata(metadata).shouldUpsert(true).build());
    if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
      LOGGER.info("Plant updated !");
    } else {
      throw new IllegalStateException("Could not update plant");
    }

    // get the plant
    metadata = updateResponse.getStorageMetadata();
    LOGGER.info("Fetching updated plant");
    ResourceResponse<Plant> getAfterUpdateResponse =
        plantResource.get(plantId, new GetRequestOptionsImpl.Builder().metadata(metadata).build());
    if (getAfterCreateResponse.getStatus().equals(ResponseStatus.NOT_FOUND)) {
      throw new IllegalStateException("Could not fetch plant");
    }
    LOGGER.info("Fetched plant [\n{}]", getAfterUpdateResponse.getPayload());

    // delete the plant
    metadata = getAfterUpdateResponse.getStorageMetadata();
    LOGGER.info("Deleting plant");
    ResourceResponse<Void> deleteResponse =
        plantResource.delete(plantId, new DeleteRequestOptionsImpl.Builder().metadata(metadata).build());
    if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
      LOGGER.info("Plant deleted !");
    } else {
      throw new IllegalStateException("Could not delete plant. Received status: " + deleteResponse.getPayload());
    }
  }
}
