package com.github.ptracker.server;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.github.ptracker.common.storage.StorageMetadata;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.graphql.provider.FullGraphProvider;
import com.github.ptracker.graphql.GraphQLServer;
import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.graphql.provider.PlantModuleProvider;
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


public class PlantTrackerDemo implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlantTrackerDemo.class);

  // Options
  private static final String OPT_COSMOS_DB_ACCOUNT_ENDPOINT = "cosmosDBAccountEndpoint";
  private static final String OPT_COSMOS_DB_ACCOUNT_KEY = "cosmosDBAccountKey";
  private static final String OPT_COSMOS_DB_PREFERRED_REGIONS = "cosmosDBPreferredRegions";

  // ports
  private static final int GRAPHQL_SERVER_PORT = 8080;
  private static final int PLANT_SERVICE_PORT = 30000;

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

    // plants backend
    services.add(new PlantServer(PLANT_SERVICE_PORT, _cosmosClient));

    if (!COSMOS_TESTING) {
      // graphql server
      GraphQLModuleProvider fullGraphProvider =
          new FullGraphProvider(Collections.singleton(new PlantModuleProvider("localhost", PLANT_SERVICE_PORT)));
      services.add(new GraphQLServer(GRAPHQL_SERVER_PORT, fullGraphProvider));
    }

    return services;
  }

  private PlantTrackerDemo(PlantTrackerServerInitializationParams params) throws IOException {
    checkNotNull(params, "Initialization params cannot be null");
    _cosmosClient = createCosmosClient(params.getCosmosDBConfiguration());
    _services = createServices();

    for (StartStopService service : _services) {
      service.start();
    }

    _plantResource = new GrpcResource<>(new PlantClient("localhost", PLANT_SERVICE_PORT));
  }

  public static void main(String[] args) throws Exception {
    int numThreads = 10;
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
    CommandLine commandLine = parser.parse(options, args);

    PlantTrackerServerInitializationParams.Builder builder = PlantTrackerServerInitializationParams.newBuilder();
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
    ResourceResponse<Optional<Plant>> getAfterCreateResponse =
        plantResource.get(plantId, new GetRequestOptionsImpl.Builder().metadata(metadata).build());
    getAfterCreateResponse.getPayload().map(payload -> {
      LOGGER.info("Fetched plant [\n{}]", getAfterCreateResponse.getPayload().orElse(null));
      return payload;
    }).orElseThrow(() -> new IllegalStateException("Could not fetch plant"));

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
    ResourceResponse<Optional<Plant>> getAfterUpdateResponse =
        plantResource.get(plantId, new GetRequestOptionsImpl.Builder().metadata(metadata).build());
    getAfterUpdateResponse.getPayload().map(payload -> {
      LOGGER.info("Fetched plant [\n{}]", getAfterUpdateResponse.getPayload().orElse(null));
      return payload;
    }).orElseThrow(() -> new IllegalStateException("Could not fetch plant"));

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
