package com.github.ptracker.graphql;

import com.github.ptracker.graphql.api.GraphQLModuleProvider;
import com.github.ptracker.service.StartStopService;
import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;
import static javax.servlet.DispatcherType.*;
import static org.eclipse.jetty.servlet.ServletContextHandler.*;


// TODO: Need an AbstractJettyServer maybe?
public class GraphQLServer implements StartStopService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLServer.class);

  private final Server _server;
  private final int _port;

  public GraphQLServer(int port, GraphQLModuleProvider moduleProvider) {
    checkArgument(port > 0, "Port should be > 0");
    checkNotNull(moduleProvider, "GraphQLModuleProvider cannot be null");

    _port = port;
    _server = new Server(port);
    ServletContextHandler context = new ServletContextHandler(_server, "/", SESSIONS);
    context.addEventListener(new GuiceServletContextListener() {
      @Override
      protected Injector getInjector() {
        return Guice.createInjector(new ServletModule() {
                                      @Override
                                      protected void configureServlets() {
                                        serve("/graphql").with(GraphQLServlet.class);
                                      }
                                    }, new DataLoaderModule(moduleProvider),
            // Part of Rejoiner framework (Provides `@Schema// GraphQLSchema`)
            new SchemaProviderModule(), moduleProvider.getSchemaModule(), moduleProvider.getClientModule());
      }
    });

    context.addFilter(GuiceFilter.class, "/*", EnumSet.of(REQUEST, ASYNC));
    try {
      context.setBaseResource(new PathResource(new File("./src/main/resources").toPath().toRealPath()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    context.addServlet(DefaultServlet.class, "/");
  }

  @Override
  public void start() {
    LOGGER.info("Starting GraphQL server");
    try {
      _server.start();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    LOGGER.info("GraphQL server running on port {}", _port);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (!GraphQLServer.this.isShutdown()) {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("Shutting down GraphQL server since JVM is shutting down");
        try {
          GraphQLServer.this.stop();
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
        System.err.println("GraphQL server shut down");
      }
    }));
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping GraphQL server");
    try {
      _server.stop();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    LOGGER.info("GraphQL server stopped");
  }

  @Override
  public void shutdownNow() {
    stop();
  }

  @Override
  public boolean isShutdown() {
    return !_server.isStarted() || _server.isStopping() || _server.isStopped();
  }

  @Override
  public boolean isTerminated() {
    return !_server.isStarted() || _server.isStopped();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    // TODO
    throw new UnsupportedOperationException("await with timeout is not supported yet");
  }

  @Override
  public void awaitTermination() throws InterruptedException {
    _server.join();
  }
}
