package com.github.ptracker.util;

public interface IdGenerator<ID_TYPE> {

  ID_TYPE getNextId();
}
