/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.shell.tomcat;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.impl.ClassPathEntry;
import com.google.gwt.dev.resource.impl.PathPrefix;
import com.google.gwt.dev.resource.impl.PathPrefixSet;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.shell.WorkDirs;
import com.google.gwt.dev.util.Util;

import org.apache.catalina.Connector;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Embedded;
import org.apache.catalina.startup.HostConfig;
import org.apache.coyote.tomcat5.CoyoteConnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Wraps an instance of the Tomcat web server used in hosted mode.
 */
public class EmbeddedTomcatServer {

  static EmbeddedTomcatServer sTomcat;

  public static int getPort() {
    return sTomcat.port;
  }

  public static String start(TreeLogger topLogger, int port, WorkDirs workDirs) {
    return start(topLogger, port, workDirs, true);
  }

  public static synchronized String start(TreeLogger topLogger, int port,
      WorkDirs workDirs, boolean shouldAutoGenerateResources) {
    if (sTomcat != null) {
      throw new IllegalStateException("Embedded Tomcat is already running");
    }

    try {
      new EmbeddedTomcatServer(topLogger, port, workDirs,
          shouldAutoGenerateResources);
      return null;
    } catch (LifecycleException e) {
      String msg = e.getMessage();
      if (msg != null && msg.indexOf("already in use") != -1) {
        msg = "Port "
            + port
            + " is already is use; you probably still have another session active";
      } else {
        msg = "Unable to start the embedded Tomcat server; double-check that your configuration is valid";
      }
      return msg;
    }
  }

  // Stop the embedded Tomcat server.
  //
  public static synchronized void stop() {
    if (sTomcat != null) {
      try {
        sTomcat.catEmbedded.stop();
      } catch (LifecycleException e) {
        // There's nothing we can really do about this and the logger is
        // gone in many scenarios, so we just ignore it.
        //
      } finally {
        sTomcat = null;
      }
    }
  }

  /**
   * Returns what local port the Tomcat connector is running on.
   * 
   * When starting Tomcat with port 0 (i.e. choose an open port), there is just
   * no way to figure out what port it actually chose. So we're using pure
   * hackery to steal the port via reflection. The only works because we bundle
   * Tomcat with GWT and know exactly what version it is.
   */
  private static int computeLocalPort(Connector connector) {
    Throwable caught = null;
    try {
      Field phField = CoyoteConnector.class.getDeclaredField("protocolHandler");
      phField.setAccessible(true);
      Object protocolHandler = phField.get(connector);

      Field epField = protocolHandler.getClass().getDeclaredField("ep");
      epField.setAccessible(true);
      Object endPoint = epField.get(protocolHandler);

      Field ssField = endPoint.getClass().getDeclaredField("serverSocket");
      ssField.setAccessible(true);
      ServerSocket serverSocket = (ServerSocket) ssField.get(endPoint);

      return serverSocket.getLocalPort();
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchFieldException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    }
    throw new RuntimeException(
        "Failed to retrieve the startup port from Embedded Tomcat", caught);
  }

  private Embedded catEmbedded;

  private Engine catEngine;

  private StandardHost catHost = null;

  private int port;

  private final TreeLogger startupBranchLogger;

  private EmbeddedTomcatServer(final TreeLogger topLogger, int listeningPort,
      final WorkDirs workDirs, final boolean shouldAutoGenerateResources)
      throws LifecycleException {
    if (topLogger == null) {
      throw new NullPointerException("No logger specified");
    }

    final TreeLogger logger = topLogger.branch(TreeLogger.INFO,
        "Starting HTTP on port " + listeningPort, null);

    startupBranchLogger = logger;

    // Make myself the one static instance.
    // NOTE: there is only one small implementation reason that this has
    // to be a singleton, which is that the commons logger LogFactory insists
    // on creating your logger class which must have a constructor with
    // exactly one String argument, and since we want LoggerAdapter to delegate
    // to the logger instance available through instance host, there is no
    // way I can think of to delegate without accessing a static field.
    // An inner class is almost right, except there's no outer instance.
    //
    sTomcat = this;

    // Assume the working directory is simply the user's current directory.
    //
    File topWorkDir = new File(System.getProperty("user.dir"));

    // Tell Tomcat its base directory so that it won't complain.
    //
    String catBase = System.getProperty("catalina.base");
    if (catBase == null) {
      // we (briefly) supported catalina.base.create, so let's not cut support
      // until the deprecated sunset
      catBase = System.getProperty("catalina.base.create");
      if (catBase != null) {
        logger.log(TreeLogger.WARN, "catalina.base.create is deprecated.  " +
            "Use catalina.base, and it will be created if necessary.");
        topWorkDir = new File(catBase);
      }
      catBase = generateDefaultCatalinaBase(logger, topWorkDir);
      System.setProperty("catalina.base", catBase);
    }

    // Some debug messages for ourselves.
    //
    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger.log(TreeLogger.DEBUG, "catalina.base = " + catBase, null);
    }

