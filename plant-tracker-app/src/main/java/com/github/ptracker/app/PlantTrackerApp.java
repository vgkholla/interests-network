package com.github.ptracker.app;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.entity.DecoratedAccount;
import com.github.ptracker.app.entity.DecoratedFertilizationEvent;
import com.github.ptracker.app.entity.DecoratedGarden;
import com.github.ptracker.app.entity.DecoratedGardenPlant;
import com.github.ptracker.app.entity.DecoratedGardener;
import com.github.ptracker.app.entity.DecoratedOtherEvent;
import com.github.ptracker.app.entity.DecoratedWateringEvent;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class PlantTrackerApp {
  private static final String OPT_GRAPHQL_SERVER_URL = "graphQLServerUrl";
  private static final String DEFAULT_GRAPHQL_SERVER_URL = "http://localhost:8080/graphql";

  private static final String DEFAULT_GARDENER_ID = "ptracker:gardener:1";
  private static final String DEFAULT_ACCOUNT_ID = "ptracker:account:1";

  public static void main(String[] args) throws Exception {
    PlantTrackerAppInitializationParams initParams = getInitParams(args);
    ApolloClient graphQLClient = getGraphQLClient(initParams.getGraphqlServerUrl());

    Scanner scanner = new Scanner(System.in);
    System.out.println("Welcome Gardener! Maintain a log of the events for your plants with this app (and more!)");
    System.out.print("Username [" + DEFAULT_GARDENER_ID + "]: ");
    String username = scanner.nextLine();
    username = username == null || username.isEmpty() ? DEFAULT_GARDENER_ID : username;
    DecoratedGardener decoratedGardener = login(username, graphQLClient);
    System.out.print("Account [" + DEFAULT_ACCOUNT_ID + "]: ");
    String account = scanner.nextLine();
    account = account == null || account.isEmpty() ? DEFAULT_ACCOUNT_ID : account;
    DecoratedAccount decoratedAccount = getAccount(account, decoratedGardener, graphQLClient);

    System.out.println("Account: ");
    System.out.println(decoratedAccount.getAccount());
    System.out.println("Gardens: ");
    for (DecoratedGarden garden : decoratedAccount.getGardens()) {
      System.out.println(garden.getGarden());
      System.out.println("Garden plants: ");
      for (DecoratedGardenPlant gardenPlant : garden.getGardenPlants()) {
        System.out.println(gardenPlant.getGardenPlant());
        System.out.println(gardenPlant.getPlant().getPlant());
        System.out.println("Watering events");
        for (DecoratedWateringEvent event : gardenPlant.getWateringEvents()) {
          System.out.println(event.getEvent());
          System.out.println(event.getMetadata().getActor());
        }
        System.out.println("Fertilization events");
        for (DecoratedFertilizationEvent event : gardenPlant.getFertilizationEvents()) {
          System.out.println(event.getEvent());
          System.out.println(event.getMetadata().getActor());
        }
        System.out.println("Other events");
        for (DecoratedOtherEvent event : gardenPlant.getOtherEvents()) {
          System.out.println(event.getEvent());
          System.out.println(event.getMetadata().getActor());
        }
      }
    }
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
    builder.setGraphqlServerUrl(commandLine.getOptionValue(OPT_GRAPHQL_SERVER_URL, DEFAULT_GRAPHQL_SERVER_URL));
    return builder.build();
  }

  private static DecoratedGardener login(String gardenerId, ApolloClient graphQLClient) {
    return new DecoratedGardener(graphQLClient, gardenerId);
  }

  private static DecoratedAccount getAccount(String accountID, DecoratedGardener gardener, ApolloClient graphQLClient) {
    return new DecoratedAccount(graphQLClient, accountID, gardener);
  }
}
