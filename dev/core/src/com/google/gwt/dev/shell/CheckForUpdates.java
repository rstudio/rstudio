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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.About;
import com.google.gwt.dev.GwtVersion;
import com.google.gwt.dev.shell.ie.CheckForUpdatesIE6;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Orchestrates a best-effort attempt to find out if a new version of GWT is
 * available.
 */
public class CheckForUpdates {

  /**
   * Returns the result of an update check.
   */
  public interface UpdateResult {
    /**
     * @return the new version of GWT available.
     */
    GwtVersion getNewVersion();

    /**
     * @return the URL for details about the new version.
     */
    URL getURL();
  }

  public static final long ONE_DAY = 24 * 60 * 60 * 1000;
  public static final long ONE_MINUTE = 60 * 1000;

  // System properties used by CheckForUpdates
  protected static final String PROPERTY_DEBUG_HTTP_GET = "gwt.debugLowLevelHttpGet";
  protected static final String PROPERTY_FORCE_NONNATIVE = "gwt.forceVersionCheckNonNative";
  protected static final String PROPERTY_PREFS_NAME = "gwt.prefsPathName";
  protected static final String PROPERTY_QUERY_URL = "gwt.forceVersionCheckURL";

  // Log levels -- in general we want the logging of the update process
  // to not be visible to normal users.
  private static final TreeLogger.Type CHECK_ERROR = TreeLogger.DEBUG;
  private static final TreeLogger.Type CHECK_INFO = TreeLogger.SPAM;
  private static final TreeLogger.Type CHECK_SPAM = TreeLogger.SPAM;
  private static final TreeLogger.Type CHECK_WARN = TreeLogger.SPAM;

  // Preferences keys
  private static final String FIRST_LAUNCH = "firstLaunch";
  private static final String HIGHEST_RUN_VERSION = "highestRunVersion";
  private static final String LAST_PING = "lastPing";
  private static final String NEXT_PING = "nextPing";

  // Uncomment one of constants below to try different variations of failure to
  // make sure we never interfere with the app running.

  // Check against a fake server to see failure to contact server.
  // protected static final String QUERY_URL =
  // "http://nonexistenthost:1111/gwt/currentversion.xml";

  // Check 404 on a real location that doesn't have the file.
  // protected static final String QUERY_URL =
  // "http://www.google.com/gwt/currentversion.xml";

  // A test URL for seeing it actually work in a sandbox.
  // protected static final String QUERY_URL =
  // "http://localhost/gwt/currentversion.xml";

  // The real URL that should be used.
  private static final String QUERY_URL = "http://tools.google.com/webtoolkit/currentversion.xml";

  public static FutureTask<UpdateResult> checkForUpdatesInBackgroundThread(
      final TreeLogger logger, final long minCheckMillis) {
    final String entryPoint = computeEntryPoint();
    FutureTask<UpdateResult> task = new FutureTask<UpdateResult>(
        new Callable<UpdateResult>() {
          @Override
          public UpdateResult call() throws Exception {
            final CheckForUpdates updateChecker = createUpdateChecker(logger,
                entryPoint);
            return updateChecker == null ? null
                : updateChecker.check(minCheckMillis);
          }
        });
    Thread checkerThread = new Thread(task, "GWT Update Checker");
    checkerThread.setDaemon(true);
    checkerThread.start();
    return task;
  }

  /**
   * Find the first method named "main" on the call stack and use its class as
   * the entry point.
   */
  public static String computeEntryPoint() {
    Throwable t = new Throwable();
    for (StackTraceElement stackTrace : t.getStackTrace()) {
      if (stackTrace.getMethodName().equals("main")) {
        // Strip package name from main's class
        String className = stackTrace.getClassName();
        int i = className.lastIndexOf('.');
        if (i >= 0) {
          return className.substring(i + 1);
        }
        return className;
      }
    }
    return null;
  }

  public static CheckForUpdates createUpdateChecker(TreeLogger logger) {
    return createUpdateChecker(logger, computeEntryPoint());
  }

