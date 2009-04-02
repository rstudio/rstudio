/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.javac;

import java.util.Map;
import java.util.Set;

/**
 * An unmodifiable view of a module's Java source tree.
 * 
 * <p>
 * The identity of the returned sets and maps will change exactly when the
 * underlying module is refreshed.
 * </p>
 * 
 * <p>
 * Even when the identity of a returned set changes, the identity of any
 * contained {@link JavaSourceFile} values is guaranteed to differ from a
 * previous result exactly when that particular source file becomes invalid.
 * </p>
 * 
 * <p>
 * A source file could become invalid for various reasons, including:
 * <ul>
 * <li>the underlying file was deleted or modified</li>
 * <li>another file with the same logical name superceded it on the classpath</li>
 * <li>the underlying module changed to exclude this file or supercede it with
 * another file</li>
 * </ul>
 * </p>
 * 
 * <p>
 * After a refresh, a client can reliably detect changes by checking which of
 * its cached source files is still contained in the new result of
 * {@link #getSourceFiles()}.
 * </p>
 */
public interface JavaSourceOracle {

  /**
   * Frees up all existing resources and transient internal state. The
   * underlying ResourceOracle must be refreshed to be valid again.
   */
  void clear();

  /**
   * Returns an unmodifiable set of fully-qualified class names with constant
   * lookup time.
   */
  Set<String> getClassNames();

  /**
   * Returns an unmodifiable set of unique source files with constant lookup
   * time.
   */
  Set<JavaSourceFile> getSourceFiles();

  /**
   * Returns an unmodifiable map of fully-qualified class name to source file.
   */
  Map<String, JavaSourceFile> getSourceMap();
}
