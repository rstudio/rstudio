/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.shell.jetty;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.shell.ServletContainer;
import com.google.gwt.dev.shell.ServletContainerLauncher;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;

import java.io.File;

import javax.servlet.Filter;

/**
 * A launcher for an embedded Jetty server.
 */
public class JettyLauncher implements ServletContainerLauncher {

  /**
   * An adapter for the Jetty logging system to GWT's TreeLogger. This
   * implementation class is only public to allow {@link Log} to instantiate it.
   * 
   * The weird static data / default construction setup is a game we play with
   * {@link Log}'s static initializer to prevent the initial log message from
   * going to stderr.
   */
  public static final class JettyTreeLogger implements Logger {
    private static Type nextBranchLevel;
    private static TreeLogger nextLogger;

    /**
     * Returns true if the default constructor can be called.
     */
    public static boolean isDefaultConstructionReady() {
      return nextLogger != null;
    }

    /**
     * Call to set initial state for default construction; must be called again
     * each time before a default instantiation occurs.
     */
    public static void setDefaultConstruction(TreeLogger logger,
        Type branchLevel) {
      if (logger == null || branchLevel == null) {
        throw new NullPointerException();
      }
      nextLogger = logger;
      nextBranchLevel = branchLevel;
    }

    private final Type branchLevel;
    private final TreeLogger logger;

    public JettyTreeLogger() {
      this(nextLogger, nextBranchLevel);
      nextLogger = null;
      nextBranchLevel = null;
    }

    public JettyTreeLogger(TreeLogger logger, Type branchLevel) {
      if (logger == null || branchLevel == null) {
        throw new NullPointerException();
      }
      this.branchLevel = branchLevel;
      this.logger = logger;
    }

    public void debug(String msg, Object arg0, Object arg1) {
      logger.log(TreeLogger.DEBUG, format(msg, arg0, arg1));
    }

    public void debug(String msg, Throwable th) {
      logger.log(TreeLogger.DEBUG, msg, th);
    }

    public Logger getLogger(String name) {
      return new JettyTreeLogger(logger.branch(branchLevel, name), branchLevel);
    }

    public void info(String msg, Object arg0, Object arg1) {
      logger.log(TreeLogger.INFO, format(msg, arg0, arg1));
    }

    public boolean isDebugEnabled() {
      return logger.isLoggable(TreeLogger.DEBUG);
    }

    public void setDebugEnabled(boolean enabled) {
      // ignored
    }

    public void warn(String msg, Object arg0, Object arg1) {
      logger.log(TreeLogger.WARN, format(msg, arg0, arg1));
    }

    public void warn(String msg, Throwable th) {
      logger.log(TreeLogger.WARN, msg, th);
    }

    /**
     * Copied from org.mortbay.log.StdErrLog.
     */
    private String format(String msg, Object arg0, Object arg1) {
      int i0 = msg.indexOf("{}");
      int i1 = i0 < 0 ? -1 : msg.indexOf("{}", i0 + 2);

      if (arg1 != null && i1 >= 0) {
        msg = msg.substring(0, i1) + arg1 + msg.substring(i1 + 2);
      }
      if (arg0 != null && i0 >= 0) {
        msg = msg.substring(0, i0) + arg0 + msg.substring(i0 + 2);
      }
      return msg;
    }
  }

  private static class JettyServletContainer implements ServletContainer {

    private final int actualPort;
    private final File appRootDir;
    private final TreeLogger logger;
    private final WebAppContext wac;

    public JettyServletContainer(TreeLogger logger, WebAppContext wac,
        int actualPort, File appRootDir) {
      this.logger = logger;
      this.wac = wac;
      this.actualPort = actualPort;
      this.appRootDir = appRootDir;
    }

    public int getPort() {
      return actualPort;
    }

    public void refresh() throws UnableToCompleteException {
      String msg = "Reloading web app to reflect changes in "
          + appRootDir.getAbsolutePath();
      TreeLogger branch = logger.branch(TreeLogger.INFO, msg);
      try {
        wac.stop();
      } catch (Exception e) {
        branch.log(TreeLogger.ERROR, "Unable to stop embedded Jetty server", e);
        throw new UnableToCompleteException();
      }

      try {
        wac.start();
      } catch (Exception e) {
        branch.log(TreeLogger.ERROR, "Unable to stop embedded Jetty server", e);
        throw new UnableToCompleteException();
      }

      branch.log(TreeLogger.INFO, "Reload completed successfully");
    }

    public void stop() throws UnableToCompleteException {
      TreeLogger branch = logger.branch(TreeLogger.INFO,
          "Stopping Jetty server");
      try {
        wac.stop();
      } catch (Exception e) {
        branch.log(TreeLogger.ERROR, "Unable to stop embedded Jetty server", e);
        throw new UnableToCompleteException();
      }
      branch.log(TreeLogger.INFO, "Stopped successfully");
    }
  }

  @SuppressWarnings("unchecked")
  public ServletContainer start(TreeLogger logger, int port, File appRootDir,
      Filter shellServletFilter) throws UnableToCompleteException {
    checkStartParams(logger, port, appRootDir);

    // The dance we do with Jetty's logging system.
    System.setProperty("VERBOSE", "true");
    JettyTreeLogger.setDefaultConstruction(logger, TreeLogger.INFO);
    System.setProperty("org.mortbay.log.class", JettyTreeLogger.class.getName());
    // Force initialization.
    Log.isDebugEnabled();
    if (JettyTreeLogger.isDefaultConstructionReady()) {
      // The log system was already initialized and did not use our
      // newly-constructed logger, set it explicitly now.
      Log.setLog(new JettyTreeLogger());
    }

    Server server = new Server();
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setPort(port);
    connector.setHost("127.0.0.1");
    server.addConnector(connector);

    // Create a new web app in the war directory.
    WebAppContext wac = new WebAppContext(appRootDir.getAbsolutePath(), "/");

    // Prevent file locking on windows; pick up file changes.
    wac.getInitParams().put(
        "org.mortbay.jetty.servlet.Default.useFileMappedBuffer", "false");

    // Setup the shell servlet filter to generate nocache.js files (and run
    // the hosted mode linker stack.
    FilterHolder filterHolder = new FilterHolder();
    filterHolder.setFilter(shellServletFilter);
    wac.addFilter(filterHolder, "/*", Handler.ALL);

    server.setHandler(wac);
    server.setStopAtShutdown(true);

    try {
      server.start();
      int actualPort = connector.getPort();
      return new JettyServletContainer(logger, wac, actualPort, appRootDir);
    } catch (Exception e) {
      logger.log(TreeLogger.ERROR, "Unable to start embedded Jetty server", e);
      throw new UnableToCompleteException();
    }
  }

  private void checkStartParams(TreeLogger logger, int port, File appRootDir) {
    if (logger == null) {
      throw new NullPointerException("logger cannot be null");
    }

    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException(
          "port must be either 0 (for auto) or less than 65536");
    }

    if (appRootDir == null) {
      throw new NullPointerException("app root direcotry cannot be null");
    }
  }

}
