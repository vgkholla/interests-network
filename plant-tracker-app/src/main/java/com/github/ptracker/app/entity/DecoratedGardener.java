package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.GetGardenerQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.Gardener;
import java.util.concurrent.TimeUnit;

import static com.github.ptracker.app.util.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedGardener {
  private final ApolloClient _graphQLClient;
  private final String _id;

  private Gardener _gardener;

  public DecoratedGardener(ApolloClient graphQLClient, String id) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringNotNullOrEmpty(id, "Gardener ID cannot be empty");
  }

  public String getId() {
    return _id;
  }

  public Gardener getGardener() {
    populate();
    return _gardener;
  }

  @Override
  public String toString() {
    return "DecoratedGardener{" + "_gardenerId='" + _id + '\'' + '}';
  }

  private void populate() {
    if (_gardener == null) {
      synchronized (this) {
        if (_gardener == null) {
          ApolloClientCallback<GetGardenerQuery.Data, Gardener> callback = new ApolloClientCallback<>(data -> {
            GetGardenerQuery.GetGardener getGardener = data.getGardener();
            if (getGardener == null || getGardener.id() == null) {
              return null;
            }
            return Gardener.newBuilder()
                .setId(_id)
                .setFirstName(getGardener.firstName())
                .setLastName(getGardener.lastName())
                .build();
          });
          _graphQLClient.query(new GetGardenerQuery(_id)).enqueue(callback);
          _gardener = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
        }
      }
    }
  }
}
