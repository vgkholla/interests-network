package com.github.ptracker.app.entity;

class VerifierUtils {

  static String verifyStringFieldNotNullOrEmpty(String fieldValue, String message) {
    if (fieldValue == null || fieldValue.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return fieldValue;
  }

  private VerifierUtils() {

  }
}
