package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.gitgub.ptracker.app.GetAccountQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.Account;
import com.github.ptracker.entity.Garden;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.ptracker.app.entity.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedAccount {
  private final ApolloClient _graphQLClient;
  private final String _id;
  private final DecoratedGardener _gardener;

  private Account _account;
  private List<Garden> _displayGardens;
  private List<DecoratedGarden> _gardens;

  public DecoratedAccount(ApolloClient graphQLClient, String id, DecoratedGardener gardener) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringFieldNotNullOrEmpty(id, "Account ID cannot be empty");
    _gardener = checkNotNull(gardener, "Gardener cannot be null");
  }

  public String getId() {
    return _id;
  }

  public Account getAccount() {
    populate();
    return _account;
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
    return "DecoratedAccount{" + "_accountId='" + _id + '\'' + ", _gardener=" + _gardener + '}';
  }

  private void populate() {
    if (_account == null) {
      synchronized (this) {
        if (_account == null) {
          ApolloClientCallback<GetAccountQuery.Data, GetAccountQuery.GetAccount> callback =
              new ApolloClientCallback<>(GetAccountQuery.Data::getAccount);
          _graphQLClient.query(new GetAccountQuery(_id)).enqueue(callback);
          GetAccountQuery.GetAccount getAccount = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getAccount.id() == null) {
            throw new IllegalStateException("Could not find account with ID: " + _id);
          }
          _account = Account.newBuilder().setId(_id).setName(getAccount.name()).build();
          _displayGardens = getAccount.gardens() == null ? Collections.emptyList() : getAccount.gardens()
              .stream()
              .map(garden -> Garden.newBuilder()
                  .setId(garden.id())
                  .setName(garden.name())
                  .setAccountId(_id)
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
