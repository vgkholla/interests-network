package com.github.ptracker.app;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.entity.DecoratedSpace;
import com.github.ptracker.app.entity.DecoratedFertilizationEvent;
import com.github.ptracker.app.entity.DecoratedGarden;
import com.github.ptracker.app.entity.DecoratedGardenPlant;
import com.github.ptracker.app.entity.DecoratedGardener;
import com.github.ptracker.app.entity.DecoratedNoteEvent;
import com.github.ptracker.app.entity.DecoratedPlant;
import com.github.ptracker.app.entity.DecoratedWateringEvent;
import com.github.ptracker.app.util.Prompt;
import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.entity.WateringEvent;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
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
  private static final String DEFAULT_SPACE_ID = "ptracker:space:1";
  private static final String GENERIC_PLANT_ID = "ptracker:plant:1";

  public static void main(String[] args) throws Exception {
    PlantTrackerAppInitializationParams initParams = getInitParams(args);
    ApolloClient graphQLClient = getGraphQLClient(initParams.getGraphqlServerUrl());

    Prompt prompt = new Prompt();
    Scanner scanner = new Scanner(System.in);
    System.out.println("Welcome Gardener! Maintain a log of the events for your plants with this app (and more!)");
    System.out.print(prompt.prompt() + " Username [" + DEFAULT_GARDENER_ID + "]: ");
    String username = scanner.nextLine();
    username = username == null || username.isEmpty() ? DEFAULT_GARDENER_ID : username;
    DecoratedGardener decoratedGardener = login(username, graphQLClient);
    prompt.push(decoratedGardener.getGardener().getFirstName());
    System.out.print(prompt.prompt() + " Space [" + DEFAULT_SPACE_ID + "]: ");
    String space = scanner.nextLine();
    space = space == null || space.isEmpty() ? DEFAULT_SPACE_ID : space;
    DecoratedSpace decoratedSpace = getSpace(space, decoratedGardener, graphQLClient);
    prompt.push(decoratedSpace.getSpace().getName());
    PlantCatalog plantCatalog = new PlantCatalog(graphQLClient, GENERIC_PLANT_ID);
    onSpace(prompt, scanner, decoratedSpace, plantCatalog);
    System.out.println("Buh-bye ! Come again!");
  }

  private static void onSpace(Prompt prompt, Scanner scanner, DecoratedSpace space, PlantCatalog plantCatalog) {
    List<Garden> displayGardens = space.getDisplayGardens();
    if (!displayGardens.isEmpty()) {
      int choice;
      do {
        System.out.println("Please select a Garden");
        choice = select(prompt, scanner, displayGardens, Garden::getName);
        if (choice >= 0) {
          onGarden(prompt, scanner, displayGardens.get(choice).getName(), space.getGardens().get(choice),
              plantCatalog);
        }
      } while (choice >= 0);
    } else {
      System.out.println("There are no gardens in this space ! This app does not yet support creating gardens");
    }
  }

  private static void onGarden(Prompt prompt, Scanner scanner, String gardenName, DecoratedGarden garden,
      PlantCatalog plantCatalog) {
    int choice;
    do {
      List<GardenPlant> gardenPlants = garden.getDisplayGardenPlants();
      int choiceNum = 0;
      System.out.println("What would you like to do?");
      System.out.println(formatChoice(choiceNum++, "Add a plant to the garden"));
      if (!gardenPlants.isEmpty()) {
        System.out.println(formatChoice(choiceNum++, "Select a plant from the garden"));
      }
      choice = select(prompt, scanner, choiceNum);
      if (choice >= choiceNum) {
        throw new IllegalStateException("Not expecting " + choice);
      }
      switch (choice) {
        case 0:
          System.out.print(prompt.prompt() + " Name your plant (keep it unique!): ");
          String gardenPlantName = scanner.nextLine();
          DecoratedPlant plant = plantCatalog.getGenericPlant();
          GardenPlant gardenPlant = GardenPlant.newBuilder().setName(gardenPlantName).setPlantId(plant.getId()).build();
          garden.addGardenPlant(gardenPlant);
          System.out.println("---Added \"" + gardenPlantName + "\" to \"" + gardenName + "\"---");
          break;
        case 1:
          viewGardenPlants(prompt, scanner, garden);
          break;
      }
    } while (choice >= 0);
  }

  private static void viewGardenPlants(Prompt prompt, Scanner scanner, DecoratedGarden garden) {
    List<GardenPlant> displayGardenPlants = garden.getDisplayGardenPlants();
    if (!displayGardenPlants.isEmpty()) {
      int choice;
      do {
        System.out.println("Please select a Garden Plant");
        choice = select(prompt, scanner, displayGardenPlants, GardenPlant::getName);
        if (choice >= 0) {
          onGardenPlant(prompt, scanner, displayGardenPlants.get(choice).getName(),
              garden.getGardenPlants().get(choice));
        }
      } while (choice >= 0);
    }
  }

  private static void onGardenPlant(Prompt prompt, Scanner scanner, String gardenPlantName,
      DecoratedGardenPlant gardenPlant) {
    int choice;
    do {
      int choiceNum = 0;
      System.out.println("What would you like to do?");
      System.out.println(formatChoice(choiceNum++, "Log watering"));
      System.out.println(formatChoice(choiceNum++, "Log fertilization"));
      System.out.println(formatChoice(choiceNum++, "Add note"));
      System.out.println(formatChoice(choiceNum++, "See watering log"));
      System.out.println(formatChoice(choiceNum++, "See fertilization log"));
      System.out.println(formatChoice(choiceNum++, "See notes"));
      choice = select(prompt, scanner, choiceNum);
      if (choice >= choiceNum) {
        throw new IllegalStateException("Not expecting " + choice);
      }
      switch (choice) {
        case 0: {
          System.out.print(
              prompt.prompt() + " How much water (ml) did you add? (please use a positive whole number): ");
          int quantityMl = nextInt(scanner);
          WateringEvent event =
              WateringEvent.newBuilder().setQuantityMl(quantityMl).setMetadata(getMetadata(prompt, scanner)).build();
          gardenPlant.logWatering(event);
          System.out.println("---Logged watering of \"" + gardenPlantName + "\"---");
          break;
        }
        case 1: {
          System.out.print(
              prompt.prompt() + " How much fertilizer (mg) did you add? (please use a positive whole number): ");
          int quantityMg = nextInt(scanner);
          FertilizationEvent event = FertilizationEvent.newBuilder()
              .setQuantityMg(quantityMg)
              .setMetadata(getMetadata(prompt, scanner))
              .build();
          gardenPlant.logFertilization(event);
          System.out.println("---Logged fertilization of \"" + gardenPlantName + "\"---");
          break;
        }
        case 2: {
          System.out.print(prompt.prompt() + " What note would you like to add?: ");
          String description = scanner.nextLine();
          NoteEvent event =
              NoteEvent.newBuilder().setDescription(description).setMetadata(getMetadata(prompt, scanner)).build();
          gardenPlant.addNote(event);
          System.out.println("---Added note to \"" + gardenPlantName + "\"---");
          break;
        }
        case 3:
          System.out.println("---Watering log---");
          for (DecoratedWateringEvent wateringEvent : gardenPlant.getWateringEvents()) {
            WateringEvent wEvent = wateringEvent.getEvent();
            Gardener actor = wateringEvent.getDisplayGardener();
            System.out.println(formatLogEntry(wEvent.getMetadata().getTimestamp(),
                actor.getFirstName() + " added " + wEvent.getQuantityMl() + "ml of water"));
          }
          System.out.println("---end---");
          break;
        case 4:
          System.out.println("---Fertilization log---");
          for (DecoratedFertilizationEvent fertilizationEvent : gardenPlant.getFertilizationEvents()) {
            FertilizationEvent fEvent = fertilizationEvent.getEvent();
            Gardener actor = fertilizationEvent.getDisplayGardener();
            System.out.println(formatLogEntry(fEvent.getMetadata().getTimestamp(),
                actor.getFirstName() + " added " + fEvent.getQuantityMg() + "mg of fertilizer"));
          }
          System.out.println("---end---");
          break;
        case 5:
          System.out.println("---Notes---");
          for (DecoratedNoteEvent noteEvent : gardenPlant.getNoteEvents()) {
            NoteEvent oEvent = noteEvent.getEvent();
            Gardener actor = noteEvent.getDisplayGardener();
            System.out.println(formatLogEntry(oEvent.getMetadata().getTimestamp(),
                actor.getFirstName() + " added a note - \"" + oEvent.getDescription() + "\""));
          }
          System.out.println("---end---");
          break;
      }
    } while (choice >= 0);
  }

  private static EventMetadata getMetadata(Prompt prompt, Scanner scanner) {
    System.out.print(prompt.prompt() + " Additional comments? (leave empty if no comment)");
    String comment = scanner.nextLine();
    EventMetadata.Builder builder = EventMetadata.newBuilder();
    if (!comment.isEmpty()) {
      builder.setComment(comment);
    }
    return builder.build();
  }

  private static <T> int select(Prompt prompt, Scanner scanner, List<T> choices,
      Function<T, String> displayNameGetter) {
    int numChoices = choices.size();
    for (int i = 0; i < numChoices; i++) {
      System.out.println(formatChoice(i, displayNameGetter.apply(choices.get(i))));
    }
    return select(prompt, scanner, numChoices);
  }

  private static int select(Prompt prompt, Scanner scanner, int numChoices) {
    if (numChoices <= 0) {
      throw new IllegalArgumentException("Number of choices should be > 0. Is " + numChoices);
    }
    int choice = numChoices;
    while (choice >= numChoices) {
      System.out.print(prompt.prompt() + " Choice [-1 to exit]: ");
      choice = nextInt(scanner);
      if (choice < 0) {
        break;
      } else if (choice >= numChoices) {
        System.out.println("Invalid choice. Please input a number in range [0, " + (numChoices - 1) + "]");
      }
    }
    return choice;
  }

  private static String formatChoice(int choiceNum, String choiceText) {
    return choiceNum + ". " + choiceText;
  }

  private static String formatLogEntry(long timestampMs, String entry) {
    return new Date(timestampMs) + ": " + entry;
  }

  private static int nextInt(Scanner scanner) {
    int input = scanner.nextInt();
    // throw away the \n not consumed by nextInt()
    scanner.nextLine();
    return input;
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

  private static DecoratedSpace getSpace(String spaceID, DecoratedGardener gardener, ApolloClient graphQLClient) {
    return new DecoratedSpace(graphQLClient, spaceID, gardener);
  }
}
