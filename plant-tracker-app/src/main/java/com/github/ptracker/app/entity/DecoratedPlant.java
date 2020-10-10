package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.gitgub.ptracker.app.GetPlantQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.Plant;
import java.util.concurrent.TimeUnit;

import static com.github.ptracker.app.entity.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedPlant {

  private final ApolloClient _graphQLClient;
  private final String _id;

  private Plant _plant;

  public DecoratedPlant(ApolloClient graphQLClient, String id) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringFieldNotNullOrEmpty(id, "Plant ID cannot be empty");
  }

  public String getId() {
    return _id;
  }

  public Plant getPlant() {
    populate();
    return _plant;
  }

  @Override
  public String toString() {
    return "DecoratedPlant{" + "_plantId='" + _id + '\'' + '}';
  }

  private void populate() {
    if (_plant == null) {
      synchronized (this) {
        if (_plant == null) {
          ApolloClientCallback<GetPlantQuery.Data, GetPlantQuery.GetPlant> callback =
              new ApolloClientCallback<>(GetPlantQuery.Data::getPlant);
          _graphQLClient.query(new GetPlantQuery(_id)).enqueue(callback);
          GetPlantQuery.GetPlant getPlant = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getPlant.id() == null) {
            throw new IllegalStateException("Could not find plant with ID: " + _id);
          }
          _plant = Plant.newBuilder().setId(_id).setName(getPlant.name()).build();
        }
      }
    }
  }
}
