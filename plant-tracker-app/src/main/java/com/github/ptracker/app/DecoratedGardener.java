package com.github.ptracker.app;

import com.apollographql.apollo.ApolloClient;
import com.gitgub.ptracker.app.GetGardenerQuery;
import com.github.ptracker.entity.Gardener;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.*;


public class DecoratedGardener {
  private final ApolloClient _graphQLClient;
  private final String _gardenerId;

  private Gardener _gardener;

  public DecoratedGardener(ApolloClient graphQLClient, String gardenerId) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _gardenerId = checkNotNull(gardenerId, "Gardner ID cannot be null");
  }

  @Override
  public String toString() {
    return "DecoratedGardener{" + "_graphQLClient=" + _graphQLClient + ", _gardenerId='" + _gardenerId + '\''
        + ", _gardener=" + _gardener + '}';
  }

  public Gardener getGardener() {
    if (_gardener == null) {
      synchronized (this) {
        if (_gardener == null) {
          ApolloClientCallback<GetGardenerQuery.Data, Gardener> callback = new ApolloClientCallback<>(data -> {
            GetGardenerQuery.GetGardener getGardener = data.getGardener();
            if (getGardener == null || getGardener.id() == null) {
              return null;
            }
            return Gardener.newBuilder()
                .setId(_gardenerId)
                .setFirstName(getGardener.firstname())
                .setLastName(getGardener.lastname())
                .build();
          });
          _graphQLClient.query(new GetGardenerQuery(_gardenerId)).enqueue(callback);
          _gardener = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
        }
      }
    }
    return _gardener;
  }
}
