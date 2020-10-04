package com.github.ptracker.util;

import java.util.UUID;


public class RandomLongIdGenerator implements IdGenerator<Long> {

  @Override
  public Long getNextId() {
    long generatedNum;
    do {
      generatedNum = UUID.randomUUID().getLeastSignificantBits();
    } while (generatedNum == Long.MIN_VALUE);
    return Math.abs(generatedNum);
  }
}
