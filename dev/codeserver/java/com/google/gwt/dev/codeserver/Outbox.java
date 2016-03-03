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
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.codeserver.CompileDir.PolicyFile;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the compiler output for one module.
 * TODO(skybrian) there will later be a separate Outbox for each set of binding properties.
 */
class Outbox {

  /**
   * The suffix that the GWT compiler uses when writing a sourcemap file.
   */
  static final String SOURCEMAP_FILE_SUFFIX = "_sourceMap0.json";

  private final String id;
  private final Recompiler recompiler;
  private final Options options;

  private final AtomicReference<Result> published = new AtomicReference<Result>();
  private Job publishedJob; // may be null if the Result wasn't created by a Job.

  Outbox(String id, Recompiler recompiler, Options options, TreeLogger logger)
      throws UnableToCompleteException {
    Preconditions.checkArgument(isValidOutboxId(id));
    this.id = id;
    this.recompiler = recompiler;
    this.options = options;
    maybePrecompile(logger);
  }

  private boolean isValidOutboxId(String id) {
    return ModuleDef.isValidModuleName(id);
  }

  /**
   * Forces the next recompile even if no input files have changed.
   */
  void forceNextRecompile() {
    recompiler.forceNextRecompile();
  }

  /**
   * A unique id for this outbox. (This should be treated as an opaque string.)
   */
  String getId() {
    return id;
  }

  /**
   * Loads the module and maybe compiles it. Sets up the output directory.
   * Throws an exception if unable. (In this case, Super Dev Mode fails to start.)
   */
  void maybePrecompile(TreeLogger logger) throws UnableToCompleteException {

    if (options.getNoPrecompile()) {
      publish(recompiler.initWithoutPrecompile(logger), null);
      return;
    }

    // TODO: each box will have its own binding properties
    Map<String, String> defaultProps = new HashMap<String, String>();
    defaultProps.put("user.agent", "safari");
    defaultProps.put("locale", "en");

    // Create a dummy job for the first compile.
    // Its progress is not visible externally but will still be logged.
    JobEventTable dummy = new JobEventTable();
    Job job = makeJob(defaultProps, logger);
    job.onSubmitted(dummy);
    publish(recompiler.precompile(job), job);

    if (options.isCompileTest()) {

      // Listener errors are fatal in compile tests

      Throwable error = job.getListenerFailure();
      if (error != null) {
        UnableToCompleteException e = new UnableToCompleteException();
        e.initCause(error);
        throw e;
      }
    }
  }

  /**
   * Creates a Job whose output will be saved in this outbox.
   */
  Job makeJob(Map<String, String> bindingProperties, TreeLogger parentLogger) {
    return new Job(this, bindingProperties, parentLogger, options);
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
      publish(result, job);
    } else {
      job.getLogger().log(TreeLogger.Type.WARN, "continuing to serve previous version");
    }
  }

  /**
   * Makes the result of a compile downloadable via HTTP.
   * @param job the job that created this result, or null if none.
   */
  private synchronized void publish(Result result, Job job) {
    if (publishedJob != null) {
      publishedJob.onGone();
    }
    publishedJob = job;
    published.set(result);
  }

  private CompileDir getOutputDir() {
    return published.get().outputDir;
  }

  /**
   * Returns true if we haven't done a real compile yet, so the Outbox contains
   * a stub that will automatically start a compile.
   */
  synchronized boolean containsStubCompile() {
    return publishedJob == null;
  }

  /**
   * Returns the module name that will be sent to the compiler (before renaming).
   */
  String getInputModuleName() {
    return recompiler.getInputModuleName();
  }

  /**
   * Returns the module name last received from the compiler (after renaming).
   */
  String getOutputModuleName() {
    return recompiler.getOutputModuleName();
  }

  /**
   * Returns the source map file from the most recent recompile,
   * assuming there is one permutation.
   *
   * @throws RuntimeException if unable
   */
  File findSourceMapForOnePermutation() {
    String moduleName = recompiler.getOutputModuleName();

    List<File> sourceMapFiles = getOutputDir().findSourceMapFiles(moduleName);
    if (sourceMapFiles == null) {
      throw new RuntimeException("Can't find sourcemap files.");
    }

    if (sourceMapFiles.size() > 1) {
      throw new RuntimeException("Multiple fragment 0 sourcemaps found. Too many permutations.");
    }

    if (sourceMapFiles.isEmpty()) {
      throw new RuntimeException("No sourcemaps found. Not enabled?");
    }

    return sourceMapFiles.get(0);
  }

  /**
   * Returns the source map file given a strong name.
   *
   * @throws RuntimeException if unable
   */
  File findSourceMap(String strongName) {
    File dir = findSymbolMapDir();
    File file = new File(dir, strongName + SOURCEMAP_FILE_SUFFIX);
    if (!file.isFile()) {
      throw new RuntimeException("Sourcemap file doesn't exist for " + strongName);
    }
    return file;
  }

  /**
   * Returns the symbol map file given a strong name.
   *
   * @throws RuntimeException if unable
   */
  File findSymbolMap(String strongName) {
    File dir = findSymbolMapDir();
    File file = new File(dir, strongName + ".symbolMap");
    if (!file.isFile()) {
      throw new RuntimeException("Symbolmap file doesn't exist for " + strongName);
    }
    return file;
  }

  /**
   * Returns the symbols map folder for this modulename.
   * @throws RuntimeException if unable
   */
  private File findSymbolMapDir() {
    String moduleName = recompiler.getOutputModuleName();
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
   * Return fresh Js that knows how to request the specific permutation recompile.
   */
  public String getRecompileJs(TreeLogger logger) throws UnableToCompleteException {
    return recompiler.getRecompileJs(logger);
  }

  /**
   * Reads the GWT-RPC serialization policy manifest in this outbox.
   * If it's not there, returns the empty list.
   * @return a PolicyFile record for each entry in the policy file.
   */
  List<PolicyFile> readRpcPolicyManifest() throws IOException {
    return getOutputDir().readRpcPolicyManifest(getOutputModuleName());
  }
}
