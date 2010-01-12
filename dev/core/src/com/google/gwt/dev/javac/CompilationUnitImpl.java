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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.util.collect.Lists;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.Collection;
import java.util.List;
import java.util.Set;

abstract class CompilationUnitImpl extends CompilationUnit {

  private final Set<ContentId> dependencies;
  private final List<CompiledClass> exposedCompiledClasses;
  private final List<JsniMethod> jsniMethods;
  private final CategorizedProblem[] problems;
  private final MethodArgNamesLookup methodArgs;

  public CompilationUnitImpl(List<CompiledClass> compiledClasses,
      Set<ContentId> dependencies,
      Collection<? extends JsniMethod> jsniMethods,
      MethodArgNamesLookup methodArgs, CategorizedProblem[] problems) {
    this.exposedCompiledClasses = Lists.normalizeUnmodifiable(compiledClasses);
    this.dependencies = dependencies;
    this.jsniMethods = Lists.create(jsniMethods.toArray(new JsniMethod[jsniMethods.size()]));
    this.methodArgs = methodArgs;
    this.problems = problems;
    for (CompiledClass cc : compiledClasses) {
      cc.initUnit(this);
    }
  }

  @Override
  public List<JsniMethod> getJsniMethods() {
    return jsniMethods;
  }

  @Override
  public MethodArgNamesLookup getMethodArgs() {
    return methodArgs;
  }

  /**
   * Returns all contained classes.
   */
  @Override
  Collection<CompiledClass> getCompiledClasses() {
    return exposedCompiledClasses;
  }

  @Override
  Set<ContentId> getDependencies() {
    return dependencies;
  }

  @Override
  CategorizedProblem[] getProblems() {
    return problems;
  }
}
