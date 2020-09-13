package com.github.inet.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public interface StartStopService {

  void start() throws IOException;

  void stop();

  void shutdownNow();

  boolean isShutdown();

  boolean isTerminated();

  boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

  void awaitTermination() throws InterruptedException;
}
