package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.GetNoteEventQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.common.EventMetadata;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.NoteEvent;
import java.util.concurrent.TimeUnit;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedNoteEvent {
  private final ApolloClient _graphQLClient;
  private final String _eventId;
  private final DecoratedGardenPlant _parentGardenPlant;

  private NoteEvent _event;
  private Gardener _displayGardener;
  private DecoratedEventMetadata<DecoratedNoteEvent, DecoratedGardener> _metadata;

  DecoratedNoteEvent(ApolloClient graphQLClient, String eventId, DecoratedGardenPlant parentGardenPlant) {
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

  public NoteEvent getEvent() {
    populate();
    return _event;
  }

  public Gardener getDisplayGardener() {
    populate();
    return _displayGardener;
  }

  public DecoratedEventMetadata<DecoratedNoteEvent, DecoratedGardener> getMetadata() {
    populate();
    return _metadata;
  }

  @Override
  public String toString() {
    return "DecoratedNoteEvent{" + "_eventId='" + _eventId + '\'' + ", _parentGardenPlant=" + _parentGardenPlant
        + '}';
  }

  private void populate() {
    if (_event == null) {
      synchronized (this) {
        if (_event == null) {
          ApolloClientCallback<GetNoteEventQuery.Data, GetNoteEventQuery.GetNoteEvent> callback =
              new ApolloClientCallback<>(GetNoteEventQuery.Data::getNoteEvent);
          _graphQLClient.query(new GetNoteEventQuery(_eventId)).enqueue(callback);
          GetNoteEventQuery.GetNoteEvent getNoteEvent = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getNoteEvent.id() == null) {
            throw new IllegalStateException("Could not find event with ID: " + _eventId);
          }
          GetNoteEventQuery.Gardener gardener = getNoteEvent.metadata().gardener();
          EventMetadata.Builder metadataBuilder = EventMetadata.newBuilder()
              .setTimestamp(getNoteEvent.metadata().timestamp())
              .setGardenerId(gardener.id());
          if (getNoteEvent.metadata().comment() != null) {
            metadataBuilder.setComment(getNoteEvent.metadata().comment());
          }
          EventMetadata metadata = metadataBuilder.build();
          _event = NoteEvent.newBuilder()
              .setId(_eventId)
              .setDescription(getNoteEvent.description())
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
