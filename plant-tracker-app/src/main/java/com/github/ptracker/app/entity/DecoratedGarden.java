package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.CreateGardenPlantMutation;
import com.github.ptracker.app.GetGardenQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.entity.GardenPlant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedGarden {
  private final ApolloClient _graphQLClient;
  private final String _id;
  private final DecoratedSpace _parentSpace;

  private Garden _garden;
  private List<GardenPlant> _displayGardenPlants;
  private List<DecoratedGardenPlant> _gardenPlants;

  DecoratedGarden(ApolloClient graphQLClient, String id, DecoratedSpace space) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringNotNullOrEmpty(id, "Garden ID cannot be empty");
    _parentSpace = checkNotNull(space, "Space cannot be null");
  }

  public String getId() {
    return _id;
  }

  public DecoratedSpace getParentSpace() {
    return _parentSpace;
  }

  public Garden getGarden() {
    populate();
    return _garden;
  }

  public List<GardenPlant> getDisplayGardenPlants() {
    populate();
    return _displayGardenPlants;
  }

  public List<DecoratedGardenPlant> getGardenPlants() {
    populate();
    return _gardenPlants;
  }

  public void addGardenPlant(GardenPlant gardenPlant) {
    populate();
    ApolloClientCallback<CreateGardenPlantMutation.Data, CreateGardenPlantMutation.CreateGardenPlant> callback =
        new ApolloClientCallback<>(CreateGardenPlantMutation.Data::createGardenPlant);
    _graphQLClient.mutate(new CreateGardenPlantMutation(gardenPlant.getName(), _id, gardenPlant.getPlantId()))
        .enqueue(callback);
    CreateGardenPlantMutation.CreateGardenPlant createGardenPlant = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
    if (createGardenPlant.id() == null) {
      throw new IllegalStateException("No id returned on creation of garden plant!");
    }
    GardenPlant gardenPlantWithId =
        GardenPlant.newBuilder(gardenPlant).setId(createGardenPlant.id()).setGardenId(_id).build();
    _displayGardenPlants.add(gardenPlantWithId);
    _gardenPlants.add(new DecoratedGardenPlant(_graphQLClient, gardenPlantWithId.getId(), this));
  }

  @Override
  public String toString() {
    return "DecoratedGarden{" + "_gardenId='" + _id + '\'' + ", _parentSpace=" + _parentSpace + '}';
  }

  private void populate() {
    if (_garden == null) {
      synchronized (this) {
        if (_garden == null) {
          ApolloClientCallback<GetGardenQuery.Data, GetGardenQuery.GetGarden> callback =
              new ApolloClientCallback<>(GetGardenQuery.Data::getGarden);
          _graphQLClient.query(new GetGardenQuery(_id)).enqueue(callback);
          GetGardenQuery.GetGarden getGarden = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getGarden.id() == null) {
            throw new IllegalStateException("Could not find garden with ID: " + _id);
          }
          _garden =
              Garden.newBuilder().setId(_id).setName(getGarden.name()).setSpaceId(_parentSpace.getId()).build();
          _displayGardenPlants = getGarden.gardenPlants() == null ? new ArrayList<>() : getGarden.gardenPlants()
              .stream()
              .map(gardenPlant -> GardenPlant.newBuilder()
                  .setId(gardenPlant.id())
                  .setName(gardenPlant.name())
                  .setGardenId(_id)
                  .build())
              .collect(Collectors.toCollection(ArrayList::new));
          _gardenPlants = _displayGardenPlants.stream()
              .map(gardenPlant -> new DecoratedGardenPlant(_graphQLClient, gardenPlant.getId(), this))
              .collect(Collectors.toCollection(ArrayList::new));
        }
      }
    }
  }
}
