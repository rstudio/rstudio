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
 * A CompilationErrorsIndex facade that exposes the contents of two contained
 * CompilationErrorsIndices.
 */
public class CombinedCompilationErrorsIndex implements CompilationErrorsIndex {

  private CompilationErrorsIndex libraryCompilationErrorsIndex;
  private CompilationErrorsIndex localCompilationErrorsIndexes;

  public CombinedCompilationErrorsIndex(CompilationErrorsIndex localCompilationErrorsIndexes,
      CompilationErrorsIndex libraryCompilationErrorsIndex) {
    this.localCompilationErrorsIndexes = localCompilationErrorsIndexes;
    this.libraryCompilationErrorsIndex = libraryCompilationErrorsIndex;
  }

  @Override
  public void add(String typeSourceName, String fileName, List<String> typeReferences,
      List<String> compilationErrors) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getCompileErrors(String typeSourceName) {
    if (localCompilationErrorsIndexes.hasCompileErrors(typeSourceName)) {
      return localCompilationErrorsIndexes.getCompileErrors(typeSourceName);
    }
    if (libraryCompilationErrorsIndex.hasCompileErrors(typeSourceName)) {
      return libraryCompilationErrorsIndex.getCompileErrors(typeSourceName);
    }
    return null;
  }

  @Override
  public String getFileName(String typeSourceName) {
    String localFileName = localCompilationErrorsIndexes.getFileName(typeSourceName);
    if (localFileName != null) {
      return localFileName;
    }
    String libraryFileName = libraryCompilationErrorsIndex.getFileName(typeSourceName);
    if (libraryFileName != null) {
      return libraryFileName;
    }
    return null;
  }

  @Override
  public Set<String> getTypeReferences(String typeSourceName) {
    if (localCompilationErrorsIndexes.hasTypeReferences(typeSourceName)) {
      return localCompilationErrorsIndexes.getTypeReferences(typeSourceName);
    }
    if (libraryCompilationErrorsIndex.hasTypeReferences(typeSourceName)) {
      return libraryCompilationErrorsIndex.getTypeReferences(typeSourceName);
    }
    return null;
  }

  @Override
  public boolean hasCompileErrors(String typeSourceName) {
    return localCompilationErrorsIndexes.hasCompileErrors(typeSourceName)
        || libraryCompilationErrorsIndex.hasCompileErrors(typeSourceName);
  }

  @Override
  public boolean hasTypeReferences(String typeSourceName) {
    return localCompilationErrorsIndexes.hasTypeReferences(typeSourceName)
        || libraryCompilationErrorsIndex.hasTypeReferences(typeSourceName);
  }
}
