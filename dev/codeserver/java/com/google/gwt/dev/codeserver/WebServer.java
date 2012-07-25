/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.thirdparty.org.mortbay.jetty.HttpConnection;
import com.google.gwt.thirdparty.org.mortbay.jetty.Request;
import com.google.gwt.thirdparty.org.mortbay.jetty.Server;
import com.google.gwt.thirdparty.org.mortbay.jetty.handler.AbstractHandler;
import com.google.gwt.thirdparty.org.mortbay.jetty.nio.SelectChannelConnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The web server for Super Dev Mode, also known as the code server. The URLs handled include:
 * <ul>
 *   <li>HTML pages for the front page and module pages</li>
 *   <li>JavaScript that implementing the bookmarklets</li>
 *   <li>The web API for recompiling a GWT app</li>
 *   <li>The output files and log files from the GWT compiler</li>
 *   <li>Java source code (for source-level debugging)</li>
 * </ul>
 *
 * <p>EXPERIMENTAL. There is no authentication, encryption, or XSS protection, so this server is
 * only safe to run on localhost.</p>
 */
public class WebServer {

  private static final Pattern SAFE_DIRECTORY =
      Pattern.compile("([a-zA-Z0-9_-]+\\.)*[a-zA-Z0-9_-]+"); // no extension needed

  private static final Pattern SAFE_FILENAME =
      Pattern.compile("([a-zA-Z0-9_-]+\\.)+[a-zA-Z0-9_-]+"); // an extension is required

  private static final Pattern SAFE_MODULE_PATH =
      Pattern.compile("/(" + SAFE_DIRECTORY + ")/$");

  static final Pattern SAFE_DIRECTORY_PATH =
      Pattern.compile("/(" + SAFE_DIRECTORY + "/)+$");

  /* visible for testing */
  static final Pattern SAFE_FILE_PATH =
      Pattern.compile("/(" + SAFE_DIRECTORY + "/)+" + SAFE_FILENAME + "$");