    // Set up the logger that will be returned by the Commons logging factory.
    //
    String adapterClassName = CommonsLoggerAdapter.class.getName();
    System.setProperty("org.apache.commons.logging.Log", adapterClassName);

    // And set up an adapter that will work with the Catalina logger family.
    //
    Logger catalinaLogger = new CatalinaLoggerAdapter(topLogger);

    // Create an embedded server.
    //
    catEmbedded = new Embedded();
    catEmbedded.setDebug(0);
    catEmbedded.setLogger(catalinaLogger);

    // The embedded engine is called "gwt".
    //
    catEngine = catEmbedded.createEngine();
    catEngine.setName("gwt");
    catEngine.setDefaultHost("localhost");
    catEngine.setParentClassLoader(this.getClass().getClassLoader());

    // It answers localhost requests.
    //
    // String appBase = fCatalinaBaseDir.getAbsolutePath();
    String appBase = catBase + "/webapps";
    catHost = (StandardHost) catEmbedded.createHost("localhost", appBase);

    // Hook up a host config to search for and pull in webapps.
    //
    HostConfig hostConfig = new HostConfig();
    catHost.addLifecycleListener(hostConfig);

    // Hook pre-install events so that we can add attributes to allow loaded
    // instances to find their development instance host.
    //
    catHost.addContainerListener(new ContainerListener() {
      public void containerEvent(ContainerEvent event) {
        if (StandardHost.PRE_INSTALL_EVENT.equals(event.getType())) {
          StandardContext webapp = (StandardContext) event.getData();
          publishShellLoggerAttribute(logger, topLogger, webapp);
          publishShellWorkDirsAttribute(logger, workDirs, webapp);
          publishShouldAutoGenerateResourcesAttribute(logger,
              shouldAutoGenerateResources, webapp);
        }
      }
    });

    // Tell the engine about the host.
    //
    catEngine.addChild(catHost);
    catEngine.setDefaultHost(catHost.getName());

    // Tell the embedded manager about the engine.
    //
    catEmbedded.addEngine(catEngine);
    InetAddress nullAddr = null;
    Connector connector = catEmbedded.createConnector(nullAddr, listeningPort,
        false);
    catEmbedded.addConnector(connector);

    // start up!
    catEmbedded.start();
    port = computeLocalPort(connector);

