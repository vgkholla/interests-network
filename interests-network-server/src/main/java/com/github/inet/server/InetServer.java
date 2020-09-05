package com.github.inet.server;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.github.inet.resource.GetRequestOptionsImpl;
import com.github.inet.resource.Resource;
import com.github.inet.common.MetadataProtos.Metadata;
import com.github.inet.entities.GroupProtos;
import com.github.inet.resource.group.GroupCosmosResource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.*;


public class InetServer implements AutoCloseable {
  // Options
  private static final String OPT_COSMOS_DB_ACCOUNT_ENDPOINT = "cosmosDBAccountEndpoint";
  private static final String OPT_COSMOS_DB_ACCOUNT_KEY = "cosmosDBAccountKey";
  private static final String OPT_COSMOS_DB_PREFERRED_REGIONS = "cosmosDBPreferredRegions";

  // Storage Resources
  private static final String GROUP_DATABASE_NAME = "Groups";
  private static final String GROUP_CONTAINER_NAME = "groups";

  private final CosmosClient _client;
  private final Resource<String, GroupProtos.Group> _groupsResource;

  public Resource<String, GroupProtos.Group> getGroupsResource() {
    return _groupsResource;
  }

  @Override
  public void close() {
    _client.close();
  }

  private CosmosClient createCosmosClient(InetServerInitializationParamsProtos.CosmosDBConfiguration configuration) {
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

  private Resource<String, GroupProtos.Group> createGroupsResource() {
    CosmosDatabase cosmosDatabase = _client.getDatabase(GROUP_DATABASE_NAME);
    CosmosContainer groupsContainer = cosmosDatabase.getContainer(GROUP_CONTAINER_NAME);
    return new GroupCosmosResource(groupsContainer);
  }

  public static void main(String[] args) throws Exception {
    InetServer inetServer = new InetServer(getInitParams(args));
    Resource<String, GroupProtos.Group> groupsResource = inetServer.getGroupsResource();
    System.out.println(
        groupsResource.get("inet:group:1", new GetRequestOptionsImpl(Metadata.newBuilder().build())).orElse(null));

    inetServer.close();
  }

  private static InetServerInitializationParamsProtos.InetServerInitializationParams getInitParams(String[] args)
      throws ParseException {
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    getCosmosDBOptions().getOptions().forEach(options::addOption);
    CommandLine commandLine = parser.parse(options, args);

    InetServerInitializationParamsProtos.InetServerInitializationParams.Builder builder =
        InetServerInitializationParamsProtos.InetServerInitializationParams.newBuilder();
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

  private static InetServerInitializationParamsProtos.CosmosDBConfiguration getCosmosDBConfiguration(
      CommandLine commandLine) {
    InetServerInitializationParamsProtos.CosmosDBConfiguration.Builder builder =
        InetServerInitializationParamsProtos.CosmosDBConfiguration.newBuilder();
    builder.setCosmosDBAccountEndpoint(commandLine.getOptionValue(OPT_COSMOS_DB_ACCOUNT_ENDPOINT));
    builder.setCosmosDBAccountKey(commandLine.getOptionValue(OPT_COSMOS_DB_ACCOUNT_KEY));
    for (String region : commandLine.getOptionValues(OPT_COSMOS_DB_PREFERRED_REGIONS)) {
      builder.addPreferredRegions(region);
    }
    return builder.build();
  }

  private InetServer(InetServerInitializationParamsProtos.InetServerInitializationParams params) {
    checkNotNull(params, "Initialization params cannot be null");
    _client = createCosmosClient(params.getCosmosDBConfiguration());
    _groupsResource = createGroupsResource();
  }
}
