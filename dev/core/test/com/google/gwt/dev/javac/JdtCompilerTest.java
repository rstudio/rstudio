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

import com.google.gwt.dev.javac.CompilationUnitBuilder.ResourceCompilationUnitBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.resource.Resource;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test class for {@link JdtCompiler}.
 */
public class JdtCompilerTest extends TestCase {

  static void assertUnitHasErrors(CompilationUnit unit, int numErrors) {
    assertTrue(unit.isError());
    assertEquals(numErrors, unit.getProblems().length);
  }

  static void assertUnitsCompiled(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      assertFalse(unit.isError());
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  public void testCompile() {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, JavaResourceBase.FOO, JavaResourceBase.BAR);
    Collection<CompilationUnit> units = JdtCompiler.compile(builders);
    assertUnitsCompiled(units);
  }

  public void testCompileError() {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, JavaResourceBase.BAR);
    List<CompilationUnit> units = JdtCompiler.compile(builders);
    assertUnitsCompiled(units.subList(0, units.size() - 1));
    assertUnitHasErrors(units.get(units.size() - 1), 1);
  }

  public void testCompileIncremental() {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    Collection<CompilationUnit> units = JdtCompiler.compile(builders);
    assertUnitsCompiled(units);
    addAll(builders, JavaResourceBase.FOO, JavaResourceBase.BAR);
    JdtCompiler.compile(builders);
    assertUnitsCompiled(units);
  }

  private void addAll(Collection<CompilationUnitBuilder> units,
      Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      units.add(CompilationUnitBuilder.create(sourceFile));
    }
  }
}
