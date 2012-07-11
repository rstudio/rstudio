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
import com.google.gwt.dev.json.JsonException;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

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

    if (target.equals(SOURCEMAP_PATH + moduleName + "/gwtSourceMap.json")) {
        sendSourceMap(moduleName, request, response);
        return;
    }

    if (target.endsWith(".java")) {
      sendSourceFile(target, moduleName, response);
      return;
    }

    response.sendError(HttpServletResponse.SC_NOT_FOUND);
    logger.log(TreeLogger.WARN, "returned not found for request: " + target);
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

    ModuleState moduleState = modules.get(moduleName);
    String sourceMapJson = Util.readFileAsString(moduleState.findSourceMap());

    // hack: rewrite the source map so that each filename is a URL
    String serverPrefix = String.format("http://%s:%d/sourcemaps/%s/", request.getServerName(),
        request.getServerPort(), moduleName);
    sourceMapJson = addPrefixToSourceMapFilenames(serverPrefix, sourceMapJson);

    PageUtil.sendString("application/json", sourceMapJson, response);
    logger.log(TreeLogger.WARN, "sent source map for module: " + moduleName);
  }

  private void sendSourceFile(String target, String moduleName, HttpServletResponse response)
      throws IOException {
    String path = removePrefix(target, "/sourcemaps/" + moduleName + "/");
    ModuleState moduleState = modules.get(moduleName);
    // generated file?
    if (path.startsWith("gen/")) {
      File fileInGenDir = new File(moduleState.getGenDir(), removePrefix(path, "gen/"));
      if (!fileInGenDir.isFile()) {
        sendNotFound(response, target);
        return;
      }
      PageUtil.sendFile("text/plain", fileInGenDir, response);
      return;
    }

    // regular source file?
    InputStream pageBytes = moduleState.openSourceFile(path);
    if (pageBytes == null) {
      sendNotFound(response, target);
      return;
    }

    PageUtil.sendStream("text/plain", pageBytes, response);
  }

  /**
   * Adds the given prefix to each filename in a source map.
   * @return the JSON of the modified source map
   */
  private String addPrefixToSourceMapFilenames(String serverPrefix, String sourceMapJson) {

    JsonObject sourceMap = null;
    try {
      sourceMap = JsonObject.parse(new StringReader(sourceMapJson));
    } catch (JsonException e) {
      throw new RuntimeException("can't parse sourcemap as json", e);
    } catch (IOException e) {
      throw new RuntimeException("can't parse sourcemap as json", e);
    }
    JsonArray sources = (JsonArray) sourceMap.get("sources");
    JsonArray newSources = new JsonArray();
    for (int i = 0; i < sources.getLength(); i++) {
      String filename = sources.get(i).asString().getString();
      newSources.add(serverPrefix + filename);
    }
    sourceMap.put("sources", newSources);
    StringWriter buffer = new StringWriter();
    try {
      sourceMap.write(buffer);
    } catch (IOException e) {
      throw new RuntimeException("can't convert sourcemap to json");
    }
    return buffer.toString();
  }

  private void sendNotFound(HttpServletResponse response, String target) throws IOException {
    response.sendError(HttpServletResponse.SC_NOT_FOUND);
    logger.log(TreeLogger.WARN, "unknown source file: " + target);
  }

  private String removePrefix(String s, String prefix) {
    assert s.startsWith(prefix);
    return s.substring(prefix.length());
  }
}
