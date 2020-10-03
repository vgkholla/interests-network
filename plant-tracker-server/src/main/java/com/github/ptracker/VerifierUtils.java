package com.github.ptracker;

import com.github.ptracker.common.EventMetadata;


public class VerifierUtils {

  public static void verifyStringFieldNotNullOrEmpty(String fieldValue, String entityName, String fieldName) {
    if (fieldValue == null || fieldValue.isEmpty()) {
      throw new IllegalArgumentException(entityName + " does not have a " + fieldName);
    }
  }

  public static void verifyIntNotNegative(int fieldValue, String entityName, String fieldName) {
    if (fieldValue < 0) {
      throw new IllegalArgumentException(fieldName + " in " + entityName + " has negative value of " + fieldValue);
    }
  }

  public static void verifyLongNotNegative(long fieldValue, String entityName, String fieldName) {
    if (fieldValue < 0) {
      throw new IllegalArgumentException(fieldName + " in " + entityName + " has negative value of " + fieldValue);
    }
  }

  public static void verifyEventMetadata(EventMetadata metadata, String entityName, String fieldName) {
    if (metadata == null) {
      throw new IllegalArgumentException(fieldName + " in " + entityName + " containing event metadata is null");
    }
    verifyStringFieldNotNullOrEmpty(metadata.getGardenerId(), entityName, fieldName + "/gardenerId");
    verifyLongNotNegative(metadata.getTimestamp(), entityName, fieldName + "/timestamp");
  }
}
