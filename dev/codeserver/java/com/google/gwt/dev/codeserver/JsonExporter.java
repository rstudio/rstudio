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

import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonObject;

import java.io.File;
import java.util.Arrays;
import java.util.SortedMap;

/**
 * Creates JSON descriptions of the code server's internal objects.
 * The code server uses JSON when rendering HTML templates and in its
 * JavaScript API's.
 */
class JsonExporter {
  private final Options options;
  private final OutboxTable outboxTable;

  JsonExporter(Options options, OutboxTable outboxTable) {
    this.options = options;
    this.outboxTable = outboxTable;
  }

  // === API responses ===

  /**
   * Creates the response to a /recompile request.
   */
  JsonObject exportRecompileResponse(Result result) {
    JsonObject out = JsonObject.create();
    out.put("status", result.isOk() ? "ok" : "failed");
    // This doesn't seem to be used but was there in 2.6.1.
    // TODO: consider removing for 2.7?
    out.put("moduleNames", exportOutputModuleNames());
    return out;
  }

  /**
   * Creates the response to a /progress request.
   */
  JsonObject exportProgressResponse(JobEvent progress) {
    // TODO: upgrade for multiple compiles and finalize API for 2.7.
    JsonObject out = new JsonObject();
    out.put("jobId", progress.getJobId());
    out.put("status", progress.getStatus().jsonName);
    out.put("message", progress.getMessage());
    out.put("inputModule", progress.getInputModuleName());
    out.put("bindings", exportMap(progress.getBindings()));
    return out;
  }

  // === page template variables ===

  /**
   * Exports the template variables for frontpage.html.
   */
  JsonObject exportFrontPageVars() {
    JsonObject out = JsonObject.create();
    out.put("moduleNames", exportOutputModuleNames()); // TODO: rename
    return out;
  }

  /**
   * Exports the template variables for modulepage.html.
   */
  JsonObject exportModulePageVars(Outbox box) {
    JsonObject result = new JsonObject();
    result.put("moduleName", box.getOutputModuleName()); // TODO: rename
    result.put("files", exportOutputFiles(box));
    result.put("isCompiled", !box.containsStubCompile());
    return result;
  }

  /**
   * Exports the template variables for success.
   */
  JsonObject exportOk(String message) {
    JsonObject out = JsonObject.create();
    out.put("status", "ok");
    out.put("message", message);
    return out;
  }

  /**
   * Exports the template variables for failure.
   */
  JsonObject exportError(String message) {
    JsonObject out = JsonObject.create();
    out.put("status", "error");
    out.put("message", message);
    return out;
  }

  /**
   * Returns a JSON representation of the directories containing at least one source file
   * in the source map.
   * (These directories are relative to a classpath entry or -sourceDir argument.)
   * Used in directorylist.html.
   */
  JsonObject exportSourceMapDirectoryListVars(Outbox box, SourceMap map) {
    JsonObject out = new JsonObject();
    out.put("moduleName", box.getOutputModuleName()); // TODO: rename
    JsonArray directories = new JsonArray();
    for (String name : map.getSourceDirectories()) {
      JsonObject dir = new JsonObject();
      dir.put("name", name);
      dir.put("link", name + "/");
      directories.add(dir);
    }
    out.put("directories", directories);
    return out;
  }

  /**
   * Returns a JSON representation of the files in one directory in the source map.
   * Used in filelist.html.
   * @param directory A directory name ending with "/".
   */
  JsonObject exportSourceMapFileListVars(Outbox box, SourceMap map,
      String directory) {
    JsonObject out = new JsonObject();
    out.put("moduleName", box.getOutputModuleName()); // TODO: rename
    out.put("directory", directory);
    JsonArray files = new JsonArray();
    for (String name : map.getSourceFilesInDirectory(directory)) {
      JsonObject file = new JsonObject();
      file.put("name", name);
      file.put("link", name + "?html");
      files.add(file);
    }
    out.put("files", files);
    return out;
  }

  // === JSON used in JavaScript files ===

  /**
   * Exports the template variables for dev_mode_on.js.
   */
  JsonObject exportDevModeOnVars() {
    JsonObject out = JsonObject.create();
    out.put("moduleNames", exportOutputModuleNames()); // TODO: rename
    out.put("warnings", exportWarnings());
    return out;
  }

  // === utility methods ===

  private JsonArray exportOutputModuleNames() {
    JsonArray moduleNames = new JsonArray();
    for (String module : outboxTable.getOutputModuleNames()) {
      moduleNames.add(module);
    }
    return moduleNames;
  }

  private JsonArray exportWarnings() {
    JsonArray out = new JsonArray();
    // Add warnings if any
    return out;
  }

  /**
   * Lists the files that the last successful GWT compiler generated in an outbox.
   */
  private JsonArray exportOutputFiles(Outbox box) {
    File[] files = new File(box.getWarDir(), box.getOutputModuleName()).listFiles();
    if (files == null) {
      return new JsonArray();
    }
    Arrays.sort(files);

    JsonArray result = new JsonArray();
    for (File file : files) {
      if (file.isFile()) {
        JsonObject map = new JsonObject();
        map.put("name", file.getName());
        map.put("link", file.getName());
        result.add(map);
      }
    }
    return result;
  }

  private JsonObject exportMap(SortedMap<String, String> bindings) {
    JsonObject out = new JsonObject();
    for (String name : bindings.keySet()) {
      out.put(name, bindings.get(name));
    }
    return out;
  }
}
