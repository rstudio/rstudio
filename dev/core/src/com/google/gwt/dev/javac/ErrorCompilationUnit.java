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
package com.google.gwt.dev.javac;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A compilation unit with errors.
 */
class ErrorCompilationUnit extends CompilationUnit {

  private final CompilationUnit unit;

  public ErrorCompilationUnit(CompilationUnit unit) {
    this.unit = unit;
  }

  public String getDisplayLocation() {
    return unit.getDisplayLocation();
  }

  public List<JsniMethod> getJsniMethods() {
    return unit.getJsniMethods();
  }

  public long getLastModified() {
    return unit.getLastModified();
  }

  public String getSource() {
    return unit.getSource();
  }

  public String getTypeName() {
    return unit.getTypeName();
  }

  public boolean isCompiled() {
    return false;
  }

  public boolean isError() {
    return true;
  }

  public boolean isGenerated() {
    return unit.isGenerated();
  }

  public boolean isSuperSource() {
    return unit.isSuperSource();
  }

  @Override
  Collection<CompiledClass> getCompiledClasses() {
    return unit.getCompiledClasses();
  }

  @Override
  ContentId getContentId() {
    return unit.getContentId();
  }

  @Override
  Set<ContentId> getDependencies() {
    return unit.getDependencies();
  }

  @Override
  CategorizedProblem[] getProblems() {
    return unit.getProblems();
  }

}
