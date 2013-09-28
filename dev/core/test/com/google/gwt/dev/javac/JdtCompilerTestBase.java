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

import static org.junit.Assert.assertArrayEquals;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.Collection;
import java.util.List;

/**
 * Base class for all JdtCompiler tests.
 */
public abstract class JdtCompilerTestBase extends TestCase {

  protected void assertResourcesCompileSuccessfully(Resource... resources)
      throws UnableToCompleteException {
    assertUnitsCompiled(compile(resources));
  }

  protected List<CompilationUnit> compile(Resource... resources) throws UnableToCompleteException {
    List<CompilationUnitBuilder> builders = buildersFor(resources);
    return compile(builders);
  }

  protected List<CompilationUnit> compile(Collection<CompilationUnitBuilder> builders)
      throws UnableToCompleteException {
    return  JdtCompiler.compile(TreeLogger.NULL, builders, getSourceLevel());
  }

  /**
   * Returns the java source level to be used when compiling.
   *
   * <p>Override this method in the derived test case to request a specific source level.
   */
  protected SourceLevel getSourceLevel() {
    return SourceLevel.DEFAULT_SOURCE_LEVEL;
  }

  protected static void addAll(Collection<CompilationUnitBuilder> units, Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      units.add(CompilationUnitBuilder.create(sourceFile));
    }
  }

  protected static void assertOnlyLastUnitHasErrors(List<CompilationUnit> units,
      String... errorPatterns) {
    assertUnitsCompiled(units.subList(0, units.size() - 1));
    assertUnitHasErrors(units.get(units.size() - 1), errorPatterns);
  }

  protected static void assertUnitHasErrors(CompilationUnit unit, String... expectedErrors) {
    assertTrue(unit.isError());
    // Filter errors
    List<String> actualErrors = Lists.newArrayList();
    for (CategorizedProblem problem : unit.getProblems()) {
      if (problem.isError()) {
        actualErrors.add(problem.getMessage());
      }
    }
    assertArrayEquals(expectedErrors, actualErrors.toArray());
  }

  protected static void assertUnitsCompiled(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      if (unit.isError()) {
        String[] messages = new String[unit.getProblems().length];
        int i = 0;
        for (CategorizedProblem pb : unit.getProblems()) {
          messages[i] = pb.getMessage();
        }
        fail(Joiner.on("\n").join(messages));
      }
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  protected static List<CompilationUnitBuilder> buildersFor(Resource... resources) {
    List<CompilationUnitBuilder> builders = Lists.newArrayList();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, resources);
    return builders;
  }
}
