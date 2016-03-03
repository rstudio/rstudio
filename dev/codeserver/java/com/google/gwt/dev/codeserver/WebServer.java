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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.codeserver.CompileDir.PolicyFile;
import com.google.gwt.dev.codeserver.Pages.ErrorPage;
import com.google.gwt.dev.json.JsonObject;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
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

  static final Pattern STRONG_NAME = Pattern.compile("[\\dA-F]{32}");

  private static final Pattern CACHE_JS_FILE = Pattern.compile("/(" + STRONG_NAME + ").cache.js$");

  private static final MimeTypes MIME_TYPES = new MimeTypes();

  private static final String TIME_IN_THE_PAST = "Mon, 01 Jan 1990 00:00:00 GMT";

  private final SourceHandler sourceHandler;
  private final SymbolMapHandler symbolMapHandler;
  private final JsonExporter jsonExporter;
  private final OutboxTable outboxes;
  private final JobRunner runner;
  private final JobEventTable eventTable;

  private final String bindAddress;
  private final int port;

  private Server server;

  WebServer(SourceHandler handler, SymbolMapHandler symbolMapHandler, JsonExporter jsonExporter,
      OutboxTable outboxes, JobRunner runner, JobEventTable eventTable, String bindAddress,
      int port) {
    this.sourceHandler = handler;
    this.symbolMapHandler = symbolMapHandler;
    this.jsonExporter = jsonExporter;
    this.outboxes = outboxes;
    this.runner = runner;
    this.eventTable = eventTable;
    this.bindAddress = bindAddress;
    this.port = port;
  }

  void start(final TreeLogger logger) throws UnableToCompleteException {

    Server newServer = new Server();
    ServerConnector connector = new ServerConnector(newServer);
    connector.setHost(bindAddress);
    connector.setPort(port);
    connector.setReuseAddress(false);
    connector.setSoLingerTime(0);
    newServer.addConnector(connector);

    ServletContextHandler newHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    newHandler.setContextPath("/");
    newHandler.addServlet(new ServletHolder(new HttpServlet() {
      @Override
      protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
        handleRequest(request.getPathInfo(), request, response, logger);
      }
    }), "/*");
    newHandler.addFilter(GzipFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    newServer.setHandler(newHandler);
    try {
      newServer.start();
    } catch (Exception e) {
      logger.log(TreeLogger.ERROR, "cannot start web server", e);
      throw new UnableToCompleteException();
    }
    this.server = newServer;
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
   * @param outputModuleName the module name that the GWT compiler used in its output.
   */
  public File getCurrentWarDir(String outputModuleName) {
    return outboxes.findByOutputModuleName(outputModuleName).getWarDir();
  }

  private void handleRequest(String target, HttpServletRequest request,
      HttpServletResponse response, TreeLogger parentLogger)
      throws IOException {

    if (request.getMethod().equalsIgnoreCase("get")) {

      TreeLogger logger = parentLogger.branch(Type.TRACE, "GET " + target);

      Response page = doGet(target, request, logger);
      if (page == null) {
        logger.log(Type.WARN, "not handled: " + target);
        return;
      }

      setHandled(request);
      if (!target.endsWith(".cache.js")) {
        // Make sure IE9 doesn't cache any pages.
        // (Nearly all pages may change on server restart.)
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", TIME_IN_THE_PAST);
        response.setDateHeader("Date", new Date().getTime());
      }
      page.send(request, response, logger);
    }
  }

  /**
   * Returns the page that should be sent in response to a GET request, or null for no response.
   */
  private Response doGet(String target, HttpServletRequest request, TreeLogger logger)
      throws IOException {

    if (target.equals("/")) {
      JsonObject json = jsonExporter.exportFrontPageVars();
      return Pages.newHtmlPage("config", json, "frontpage.html");
    }

    if (target.equals("/dev_mode_on.js")) {
      JsonObject json = jsonExporter.exportDevModeOnVars();
      return Responses.newJavascriptResponse("__gwt_codeserver_config", json,
          "dev_mode_on.js");
    }

    // Recompile on request from the bookmarklet.
    // This is a GET because a bookmarklet can call it from a different origin (JSONP).
    if (target.startsWith("/recompile/")) {
      String moduleName = target.substring("/recompile/".length());
      Outbox box = outboxes.findByOutputModuleName(moduleName);
      if (box == null) {
        return new ErrorPage("No such module: " + moduleName);
      }

      // We are passing properties from an unauthenticated GET request directly to the compiler.
      // This should be safe, but only because these are binding properties. For each binding
      // property, you can only choose from a set of predefined values. So all an attacker can do is
      // cause a spurious recompile, resulting in an unexpected permutation being loaded later.
      //
      // It would be unsafe to allow a configuration property to be changed.
      Job job = box.makeJob(getBindingProperties(request), logger);
      runner.submit(job);
      Job.Result result = job.waitForResult();
      JsonObject json = jsonExporter.exportRecompileResponse(result);
      return Responses.newJsonResponse(json);
    }

    if (target.startsWith("/clean")) {
      JsonObject json = null;
      try {
        runner.clean(logger, outboxes);
        json = jsonExporter.exportOk("Cleaned disk caches.");
      } catch (ExecutionException e) {
        json = jsonExporter.exportError(e.getMessage());
      }
      return Responses.newJsonResponse(json);
    }

    // GET the Js that knows how to request the specific permutation recompile.
    if (target.startsWith("/recompile-requester/")) {
      String moduleName = target.substring("/recompile-requester/".length());
      Outbox box = outboxes.findByOutputModuleName(moduleName);
      if (box == null) {
        return new ErrorPage("No such module: " + moduleName);
      }

      try {
        String recompileJs = runner.getRecompileJs(logger, box);
        return Responses.newJavascriptResponse(recompileJs);
      } catch (ExecutionException e) {
        // Already logged.
        return new ErrorPage("Failed to generate the Js recompile requester.");
      }
    }

    if (target.startsWith("/log/")) {
      String moduleName = target.substring("/log/".length());
      Outbox box = outboxes.findByOutputModuleName(moduleName);
      if (box == null) {
        return new ErrorPage("No such module: " + moduleName);
      } else if (box.containsStubCompile()) {
        return new ErrorPage("This module hasn't been compiled yet.");
      } else {
        return makeLogPage(box);
      }
    }

    if (target.equals("/favicon.ico")) {
      InputStream faviconStream = getClass().getResourceAsStream("favicon.ico");
      if (faviconStream == null) {
        return new ErrorPage("icon not found");
      }
      // IE8 will not load the favicon in an img tag with the default MIME type,
      // so use "image/x-icon" instead.
      return Responses.newBinaryStreamResponse("image/x-icon", faviconStream);
    }

    if (target.equals("/policies/")) {
      return makePolicyIndexPage();
    }

    if (target.equals("/progress")) {
      // TODO: return a list of progress objects here, one for each job.
      JobEvent event = eventTable.getCompilingJobEvent();

      JsonObject json;
      if (event == null) {
        json = new JsonObject();
        json.put("status", "idle");
      } else {
        json = jsonExporter.exportProgressResponse(event);
      }
      return Responses.newJsonResponse(json);
    }

    Matcher matcher = SAFE_MODULE_PATH.matcher(target);
    if (matcher.matches()) {
      return makeModulePage(matcher.group(1));
    }

    matcher = SAFE_DIRECTORY_PATH.matcher(target);
    if (matcher.matches() && SourceHandler.isSourceMapRequest(target)) {
      return sourceHandler.handle(target, request, logger);
    }

    matcher = SAFE_FILE_PATH.matcher(target);
    if (matcher.matches()) {
      if (SourceHandler.isSourceMapRequest(target)) {
        return sourceHandler.handle(target, request, logger);
      }
      if (SymbolMapHandler.isSymbolMapRequest(target)) {
        return symbolMapHandler.handle(target, request, logger);
      }
      if (target.startsWith("/policies/")) {
        return makePolicyFilePage(target);
      }
      return makeCompilerOutputPage(target);
    }

    logger.log(TreeLogger.WARN, "ignored get request: " + target);
    return null; // not handled
  }

  /**
   * Returns a file that the compiler wrote to its war directory.
   */
  private Response makeCompilerOutputPage(String target) {

    int secondSlash = target.indexOf('/', 1);
    String moduleName = target.substring(1, secondSlash);
    Outbox box = outboxes.findByOutputModuleName(moduleName);
    if (box == null) {
      return new ErrorPage("No such module: " + moduleName);
    }

    final String contentEncoding;
    File file = box.getOutputFile(target);
    if (!file.isFile()) {
      // perhaps it's compressed
      file = box.getOutputFile(target + ".gz");
      if (!file.isFile()) {
        return new ErrorPage("not found: " + file.toString());
      }
      contentEncoding = "gzip";
    } else {
      contentEncoding = null;
    }

    final String sourceMapUrl;
    Matcher match = CACHE_JS_FILE.matcher(target);
    if (match.matches()) {
      String strongName = match.group(1);
      String template = SourceHandler.sourceMapLocationTemplate(moduleName);
      sourceMapUrl = template.replace("__HASH__", strongName);
    } else {
      sourceMapUrl = null;
    }

    String mimeType = guessMimeType(target);
    final Response barePage = Responses.newFileResponse(mimeType, file);

    // Wrap the response to send the extra headers.
    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        // TODO: why do we need this? Looks like Ray added it a long time ago.
        response.setHeader("Access-Control-Allow-Origin", "*");

        if (sourceMapUrl != null) {
          response.setHeader("X-SourceMap", sourceMapUrl);
          response.setHeader("SourceMap", sourceMapUrl);
        }

        if (contentEncoding != null) {
          if (!request.getHeader("Accept-Encoding").contains("gzip")) {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            logger.log(TreeLogger.WARN, "client doesn't accept gzip; bailing");
            return;
          }
          response.setHeader("Content-Encoding", "gzip");
        }

        barePage.send(request, response, logger);
      }
    };
  }

  private Response makeModulePage(String moduleName) {
    Outbox box = outboxes.findByOutputModuleName(moduleName);
    if (box == null) {
      return new ErrorPage("No such module: " + moduleName);
    }

    JsonObject json = jsonExporter.exportModulePageVars(box);
    return Pages.newHtmlPage("config", json, "modulepage.html");
  }

  private Response makePolicyIndexPage() {

    return new Response() {

      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        response.setContentType("text/html");

        HtmlWriter out = new HtmlWriter(response.getWriter());

        out.startTag("html").nl();
        out.startTag("head").nl();
        out.startTag("title").text("Policy Files").endTag("title").nl();
        out.endTag("head");
        out.startTag("body");

        out.startTag("h1").text("Policy Files").endTag("h1").nl();

        for (Outbox box : outboxes.getOutboxes()) {
          List<PolicyFile> policies = box.readRpcPolicyManifest();
          if (!policies.isEmpty()) {
            out.startTag("h2").text(box.getOutputModuleName()).endTag("h2").nl();

            out.startTag("table").nl();
            for (PolicyFile policy : policies) {

              out.startTag("tr");

              out.startTag("td");

              out.startTag("a", "href=", policy.getServiceSourceUrl());
              out.text(policy.getServiceName());
              out.endTag("a");

              out.endTag("td");

              out.startTag("td");

              out.startTag("a", "href=", policy.getUrl());
              out.text(policy.getName());
              out.endTag("a");

              out.endTag("td");

              out.endTag("tr").nl();
            }
            out.endTag("table").nl();
          }
        }

        out.endTag("body").nl();
        out.endTag("html").nl();
      }
    };
  }

  private Response makePolicyFilePage(String target) {

    int secondSlash = target.indexOf('/', 1);
    if (secondSlash < 1) {
      return new ErrorPage("invalid URL for policy file: " + target);
    }

    String rest = target.substring(secondSlash + 1);
    if (rest.contains("/") || !rest.endsWith(".gwt.rpc")) {
      return new ErrorPage("invalid name for policy file: " + rest);
    }

    File fileToSend = outboxes.findPolicyFile(rest);
    if (fileToSend == null) {
      return new ErrorPage("Policy file not found: " + rest);
    }

    return Responses.newFileResponse("text/plain", fileToSend);
  }

  /**
   * Sends the log file as html with errors highlighted in red.
   */
  private Response makeLogPage(final Outbox box) {
    final File file = box.getCompileLog();
    if (!file.isFile()) {
      return new ErrorPage("log file not found");
    }

    return new Response() {

      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        response.setHeader("Content-Style-Type", "text/css");

        HtmlWriter out = new HtmlWriter(response.getWriter());
        out.startTag("html").nl();
        out.startTag("head").nl();
        out.startTag("title").text(box.getOutputModuleName() + " compile log").endTag("title").nl();
        out.startTag("style").nl();
        out.text(".error { color: red; font-weight: bold; }").nl();
        out.endTag("style").nl();
        out.endTag("head").nl();
        out.startTag("body").nl();
        sendLogAsHtml(reader, out);
        out.endTag("body").nl();
        out.endTag("html").nl();
      }
    };
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

  /* visible for testing */
  static String guessMimeType(String filename) {
    String mimeType = MIME_TYPES.getMimeByExtension(filename);
    return mimeType != null ? mimeType : "";
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
        HttpConnection.getCurrentConnection().getHttpChannel().getRequest();
    baseRequest.setHandled(true);
  }
}
