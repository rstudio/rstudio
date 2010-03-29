/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.server.runasync;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for {@link com.google.gwt.dev.jjs.test.RunAsyncFailureTest}.
 */
public class RunAsyncFailureServlet extends HttpServlet {

  private static final boolean DEBUG = false;
  private static final HashSet<String> errorFragments = new HashSet<String>();

  /**
   * Sequence of response codes to send back. SC_OK must be last.
   */
  private static final int[] responses = {
      HttpServletResponse.SC_SERVICE_UNAVAILABLE,
      HttpServletResponse.SC_GATEWAY_TIMEOUT,
      HttpServletResponse.SC_SERVICE_UNAVAILABLE,
      HttpServletResponse.SC_GATEWAY_TIMEOUT, HttpServletResponse.SC_OK};

  static {
    errorFragments.add("2.cache.js");
  }

  private static void debug(String message) {
    if (DEBUG) {
      System.out.println(message);
    }
  }

  HashMap<String, Integer> triesMap = new HashMap<String, Integer>();

  private int sSerial = 0;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String originalUri = req.getRequestURI();
    debug("doGet: " + originalUri);
    String uri = originalUri.replace("/runAsyncFailure", "");

    int response = getDesiredResponse(uri);
    String fragment = uri.substring(uri.lastIndexOf('/') + 1);
    if (!errorFragments.contains(fragment)
        || response == HttpServletResponse.SC_OK) {
      // Delegate the actual data fetch to the main servlet

      String host = req.getLocalName();
      int port = req.getLocalPort();
      String realUrl = "http://" + host + ":" + port + uri;
      debug("Fetching: " + realUrl);

      int bytes = 0;
      try {
        URL url = new URL(realUrl);
        InputStream is = url.openStream();
        OutputStream os = resp.getOutputStream();

        byte[] data = new byte[8192];
        int nbytes;
        while ((nbytes = is.read(data)) != -1) {
          os.write(data, 0, nbytes);
          bytes += nbytes;
        }
        is.close();
        os.close();
      } catch (IOException e) {
        debug("IOException fetching real data: " + e);
        throw e;
      }

      resp.setContentType("text/javascript");
      resp.setHeader("Cache-Control", "no-cache");
      resp.setContentLength(bytes);
      resp.setStatus(HttpServletResponse.SC_OK);

      debug("doGet: served " + uri + " (" + bytes + " bytes)");
    } else {
      resp.setHeader("Cache-Control", "no-cache");
      resp.sendError(response, "serial=" + getNextSerial());

      debug("doGet: sent error " + response + " for " + uri);
    }
  }

  private int getDesiredResponse(String resource) {
    Integer t = triesMap.get(resource);
    int tries = t == null ? 0 : t.intValue();
    triesMap.put(resource, tries + 1);

    return responses[tries % responses.length];
  }

  private synchronized int getNextSerial() {
    return sSerial++;
  }
}
