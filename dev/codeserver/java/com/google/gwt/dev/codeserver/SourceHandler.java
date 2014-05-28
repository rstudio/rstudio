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
import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves Java source files so that a browser's debugger can display them.
 * (This works with browsers that follow the <a
 * href="https://docs.google.com/document/d/1U1RGAehQwRypUTovF1KRlpiOFze0b-_2gc6fAH0KY0k/edit"
 * >Source Map Spec</a>, such as Chrome.)
 *
 * <p>The debugger will first fetch the source map from
 * /sourcemaps/\{module name\}/gwtSourceMap.json. This file contains the names of Java
 * source files to download. Each source file will have a path like
 * "/sourcemaps/\{module name\}/src/{filename}".</p>
 */
class SourceHandler {

  /**
   * The URL prefix for all source maps and Java source code.
   */
  static final String SOURCEMAP_PATH = "/sourcemaps/";

  static final String SOURCEROOT_TEMPLATE_VARIABLE = "$sourceroot_goes_here$";

  private Modules modules;

  private final TreeLogger logger;

  SourceHandler(Modules modules, TreeLogger logger) {
    this.modules = modules;
    this.logger = logger;
  }

  boolean isSourceMapRequest(String target) {
    return getModuleNameFromRequest(target) != null;
  }

  void handle(String target, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String moduleName = getModuleNameFromRequest(target);
    if (moduleName == null) {
      throw new RuntimeException("invalid request (shouldn't happen): " + target);
    }

    String rootDir = SOURCEMAP_PATH + moduleName + "/";
    if (!target.startsWith(rootDir)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.WARN, "returned not found for request: " + target);
      return;
    }

    String rest = target.substring(rootDir.length());

    if (rest.isEmpty()) {
      sendDirectoryListPage(moduleName, response);

    } else if (rest.endsWith("/")) {
      sendFileListPage(moduleName, rest, response);

    } else if (rest.equals("gwtSourceMap.json")) {
      sendSourceMap(moduleName, request, response);

    } else if (rest.endsWith(".java")) {
      sendSourceFile(moduleName, rest, request.getQueryString(), response);

    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.WARN, "returned not found for request: " + target);
    }
  }

  private String getModuleNameFromRequest(String target) {
      if (target.startsWith(SOURCEMAP_PATH)) {
        int prefixLen = SOURCEMAP_PATH.length();
        // find next slash (if any) after prefix
        int endSlash = target.indexOf("/", prefixLen + 1);
        // case 1: /sourcemaps/modulename
        // case 2: /sourcemaps/modulename/path
        return target.substring(prefixLen, endSlash == -1 ? target.length() : endSlash);
      }
      return null;
  }

  private void sendSourceMap(String moduleName, HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    long startTime = System.currentTimeMillis();

    ModuleState moduleState = modules.get(moduleName);
    File sourceMap = moduleState.findSourceMap();

    // Stream the file, substituting the sourceroot variable with the filename.
    // (This is more efficient than parsing the file as JSON.)

    // We need to do this at runtime because we don't know what the hostname will be
    // until we get a request. (For example, some people run the Code Server behind
    // a reverse proxy to support https.)

    String sourceRoot = String.format("http://%s:%d/sourcemaps/%s/", request.getServerName(),
        request.getServerPort(), moduleName);

    PageUtil.sendTemplateFile("application/json", sourceMap,
        "\"" + SOURCEROOT_TEMPLATE_VARIABLE + "\"",
        "\"" + sourceRoot + "\"", response);

    long elapsedTime = System.currentTimeMillis() - startTime;

    logger.log(TreeLogger.WARN, "sent source map for module '" + moduleName +
        "' in " + elapsedTime + " ms");
  }

  private void sendDirectoryListPage(String moduleName, HttpServletResponse response)
      throws IOException {

    SourceMap map = loadSourceMap(moduleName);

    JsonObject config = new JsonObject();
    config.put("moduleName", moduleName);
    JsonArray directories = new JsonArray();
    for (String name : map.getSourceDirectories()) {
      JsonObject dir = new JsonObject();
      dir.put("name", name);
      dir.put("link", name + "/");
      directories.add(dir);
    }
    config.put("directories", directories);
    PageUtil.sendJsonAndHtml("config", config, "directorylist.html", response, logger);
  }

  private void sendFileListPage(String moduleName, String rest, HttpServletResponse response)
      throws IOException {

    SourceMap map = loadSourceMap(moduleName);

    JsonObject config = new JsonObject();
    config.put("moduleName", moduleName);
    config.put("directory", rest);
    JsonArray files = new JsonArray();
    for (String name : map.getSourceFilesInDirectory(rest)) {
      JsonObject file = new JsonObject();
      file.put("name", name);
      file.put("link", name + "?html");
      files.add(file);
    }
    config.put("files", files);
    PageUtil.sendJsonAndHtml("config", config, "filelist.html", response, logger);
  }

  /**
   * Sends an HTTP response containing a Java source. It will be sent as plain text by default,
   * or as HTML if the query string is equal to "html".
   */
  private void sendSourceFile(String moduleName, String sourcePath, String query,
      HttpServletResponse response)
      throws IOException {
    ModuleState moduleState = modules.get(moduleName);
    InputStream pageBytes = moduleState.openSourceFile(sourcePath);

    if (pageBytes == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.WARN, "unknown source file: " + sourcePath);
      return;
    }

    if (query != null && query.equals("html")) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(pageBytes));
      sendSourceFileAsHtml(moduleName, sourcePath, reader, response);
    } else {
      PageUtil.sendStream("text/plain", pageBytes, response);
    }
  }

  /**
   * Sends an HTTP response containing Java source rendered as HTML. The lines of source
   * that have corresponding JavaScript will be highlighted (as determined by reading the
   * source map).
   */
  private void sendSourceFileAsHtml(String moduleName, String sourcePath, BufferedReader lines,
      HttpServletResponse response) throws IOException {

    ReverseSourceMap sourceMap = ReverseSourceMap.load(logger, modules.get(moduleName));

    File sourceFile = new File(sourcePath);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("text/html");

    HtmlWriter out = new HtmlWriter(response.getWriter());
    out.startTag("html").nl();
    out.startTag("head").nl();
    out.startTag("title").text(sourceFile.getName() + " (GWT Code Server)").endTag("title").nl();
    out.startTag("style").nl();
    out.text(".unused { color: grey; }").nl();
    out.text(".used { color: black; }").nl();
    out.text(".title { margin-top: 0; }").nl();
    out.endTag("style").nl();
    out.endTag("head").nl();
    out.startTag("body").nl();

    out.startTag("a", "href=", ".").text(sourceFile.getParent()).endTag("a").nl();
    out.startTag("h1", "class=", "title").text(sourceFile.getName()).endTag("h1").nl();

    out.startTag("pre", "class=", "unused").nl();
    try {
      int lineNumber = 1;
      for (String line = lines.readLine(); line != null; line = lines.readLine()) {
        if (sourceMap.appearsInJavaScript(sourcePath, lineNumber)) {
          out.startTag("span", "class=", "used").text(line).endTag("span").nl();
        } else {
          out.text(line).nl();
        }
        lineNumber++;
      }

    } finally {
      lines.close();
    }
    out.endTag("pre").nl();

    out.endTag("body").nl();
    out.endTag("html").nl();
  }

  private SourceMap loadSourceMap(String moduleName) {
    ModuleState moduleState = modules.get(moduleName);
    return SourceMap.load(moduleState.findSourceMap());
  }
}
