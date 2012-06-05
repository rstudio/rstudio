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

import java.io.File;

/**
 * Defines a directory tree used for compiling a GWT app one time. Each time we recompile
 * the app, we will create a new, empty CompileDir. This way, a failed compile doesn't
 * modify the last good compile.
 *
 * The CompileDir gets created within the appropriate {@link AppSpace} for the app
 * being compiled.
 */
class CompileDir {
  private final File dir;

  /**
   * @see #create
   */
  private CompileDir(File dir) {
    this.dir = dir;
  }

  File getWarDir() {
    return new File(dir, "war");
  }

  File getDeployDir() {
    return new File(getWarDir(), "WEB-INF/deploy");
  }

  File getExtraDir() {
    return new File(dir, "extras");
  }

  File getGenDir() {
    return new File(dir, "gen");
  }

  File getWorkDir() {
    return new File(dir, "work");
  }

  File getLogFile() {
    return new File(dir, "compile.log");
  }

  File findSymbolMapDir(String moduleName) {
    // The JUnit module moves the symbolMaps directory in a post linker.
    // TODO(skybrian) query this information from the compiler somehow?
    File[] candidates = {
        new File(getExtraDir(), moduleName + "/symbolMaps"),
        new File(getWarDir(), moduleName + "/.junit_symbolMaps")
    };

    for (File candidate : candidates) {
      if (candidate.isDirectory()) {
        return candidate;
      }
    }

    return null;
  }

  /**
   * Creates a new compileDir directory structure. The directory must not already exist,
   * but its parent should exist.
   * @throws UnableToCompleteException if unable to create the directory
   */
  static CompileDir create(File dir, TreeLogger logger)
      throws UnableToCompleteException {

    CompileDir result = new CompileDir(dir);

    mkdir(dir, logger);
    mkdir(result.getWarDir(), logger);

    return result;
  }

  private static void mkdir(File dirToCreate, TreeLogger logger)
      throws UnableToCompleteException {
    if (!dirToCreate.mkdir()) {
      logger.log(TreeLogger.Type.ERROR, "unable to create directory: " + dirToCreate);
      throw new UnableToCompleteException();
    }
  }
}
