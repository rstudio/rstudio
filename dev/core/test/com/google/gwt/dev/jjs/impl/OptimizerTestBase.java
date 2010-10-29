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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.dev.util.Strings;

import java.util.regex.Pattern;

/**
 * Test case for testing Jjs optimizers. Adds a convenient Result class.
 */
public abstract class OptimizerTestBase extends JJSTestBase {
  protected boolean runDeadCodeElimination = false;
  
  protected final class Result {
    private final String returnType;
    private final String originalCode;
    private final boolean madeChanges;
    private final JProgram optimizedProgram;
    private final String methodName;

    public Result(JProgram optimizedProgram, String returnType, 
        String methodName, String originalCode, boolean madeChanges) {
      this.optimizedProgram = optimizedProgram;
      this.returnType = returnType;
      this.methodName = methodName;
      this.originalCode = originalCode;
      this.madeChanges = madeChanges;
    }

    public void into(String... expected) throws UnableToCompleteException {
      // We can't compile expected code into non-main method.
      Preconditions.checkState(methodName.equals(MAIN_METHOD_NAME));
      JProgram program = compileSnippet(returnType,
          Strings.join(expected, "\n"));
      String expectedSource = 
        OptimizerTestBase.findMethod(program, methodName).getBody().toSource();
      String actualSource = 
        OptimizerTestBase.findMethod(optimizedProgram, methodName).
        getBody().toSource();
      assertEquals(originalCode, expectedSource, actualSource);
    }

    public void intoString(String... expected) {
      String expectedSource = Strings.join(expected, "\n");
      String actualSource = 
        OptimizerTestBase.findMethod(optimizedProgram, methodName).
        getBody().toSource();

      // Trim surrounding {} and unindent body once
      assertTrue(actualSource.startsWith("{"));
      assertTrue(actualSource.endsWith("}"));
      actualSource = actualSource.substring(1, actualSource.length() - 2).trim();
      actualSource = Pattern.compile("^  ", Pattern.MULTILINE).
          matcher(actualSource).replaceAll("");
      
      assertEquals(originalCode, expectedSource, actualSource);
    }

    public void noChange() {
      assertFalse(madeChanges);
    }

    public JMethod findMethod(String methodName) {
      return OptimizerTestBase.findMethod(optimizedProgram, methodName);
    }

    public JField findField(String fieldName) {
      return OptimizerTestBase.findField(optimizedProgram, 
          "EntryPoint." + fieldName);
    }

    public JDeclaredType findClass(String className) {
      return OptimizerTestBase.findType(optimizedProgram, className);
    }
  }

  protected final Result optimize(final String returnType, 
      final String... codeSnippet) throws UnableToCompleteException {
    return optimizeMethod(MAIN_METHOD_NAME, returnType, codeSnippet);
  }

  protected final Result optimizeMethod(final String methodName,
      final String mainMethodReturnType, final String... mainMethodSnippet)
      throws UnableToCompleteException {
    String snippet = Strings.join(mainMethodSnippet, "\n");
    JProgram program = compileSnippet(mainMethodReturnType, snippet);
    JMethod method = findMethod(program, methodName);
    boolean madeChanges = optimizeMethod(program, method);
    if (madeChanges && runDeadCodeElimination) {
      DeadCodeElimination.exec(program);
    }
    return new Result(program, mainMethodReturnType, methodName, snippet, madeChanges);
  }

  protected abstract boolean optimizeMethod(JProgram program, JMethod method);
}
