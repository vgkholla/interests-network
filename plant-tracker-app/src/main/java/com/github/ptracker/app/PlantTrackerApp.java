package com.github.ptracker.app;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.entity.Account;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class PlantTrackerApp {
  private static final String OPT_GRAPHQL_SERVER_URL = "graphQLServerUrl";
  private static final String DEFAULT_GRAPHQL_SERVER_URL = "http://localhost:8080/graphql";

  public static void main(String[] args) throws Exception {
    PlantTrackerAppInitializationParams initParams = getInitParams(args);
    ApolloClient graphQLClient = getGraphQLClient(initParams.getGraphQLServerUrl());
    String gardenerId = "ptracker:gardener:2";
    DecoratedGardener gardener = login(gardenerId, graphQLClient);
    String accountId = "ptracker:account:1";
    DecoratedAccount account = getAccount(accountId, gardener, graphQLClient);
    System.out.println(account.getAccount());
    System.out.println(gardener.getGardener());
    System.out.println();
  }

  private static ApolloClient getGraphQLClient(String graphQLServerUrl) {
    // TODO: add when the first query with "Long" occurs
//    CustomTypeAdapter<Long> longAdapter = new CustomTypeAdapter<Long>() {
//      @Override
//      public Long decode(@NotNull CustomTypeValue<?> customTypeValue) {
//        return Long.parseLong(customTypeValue.value.toString());
//      }
//
//      @NotNull
//      @Override
//      public CustomTypeValue<?> encode(Long aLong) {
//        return new CustomTypeValue.GraphQLString(Long.toString(aLong));
//      }
//    };
    return ApolloClient.builder().serverUrl(graphQLServerUrl).build();
  }

  private static PlantTrackerAppInitializationParams getInitParams(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption(Option.builder()
        .longOpt(OPT_GRAPHQL_SERVER_URL)
        .desc("URL of the GraphQL server")
        .required(false)
        .hasArg()
        .argName("GRAPHQL_SERVER_URL")
        .build());

    CommandLineParser parser = new DefaultParser();
    CommandLine commandLine = parser.parse(options, args);

    PlantTrackerAppInitializationParams.Builder builder = PlantTrackerAppInitializationParams.newBuilder();
    builder.setGraphQLServerUrl(commandLine.getOptionValue(OPT_GRAPHQL_SERVER_URL, DEFAULT_GRAPHQL_SERVER_URL));
    return builder.build();
  }

  private static DecoratedGardener login(String gardenerId, ApolloClient graphQLClient) {
    return new DecoratedGardener(graphQLClient, gardenerId);
  }

  private static DecoratedAccount getAccount(String accountID, DecoratedGardener gardener, ApolloClient graphQLClient) {
    return new DecoratedAccount(graphQLClient, accountID, gardener);
  }
}
