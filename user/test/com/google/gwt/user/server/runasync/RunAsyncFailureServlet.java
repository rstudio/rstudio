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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for {@link com.google.gwt.dev.jjs.test.RunAsyncFailureTest}.
 */
public class RunAsyncFailureServlet extends HttpServlet {

  private static final boolean DEBUG = false;
  private static final HashMap<String, RealContents> realContentsCache = new HashMap<String, RealContents>();

  private static class RealContents {
    public RealContents(int numBytes, byte[] bytes) {
      this.numBytes = numBytes;
      this.bytes = bytes;
    }
    public int numBytes;
    public byte[] bytes;
  }
  /**
   * Sequence of response codes to send back. SC_OK must be last.
   */
  private static final int[] responses = {
      HttpServletResponse.SC_SERVICE_UNAVAILABLE,
      HttpServletResponse.SC_GATEWAY_TIMEOUT,
      HttpServletResponse.SC_NOT_FOUND,
      HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
      HttpServletResponse.SC_OK,
  };

  private static void debug(String message) {
    if (DEBUG) {
      System.out.println(message);
    }
  }

  HashMap<String, Integer> triesMap = new HashMap<String, Integer>();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String originalUri = req.getRequestURI();
    debug("doGet Original: " + originalUri);
    String uri = originalUri.replace("/runAsyncFailure", "");

    int response = getDesiredResponse(uri);
    RealContents rc = this.getRealContents(req, uri);
    String realContentsAsString = new String(rc.bytes);

    String fragment = uri.substring(uri.lastIndexOf('/') + 1);
      if (!realContentsAsString.contains("DOWNLOAD_FAILURE_TEST")
          || response == HttpServletResponse.SC_OK) {      
      int bytes = 0;
      if (!realContentsAsString.contains("INSTALL_FAILURE_TEST")) {
        OutputStream os = resp.getOutputStream();
        os.write(rc.bytes, 0, rc.numBytes);
        bytes = rc.numBytes;
        os.close();
      }
      
      resp.setContentType("text/javascript");
      resp.setHeader("Cache-Control", "no-cache");
      resp.setContentLength(bytes);
      resp.setStatus(HttpServletResponse.SC_OK);

      debug("doGet: served " + uri + " (" + bytes + " bytes)");
    } else {
      resp.setHeader("Cache-Control", "no-cache");
      resp.sendError(response);

      debug("doGet: sent error " + response + " for " + uri);
    }
  }

  private int getDesiredResponse(String resource) {
    Integer t = triesMap.get(resource);
    int tries = t == null ? 0 : t.intValue();
    triesMap.put(resource, tries + 1);

    return responses[tries % responses.length];
  }

  private RealContents getRealContents(HttpServletRequest req, String uri) throws IOException {
    if (realContentsCache.containsKey(uri)) {
      return realContentsCache.get(uri);
    }
    
    // Delegate the actual data fetch to the main servlet
    String host = req.getLocalName();
    int port = req.getLocalPort();
    String realUrl = "http://" + host + ":" + port + uri;
    debug("Fetching: " + realUrl);
    int bytes = 0;
    byte[] data = new byte[32768];
    try {
      URL url = new URL(realUrl);
      InputStream is = url.openStream();

      bytes = is.read(data);
      is.close();
    } catch (IOException e) {
      debug("IOException fetching real data: " + e);
      throw e;
    }

    RealContents rc = new RealContents(bytes, data);
    realContentsCache.put(uri, rc);
    return rc;
  }
}
