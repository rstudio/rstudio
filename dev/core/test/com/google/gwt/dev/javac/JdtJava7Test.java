/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.Java7MockResources;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Strings;
import com.google.gwt.dev.util.arg.SourceLevel;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test class for language features introduced in Java 7.
 *
 * Only tests that the JDT accepts and compiles the new syntax..
 */
public class JdtJava7Test extends TestCase {

  static void assertUnitHasErrors(CompilationUnit unit, int numErrors) {
    assertTrue(unit.isError());
    assertEquals(numErrors, unit.getProblems().length);
  }

  static void assertUnitsCompiled(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      if (unit.isError()) {
        String[] messages = new String[unit.getProblems().length];
        int i = 0;
        for (CategorizedProblem pb : unit.getProblems()) {
          messages[i] = pb.getMessage();
        }
        fail(Strings.join(messages, "\n"));
      }
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  public void testCompileNewStyleLiterals() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, Java7MockResources.LIST_T, Java7MockResources.ARRAYLIST_T,
        Java7MockResources.NEW_INTEGER_LITERALS_TEST);
    Collection<CompilationUnit> units = compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units);
  }

  public void testCompileSwitchWithStrings() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, Java7MockResources.LIST_T, Java7MockResources.ARRAYLIST_T,
        Java7MockResources.SWITCH_ON_STRINGS_TEST);
    Collection<CompilationUnit> units = compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units);
  }

  public void testCompileDiamondOperator() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, Java7MockResources.LIST_T, Java7MockResources.ARRAYLIST_T,
        Java7MockResources.DIAMOND_OPERATOR_TEST);
    Collection<CompilationUnit> units = compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units);
  }

  public void testCompileTryWithResources() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders,
        Java7MockResources.TEST_RESOURCE, Java7MockResources.TRY_WITH_RESOURCES_TEST);
    Collection<CompilationUnit> units = compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units);
  }

  public void testCompileMultiExceptions() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, Java7MockResources.EXCEPTION1, Java7MockResources.EXCEPTION2,
        Java7MockResources.MULTI_EXCEPTION_TEST);
    Collection<CompilationUnit> units = compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units);
  }

  private void addAll(Collection<CompilationUnitBuilder> units,
                      Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      units.add(CompilationUnitBuilder.create(sourceFile));
    }
  }

  private List<CompilationUnit> compile(TreeLogger logger,
      Collection<CompilationUnitBuilder> builders) throws UnableToCompleteException {
    return JdtCompiler.compile(logger, builders, SourceLevel.JAVA7);
  }
}
