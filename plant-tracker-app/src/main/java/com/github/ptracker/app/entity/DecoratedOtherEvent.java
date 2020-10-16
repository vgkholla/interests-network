package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.GetOtherEventQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.OtherEvent;
import java.util.concurrent.TimeUnit;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedOtherEvent {
  private final ApolloClient _graphQLClient;
  private final String _eventId;
  private final DecoratedGardenPlant _parentGardenPlant;

  private OtherEvent _event;
  private Gardener _displayGardener;
  private DecoratedEventMetadata<DecoratedOtherEvent, DecoratedGardener> _metadata;

  DecoratedOtherEvent(ApolloClient graphQLClient, String eventId, DecoratedGardenPlant parentGardenPlant) {
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

  public OtherEvent getEvent() {
    populate();
    return _event;
  }

  public Gardener getDisplayGardener() {
    populate();
    return _displayGardener;
  }

  public DecoratedEventMetadata<DecoratedOtherEvent, DecoratedGardener> getMetadata() {
    populate();
    return _metadata;
  }

  @Override
  public String toString() {
    return "DecoratedOtherEvent{" + "_eventId='" + _eventId + '\'' + ", _parentGardenPlant=" + _parentGardenPlant
        + '}';
  }

  private void populate() {
    if (_event == null) {
      synchronized (this) {
        if (_event == null) {
          ApolloClientCallback<GetOtherEventQuery.Data, GetOtherEventQuery.GetOtherEvent> callback =
              new ApolloClientCallback<>(GetOtherEventQuery.Data::getOtherEvent);
          _graphQLClient.query(new GetOtherEventQuery(_eventId)).enqueue(callback);
          GetOtherEventQuery.GetOtherEvent getOtherEvent = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getOtherEvent.id() == null) {
            throw new IllegalStateException("Could not find event with ID: " + _eventId);
          }
          GetOtherEventQuery.Gardener gardener = getOtherEvent.metadata().gardener();
          EventMetadata.Builder metadataBuilder = EventMetadata.newBuilder()
              .setTimestamp(getOtherEvent.metadata().timestamp())
              .setGardenerId(gardener.id());
          if (getOtherEvent.metadata().comment() != null) {
            metadataBuilder.setComment(getOtherEvent.metadata().comment());
          }
          EventMetadata metadata = metadataBuilder.build();
          _event = OtherEvent.newBuilder()
              .setId(_eventId)
              .setDescription(getOtherEvent.description())
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
