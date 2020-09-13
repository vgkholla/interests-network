package com.github.inet.server.graphql.client;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import java.util.Collection;

import static com.google.common.base.Preconditions.*;


public class OverallClientModule extends AbstractModule {
  private final Collection<Module> _clientModules;

  public OverallClientModule(Collection<Module> clientModules) {
    _clientModules = checkNotNull(clientModules, "Client modules cannot be null");
  }

  @Override
  protected void configure() {
    _clientModules.forEach(this::install);
  }
}