  public static CheckForUpdates createUpdateChecker(TreeLogger logger,
      String entryPoint) {
    // Windows has a custom implementation to handle proxies.
    if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")) {
      return new CheckForUpdatesIE6(logger, entryPoint);
    } else {
      return new CheckForUpdates(logger, entryPoint);
    }
  }

  public static void logUpdateAvailable(TreeLogger logger,
      FutureTask<UpdateResult> updater) {
    if (updater != null && updater.isDone()) {
      UpdateResult result = null;
      try {
        result = updater.get(0, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // Silently ignore exception
      } catch (ExecutionException e) {
        // Silently ignore exception
      } catch (TimeoutException e) {
        // Silently ignore exception
      }
      logUpdateAvailable(logger, result);
    }
  }

  public static void logUpdateAvailable(TreeLogger logger,
      UpdateResult result) {
    if (result != null) {
      final URL url = result.getURL();
      logger.log(TreeLogger.WARN, "A new version of GWT ("
          + result.getNewVersion() + ") is available", null, new HelpInfo() {
            @Override
            public String getAnchorText() {
              return "Release Notes";
            }

            @Override
            public URL getURL() {
              return url;
            }
          });
    }
  }

  private static String getTextOfLastElementHavingTag(Document doc,
      String tagName) {
    NodeList nodeList = doc.getElementsByTagName(tagName);
    int n = nodeList.getLength();
    if (n > 0) {
      Element elem = (Element) nodeList.item(n - 1);
      // Assume the first child is the value.
      //
      Node firstChild = elem.getFirstChild();
      if (firstChild != null) {
        String text = firstChild.getNodeValue();
        return text;
      }
    }

    return null;
  }

  private String entryPoint;

  private TreeLogger logger;

  private GwtVersion myVersion;

  /**
   * Create an update checker which will poll a server URL and log a message
   * about an update if available.
   *
   * @param logger TreeLogger to use
   * @param entryPoint the name of the main entry point used for this execution
   */
  public CheckForUpdates(TreeLogger logger, String entryPoint) {
    this.logger = logger;
    this.entryPoint = entryPoint;
    myVersion = About.getGwtVersionObject();
  }

  /**
   * Check for updates and log to the logger if they are available.
   *
   * @return an UpdateResult or null if there is no new update
   */
  public UpdateResult check() {
    return check(0);
  }

  /**
   * Check for updates and log to the logger if they are available.
   *
   * @return an UpdateResult or null if there is no new update
   */
  public UpdateResult check(long minCheckMillis) {
    TreeLogger branch = logger.branch(CHECK_INFO, "Checking for updates");
    try {
      String prefsName = System.getProperty(PROPERTY_PREFS_NAME);
      Preferences prefs;
      if (prefsName != null) {
        prefs = Preferences.userRoot().node(prefsName);
      } else {
        prefs = Preferences.userNodeForPackage(CheckForUpdates.class);
      }

      String queryURL = QUERY_URL;
      String forceCheckURL = System.getProperty(PROPERTY_QUERY_URL);

      if (forceCheckURL != null) {
        branch.log(CHECK_INFO, "Explicit version check URL: " + forceCheckURL);
        queryURL = forceCheckURL;
      }

      // Get our unique user id (based on absolute timestamp).
      //
      long currentTimeMillis = System.currentTimeMillis();
      String firstLaunch = prefs.get(FIRST_LAUNCH, null);
      if (firstLaunch == null) {
        firstLaunch = Long.toHexString(currentTimeMillis);
        prefs.put(FIRST_LAUNCH, firstLaunch);
        branch.log(CHECK_SPAM, "Setting first launch to " + firstLaunch);
      } else {
        branch.log(CHECK_SPAM, "First launch was " + firstLaunch);
      }

      // See if enough time has passed.
      //
      String lastPing = prefs.get(LAST_PING, "0");
      if (lastPing != null) {
        try {
          long lastPingTime = Long.parseLong(lastPing);
          if (currentTimeMillis < lastPingTime + minCheckMillis) {
            // it's not time yet
            branch.log(CHECK_INFO, "Last ping was " + new Date(lastPingTime)
                + ", min wait is " + minCheckMillis + "ms");
            return null;
          }
        } catch (NumberFormatException e) {
          branch.log(CHECK_WARN, "Error parsing last ping time", e);
        }
      }

      // See if it's time for our next ping yet.
      //
      String nextPing = prefs.get(NEXT_PING, "0");
      if (nextPing != null) {
        try {
          long nextPingTime = Long.parseLong(nextPing);
          if (currentTimeMillis < nextPingTime) {
            // it's not time yet
            branch.log(CHECK_INFO, "Next ping is not until "
                + new Date(nextPingTime));
            return null;
          }
        } catch (NumberFormatException e) {
          branch.log(CHECK_WARN, "Error parsing next ping time", e);
        }
      }

      // See if new version is available.
      //
      String url = queryURL + "?v=" + myVersion.toString() + "&id="
          + firstLaunch + "&r=" + About.getGwtSvnRev();
      if (entryPoint != null) {
        url += "&e=" + entryPoint;
      }

      branch.log(CHECK_INFO, "Checking for new version at " + url);

      // Do the HTTP GET.
      //
      byte[] response;
      String fullUserAgent = makeUserAgent();
      if (System.getProperty(PROPERTY_FORCE_NONNATIVE) == null) {
        // Use subclass.
        //
        response = doHttpGet(branch, fullUserAgent, url);
      } else {
        // Use the pure Java version, but it probably doesn't work with proxies.
        //
        response = httpGetNonNative(branch, fullUserAgent, url);
      }

      if (response == null || response.length == 0) {
        // Problem. Quietly fail.
        //
        branch.log(CHECK_ERROR,
            "Failed to obtain current version info via HTTP");
        return null;
      }

      // Parse and process the response.
      // Bad responses will be silently ignored.
      //
      return parseResponse(branch, prefs, response);

    } catch (Throwable e) {
      // Always silently ignore any errors.
      //
      branch.log(CHECK_INFO, "Exception while processing version info", e);
    }
    return null;
  }

  /**
   * Default implementation just uses the platform-independent method. A
   * subclass should override this method for platform-dependent proxy handling,
   * for example.
   *
   * @param branch TreeLogger to use
   * @param userAgent user agent string to send in request
   * @param url URL to fetch
   * @return byte array of response, or null if an error
   */
  protected byte[] doHttpGet(TreeLogger branch, String userAgent, String url) {
    return httpGetNonNative(branch, userAgent, url);
  }

  /**
   * This default implementation uses regular Java HTTP, which doesn't deal with
   * proxies automagically. See the IE6 subclasses for an implementation that
   * does deal with proxies.
   *
   * @param branch TreeLogger to use
   * @param userAgent user agent string to send in request
   * @param url URL to fetch
   * @return byte array of response, or null if an error
   */
  protected byte[] httpGetNonNative(TreeLogger branch, String userAgent,
      String url) {
    Throwable caught;
    InputStream is = null;
    try {
      URL urlToGet = new URL(url);
      URLConnection conn = urlToGet.openConnection();
      conn.setRequestProperty("User-Agent", userAgent);
      is = conn.getInputStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      byte[] response = baos.toByteArray();
      return response;
    } catch (MalformedURLException e) {
      caught = e;
    } catch (IOException e) {
      caught = e;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }

    if (System.getProperty(PROPERTY_DEBUG_HTTP_GET) != null) {
      branch.log(CHECK_ERROR, "Exception in HTTP request", caught);
    }

    return null;
  }

  private void appendUserAgentProperty(StringBuffer sb, String propName) {
    String propValue = System.getProperty(propName);
    if (propValue != null) {
      if (sb.length() > 0) {
        sb.append("; ");
      }
      sb.append(propName);
      sb.append("=");
      sb.append(propValue);
    }
  }

  /**
   * Creates a user-agent string by combining standard Java properties.
   */
  private String makeUserAgent() {
    String ua = "GWT Freshness Checker";

    StringBuffer extra = new StringBuffer();
    appendUserAgentProperty(extra, "java.vendor");
    appendUserAgentProperty(extra, "java.version");
    appendUserAgentProperty(extra, "os.arch");
    appendUserAgentProperty(extra, "os.name");
    appendUserAgentProperty(extra, "os.version");

    if (extra.length() > 0) {
      ua += " (" + extra.toString() + ")";
    }

    return ua;
  }

  private UpdateResult parseResponse(TreeLogger branch, Preferences prefs,
      byte[] response) throws IOException, ParserConfigurationException,
      SAXException {

    branch.log(CHECK_SPAM, "Parsing response (length " + response.length + ")");

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    ByteArrayInputStream bais = new ByteArrayInputStream(response);

    // Parse the XML.
    //
    builder.setErrorHandler(new ErrorHandler() {

      @Override
      public void error(SAXParseException exception) throws SAXException {
        // fail quietly
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        // fail quietly
      }

      @Override
      public void warning(SAXParseException exception) throws SAXException {
        // fail quietly
      }
    });
    Document doc = builder.parse(bais);

    // The latest version number.
    //
    String versionString = getTextOfLastElementHavingTag(doc, "latest-version");
    if (versionString == null) {
      // Not valid; quietly fail.
      //
      branch.log(CHECK_ERROR, "Failed to find <latest-version>");
      return null;
    }
    GwtVersion currentReleasedVersion;
    try {
      currentReleasedVersion = new GwtVersion(versionString.trim());
    } catch (NumberFormatException e) {
      branch.log(CHECK_ERROR, "Bad version: " + versionString, e);
      return null;
    }

    // Ping delay for server-controlled throttling.
    //
    String pingDelaySecsStr = getTextOfLastElementHavingTag(doc,
        "min-wait-seconds");
    int pingDelaySecs = 0;
    if (pingDelaySecsStr == null) {
      // Not valid; quietly fail.
      //
      branch.log(CHECK_ERROR, "Missing <min-wait-seconds>");
      return null;
    }
    try {
      pingDelaySecs = Integer.parseInt(pingDelaySecsStr.trim());
    } catch (NumberFormatException e) {
      // Not a valid number; quietly fail.
      //
      branch.log(CHECK_ERROR, "Bad min-wait-seconds number: "
          + pingDelaySecsStr);
      return null;
    }

    String url = getTextOfLastElementHavingTag(doc, "notification-url");

    if (url == null) {
      // no URL, so write the HTML locally and provide a URL from that

      // Read the HTML.
      //
      String html = getTextOfLastElementHavingTag(doc, "notification");

      if (html == null) {
        // Not valid; quietly fail.
        //
        branch.log(CHECK_ERROR, "Missing <notification>");
        return null;
      }
      PrintWriter writer = null;
      try {
        String tempDir = System.getProperty("java.io.tmpdir");
        File updateHtml = new File(tempDir, "gwt-update-"
            + currentReleasedVersion + ".html");
        writer = new PrintWriter(new FileOutputStream(updateHtml));
        writer.print(html);
        url = "file://" + updateHtml.getAbsolutePath();
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    }

    // Okay -- this is a valid response.
    //
    return processResponse(branch, prefs, currentReleasedVersion,
        pingDelaySecs, url);
  }

  private UpdateResult processResponse(TreeLogger branch, Preferences prefs,
      final GwtVersion serverVersion, int pingDelaySecs, final String notifyUrl) {

    // Record a ping; don't ping again until the delay is up.
    //
    long currentTimeMillis = System.currentTimeMillis();
    long nextPingTime = currentTimeMillis + pingDelaySecs * 1000;
    prefs.put(NEXT_PING, String.valueOf(nextPingTime));
    prefs.put(LAST_PING, String.valueOf(currentTimeMillis));

    branch.log(CHECK_INFO, "Ping delay is " + pingDelaySecs + "; next ping at "
        + new Date(nextPingTime));

    if (myVersion.isNoNagVersion()) {
      // If the version number indicates no nagging about updates, exit here
      // once we have recorded the next ping time. No-nag versions (ie,
      // trunk builds) should also not update the highest version that has been
      // run.
      return null;
    }

    // Update the highest version of GWT that has been run if we are later.
    GwtVersion highestRunVersion = new GwtVersion(prefs.get(
        HIGHEST_RUN_VERSION, null));
    if (myVersion.compareTo(highestRunVersion) > 0) {
      highestRunVersion = myVersion;
      prefs.put(HIGHEST_RUN_VERSION, highestRunVersion.toString());
    }

    // Are we up to date already?
    //
    if (highestRunVersion.compareTo(serverVersion) >= 0) {
      // Yes, we are.
      //
      branch.log(CHECK_INFO, "Server version (" + serverVersion
          + ") is not newer than " + highestRunVersion);
      return null;
    }

    // Commence nagging.
    //
    URL url = null;
    try {
      url = new URL(notifyUrl);
    } catch (MalformedURLException e) {
      logger.log(CHECK_ERROR, "Malformed notify URL: " + notifyUrl, e);
    }
    final URL finalUrl = url;
    return new UpdateResult() {
      @Override
      public GwtVersion getNewVersion() {
        return serverVersion;
      }

      @Override
      public URL getURL() {
        return finalUrl;
      }
    };
  }
}
