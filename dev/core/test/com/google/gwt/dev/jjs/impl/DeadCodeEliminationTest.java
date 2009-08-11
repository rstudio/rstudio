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
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests {@link DeadCodeElimination}.
 */
public class DeadCodeEliminationTest extends OptimizerTestBase {

  private final class Result {
    private final String optimized;
    private final String returnType;
    private final String userCode;

    public Result(String returnType, String userCode, String optimized) {
      this.returnType = returnType;
      this.userCode = userCode;
      this.optimized = optimized;
    }

    public void into(String expected) throws UnableToCompleteException {
      JProgram program = compileSnippet(returnType, expected);
      expected = getMainMethodSource(program);
      assertEquals(userCode, expected, optimized);
    }
  }

  public void testConditionalOptimizations() throws Exception {
    optimize("int", "return true ? 3 : 4;").into("return 3;");
    optimize("int", "return false ? 3 : 4;").into("return 4;");

    addSnippetClassDecl("static volatile boolean TEST");
    addSnippetClassDecl("static volatile boolean RESULT");

    optimize("boolean", "return TEST ? true : RESULT;").into(
        "return TEST || RESULT;");

    optimize("boolean", "return TEST ? false : RESULT;").into(
        "return !TEST && RESULT;");

    optimize("boolean", "return TEST ? RESULT : true;").into(
        "return !TEST || RESULT;");

    optimize("boolean", "return TEST ? RESULT : false;").into(
        "return TEST && RESULT;");
  }

  public void testIfOptimizations() throws Exception {
    optimize("int", "if (true) return 1; return 0;").into("return 1;");
    optimize("int", "if (false) return 1; return 0;").into("return 0;");
    optimize("int", "if (true) return 1; else return 2;").into("return 1;");
    optimize("int", "if (false) return 1; else return 2;").into("return 2;");

    optimize("int", "if (true) {} else return 4; return 0;").into("return 0;");

    addSnippetClassDecl("static volatile boolean TEST");
    addSnippetClassDecl("static boolean test() { return TEST; }");
    optimize("int", "if (test()) {} else {}; return 0;").into(
        "test(); return 0;");
  }

  private Result optimize(final String returnType, final String codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(returnType, codeSnippet);
    DeadCodeElimination.exec(program);
    return new Result(returnType, codeSnippet, getMainMethodSource(program));
  }
}
