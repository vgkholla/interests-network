package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.GetSpaceQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.Space;
import com.github.ptracker.entity.Garden;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedSpace {
  private final ApolloClient _graphQLClient;
  private final String _id;
  private final DecoratedGardener _gardener;

  private Space _space;
  private List<Garden> _displayGardens;
  private List<DecoratedGarden> _gardens;

  public DecoratedSpace(ApolloClient graphQLClient, String id, DecoratedGardener gardener) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringNotNullOrEmpty(id, "Space ID cannot be empty");
    _gardener = checkNotNull(gardener, "Gardener cannot be null");
  }

  public String getId() {
    return _id;
  }

  public Space getSpace() {
    populate();
    return _space;
  }

  public DecoratedGardener getGardener() {
    return _gardener;
  }

  public List<Garden> getDisplayGardens() {
    return _displayGardens;
  }

  public List<DecoratedGarden> getGardens() {
    populate();
    return _gardens;
  }

  @Override
  public String toString() {
    return "DecoratedSpace{" + "_spaceId='" + _id + '\'' + ", _gardener=" + _gardener + '}';
  }

  private void populate() {
    if (_space == null) {
      synchronized (this) {
        if (_space == null) {
          ApolloClientCallback<GetSpaceQuery.Data, GetSpaceQuery.GetSpace> callback =
              new ApolloClientCallback<>(GetSpaceQuery.Data::getSpace);
          _graphQLClient.query(new GetSpaceQuery(_id)).enqueue(callback);
          GetSpaceQuery.GetSpace getSpace = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getSpace.id() == null) {
            throw new IllegalStateException("Could not find space with ID: " + _id);
          }
          _space = Space.newBuilder().setId(_id).setName(getSpace.name()).build();
          _displayGardens = getSpace.gardens() == null ? Collections.emptyList() : getSpace.gardens()
              .stream()
              .map(garden -> Garden.newBuilder()
                  .setId(garden.id())
                  .setName(garden.name())
                  .setSpaceId(_id)
                  .build())
              .collect(Collectors.toList());
          _gardens = _displayGardens.stream()
              .map(garden -> new DecoratedGarden(_graphQLClient, garden.getId(), this))
              .collect(Collectors.toList());
        }
      }
    }
  }
}
