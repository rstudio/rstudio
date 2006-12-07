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

import com.google.gwt.dev.About;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Orchestrates a best-effort attempt to find out if a new version of GWT is
 * available.
 */
public abstract class CheckForUpdates {

  public static interface UpdateAvailableCallback {
    void onUpdateAvailable(String html);
  }

  protected static final String LAST_SERVER_VERSION = "lastServerVersion";
  private static final boolean DEBUG_VERSION_CHECK;
  private static final String FIRST_LAUNCH = "firstLaunch";
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

  private static final int VERSION_PARTS = 3;
  private static final String VERSION_REGEXP = "\\d+\\.\\d+\\.\\d+";

  /**
   * Determines whether the server version is definitively newer than the client
   * version. If any errors occur in the comparison, this method returns false
   * to avoid unwanted erroneous notifications.
   * 
   * @param clientVersion The current client version
   * @param serverVersion The current server version
   * @return true if the server is definitely newer, otherwise false
   */
  protected static boolean isServerVersionNewer(String clientVersion,
      String serverVersion) {
    if (clientVersion == null || serverVersion == null) {
      return false;
    }
    
    // must match expected format
    if (!clientVersion.matches(VERSION_REGEXP)
        || !serverVersion.matches(VERSION_REGEXP)) {
      return false;
    }
    
    // extract the relevant parts
    String[] clientParts = clientVersion.split("\\.");
    String[] serverParts = serverVersion.split("\\.");
    if (clientParts.length != VERSION_PARTS
        || serverParts.length != VERSION_PARTS) {
      return false;
    }

    // examine piece by piece from most significant to least significant
    for (int i = 0; i < VERSION_PARTS; ++i) {
      try {
        int clientPart = Integer.parseInt(clientParts[i]);
        int serverPart = Integer.parseInt(serverParts[i]);
        if (serverPart < clientPart) {
          return false;
        }
        
        if (serverPart > clientPart) {
          return true;
        }
      } catch (NumberFormatException e) {
        return false;
      }
    }

    return false;
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

  private static void parseResponse(Preferences prefs, byte[] response,
      UpdateAvailableCallback callback) throws IOException,
      ParserConfigurationException, SAXException {

    if (DEBUG_VERSION_CHECK) {
      System.out.println("Parsing response (length " + response.length + ")");
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    ByteArrayInputStream bais = new ByteArrayInputStream(response);

    // Parse the XML.
    //
    builder.setErrorHandler(new ErrorHandler() {

      public void error(SAXParseException exception) throws SAXException {
        // fail quietly
      }

      public void fatalError(SAXParseException exception) throws SAXException {
        // fail quietly
      }

      public void warning(SAXParseException exception) throws SAXException {
        // fail quietly
      }
    });
    Document doc = builder.parse(bais);

    // The latest version number.
    //
    String version = getTextOfLastElementHavingTag(doc, "latest-version");
    if (version == null) {
      // Not valid; quietly fail.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Failed to find <latest-version>");
      }
      return;
    } else {
      version = version.trim();
    }

    String[] versionParts = version.split("\\.");
    if (versionParts.length != 3) {
      // Not valid; quietly fail.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Bad version format: " + version);
      }
      return;
    }
    try {
      Integer.parseInt(versionParts[0]);
      Integer.parseInt(versionParts[1]);
      Integer.parseInt(versionParts[2]);
    } catch (NumberFormatException e) {
      // Not valid; quietly fail.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Bad version number: " + version);
      }
      return;
    }

