package com.github.ptracker.app;

import com.apollographql.apollo.ApolloClient;
import com.gitgub.ptracker.app.GetAccountQuery;
import com.github.ptracker.entity.Account;
import com.github.ptracker.entity.Garden;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;


class DecoratedAccount {
  private final ApolloClient _graphQLClient;
  private final String _accountId;
  private final DecoratedGardener _gardener;

  private Account _account;
  // TODO: should be list of decorated gardens
  private List<Garden> _gardens;

  public DecoratedAccount(ApolloClient graphQLClient, String accountID, DecoratedGardener gardener) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _accountId = checkNotNull(accountID, "Account ID cannot be null");
    _gardener = checkNotNull(gardener, "Gardener cannot be null");
  }

  public Account getAccount() {
    populate();
    return _account;
  }

  public DecoratedGardener getGardener() {
    return _gardener;
  }

  public List<Garden> getGardens() {
    populate();
    return _gardens;
  }

  @Override
  public String toString() {
    return "DecoratedAccount{" + "_graphQLClient=" + _graphQLClient + ", _accountId='" + _accountId + '\''
        + ", _gardener=" + _gardener + ", _account=" + _account + ", _gardens=" + _gardens + '}';
  }

  private void populate() {
    if (_account == null) {
      synchronized (this) {
        if (_account == null) {
          ApolloClientCallback<GetAccountQuery.Data, GetAccountQuery.GetAccount> callback =
              new ApolloClientCallback<>(GetAccountQuery.Data::getAccount);
          _graphQLClient.query(new GetAccountQuery(_accountId)).enqueue(callback);
          GetAccountQuery.GetAccount getAccount = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getAccount.id() == null) {
            throw new IllegalStateException("Could not find account with ID: " + _accountId);
          }
          _account = Account.newBuilder().setId(_accountId).setName(getAccount.name()).build();
          _gardens = getAccount.gardens() == null ? Collections.emptyList() : getAccount.gardens()
              .stream()
              .map(garden -> Garden.newBuilder()
                  .setId(garden.id())
                  .setName(garden.name())
                  .setAccountId(_accountId)
                  .build())
              .collect(Collectors.toList());
        }
      }
    }
  }
}
