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
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.io.Files;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Static utility methods for sending web pages.
 */
class PageUtil {

  private static final String TIME_IN_THE_PAST = "Fri, 01 Jan 1990 00:00:00 GMT";

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
   * Sets all headers that might help to prevent a page from being cached.
   */
  static void setNoCacheHeaders(HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", TIME_IN_THE_PAST);
    response.setDateHeader("Date", new Date().getTime());
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
   * Sends a text file, substituting one variable. (Doesn't preserve line endings.)
   * @param templateVariable the string to replace
   * @param replacement the replacement
   */
  static void sendTemplateFile(String mimeType, File file, String templateVariable,
      String replacement, HttpServletResponse response) throws IOException {

    BufferedReader reader = Files.newReader(file, Charsets.UTF_8);
    try {
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(mimeType);
      PrintWriter out = response.getWriter();
      while (true) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        line = line.replace(templateVariable, replacement);
        out.println(line);
      }
    } finally {
      reader.close();
    }
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
  static void copyStream(InputStream in, OutputStream out) throws IOException {
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

  /**
   * Reads a resource into a String.
   */
  static String loadResource(Class<?> base, String path) throws IOException {
    InputStream resourceInputStream = base.getResourceAsStream(path);
    if (resourceInputStream == null) {
      throw new IOException("Resource " + path + " not found.");
    }
    ByteArrayOutputStream resourceBaos = new ByteArrayOutputStream();
    copyStream(resourceInputStream, resourceBaos);
    return resourceBaos.toString("UTF-8");
  }

  /**
   * Reads a text file into a String.
   */
  static String loadFile(File file) throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      copyStream(in, bytes);
      return bytes.toString("UTF-8");
    } finally {
      in.close();
    }
  }

  /**
   * Writes a String to a file.
   */
  static void writeFile(String path, String content) throws IOException {
    InputStream in = new ByteArrayInputStream(content.getBytes("UTF-8"));
    OutputStream out = new FileOutputStream(path);
    PageUtil.copyStream(in, out);
  }
}
