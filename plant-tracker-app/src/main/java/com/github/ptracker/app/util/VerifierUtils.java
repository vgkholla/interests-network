package com.github.ptracker.app.util;

public class VerifierUtils {

  public static String verifyStringNotNullOrEmpty(String value, String message) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private VerifierUtils() {

  }
}
