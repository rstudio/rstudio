/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Represents a unique compilation of the module. Multiple permutations may
 * result in identical JavaScript.
 */
public abstract class CompilationResult extends Artifact<CompilationResult> {

  protected CompilationResult(Class<? extends Linker> linkerType) {
    super(linkerType);
  }

  /**
   * Returns the JavaScript compilation. The exact form and function of the
   * JavaScript should be considered opaque.
   */
  public abstract String getJavaScript();

  /**
   * Provides values for {@link SelectionProperty} instances that are not
   * explicitly set during the compilation phase. This method will return
   * multiple mappings, one for each permutation that resulted in the
   * compilation.
   */
  public abstract SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap();

  @Override
  public final int hashCode() {
    return getJavaScript().hashCode();
  }

  @Override
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("{");
    for (SortedMap<SelectionProperty, String> map : getPropertyMap()) {
      b.append(" {");
      for (Map.Entry<SelectionProperty, String> entry : map.entrySet()) {
        b.append(" ").append(entry.getKey().getName()).append(":").append(
            entry.getValue());
      }
      b.append(" }");
    }
    b.append(" }");

    return b.toString();
  }

  @Override
  protected final int compareToComparableArtifact(CompilationResult o) {
    return getJavaScript().compareTo(o.getJavaScript());
  }

  @Override
  protected final Class<CompilationResult> getComparableArtifactType() {
    return CompilationResult.class;
  }
}