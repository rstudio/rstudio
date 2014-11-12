/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.codeserver.Pages.ErrorPage;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.io.ByteStreams;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.thirdparty.guava.common.io.Resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Common HTTP responses other than HTML pages, which are in {@link Pages}.
 */
public class Responses {

  private static final Pattern SAFE_CALLBACK =
      Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*\\.)*[a-zA-Z_][a-zA-Z0-9_]*");

  /**
   * A HTTP response that sends a file.
   */
  static Response newFileResponse(final String mimeType, final File file) {
    if (!file.isFile()) {
      return new ErrorPage("file not found: " + file.toString());
    }

    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mimeType);
        Files.copy(file, response.getOutputStream());
      }
    };
  }

  /**
   * Returns a JSON response. If the request contains a _callback parameter, it will
   * automatically be sent as a JSONP response. Otherwise, it's an AJAX response.
   */
  static Response newJsonResponse(final JsonObject json) {

    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-control", "no-cache");
        PrintWriter out = response.getWriter();

        String callbackExpression = request.getParameter("_callback");
        if (callbackExpression == null) {
          // AJAX
          response.setContentType("application/json");
          json.write(out);
        } else {
          // JSONP
          response.setContentType("application/javascript");
          if (SAFE_CALLBACK.matcher(callbackExpression).matches()) {
            out.print("/* API response */ " + callbackExpression + "(");
            json.write(out);
            out.println(");");
          } else {
            logger.log(TreeLogger.ERROR, "invalid callback: " + callbackExpression);
            // Notice that we cannot execute the callback
            out.print("alert('invalid callback parameter');\n");
            json.write(out);
          }
        }
      }
    };
  }

  /**
   * Sends an entire Js script.
   */
  static Response newJavascriptResponse(final String jsScript) {
    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/javascript");

        ServletOutputStream outBytes = response.getOutputStream();
        Writer out = new OutputStreamWriter(outBytes, "UTF-8");
        out.write(jsScript);
        out.close();
      }
    };
  }

  /**
   * Sends a JavaScript file with some JSON data prepended to it.
   * @param variableName the global variable where the JSON should be stored.
   * @param json the data to include.
   * @param resourceName the name of the JavaScript file.
   */
  static Response newJavascriptResponse(final String variableName, final JsonObject json,
      final String resourceName) {

    final URL resource = WebServer.class.getResource(resourceName);
    if (resource == null) {
      return new ErrorPage("resource not found: " + resourceName);
    }

    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/javascript");

        ServletOutputStream outBytes = response.getOutputStream();
        Writer out = new OutputStreamWriter(outBytes, "UTF-8");

        out.append("window." + variableName + " = ");
        json.write(out);
        out.append(";\n");
        out.flush();

        Resources.copy(resource, outBytes);
      }
    };
  }

  /**
   * Sends a text file, substituting one variable. (Doesn't preserve line endings.)
   * @param templateVariable the string to replace
   * @param replacement the replacement
   */
  static Response newTextTemplateResponse(final String mimeType, final File file,
      final String templateVariable, final String replacement) {
    if (!file.isFile()) {
      return new ErrorPage("file not found: " + file.toString());
    }

    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
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
    };
  }

  /**
   * Creates a page that sends the given stream of bytes.
   * The response will close the stream after sending it.
   * (Beware that if the page is never sent, the file handle will leak.)
   * TODO: fix the callers and get rid of this.
   */
  static Response newBinaryStreamResponse(final String mimeType, final InputStream pageBytes) {
    return new Response() {
      boolean sent = false;

      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        Preconditions.checkState(!sent);

        try {
          response.setStatus(HttpServletResponse.SC_OK);
          response.setContentType(mimeType);
          ByteStreams.copy(pageBytes, response.getOutputStream());
        } finally {
          pageBytes.close();
        }
        sent = true;
      }
    };
  }

  /**
   * Wraps another response in order to log how long it takes to send it.
   */
  static Response newTimedResponse(final Response barePage, final String message) {
    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        long startTime = System.currentTimeMillis();
        barePage.send(request, response, logger);
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.log(TreeLogger.INFO, message + " in " + elapsedTime + " ms");
      }
    };
  }
}
