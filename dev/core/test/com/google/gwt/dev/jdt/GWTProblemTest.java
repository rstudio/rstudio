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
package com.google.gwt.dev.jdt;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * 
 */
public class GWTProblemTest extends TestCase {
  public void testRecordError() {
    String fileName = "TestCompilationUnit.java";
    String errorMessage = "Unit has errors";
    CompilationResult compilationResult = new CompilationResult(
        fileName.toCharArray(), 0, 0, 0);
    CompilationUnitDeclaration cud = new CompilationUnitDeclaration(null,
        compilationResult, 0);

    // Pick an Expression subtype to pass in
    GWTProblem.recordInCud(new Wildcard(Wildcard.EXTENDS), cud, errorMessage);

    CategorizedProblem[] errors = compilationResult.getErrors();
    assertEquals(1, errors.length);
    CategorizedProblem problem = errors[0];
    assertTrue(problem.isError());
    assertEquals(1, problem.getSourceLineNumber());
    assertEquals(errorMessage, problem.getMessage());
  }
}
