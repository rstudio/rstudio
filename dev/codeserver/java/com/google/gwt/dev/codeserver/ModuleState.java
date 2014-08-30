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
import com.google.gwt.dev.codeserver.Job.Result;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains everything that the code server knows about a GWT app (module), including how
 * to recompile it and where the compiler output is.
 */
class ModuleState {

  /**
   * The suffix that the GWT compiler uses when writing a sourcemap file.
   */
  private static final String SOURCEMAP_FILE_SUFFIX = "_sourceMap0.json";

  private final AtomicReference<Job.Result> published = new AtomicReference<Job.Result>();
  private final Recompiler recompiler;

  ModuleState(Recompiler recompiler, boolean noPrecompile,
      TreeLogger logger)
      throws UnableToCompleteException {
    this.recompiler = recompiler;
    maybePrecompile(noPrecompile, logger);
  }

  /**
   * Loads the module and maybe compiles it. Sets up the output directory.
   * Throws an exception if unable. (In this case, Super Dev Mode fails to start.)
   */
  void maybePrecompile(boolean noPrecompile, TreeLogger logger) throws UnableToCompleteException {
    if (noPrecompile) {
      publish(recompiler.initWithoutPrecompile(logger));
    } else {
      publish(recompiler.precompile(logger));
    }
  }

  /**
   * Compiles the module again, possibly changing the output directory.
   * After returning, the result of the compile can be found via {@link Job#waitForResult}
   */
  void recompile(Job job) {
    if (!job.wasSubmitted() || job.isDone()) {
      throw new IllegalStateException(
          "tried to recompile using a job in the wrong state:"  + job.getId());
    }

    Result result = recompiler.recompile(job);

    if (result.isOk()) {
      publish(result);
    } else {
      job.getLogger().log(TreeLogger.Type.WARN, "continuing to serve previous version");
    }
  }

  /**
   * Makes the result of a compile downloadable via HTTP.
   */
  private void publish(Result result) {
    Result previous = published.getAndSet(result);
    if (previous != null) {
      previous.job.onGone();
    }
  }

  private CompileDir getOutputDir() {
    return published.get().outputDir;
  }

  /**
   * Returns the name of this module (after renaming).
   */
  String getModuleName() {
    return recompiler.getModuleName();
  }

  /**
   * Returns the source map file from the most recent recompile,
   * assuming there is one permutation.
   *
   * @throws RuntimeException if unable
   */
  File findSourceMapForOnePermutation() {
    File dir = findSymbolMapDir();

    File[] sourceMapFiles = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(SOURCEMAP_FILE_SUFFIX);
      }
    });

    if (sourceMapFiles == null) {
      throw new RuntimeException("Can't list contents of symbol map directory: " + dir);
    }

    if (sourceMapFiles.length > 1) {
      throw new RuntimeException("Multiple fragment 0 sourcemaps found. Too many permutations.");
    }

    if (sourceMapFiles.length == 0) {
      throw new RuntimeException("No sourcemaps found. Not enabled?");
    }

    return new File(dir, sourceMapFiles[0].getName());
  }

  /**
   * Returns the source map file given a strong name.
   *
   * @throws RuntimeException if unable
   */
  public File findSourceMap(String strongName) {
    File dir = findSymbolMapDir();
    File file = new File(dir, strongName + SOURCEMAP_FILE_SUFFIX);
    if (!file.isFile()) {
      throw new RuntimeException("Sourcemap file doesn't exist for " + strongName);
    }
    return file;
  }

  /**
   * Returns the symbols map folder for this modulename.
   * @throws RuntimeException if unable
   */
  private File findSymbolMapDir() {
    String moduleName = recompiler.getModuleName();
    File symbolMapsDir = getOutputDir().findSymbolMapDir(moduleName);
    if (symbolMapsDir == null) {
      throw new RuntimeException("Can't find symbol map directory for " + moduleName);
    }
    return symbolMapsDir;
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
    return new File(getOutputDir().getWarDir(), urlPath);
  }

  /**
   * Returns the log file from the last time this module was recompiled. This changes
   * after each compile.
   */
  File getCompileLog() {
    return recompiler.getLastLog();
  }

  File getGenDir() {
    return getOutputDir().getGenDir();
  }

  File getWarDir() {
    return getOutputDir().getWarDir();
  }

  /**
   * Returns a file out of the "extras" directory.
   * @param path relative path of the file, not including the module name.
   * @return The location of the file, which might not actually exist.
   */
  File getExtraFile(String path) {
    File prefix = new File(getOutputDir().getExtraDir(), getModuleName());
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