    if (port != listeningPort) {
      if (logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO, "HTTP listening on port " + port, null);
      }
    }
  }

  public TreeLogger getLogger() {
    return startupBranchLogger;
  }

  /*
   * Assumes that the leaf is a file (not a directory).
   */
  private void copyFileNoOverwrite(TreeLogger logger, String srcResName,
      Resource srcRes, File catBase) {

    File dest = new File(catBase, srcResName);
    try {
      // Only copy if src is newer than desc.
      long srcLastModified = srcRes.getLastModified();
      long dstLastModified = dest.lastModified();

      if (srcLastModified < dstLastModified) {
        // Don't copy over it.
        if (logger.isLoggable(TreeLogger.SPAM)) {
          logger.log(TreeLogger.SPAM, "Source is older than existing: "
              + dest.getAbsolutePath(), null);
        }
        return;
      } else if (srcLastModified == dstLastModified) {
        // Exact same time; quietly don't overwrite.
        return;
      } else if (dest.exists()) {
        // Warn about the overwrite
        logger.log(TreeLogger.WARN, "Overwriting existing file '"
            + dest.getAbsolutePath() + "' with '" + srcRes.getLocation()
            + "', which has a newer timestamp");
      }

      // Make dest directories as required.
      File destParent = dest.getParentFile();
      if (destParent != null) {
        // No need to check mkdirs result because IOException later anyway.
        destParent.mkdirs();
      }

      Util.copy(srcRes.openContents(), new FileOutputStream(dest));
      dest.setLastModified(srcLastModified);

      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, "Wrote: " + dest.getAbsolutePath(), null);
      }
    } catch (IOException e) {
      logger.log(TreeLogger.WARN, "Failed to write: " + dest.getAbsolutePath(),
          e);
    } 
  }

  /**
   * Extracts a valid catalina base instance from the classpath. Does not
   * overwrite any existing files.
   */
  private String generateDefaultCatalinaBase(TreeLogger logger, File workDir) {
    logger = logger.branch(
        TreeLogger.TRACE,
        "Property 'catalina.base' not specified; checking for a standard catalina base image instead",
        null);

    // Recursively copies out files and directories
    String tomcatEtcDir = "com/google/gwt/dev/etc/tomcat/";
    Map<String, Resource> resourceMap = null;
    Throwable caught = null;
    try {
      resourceMap = getResourcesFor(logger, tomcatEtcDir);
    } catch (URISyntaxException e) {
      caught = e;
    } catch (IOException e) {
      caught = e;
    }

    File catBase = new File(workDir, "tomcat");
    if (resourceMap == null || resourceMap.isEmpty()) {
      logger.log(TreeLogger.WARN, "Could not find " + tomcatEtcDir, caught);
    } else {
      for (Entry<String, Resource> entry : resourceMap.entrySet()) {
        copyFileNoOverwrite(logger, entry.getKey(), entry.getValue(), catBase);
      }
    }

    return catBase.getAbsolutePath();
  }

  /**
   * Hacky, but fast.
   */
  private Map<String, Resource> getResourcesFor(TreeLogger logger,
      String tomcatEtcDir) throws URISyntaxException, IOException {
    ClassLoader contextClassLoader = this.getClass().getClassLoader();
    URL url = contextClassLoader.getResource(tomcatEtcDir);
    if (url == null) {
      return null;
    }
    String prefix = "";
    String urlString = url.toString();
    if (urlString.startsWith("jar:")) {
      assert urlString.toLowerCase(Locale.ENGLISH).contains(".jar!/"
          + tomcatEtcDir);
      urlString = urlString.substring(4, urlString.indexOf('!'));
      url = new URL(urlString);
      prefix = tomcatEtcDir;
    } else if (urlString.startsWith("zip:")) {
      assert urlString.toLowerCase(Locale.ENGLISH).contains(".zip!/"
          + tomcatEtcDir);
      urlString = urlString.substring(4, urlString.indexOf('!'));
      url = new URL(urlString);
      prefix = tomcatEtcDir;
    }
    ClassPathEntry entry = ResourceOracleImpl.createEntryForUrl(logger, url);
    assert (entry != null);
    ResourceOracleImpl resourceOracle = new ResourceOracleImpl(
        Collections.singletonList(entry));
    PathPrefixSet pathPrefixSet = new PathPrefixSet();
    PathPrefix pathPrefix = new PathPrefix(prefix, null, true);
    pathPrefixSet.add(pathPrefix);
    resourceOracle.setPathPrefixes(pathPrefixSet);
    ResourceOracleImpl.refresh(logger, resourceOracle);
    Map<String, Resource> resourceMap = resourceOracle.getResourceMap();
    return resourceMap;
  }

  private void publishAttributeToWebApp(TreeLogger logger,
      StandardContext webapp, String attrName, Object attrValue) {
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Adding attribute  '" + attrName
          + "' to web app '" + webapp.getName() + "'", null);
    }
    webapp.getServletContext().setAttribute(attrName, attrValue);
  }

  /**
   * Publish the shell's tree logger as an attribute. This attribute is used to
   * find the logger out of the thin air within the shell servlet.
   */
  private void publishShellLoggerAttribute(TreeLogger logger,
      TreeLogger loggerToPublish, StandardContext webapp) {
    final String attr = "com.google.gwt.dev.shell.logger";
    publishAttributeToWebApp(logger, webapp, attr, loggerToPublish);
  }

  /**
   * Publish the shell's work dir as an attribute. This attribute is used to
   * find it out of the thin air within the shell servlet.
   */
  private void publishShellWorkDirsAttribute(TreeLogger logger,
      WorkDirs workDirs, StandardContext webapp) {
    final String attr = "com.google.gwt.dev.shell.workdirs";
    publishAttributeToWebApp(logger, webapp, attr, workDirs);
  }

  /**
   * Publish to the web app whether it should automatically generate resources.
   */
  private void publishShouldAutoGenerateResourcesAttribute(TreeLogger logger,
      boolean shouldAutoGenerateResources, StandardContext webapp) {
    publishAttributeToWebApp(logger, webapp,
        "com.google.gwt.dev.shell.shouldAutoGenerateResources",
        shouldAutoGenerateResources);
  }
}
