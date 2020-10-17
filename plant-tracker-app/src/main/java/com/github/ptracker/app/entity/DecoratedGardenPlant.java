package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Input;
import com.github.ptracker.app.CreateFertilizationEventMutation;
import com.github.ptracker.app.CreateNoteEventMutation;
import com.github.ptracker.app.CreateWateringEventMutation;
import com.github.ptracker.app.GetGardenPlantQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.entity.WateringEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.ptracker.app.util.VerifierUtils.*;
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
  private List<DecoratedNoteEvent> _noteEvents;

  DecoratedGardenPlant(ApolloClient graphQLClient, String id, DecoratedGarden parentGarden) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringNotNullOrEmpty(id, "GardenPlant ID cannot be empty");
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

  public List<DecoratedNoteEvent> getNoteEvents() {
    populate();
    return _noteEvents;
  }

  public void logWatering(WateringEvent event) {
    populate();
    EventMetadata metadata = getMetadata(event.getMetadata());
    ApolloClientCallback<CreateWateringEventMutation.Data, CreateWateringEventMutation.CreateWateringEvent> callback =
        new ApolloClientCallback<>(CreateWateringEventMutation.Data::createWateringEvent);
    _graphQLClient.mutate(
        new CreateWateringEventMutation(event.getQuantityMl(), _id, metadata.getGardenerId(), metadata.getTimestamp(),
            Input.fromNullable(metadata.getComment()))).enqueue(callback);
    CreateWateringEventMutation.CreateWateringEvent createWateringEvent =
        callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
    if (createWateringEvent.id() == null) {
      throw new IllegalStateException("No id returned on creation of event!");
    }
    _wateringEvents.add(new DecoratedWateringEvent(_graphQLClient, createWateringEvent.id(), this));
  }

  public void logFertilization(FertilizationEvent event) {
    populate();
    EventMetadata metadata = getMetadata(event.getMetadata());
    ApolloClientCallback<CreateFertilizationEventMutation.Data, CreateFertilizationEventMutation.CreateFertilizationEvent>
        callback = new ApolloClientCallback<>(CreateFertilizationEventMutation.Data::createFertilizationEvent);
    _graphQLClient.mutate(new CreateFertilizationEventMutation(event.getQuantityMg(), _id, metadata.getGardenerId(),
        metadata.getTimestamp(), Input.fromNullable(metadata.getComment()))).enqueue(callback);
    CreateFertilizationEventMutation.CreateFertilizationEvent createFertilizationEvent =
        callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
    if (createFertilizationEvent.id() == null) {
      throw new IllegalStateException("No id returned on creation of event!");
    }
    _fertilizationEvents.add(new DecoratedFertilizationEvent(_graphQLClient, createFertilizationEvent.id(), this));
  }

  public void addNote(NoteEvent event) {
    populate();
    EventMetadata metadata = getMetadata(event.getMetadata());
    ApolloClientCallback<CreateNoteEventMutation.Data, CreateNoteEventMutation.CreateNoteEvent> callback =
        new ApolloClientCallback<>(CreateNoteEventMutation.Data::createNoteEvent);
    _graphQLClient.mutate(
        new CreateNoteEventMutation(event.getDescription(), _id, metadata.getGardenerId(), metadata.getTimestamp(),
            Input.fromNullable(metadata.getComment()))).enqueue(callback);
    CreateNoteEventMutation.CreateNoteEvent createNoteEvent = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
    if (createNoteEvent.id() == null) {
      throw new IllegalStateException("No id returned on creation of event!");
    }
    _noteEvents.add(new DecoratedNoteEvent(_graphQLClient, createNoteEvent.id(), this));
  }

  private void populate() {
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
          _wateringEvents = getGardenPlant.wateringEvents() == null ? new ArrayList<>()
              : getGardenPlant.wateringEvents()
                  .stream()
                  .map(event -> new DecoratedWateringEvent(_graphQLClient, event.id(), this))
                  .collect(Collectors.toCollection(ArrayList::new));
          _fertilizationEvents = getGardenPlant.fertilizationEvents() == null ? new ArrayList<>()
              : getGardenPlant.fertilizationEvents()
                  .stream()
                  .map(event -> new DecoratedFertilizationEvent(_graphQLClient, event.id(), this))
                  .collect(Collectors.toCollection(ArrayList::new));
          _noteEvents = getGardenPlant.noteEvents() == null ? new ArrayList<>() : getGardenPlant.noteEvents()
              .stream()
              .map(event -> new DecoratedNoteEvent(_graphQLClient, event.id(), this))
              .collect(Collectors.toCollection(ArrayList::new));
        }
      }
    }
  }

  private EventMetadata getMetadata(EventMetadata prototype) {
    EventMetadata.Builder metadataBuilder =
        prototype != null ? EventMetadata.newBuilder(prototype) : EventMetadata.newBuilder();
    if (metadataBuilder.getTimestamp() <= 0) {
      metadataBuilder.setTimestamp(System.currentTimeMillis());
    }
    metadataBuilder.setGardenerId(_parentGarden.getParentSpace().getGardener().getId());
    return metadataBuilder.build();
  }
}
