/*
 * Copyright 2012 Google Inc.
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

package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.json.JsonObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Static utility methods for sending web pages.
 *
 * @author skybrian@google.com (Brian Slesinsky)
 */
class PageUtil {

  /**
   * Sends a page represented as a string.
   */
  static void sendString(String mimeType, String page, HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(mimeType);
    response.getWriter().append(page);
  }

  /**
   * Sends an HTML page with some JSON code prepended to it.
   *
   * @param variableName  the name of the variable to set on the "window" object.
   * @param json  the data to embed in the script.
   * @param resourceName  the name of the HTML file to send (in the current directory)
   * @param response  where to send the page
   * @param logger  where to log errors (if any)
   */
  static void sendJsonAndHtml(String variableName, JsonObject json, String resourceName,
      HttpServletResponse response, TreeLogger logger)
      throws IOException {
    URL resource = WebServer.class.getResource(resourceName);
    if (resource == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.ERROR, "resource not found: " + resourceName);
      return;
    }
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("text/html");

    ServletOutputStream outBytes = response.getOutputStream();
    Writer out = new OutputStreamWriter(outBytes, "UTF-8");

    out.append("<script>\n");
    out.append("window." + variableName + " = ");
    json.write(out);
    out.append(";\n");
    out.append("</script>\n");
    out.flush();

    copyStream(resource.openStream(), outBytes);
  }

  static void sendJsonAndJavaScript(String variableName, JsonObject json, String resourceName,
      HttpServletResponse response, TreeLogger logger)
      throws IOException {
    URL resource = WebServer.class.getResource(resourceName);
    if (resource == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.ERROR, "resource not found: " + resourceName);
      return;
    }
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/javascript");

    ServletOutputStream outBytes = response.getOutputStream();
    Writer out = new OutputStreamWriter(outBytes, "UTF-8");

    out.append("window." + variableName + " = ");
    json.write(out);
    out.append(";\n");
    out.flush();

    copyStream(resource.openStream(), outBytes);
  }

  static void sendFile(String mimeType, File file, HttpServletResponse response)
      throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
    sendStream(mimeType, in, response);
  }

  /**
   * Sends a page. Closes pageBytes when done.
   */
  static void sendStream(String mimeType, InputStream pageBytes, HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(mimeType);
    copyStream(pageBytes, response.getOutputStream());
  }

  /**
   * Copies in to out and closes in when done.
   */
  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    try {
      byte[] buffer = new byte[8 * 1024];
      while (true) {
        int bytesRead = in.read(buffer);
        if (bytesRead == -1) {
          return;
        }
        out.write(buffer, 0, bytesRead);
      }
    } finally {
      in.close();
    }
  }
}
