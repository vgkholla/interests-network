package com.github.ptracker.resource.plant;

import com.github.ptracker.entity.Plant;


public class PlantUtils {

  public static void verifyPlant(Plant plant) {
    if (plant.getId() == null || plant.getId().isEmpty()) {
      throw new IllegalArgumentException("Plant does not have an ID");
    }
    if (plant.getName() == null || plant.getName().isEmpty()) {
      throw new IllegalArgumentException("Plant does not have a name");
    }
  }

  private PlantUtils() {
  }

}
