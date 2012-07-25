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

import java.io.IOException;
import java.io.InputStream;

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
      sendSourceFile(moduleName, rest, response);

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

    ModuleState moduleState = modules.get(moduleName);
    SourceMap map = SourceMap.load(moduleState.findSourceMap());

    // hack: rewrite the source map so that each filename is a URL
    String serverPrefix = String.format("http://%s:%d/sourcemaps/%s/", request.getServerName(),
        request.getServerPort(), moduleName);
    map.addPrefixToEachSourceFile(serverPrefix);

    PageUtil.sendString("application/json", map.serialize(), response);
    logger.log(TreeLogger.WARN, "sent source map for module: " + moduleName);
  }

  private void sendDirectoryListPage(String moduleName, HttpServletResponse response)
      throws IOException {

    ModuleState moduleState = modules.get(moduleName);
    SourceMap map = SourceMap.load(moduleState.findSourceMap());

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

    ModuleState moduleState = modules.get(moduleName);
    SourceMap map = SourceMap.load(moduleState.findSourceMap());

    JsonObject config = new JsonObject();
    config.put("moduleName", moduleName);
    config.put("directory", rest);
    JsonArray files = new JsonArray();
    for (String name : map.getSourceFilesInDirectory(rest)) {
      JsonObject file = new JsonObject();
      file.put("name", name);
      file.put("link", name);
      files.add(file);
    }
    config.put("files", files);
    PageUtil.sendJsonAndHtml("config", config, "filelist.html", response, logger);
  }

  private void sendSourceFile(String moduleName, String rest, HttpServletResponse response)
      throws IOException {
    ModuleState moduleState = modules.get(moduleName);
    InputStream pageBytes = moduleState.openSourceFile(rest);

    if (pageBytes == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      logger.log(TreeLogger.WARN, "unknown source file: " + rest);
      return;
    }

    PageUtil.sendStream("text/plain", pageBytes, response);
  }
}
