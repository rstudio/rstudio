/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory for printwriters creating source files in a particular package.
 */
class PrintWriterManager {
  private final GeneratorContext genCtx;
  private final String packageName;
  private final TreeLogger logger;
  private final Set<PrintWriter> writers = new HashSet<PrintWriter>();

  PrintWriterManager(GeneratorContext genCtx, TreeLogger logger,
      String packageName) {
    this.genCtx = genCtx;
    this.packageName = packageName;
    this.logger = logger;
  }

  /**
   * Commit all writers we have vended.
   */
  void commit() {
    for (PrintWriter writer : writers) {
      genCtx.commit(logger, writer);
    }
  }

  /**
   * @param name classname
   * @return the printwriter
   * @throws RuntimeException if this class has already been written
   */
  PrintWriter makePrintWriterFor(String name) {
    PrintWriter writer = tryToMakePrintWriterFor(name);
    if (writer == null) {
      throw new RuntimeException(String.format("Tried to write %s.%s twice.",
          packageName, name));
    }
    return writer;
  }

  /**
   * @param name classname
   * @return the printwriter, or null if this class has already been written
   */
  PrintWriter tryToMakePrintWriterFor(String name) {
    PrintWriter writer = genCtx.tryCreate(logger, packageName, name);
    if (writer != null) {
      writers.add(writer);
    }
    return writer;
  }
}
