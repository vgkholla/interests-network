package com.github.ptracker.app;

import com.apollographql.apollo.ApolloClient;
import com.github.ptracker.app.entity.DecoratedPlant;
import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.*;


public class PlantCatalog {
  private final ApolloClient _graphQLClient;
  private final DecoratedPlant _genericPlant;

  public PlantCatalog(ApolloClient graphQLClient, String genericPlantId) {
    _graphQLClient = checkNotNull(graphQLClient, "GraphQL client cannot be null");
    _genericPlant = new DecoratedPlant(graphQLClient, genericPlantId);
  }

  public Collection<DecoratedPlant> search(String query) {
    return Collections.emptySet();
  }

  public DecoratedPlant getGenericPlant() {
    return _genericPlant;
  }
}
