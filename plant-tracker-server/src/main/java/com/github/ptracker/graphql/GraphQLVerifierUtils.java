package com.github.ptracker.graphql;

import java.util.List;
import java.util.Set;
import org.dataloader.DataLoaderRegistry;


public class GraphQLVerifierUtils {

  public static void verifyDataLoaderRegistryKeysUnassigned(DataLoaderRegistry registry, List<String> keys) {
    Set<String> keysInRegistry = registry.getKeys();
    keys.forEach(key -> {
      if (keysInRegistry.contains(key)) {
        throw new IllegalStateException(key + " is already registered with a different data loader!");
      }
    });
  }

  private GraphQLVerifierUtils() {

  }
}
