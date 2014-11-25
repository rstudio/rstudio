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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;

/**
 * The directory tree where Super Dev Mode keeps all the files for one GWT app.
 * Each time we recompile the app, we create a new subdirectory (see {@link CompileDir}).
 * In addition, there are some files that are shared between recompiles, such as
 * the unit cache.
 */
class OutboxDir {
  private static final String COMPILE_DIR_PREFIX = "compile-";
  private static final int MAX_CREATE_DIRECTORY_RETRIES = 50;

  private final File root;
  private int nextCompileId = 1;

  /**
   * @see #create
   */
  private OutboxDir(File root) {
    this.root = root;
  }

  File getSpeedTracerLogFile() {
    return new File(root, "speedtracer.html");
  }

  /**
   * Creates a fresh, empty compile directory.
   */
  CompileDir makeCompileDir(TreeLogger logger)
      throws UnableToCompleteException {

    for (int i = 0; i < MAX_CREATE_DIRECTORY_RETRIES; i++) {
      int candidateId = nextCompileId++;
      File candidate = new File(root, COMPILE_DIR_PREFIX + candidateId);
      try {
        return CompileDir.create(candidate, logger);
      } catch (UnableToCompleteException e) {
        // try again
      }
    }

    logger.log(Type.ERROR, "Gave up trying to create a compile directory.");
    throw new UnableToCompleteException();
  }

  /**
   * Creates an outbox directory, doing any cleanup needed.
   * @param dir the directory to use. It need not exist, but
   * the parent dir should exist.
   */
  static OutboxDir create(File dir, TreeLogger logger) throws IOException {
    if (!dir.isDirectory() && !dir.mkdir()) {
      throw new IOException("can't create app directory: " + dir);
    }

    File[] children = dir.listFiles();
    if (children == null) {
      throw new IOException("unable to list files in " + dir);
    }

    // Try to clean up existing subdirectories.
    // (This is not guaranteed to delete all directories on Windows if a directory is locked.)
    for (File candidate : children) {
      if (candidate.getName().startsWith(COMPILE_DIR_PREFIX)) {
        Util.recursiveDelete(candidate, false);
        if (candidate.exists()) {
          logger.log(Type.WARN, "unable to delete '" + candidate + "' (skipped)");
        }
      }
    }

    return new OutboxDir(dir);
  }
}