    // Ping delay for server-controlled throttling.
    //
    String pingDelaySecsStr = getTextOfLastElementHavingTag(doc,
        "min-wait-seconds");
    int pingDelaySecs = 0;
    if (pingDelaySecsStr == null) {
      // Not valid; quietly fail.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Missing <min-wait-seconds>");
      }
      return;
    } else {
      try {
        pingDelaySecs = Integer.parseInt(pingDelaySecsStr.trim());
      } catch (NumberFormatException e) {
        // Not a valid number; quietly fail.
        //
        if (DEBUG_VERSION_CHECK) {
          System.out.println("Bad min-wait-seconds number: " + pingDelaySecsStr);
        }
        return;
      }
    }

    // Read the HTML.
    //
    String html = getTextOfLastElementHavingTag(doc, "notification");

    if (html == null) {
      // Not valid; quietly fail.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Missing <notification>");
      }
      return;
    }

    // Okay -- this is a valid response.
    //
    processResponse(prefs, version, pingDelaySecs, html, callback);
  }

  private static void processResponse(Preferences prefs, String version,
      int pingDelaySecs, String html, UpdateAvailableCallback callback) {

    // Record a ping; don't ping again until the delay is up.
    //
    long nextPingTime = System.currentTimeMillis() + pingDelaySecs * 1000;
    prefs.put(NEXT_PING, String.valueOf(nextPingTime));

    if (DEBUG_VERSION_CHECK) {
      System.out.println("Ping delay is " + pingDelaySecs + "; next ping at "
          + new Date(nextPingTime));
    }

    /*
     * Stash the version we got last time for comparison below, and record for
     * next time the version we just got.
     */
    String lastServerVersion = prefs.get(LAST_SERVER_VERSION, null);
    prefs.put(LAST_SERVER_VERSION, version);

    // Are we up to date already?
    //
    if (!isServerVersionNewer(About.GWT_VERSION_NUM, version)) {

      // Yes, we are.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Server version is not newer");
      }
      return;
    }

    // Have we already prompted for this particular server version?
    //
    if (version.equals(lastServerVersion)) {

      // We've already nagged the user once. Don't do it again.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("A notification has already been shown for "
            + version);
      }
      return;
    }

    if (DEBUG_VERSION_CHECK) {
      System.out.println("Server version has changed to " + version
          + "; notification will be shown");
    }

    // Commence nagging.
    //
    callback.onUpdateAvailable(html);
  }

  public void check(final UpdateAvailableCallback callback) {

    try {
      String forceCheckURL = System.getProperty("gwt.forceVersionCheckURL");

      if (forceCheckURL != null && DEBUG_VERSION_CHECK) {
        System.out.println("Explicit version check URL: " + forceCheckURL);
      }

      // Get our unique user id (based on absolute timestamp).
      //
      long currentTimeMillis = System.currentTimeMillis();
      Preferences prefs = Preferences.userNodeForPackage(CheckForUpdates.class);

      // Get our unique user id (based on absolute timestamp).
      //
      String firstLaunch = prefs.get(FIRST_LAUNCH, null);
      if (firstLaunch == null) {
        firstLaunch = Long.toHexString(currentTimeMillis);
        prefs.put(FIRST_LAUNCH, firstLaunch);

        if (DEBUG_VERSION_CHECK) {
          System.out.println("Setting first launch to " + firstLaunch);
        }
      } else {
        if (DEBUG_VERSION_CHECK) {
          System.out.println("First launch was " + firstLaunch);
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
            if (DEBUG_VERSION_CHECK) {
              System.out.println("Next ping is not until "
                  + new Date(nextPingTime));
            }
            return;
          }
        } catch (NumberFormatException e) {
          // ignore
        }
      }

      // See if new version is available.
      //
      String queryURL = forceCheckURL != null ? forceCheckURL : QUERY_URL;
      String url = queryURL + "?v=" + About.GWT_VERSION_NUM + "&id="
          + firstLaunch;

      if (DEBUG_VERSION_CHECK) {
        System.out.println("Checking for new version at " + url);
      }

      // Do the HTTP GET.
      //
      byte[] response;
      String fullUserAgent = makeUserAgent();
      if (System.getProperty("gwt.forceVersionCheckNonNative") == null) {
        // Use subclass.
        //
        response = doHttpGet(fullUserAgent, url);
      } else {
        // Use the pure Java version, but it probably doesn't work with proxies.
        //
        response = httpGetNonNative(fullUserAgent, url);
      }

      if (response == null) {
        // Problem. Quietly fail.
        //
        if (DEBUG_VERSION_CHECK) {
          System.out.println("Failed to obtain current version info via HTTP");
        }
        return;
      }

      // Parse and process the response.
      // Bad responses will be silently ignored.
      //
      parseResponse(prefs, response, callback);

    } catch (Throwable e) {
      // Always silently ignore any errors.
      //
      if (DEBUG_VERSION_CHECK) {
        System.out.println("Exception while processing version info");
        e.printStackTrace();
      }
    }
  }

  protected abstract byte[] doHttpGet(String userAgent, String url);

  /**
   * This default implementation uses regular Java HTTP, which doesn't deal with
   * proxies automagically. See the IE6 subclasses for an implementation that
   * does deal with proxies.
   */
  protected byte[] httpGetNonNative(String userAgent, String url) {
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

    if (System.getProperty("gwt.debugLowLevelHttpGet") != null) {
      caught.printStackTrace();
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

  static {
    // Do this in a static initializer so we can ignore all exceptions.
    //
    boolean debugVersionCheck = false;
    try {
      if (System.getProperty("gwt.debugVersionCheck") != null) {
        debugVersionCheck = true;
      }
    } catch (Throwable e) {
      // Always silently ignore any errors.
      //
    } finally {
      DEBUG_VERSION_CHECK = debugVersionCheck;
    }
  }
}
