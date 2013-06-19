/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.logging.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogRecord;

/**
 * @deprecated Use com.google.gwt.core.server.StackTraceDeobfuscator instead.
 */
@Deprecated
public class StackTraceDeobfuscator extends com.google.gwt.core.server.StackTraceDeobfuscator {

  protected File symbolMapsDirectory;

  /**
   * Creates a deobfuscator that loads symbol map files from the given directory. Symbol maps are
   * generated into the location specified by the GWT compiler <code>-deploy</code> command line
   * argument.
   *
   * @param symbolMapsDirectory the <code>symbolMaps</code> directory, with or without trailing
   *                            directory separator character
   */
  public StackTraceDeobfuscator(String symbolMapsDirectory) {
    this(symbolMapsDirectory, false);
  }

  /**
   * Creates a deobfuscator that loads symbol map files from the given directory. Symbol maps are
   * generated into the location specified by the GWT compiler <code>-deploy</code> command line
   * argument.
   *
   * @param symbolMapsDirectory the <code>symbolMaps</code> directory, with or without trailing
   *                            directory separator character
   * @param lazyLoad if true, only symbols requested to be deobfuscated are cached. This provides
   *                 a large memory savings at the expense of occasional extra disk reads.
   */
  public StackTraceDeobfuscator(String symbolMapsDirectory, boolean lazyLoad) {
    setLazyLoad(lazyLoad);
    setSymbolMapsDirectory(symbolMapsDirectory);
  }

  /**
   * Best effort resymbolization of a log record's stack trace.
   *
   * @param lr         the log record to resymbolize
   * @param strongName the GWT permutation strong name
   * @return the best effort resymbolized log record
   */
  public LogRecord deobfuscateLogRecord(LogRecord lr, String strongName) {
    if (lr.getThrown() != null && strongName != null) {
      lr.setThrown(deobfuscateThrowable(lr.getThrown(), strongName));
    }
    return lr;
  }

  public StackTraceElement[] deobfuscateStackTrace(StackTraceElement[] st, String strongName) {
    return super.resymbolize(st, strongName);
  }

  public Throwable deobfuscateThrowable(Throwable old, String strongName) {
    Throwable t = new Throwable(old.getMessage());
    t.setStackTrace(deobfuscateStackTrace(old.getStackTrace(), strongName));
    if (old.getCause() != null) {
      t.initCause(deobfuscateThrowable(old.getCause(), strongName));
    }
    return t;
  }

  /**
   * @deprecated The behavior of changing symbol map after construction is undefined, please provide
   *             it in construction time. If the directory needs to be changed after construction, a
   *             new instance of this class can be instantiated with the different one.
   */
  @Deprecated
  public void setSymbolMapsDirectory(String symbolMapsDirectory) {
    // permutations are unique, no need to clear the symbolMaps hash map
    this.symbolMapsDirectory = new File(symbolMapsDirectory);
  }

  protected InputStream openInputStream(String fileName) throws IOException {
    return new FileInputStream(new File(symbolMapsDirectory, fileName));
  }
}
