package com.github.ptracker.util;

public class RandomStringIdGenerator implements IdGenerator<String> {
  private final String _prefix;
  private final IdGenerator<Long> _longIdGenerator = new RandomLongIdGenerator();

  public RandomStringIdGenerator(String prefix) {
    _prefix = prefix == null ? "" : prefix;
  }

  @Override
  public String getNextId() {
    return _prefix + _longIdGenerator.getNextId();
  }
}
