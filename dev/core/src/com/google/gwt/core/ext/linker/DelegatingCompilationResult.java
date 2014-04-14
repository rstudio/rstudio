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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;

import java.util.SortedMap;
import java.util.SortedSet;

/**
 * A delegate for {@link CompilationResult} intended for third party linkers
 * that want to modify a compilation result.  Extending this class should insulate
 * against future changes to the CompilationResult type.
 */
public class DelegatingCompilationResult extends CompilationResult {
  private final CompilationResult delegate;

  public DelegatingCompilationResult(Class<? extends Linker> linkerType,
      CompilationResult compilationResult) {
    super(linkerType);
    this.delegate = compilationResult;
  }

  @Override
  public String[] getJavaScript() {
    return delegate.getJavaScript();
  }

  @Override
  public int getPermutationId() {
    return delegate.getPermutationId();
  }

  @Override
  public SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap() {
    return delegate.getPropertyMap();
  }

  @Override
  public SoftPermutation[] getSoftPermutations() {
    return delegate.getSoftPermutations();
  }

  @Override
  public StatementRanges[] getStatementRanges() {
    return delegate.getStatementRanges();
  }

  @Override
  public String getStrongName() {
    return delegate.getStrongName();
  }

  @Override
  public SymbolData[] getSymbolMap() {
    return delegate.getSymbolMap();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
