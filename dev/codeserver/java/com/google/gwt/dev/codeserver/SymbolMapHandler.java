/*
 * Copyright 2015 Google Inc.
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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * Serves GWT symbol maps.
 */
class SymbolMapHandler {

  /**
   * The URL prefix for all symbol maps.
   */
  static final String SYMBOLMAP_PATH = "/symbolmaps/";

  /**
   * The suffix that Super Dev Mode uses in symbol map URL's.
   */
  private static final String SYMBOLMAP_URL_SUFFIX = ".symbolMap";

  /**
   * Matches a valid symbol map file request.
   */
  private static final Pattern SYMBOLMAP_FILENAME_PATTERN = Pattern.compile(
      "^(" + WebServer.STRONG_NAME + ")" + Pattern.quote(SYMBOLMAP_URL_SUFFIX) + "$");

  /**
   * Matches a valid symbol map request.
   */
  private static final Pattern SYMBOLMAP_MODULE_PATTERN = Pattern.compile(
      "^" + SYMBOLMAP_PATH + "([^/]+)/");

  private final OutboxTable outboxTable;

  SymbolMapHandler(OutboxTable outboxTable) {
    this.outboxTable = outboxTable;
  }

  static boolean isSymbolMapRequest(String target) {
    return getModuleNameFromRequest(target) != null;
  }

  Response handle(String target, HttpServletRequest request, TreeLogger logger) {
    String moduleName = getModuleNameFromRequest(target);
    if (moduleName == null) {
      throw new RuntimeException("invalid request (shouldn't happen): " + target);
    }

    Outbox box = outboxTable.findByOutputModuleName(moduleName);
    if (box == null) {
      return new ErrorPage("No such module: " + moduleName);
    } else if (box.containsStubCompile()) {
      return new ErrorPage("This module hasn't been compiled yet.");
    }

    String rootDir = SYMBOLMAP_PATH + moduleName + "/";
    String rest = target.substring(rootDir.length());

    if (rest.isEmpty()) {
      return new ErrorPage("Missing permutation id");
    } else if (rest.endsWith("/")) {
      return new ErrorPage("Can not list directory");
    } else {
      String strongName = getStrongNameFromSymbolmapFilename(rest);
      if (strongName != null) {
        File symbolMap = box.findSymbolMap(strongName).getAbsoluteFile();
        return Responses.newFileResponse("text/plain", symbolMap);
      } else {
        return new ErrorPage("page not found");
      }
    }
  }

  private static String getModuleNameFromRequest(String target) {
    Matcher matcher = SYMBOLMAP_MODULE_PATTERN.matcher(target);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static String getStrongNameFromSymbolmapFilename(String target) {
    Matcher matcher = SYMBOLMAP_FILENAME_PATTERN.matcher(target);
    return matcher.matches() ? matcher.group(1) : null;
  }
}
