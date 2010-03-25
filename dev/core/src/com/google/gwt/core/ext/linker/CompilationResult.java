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
   * Returns the JavaScript compilation. The first element of the array contains
   * the code that should be run when the application starts up. The remaining
   * elements are loaded via
   * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback)
   * GWT.runAsync}. See
   * {@link com.google.gwt.core.client.impl.AsyncFragmentLoader
   * AsyncFragmentLoader} for details on the necessary linker support for
   * runAsync.
   */
  public abstract String[] getJavaScript();

  /**
   * Returns the permutation ID.
   */
  public abstract int getPermutationId();

  /**
   * Provides values for {@link SelectionProperty} instances that are not
   * explicitly set during the compilation phase. This method will return
   * multiple mappings, one for each permutation that resulted in the
   * compilation.
   */
  public abstract SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap();

  /**
   * Returns the permutations of the collapsed deferred-binding property values
   * that are compiled into the CompilationResult.
   */
  public abstract SoftPermutation[] getSoftPermutations();

  /**
   * Returns the statement ranges for the JavaScript returned by
   * {@link #getJavaScript()}. Some subclasses return <code>null</code>, in
   * which case there is no statement range information available.
   */
  public StatementRanges[] getStatementRanges() {
    return null;
  }

  /**
   * Return a string that uniquely identifies this compilation result. Typically
   * this is a cryptographic hash of the compiled data.
   */
  public abstract String getStrongName();

  /**
   * Returns a sorted array of obfuscated symbol names in the compilation to
   * metadata about the symbol. This data can allow for on-the-fly deobfuscation
   * of stack trace information or to allow server components to have in-depth
   * knowledge of the runtime structure of compiled objects.
   */
  public abstract SymbolData[] getSymbolMap();

  @Override
  public final int hashCode() {
    return getStrongName().hashCode();
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
    return getStrongName().compareTo(o.getStrongName());
  }

  @Override
  protected final Class<CompilationResult> getComparableArtifactType() {
    return CompilationResult.class;
  }
}