  private static final Pattern SAFE_CALLBACK =
      Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*\\.)*[a-zA-Z_][a-zA-Z0-9_]*");

  private final SourceHandler handler;

  private final Modules modules;

  private final String bindAddress;
  private final int port;
  private final TreeLogger logger;
  private Server server;

  WebServer(SourceHandler handler, Modules modules, String bindAddress, int port,
      TreeLogger logger) {
    this.handler = handler;
    this.modules = modules;
    this.bindAddress = bindAddress;
    this.port = port;
    this.logger = logger;
  }

  public void start() throws UnableToCompleteException {

    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setHost(bindAddress);
    connector.setPort(port);
    connector.setReuseAddress(false);
    connector.setSoLingerTime(0);

    Server server = new Server();
    server.addConnector(connector);
    server.addHandler(new AbstractHandler() {
      @Override
      public void handle(String target, HttpServletRequest request,
          HttpServletResponse response, int port) throws IOException {
        handleRequest(target, request, response);
      }
    });
    try {
      server.start();
    } catch (Exception e) {
      logger.log(TreeLogger.ERROR, "cannot start web server", e);
      throw new UnableToCompleteException();
    }
    this.server = server;
  }

  public int getPort() {
    return port;
  }

  public void stop() throws Exception {
    server.stop();
    server = null;
  }

  /**
   * Returns the location of the compiler output. (Changes after every recompile.)
   */
  public File getCurrentWarDir(String moduleName) {
    return modules.get(moduleName).getWarDir();
  }

  private void handleRequest(String target, HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    if (request.getMethod().equalsIgnoreCase("get")) {
      doGet(target, request, response);
    }
  }

  private void doGet(String target, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (target.equals("/")) {
      setHandled(request);
      JsonObject config = makeConfig();
      PageUtil.sendJsonAndHtml("config", config, "frontpage.html", response, logger);
      return;
    }

    if (target.equals("/dev_mode_on.js")) {
      setHandled(request);
      JsonObject config = makeConfig();
      PageUtil
          .sendJsonAndJavaScript("__gwt_codeserver_config", config, "dev_mode_on.js", response,
              logger);
      return;
    }

    // Recompile on request from the bookmarklet.
    // This is a GET because a bookmarklet can call it from a different origin (JSONP).
    if (target.startsWith("/recompile/")) {
      setHandled(request);
      String moduleName = target.substring("/recompile/".length());
      ModuleState moduleState = modules.get(moduleName);
      if (moduleState == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        logger.log(TreeLogger.WARN, "not found: " + target);
        return;
      }

      // We are passing properties from an unauthenticated GET request directly to the compiler.
      // This should be safe, but only because these are binding properties. For each binding
      // property, you can only choose from a set of predefined values. So all an attacker can do is
      // cause a spurious recompile, resulting in an unexpected permutation being loaded later.
      //
      // It would be unsafe to allow a configuration property to be changed.
      boolean ok = moduleState.recompile(getBindingProperties(request));

      JsonObject config = makeConfig();
      config.put("status", ok ? "ok" : "failed");
      sendJsonpPage(config, request, response);
      return;
    }

    if (target.startsWith("/log/")) {
      setHandled(request);
      String moduleName = target.substring("/log/".length());
      File file = modules.get(moduleName).getCompileLog();
      sendLogPage(moduleName, file, response);
      return;
    }

    if (target.equals("/favicon.ico")) {
      return;
    }

    Matcher matcher = SAFE_MODULE_PATH.matcher(target);
    if (matcher.matches()) {
      setHandled(request);
      sendModulePage(matcher.group(1), response);
      return;
    }

    matcher = SAFE_DIRECTORY_PATH.matcher(target);
    if (matcher.matches() && handler.isSourceMapRequest(target)) {
      setHandled(request);
      handler.handle(target, request, response);
      return;
    }

    matcher = SAFE_FILE_PATH.matcher(target);
    if (matcher.matches()) {
      setHandled(request);
      if (handler.isSourceMapRequest(target)) {
        handler.handle(target, request, response);
        return;
      }
      sendOutputFile(target, request, response);
      return;
    }

    logger.log(TreeLogger.WARN, "ignored get request: " + target);
  }

  private void sendOutputFile(String target, HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    int secondSlash = target.indexOf('/', 1);
    String moduleName = target.substring(1, secondSlash);
    ModuleState moduleState = modules.get(moduleName);

    File file = moduleState.getOutputFile(target);
    if (!file.isFile()) {
      // perhaps it's compressed
      file = moduleState.getOutputFile(target + ".gz");
      if (!file.isFile()) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        logger.log(TreeLogger.WARN, "not found: " + file.toString());
        return;
      }
      if (!request.getHeader("Accept-Encoding").contains("gzip")) {
        response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        logger.log(TreeLogger.WARN, "client doesn't accept gzip; bailing");
        return;
      }
      response.addHeader("Content-Encoding", "gzip");
    }

    String mimeType = guessMimeType(target);
    if (target.endsWith(".cache.js")) {
      response.addHeader("X-SourceMap", SourceHandler.SOURCEMAP_PATH + moduleName +
          "/gwtSourceMap.json");
    }
    response.addHeader("Access-Control-Allow-Origin", "*");
    PageUtil.sendFile(mimeType, file, response);
  }

  private void sendModulePage(String moduleName, HttpServletResponse response) throws IOException {
    ModuleState module = modules.get(moduleName);
    if (module == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.WARN, "module not found: " + moduleName);
      return;
    }
    PageUtil
        .sendJsonAndHtml("config", module.getTemplateVariables(), "modulepage.html", response,
            logger);
  }

  private JsonObject makeConfig() {
    JsonArray moduleNames = new JsonArray();
    for (String module : modules) {
      moduleNames.add(module);
    }
    JsonObject config = JsonObject.create();
    config.put("moduleNames", moduleNames);
    return config;
  }

  private void sendJsonpPage(JsonObject json, HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/javascript");
    PrintWriter out = response.getWriter();

    String callbackExpression = request.getParameter("_callback");
    if (callbackExpression == null || !SAFE_CALLBACK.matcher(callbackExpression).matches()) {
      logger.log(TreeLogger.ERROR, "invalid callback: " + callbackExpression);
      out.print("/* invalid callback parameter */");
      return;
    }

    out.print(callbackExpression + "(");
    json.write(out);
    out.println(");");
  }

  /**
   * Sends the log file as html with errors highlighted in red.
   */
  private void sendLogPage(String moduleName, File file, HttpServletResponse response)
       throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("text/html");
    response.setHeader("Content-Style-Type", "text/css");

    HtmlWriter out = new HtmlWriter(response.getWriter());
    out.startTag("html").nl();
    out.startTag("head").nl();
    out.startTag("title").text(moduleName + " compile log").endTag("title").nl();
    out.startTag("style").nl();
    out.text(".error { color: red; font-weight: bold; }").nl();
    out.endTag("style").nl();
    out.endTag("head").nl();
    out.startTag("body").nl();
    sendLogAsHtml(reader, out);
    out.endTag("body").nl();
    out.endTag("html").nl();
  }

  private static final Pattern ERROR_PATTERN = Pattern.compile("\\[ERROR\\]");

  /**
   * Copies in to out line by line, escaping each line for html characters and highlighting
   * error lines. Closes <code>in</code> when done.
   */
  private static void sendLogAsHtml(BufferedReader in, HtmlWriter out) throws IOException {
    try {
      out.startTag("pre").nl();
      String line = in.readLine();
      while (line != null) {
        Matcher m = ERROR_PATTERN.matcher(line);
        boolean error = m.find();
        if (error) {
          out.startTag("span", "class=", "error");
        }
        out.text(line);
        if (error) {
          out.endTag("span");
        }
        out.nl(); // the readLine doesn't include the newline.
        line = in.readLine();
      }
      out.endTag("pre").nl();
    } finally {
      in.close();
    }
  }

  private static String guessMimeType(String filename) {
    return URLConnection.guessContentTypeFromName(filename);
  }

  /**
   * Returns the binding properties from the web page where dev mode is being used. (As passed in
   * by dev_mode_on.js in a JSONP request to "/recompile".)
   */
  private Map<String, String> getBindingProperties(HttpServletRequest request) {
    Map<String, String> result = new HashMap<String, String>();
    for (Object key : request.getParameterMap().keySet()) {
      String propName = (String) key;
      if (!propName.equals("_callback")) {
        result.put(propName, request.getParameter(propName));
      }
    }
    return result;
  }

  private static void setHandled(HttpServletRequest request) {
    Request baseRequest = (request instanceof Request) ? (Request) request :
        HttpConnection.getCurrentConnection().getRequest();
    baseRequest.setHandled(true);
  }
}
