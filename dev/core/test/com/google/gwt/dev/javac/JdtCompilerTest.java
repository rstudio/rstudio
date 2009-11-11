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

import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.resource.Resource;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test class for {@link JdtCompiler}.
 */
public class JdtCompilerTest extends TestCase {

  static void assertUnitHasErrors(CompilationUnit unit, int numErrors) {
    CompilationUnitDeclaration cud = unit.getJdtCud();
    CompilationResult result = cud.compilationResult();
    assertTrue(result.hasErrors());
    assertEquals(numErrors, result.getErrors().length);
    assertTrue(result.getClassFiles().length > 0);
  }

  static void assertUnitsCompiled(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      CompilationUnitDeclaration cud = unit.getJdtCud();
      assertFalse(cud.hasErrors());
      CompilationResult result = cud.compilationResult();
      assertFalse(result.hasProblems());
      assertTrue(result.getClassFiles().length > 0);
    }
  }

  public void testCompile() {
    List<CompilationUnit> units = new ArrayList<CompilationUnit>();
    addAll(units, JavaResourceBase.getStandardResources());
    addAll(units, JavaResourceBase.FOO, JavaResourceBase.BAR);
    JdtCompiler.compile(units);
    assertUnitsCompiled(units);
  }

  public void testCompileError() {
    List<CompilationUnit> units = new ArrayList<CompilationUnit>();
    addAll(units, JavaResourceBase.getStandardResources());
    addAll(units, JavaResourceBase.BAR);
    JdtCompiler.compile(units);
    assertUnitsCompiled(units.subList(0, units.size() - 1));
    assertUnitHasErrors(units.get(units.size() - 1), 1);
  }

  public void testCompileIncremental() {
    List<CompilationUnit> units = new ArrayList<CompilationUnit>();
    addAll(units, JavaResourceBase.getStandardResources());
    JdtCompiler.compile(units);
    assertUnitsCompiled(units);
    addAll(units, JavaResourceBase.FOO, JavaResourceBase.BAR);
    JdtCompiler.compile(units);
    assertUnitsCompiled(units);
  }

  private void addAll(Collection<CompilationUnit> units,
      Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      units.add(new SourceFileCompilationUnit(sourceFile));
    }
  }
}
