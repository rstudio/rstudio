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
import com.google.gwt.dev.codeserver.Pages.ErrorPage;
import com.google.gwt.dev.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves Java source files so that a browser's debugger can display them.
 * (This works with browsers that follow the <a
 * href="https://docs.google.com/document/d/1U1RGAehQwRypUTovF1KRlpiOFze0b-_2gc6fAH0KY0k/edit"
 * >Source Map Spec</a>, such as Chrome.)
 *
 * <p>The debugger will first fetch the source map from
 * /sourcemaps/[module name]/[strong name]_sourcemap.json. This file contains the names of Java
 * source files to download. Each source file will have a path like
 * "/sourcemaps/[module name]/src/[filename]".</p>
 */
class SourceHandler {

  /**
   * The URL prefix for all source maps and Java source code.
   */
  static final String SOURCEMAP_PATH = "/sourcemaps/";

  /**
   * The suffix that Super Dev Mode uses in source map URL's.
   */
  private static final String SOURCEMAP_URL_SUFFIX = "_sourcemap.json";

  /**
   * Matches a valid source map json file request.
   *
   * Used to extract the strong name of the permutation:
   *   StrongName_sourceMap0.json
   */
  private static final Pattern SOURCEMAP_FILENAME_PATTERN = Pattern.compile(
      "^(" + WebServer.STRONG_NAME + ")" + Pattern.quote(SOURCEMAP_URL_SUFFIX) + "$");

  /**
   * Matches a valid source map request.
   *
   * Used to extract the module name:
   *   /sourcemaps/ModuleName/.....
   */
  private static final Pattern SOURCEMAP_MODULE_PATTERN = Pattern.compile(
      "^" + SOURCEMAP_PATH + "([^/]+)/");

  static final String SOURCEROOT_TEMPLATE_VARIABLE = "$sourceroot_goes_here$";

  private final OutboxTable outboxTable;
  private final JsonExporter exporter;

  SourceHandler(OutboxTable outboxTable, JsonExporter exporter) {
    this.outboxTable = outboxTable;
    this.exporter = exporter;
  }

  static boolean isSourceMapRequest(String target) {
    return getModuleNameFromRequest(target) != null;
  }

  /**
   * The template for the sourcemap location to give the compiler.
   * It contains one template variable, __HASH__ for the strong name.
   */
  static String sourceMapLocationTemplate(String moduleName) {
    return SOURCEMAP_PATH + moduleName + "/__HASH__" + SOURCEMAP_URL_SUFFIX;
  }

  Response handle(String target, HttpServletRequest request, TreeLogger logger)
      throws IOException {
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

    String rootDir = SOURCEMAP_PATH + moduleName + "/";
    String rest = target.substring(rootDir.length());

    if (rest.isEmpty()) {
      return makeDirectoryListPage(box);
    } else if (rest.equals("gwtSourceMap.json")) {
      // This URL is no longer used by debuggers (we use the strong name) but is used for testing.
      // It's useful not to need the strong name to download the sourcemap.
      // (But this only works when there is one permutation.)
      return makeSourceMapPage(moduleName, box.findSourceMapForOnePermutation(), request);
    } else if (rest.endsWith("/")) {
      return sendFileListPage(box, rest);
    } else if (rest.endsWith(".java")) {
      return makeSourcePage(box, rest, request.getQueryString(), logger);
    } else {
      String strongName = getStrongNameFromSourcemapFilename(rest);
      if (strongName != null) {
        File sourceMap = box.findSourceMap(strongName).getAbsoluteFile();
        return makeSourceMapPage(moduleName, sourceMap, request);
      } else {
        return new ErrorPage("page not found");
      }
    }
  }

  static String getModuleNameFromRequest(String target) {
    Matcher matcher = SOURCEMAP_MODULE_PATTERN.matcher(target);
    return matcher.find() ? matcher.group(1) : null;
  }

  static String getStrongNameFromSourcemapFilename(String target) {
    Matcher matcher = SOURCEMAP_FILENAME_PATTERN.matcher(target);
    return matcher.matches() ? matcher.group(1) : null;
  }

  private Response makeSourceMapPage(final String moduleName, File sourceMap,
      HttpServletRequest request) {

    // Stream the file, substituting the sourceroot variable with the filename.
    // (This is more efficient than parsing the file as JSON.)

    // We need to do this at runtime because we don't know what the hostname will be
    // until we get a request. (For example, some people run the Code Server behind
    // a reverse proxy to support https.)

    String sourceRoot = String.format("http://%s:%d/sourcemaps/%s/", request.getServerName(),
        request.getServerPort(), moduleName);

    final Response barePage = Responses.newTextTemplateResponse("application/json", sourceMap,
        "\"" + SOURCEROOT_TEMPLATE_VARIABLE + "\"",
        "\"" + sourceRoot + "\"");

    // Wrap it in another response to time how long it takes.
    return Responses.newTimedResponse(barePage, "sent source map for module '" + moduleName + "'");
  }

  private Response makeDirectoryListPage(Outbox box) throws IOException {
    SourceMap map = SourceMap.load(box.findSourceMapForOnePermutation());
    JsonObject json = exporter.exportSourceMapDirectoryListVars(box, map);
    return Pages.newHtmlPage("config", json, "directorylist.html");
  }

  private Response sendFileListPage(Outbox box, String rest) throws IOException {

    SourceMap map = SourceMap.load(box.findSourceMapForOnePermutation());
    JsonObject json = exporter.exportSourceMapFileListVars(box, map, rest);
    return Pages.newHtmlPage("config", json, "filelist.html");
  }

  /**
   * Returns a page displaying a Java source file. It will be sent as plain text by default,
   * or as HTML if the query string is equal to "html".
   */
  private Response makeSourcePage(Outbox box, String sourcePath, String query, TreeLogger logger)
      throws IOException {

    InputStream pageBytes = box.openSourceFile(sourcePath);
    if (pageBytes == null) {
      return new ErrorPage("unknown source file: " + sourcePath);
    }

    if (query != null && query.equals("html")) {
      return makeHtmlSourcePage(box, sourcePath, pageBytes, logger);
    } else {
      return Responses.newBinaryStreamResponse("text/plain", pageBytes);
    }
  }

  /**
   * Returns a page that will send a Java source file as HTML. The lines of source
   * that have corresponding JavaScript will be highlighted (as determined by reading the
   * source map).
   */
  private Response makeHtmlSourcePage(Outbox box, final String sourcePath,
      final InputStream pageBytes, TreeLogger logger) throws IOException {

    final ReverseSourceMap sourceMap = ReverseSourceMap.load(logger,
        box.findSourceMapForOnePermutation());

    final File sourceFile = new File(sourcePath);

    return new Response() {
      @Override
      public void send(HttpServletRequest request, HttpServletResponse response, TreeLogger logger)
          throws IOException {
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

        BufferedReader lines = new BufferedReader(new InputStreamReader(pageBytes));
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
    };
  }
}
