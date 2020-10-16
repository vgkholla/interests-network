package com.github.ptracker.app.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;

import static com.github.ptracker.app.util.VerifierUtils.*;


public class Prompt {
  private static final String DEFAULT_PATH_SEPARATOR = ">>>";

  private final String _pathSeparator;
  private final Deque<String> _pathComponents = new ArrayDeque<>();

  private String _currentPath;

  public Prompt() {
    this(DEFAULT_PATH_SEPARATOR);
  }

  public Prompt(String pathSeparator) {
    _pathSeparator = verifyStringNotNullOrEmpty(pathSeparator, "Path separator cannot be null");
    recalculateCurrentPath();
  }

  public String prompt() {
    return _currentPath;
  }

  public void push(String component) {
    _pathComponents.push(component);
    recalculateCurrentPath();
  }

  public void pop() {
    _pathComponents.pop();
    recalculateCurrentPath();
  }

  private void recalculateCurrentPath() {
    StringBuilder path = new StringBuilder();
    Iterator<String> components = _pathComponents.descendingIterator();
    while(components.hasNext()) {
      path.append(components.next()).append(_pathSeparator);
    }
    if (path.length() == 0) {
      path.append(_pathSeparator);
    }
    _currentPath = path.toString();
  }
}
