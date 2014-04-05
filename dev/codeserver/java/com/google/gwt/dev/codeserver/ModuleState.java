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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains everything that the code server knows about a GWT app (module), including how
 * to recompile it and where the compiler output is.
 */
class ModuleState {
  private final AtomicReference<CompileDir> current = new AtomicReference<CompileDir>();
  private final Recompiler recompiler;
  private final TreeLogger logger;

  ModuleState(Recompiler recompiler, TreeLogger logger, boolean noPrecompile)
      throws UnableToCompleteException {
    this.recompiler = recompiler;
    this.logger = logger;
    defaultCompile(noPrecompile);
  }

  /**
   * Compiles the module with the default set of properties.
   */
  void defaultCompile(boolean noPrecompile) throws UnableToCompleteException {
    CompileDir compileDir;
    if (noPrecompile) {
      compileDir = recompiler.noCompile();
    } else {
      Map<String, String> defaultProps = new HashMap<String, String>();
      defaultProps.put("user.agent", "safari");
      defaultProps.put("locale", "en");
      defaultProps.put("compiler.useSourceMaps", "true");
      compileDir = recompiler.compile(defaultProps);
    }
    current.set(compileDir);
  }

  /**
   * Recompiles the module with the given binding properties. If successful, this changes the
   * location of the output directory. (The log file changes both on success and on failure.
   *
   * @param bindingProperties The properties used to compile. (Chooses the permutation.)
   * @return true if the compile finished successfully.
   */
  boolean recompile(Map<String, String> bindingProperties) {
    try {
      current.set(recompiler.compile(bindingProperties));
      return true;
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.Type.WARN, "continuing to serve previous version");
      return false;
    }
  }

  /**
   * Returns the name of this module (after renaming).
   */
  String getModuleName() {
    return recompiler.getModuleName();
  }

  /**
   * Returns the source map file from the most recent recompile.
   * @throws RuntimeException if unable
   */
  File findSourceMap() {
    String moduleName = recompiler.getModuleName();
    File symbolMapsDir = current.get().findSymbolMapDir(moduleName);
    if (symbolMapsDir == null) {
      throw new RuntimeException("Can't find symbolMaps dir for " + moduleName);
    }

    File[] sourceMapFiles = symbolMapsDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.matches(".*_sourceMap0.json");
      }
    });

    if (sourceMapFiles == null) {
      throw new RuntimeException("Can't list contents of symbolMaps dir: " + symbolMapsDir);
    }

    if (sourceMapFiles.length > 1) {
      throw new RuntimeException("Multiple fragment 0 sourcemaps found. Too many permutations.");
    }

    if (sourceMapFiles.length == 0) {
      throw new RuntimeException("No sourcemaps found. Not enabled?");
    }

    return new File(symbolMapsDir, sourceMapFiles[0].getName());
  }

  /**
   * Finds a source file (or other resource) that's either in this module's source path, or
   * is a generated file.
   * @param path location of the file relative to its directory in the classpath, or (if
   *   it starts with "gen/"), a generated file.
   * @return bytes in the file, or null if there's no such source file.
   */
  InputStream openSourceFile(String path) throws IOException {

    if (path.startsWith("gen/")) {
      // generated file?
      String rest = path.substring("gen/".length());
      File fileInGenDir = new File(getGenDir(), rest);
      if (!fileInGenDir.isFile()) {
        return null;
      }
      return new BufferedInputStream(new FileInputStream(fileInGenDir));
    } else {
      // regular source file?
      URL resource = recompiler.getResourceLoader().getResource(path);
      if (resource == null) {
        return null;
      }
      return resource.openStream();
    }
  }

  /**
   * Returns the location of a file in the compiler's output directory from the
   * last time this module was recompiled. The location will change after a successful
   * recompile.
   * @param urlPath The path to the file. This should be a relative path beginning
   * with the module name (after renaming).
   * @return The location of the file, which might not actually exist.
   */
  File getOutputFile(String urlPath) {
    return new File(current.get().getWarDir(), urlPath);
  }

  /**
   * Returns the log file from the last time this module was recompiled. This changes
   * after every call to {@link #recompile}.
   */
  File getCompileLog() {
    return recompiler.getLastLog();
  }

  File getGenDir() {
    return current.get().getGenDir();
  }

  File getWarDir() {
    return current.get().getWarDir();
  }

  /**
   * Returns a file out of the "extras" directory.
   * @param path relative path of the file, not including the module name.
   * @return The location of the file, which might not actually exist.
   */
  File getExtraFile(String path) {
    File prefix = new File(current.get().getExtraDir(), getModuleName());
    return new File(prefix, path);
  }

  JsonObject getTemplateVariables() {
    JsonObject result = new JsonObject();
    result.put("moduleName", getModuleName());
    result.put("files", listModuleFiles());
    return result;
  }

  private JsonArray listModuleFiles() {
    File[] files = new File(getWarDir(), getModuleName()).listFiles();
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
}
