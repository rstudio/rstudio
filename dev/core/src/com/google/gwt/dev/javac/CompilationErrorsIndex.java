/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.javac;

import java.util.List;
import java.util.Set;

/**
 * Provides fast access to compilation error information to support detailed logging.
 */
public interface CompilationErrorsIndex {

  /**
   * Records the filename, names of referenced types, and known compilation errors for a given type.
   */
  void add(String typeSourceName, String fileName, List<String> typeReferences,
      List<String> compilationErrors);

  /**
   * Returns the compile error strings previously recorded for a given type.
   */
  Set<String> getCompileErrors(String typeSourceName);

  /**
   * Returns the file name previously recorded for a given type.
   */
  String getFileName(String typeSourceName);

  /**
   * Returns the type reference strings previously recorded for a given type.
   */
  Set<String> getTypeReferences(String typeSourceName);

  /**
   * Returns whether a given type has any recorded compile errors.
   */
  boolean hasCompileErrors(String typeSourceName);

  /**
   * Returns whether a given type has any recorded type references.
   */
  boolean hasTypeReferences(String typeSourceName);
}
