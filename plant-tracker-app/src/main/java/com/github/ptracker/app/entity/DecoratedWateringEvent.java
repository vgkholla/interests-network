package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.GetWateringEventQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.WateringEvent;
import java.util.concurrent.TimeUnit;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedWateringEvent {

  private final ApolloClient _graphQLClient;
  private final String _eventId;
  private final DecoratedGardenPlant _parentGardenPlant;

  private WateringEvent _event;
  private Gardener _displayGardener;
  private DecoratedEventMetadata<DecoratedWateringEvent, DecoratedGardener> _metadata;

  DecoratedWateringEvent(ApolloClient graphQLClient, String eventId, DecoratedGardenPlant parentGardenPlant) {
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

  public WateringEvent getEvent() {
    populate();
    return _event;
  }

  public Gardener getDisplayGardener() {
    populate();
    return _displayGardener;
  }

  public DecoratedEventMetadata<DecoratedWateringEvent, DecoratedGardener> getMetadata() {
    populate();
    return _metadata;
  }

  @Override
  public String toString() {
    return "DecoratedWateringEvent{" + "_eventId='" + _eventId + '\'' + ", _parentGardenPlant=" + _parentGardenPlant
        + '}';
  }

  private void populate() {
    if (_event == null) {
      synchronized (this) {
        if (_event == null) {
          ApolloClientCallback<GetWateringEventQuery.Data, GetWateringEventQuery.GetWateringEvent> callback =
              new ApolloClientCallback<>(GetWateringEventQuery.Data::getWateringEvent);
          _graphQLClient.query(new GetWateringEventQuery(_eventId)).enqueue(callback);
          GetWateringEventQuery.GetWateringEvent getWateringEvent = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getWateringEvent.id() == null) {
            throw new IllegalStateException("Could not find event with ID: " + _eventId);
          }
          GetWateringEventQuery.Gardener gardener = getWateringEvent.metadata().gardener();
          EventMetadata.Builder metadataBuilder = EventMetadata.newBuilder()
              .setTimestamp(getWateringEvent.metadata().timestamp())
              .setGardenerId(gardener.id());
          if (getWateringEvent.metadata().comment() != null) {
            metadataBuilder.setComment(getWateringEvent.metadata().comment());
          }
          EventMetadata metadata = metadataBuilder.build();
          _event = WateringEvent.newBuilder()
              .setId(_eventId)
              .setQuantityMl(getWateringEvent.quantityMl())
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
