package com.github.ptracker.graphql.provider;

import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import java.util.Collection;
import java.util.stream.Collectors;
import org.dataloader.DataLoaderRegistry;

import static com.google.common.base.Preconditions.*;


public class FullGraphProvider implements GraphQLModuleProvider {
  private final Collection<GraphQLModuleProvider> _providers;
  private final Module _clientModules;
  private final Module _schemaModules;

  public FullGraphProvider(Collection<GraphQLModuleProvider> providers) {
    _providers = checkNotNull(providers, "Providers cannot be null");
    Collection<Module> clientModules = providers.stream()
        .map(provider -> checkNotNull(provider.getClientModule(), "Client module cannot be null"))
        .collect(Collectors.toList());
    _clientModules = new ModuleCollection(clientModules);
    Collection<Module> schemaModules = providers.stream()
        .map(provider -> checkNotNull(provider.getSchemaModule(), "Schema module cannot be null"))
        .collect(Collectors.toList());
    _schemaModules = new ModuleCollection(schemaModules);
  }

  @Override
  public Module getClientModule() {
    return _clientModules;
  }

  @Override
  public Module getSchemaModule() {
    return _schemaModules;
  }

  @Override
  public void registerDataLoaders(DataLoaderRegistry registry) {
    _providers.forEach(provider -> provider.registerDataLoaders(registry));
  }

  private static class ModuleCollection extends AbstractModule {
    private final Collection<Module> _modules;

    ModuleCollection(Collection<Module> clientModules) {
      _modules = clientModules;
    }

    @Override
    protected void configure() {
      _modules.forEach(this::install);
    }
  }
}
