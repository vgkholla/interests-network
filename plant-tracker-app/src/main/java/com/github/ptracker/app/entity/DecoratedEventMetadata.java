package com.github.ptracker.app.entity;

import com.github.ptracker.common.EventMetadata;

import static com.google.common.base.Preconditions.*;


public class DecoratedEventMetadata<E, A> {

  private final E _parentEvent;
  private final A _actor;
  private final EventMetadata _metadata;

  DecoratedEventMetadata(EventMetadata metadata, E parentEvent, A actor) {
    _metadata = checkNotNull(metadata, "EventMetadata cannot be null");
    _parentEvent = checkNotNull(parentEvent, "Event cannot be null");
    _actor = checkNotNull(actor, "Actor cannot be null");
  }

  public E getParentEvent() {
    return _parentEvent;
  }

  public A getActor() {
    return _actor;
  }

  public EventMetadata getMetadata() {
    return _metadata;
  }

  @Override
  public String toString() {
    return "DecoratedEventMetadata{" + "_parentEvent=" + _parentEvent + ", _actor=" + _actor + ", _metadata="
        + _metadata + '}';
  }
}
