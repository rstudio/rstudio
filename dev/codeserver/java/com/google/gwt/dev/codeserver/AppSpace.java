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

import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;

/**
 * The directory tree where Super Dev Mode keeps all the files for one GWT app.
 * Each time we recompile the app, we create a new subdirectory (see {@link CompileDir}).
 * In addition, there are some files that are shared between recompiles, such as
 * the unit cache.
 */
class AppSpace {

  static final String COMPILE_DIR_PREFIX = "compile-";
  private final File root;

  /**
   * @see #create
   */
  private AppSpace(File root) {
    this.root = root;
  }

  File getSpeedTracerLogFile() {
    return new File(root, "speedtracer.html");
  }

  File getUnitCacheDir() {
    return new File(root, "gwt-unitcache");
  }

  File getCompileDir(int compileId) {
    return new File(root, COMPILE_DIR_PREFIX + compileId);
  }

  /**
   * Creates an app directory, doing any cleanup needed.
   * @param dir the directory to use. It need not exist, but
   * the parent dir should exist.
   */
  static AppSpace create(File dir) throws IOException {
    if (!dir.exists() && !dir.mkdir()) {
      throw new IOException("can't create app directory: " + dir);
    }

    // clean up existing subdirectories
    for (File candidate : dir.listFiles()) {
      if (candidate.getName().startsWith(COMPILE_DIR_PREFIX)) {
        System.err.println("deleting: " + candidate);
        Util.recursiveDelete(candidate, false);
      }
    }

    return new AppSpace(dir);
  }
}
