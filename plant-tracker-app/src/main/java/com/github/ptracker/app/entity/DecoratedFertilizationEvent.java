package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.GetFertilizationEventQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.FertilizationEvent;
import java.util.concurrent.TimeUnit;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedFertilizationEvent {
  private final ApolloClient _graphQLClient;
  private final String _eventId;
  private final DecoratedGardenPlant _parentGardenPlant;

  private FertilizationEvent _event;
  private Gardener _displayGardener;
  private DecoratedEventMetadata<DecoratedFertilizationEvent, DecoratedGardener> _metadata;

  DecoratedFertilizationEvent(ApolloClient graphQLClient, String eventId, DecoratedGardenPlant parentGardenPlant) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _eventId = verifyStringNotNullOrEmpty(eventId, "Event ID cannot be empty");
    _parentGardenPlant = checkNotNull(parentGardenPlant, "Garden Plant cannot be null");
  }

  public String getEventId() {
    return _eventId;
  }

  public DecoratedGardenPlant getParentGardenPlant() {
    return _parentGardenPlant;
  }

  public FertilizationEvent getEvent() {
    populate();
    return _event;
  }

  public Gardener getDisplayGardener() {
    populate();
    return _displayGardener;
  }

  public DecoratedEventMetadata<DecoratedFertilizationEvent, DecoratedGardener> getMetadata() {
    populate();
    return _metadata;
  }

  @Override
  public String toString() {
    return "DecoratedFertilizationEvent{" + "_eventId='" + _eventId + '\'' + ", _parentGardenPlant=" + _parentGardenPlant
        + '}';
  }

  private void populate() {
    if (_event == null) {
      synchronized (this) {
        if (_event == null) {
          ApolloClientCallback<GetFertilizationEventQuery.Data, GetFertilizationEventQuery.GetFertilizationEvent> callback =
              new ApolloClientCallback<>(GetFertilizationEventQuery.Data::getFertilizationEvent);
          _graphQLClient.query(new GetFertilizationEventQuery(_eventId)).enqueue(callback);
          GetFertilizationEventQuery.GetFertilizationEvent getFertilizationEvent = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getFertilizationEvent.id() == null) {
            throw new IllegalStateException("Could not find event with ID: " + _eventId);
          }
          GetFertilizationEventQuery.Gardener gardener = getFertilizationEvent.metadata().gardener();
          EventMetadata.Builder metadataBuilder = EventMetadata.newBuilder()
              .setTimestamp(getFertilizationEvent.metadata().timestamp())
              .setGardenerId(gardener.id());
          if (getFertilizationEvent.metadata().comment() != null) {
            metadataBuilder.setComment(getFertilizationEvent.metadata().comment());
          }
          EventMetadata metadata = metadataBuilder.build();
          _event = FertilizationEvent.newBuilder()
              .setId(_eventId)
              .setQuantityMg(getFertilizationEvent.quantityMg())
              .setMetadata(metadata)
              .setGardenPlantId(_parentGardenPlant.getId())
              .build();
          _displayGardener = Gardener.newBuilder()
              .setId(gardener.id())
              .setFirstName(gardener.firstName())
              .setLastName(gardener.lastName())
              .build();
          DecoratedGardener decoratedGardener = new DecoratedGardener(_graphQLClient, gardener.id());
          _metadata = new DecoratedEventMetadata<>(metadata, this, decoratedGardener);
        }
      }
    }
  }
}
