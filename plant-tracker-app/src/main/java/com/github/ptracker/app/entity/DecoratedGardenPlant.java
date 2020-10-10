package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.gitgub.ptracker.app.GetGardenPlantQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.Plant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.ptracker.app.entity.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedGardenPlant {

  private final ApolloClient _graphQLClient;
  private final String _id;
  private final DecoratedGarden _parentGarden;

  private GardenPlant _gardenPlant;
  private Plant _displayPlant;
  private DecoratedPlant _plant;
  private List<DecoratedWateringEvent> _wateringEvents;
  private List<DecoratedFertilizationEvent> _fertilizationEvents;
  private List<DecoratedOtherEvent> _otherEvents;

  DecoratedGardenPlant(ApolloClient graphQLClient, String id, DecoratedGarden parentGarden) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringFieldNotNullOrEmpty(id, "GardenPlant ID cannot be empty");
    _parentGarden = checkNotNull(parentGarden, "Gardener cannot be null");
  }

  public String getId() {
    return _id;
  }

  public DecoratedGarden getParentGarden() {
    return _parentGarden;
  }

  public GardenPlant getGardenPlant() {
    populate();
    return _gardenPlant;
  }

  public Plant getDisplayPlant() {
    populate();
    return _displayPlant;
  }

  public DecoratedPlant getPlant() {
    populate();
    return _plant;
  }

  public List<DecoratedWateringEvent> getWateringEvents() {
    populate();
    return _wateringEvents;
  }

  public List<DecoratedFertilizationEvent> getFertilizationEvents() {
    populate();
    return _fertilizationEvents;
  }

  public List<DecoratedOtherEvent> getOtherEvents() {
    populate();
    return _otherEvents;
  }

  void populate() {
    if (_gardenPlant == null) {
      synchronized (this) {
        if (_gardenPlant == null) {
          ApolloClientCallback<GetGardenPlantQuery.Data, GetGardenPlantQuery.GetGardenPlant> callback =
              new ApolloClientCallback<>(GetGardenPlantQuery.Data::getGardenPlant);
          _graphQLClient.query(new GetGardenPlantQuery(_id)).enqueue(callback);
          GetGardenPlantQuery.GetGardenPlant getGardenPlant = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getGardenPlant.id() == null) {
            throw new IllegalStateException("Could not find garden plant with ID: " + _id);
          }
          _displayPlant =
              Plant.newBuilder().setId(getGardenPlant.plant().id()).setName(getGardenPlant.plant().name()).build();
          _gardenPlant = GardenPlant.newBuilder()
              .setId(_id)
              .setName(getGardenPlant.name())
              .setGardenId(_parentGarden.getId())
              .setPlantId(_displayPlant.getId())
              .build();
          _plant = new DecoratedPlant(_graphQLClient, _displayPlant.getId());
          _wateringEvents = getGardenPlant.wateringEvents() == null
              ? Collections.emptyList()
              : getGardenPlant.wateringEvents()
                  .stream()
                  .map(event -> new DecoratedWateringEvent(_graphQLClient, event.id(), this))
                  .collect(Collectors.toList());
          _fertilizationEvents = getGardenPlant.fertilizationEvents() == null
              ? Collections.emptyList()
              : getGardenPlant.fertilizationEvents()
                  .stream()
                  .map(event -> new DecoratedFertilizationEvent(_graphQLClient, event.id(), this))
                  .collect(Collectors.toList());
          _otherEvents = getGardenPlant.otherEvents() == null
              ? Collections.emptyList()
              : getGardenPlant.otherEvents()
                  .stream()
                  .map(event -> new DecoratedOtherEvent(_graphQLClient, event.id(), this))
                  .collect(Collectors.toList());
        }
      }
    }
  }
}
