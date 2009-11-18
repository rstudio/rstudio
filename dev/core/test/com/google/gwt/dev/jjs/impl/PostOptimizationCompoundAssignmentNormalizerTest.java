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
 * Tests {@link PostOptimizationCompoundAssignmentNormalizer}.
 */
public class PostOptimizationCompoundAssignmentNormalizerTest
    extends OptimizerTestBase {

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

    /**
     * Compare without compiling expected, needed when optimizations produce
     * incorrect java code (e.g. "a" || "b" is incorrect in java).
     */
    public void intoString(String expected) {
      String actual = optimized;
      assertTrue(actual.startsWith("{"));
      assertTrue(actual.endsWith("}"));
      actual = actual.substring(1, actual.length() - 2).trim();
      // Join lines in actual code and remove indentations
      actual = actual.replaceAll(" +", " ").replaceAll("\n", "");
      assertEquals(userCode, expected, actual);
    }
  }

  public void testIntegralFloatCoercion() throws Exception {
    // long op= float
    optimize("void", "long x=2L; float d=3; x += d;").into(
        "long x=2L; float d=3; x = (long)((float)x + d);");
    // long op= long
     optimize("void", "long x=2L; long d=3L; x += d;").into(
        "long x=2L; long d=3L; x = x + d;");
    // don't touch integral op= integral
    optimize("void", "int x=2; int d=3; x += d;").into(
        "int x=2; int d=3; x += d;");
    // don't touch, different integral types
    optimize("void", "int x=2; short d=3; x += d;").into(
        "int x=2; short d=3; x += d;");
    // integral with long, don't touch
    optimize("void", "int x=2; long d=3L; x += d;").into(
        "int x=2; long d=3L; x += (int)d;");
    // integral with float
    optimize("void", "int x=2; float d=3.0f; x += d;").into(
        "int x=2; float d=3.0f; x = (int)(x + d);");
    // integral with double
    optimize("void", "int x=2; double d=3.0; x += d;").into(
        "int x=2; double d=3.0; x = (int)(x + d);");
    // float and double, don't touch
    optimize("void", "float x=2; double d=3.0; x += d;").into(
        "float x=2; double d=3.0; x += d;");
  }
  
  private Result optimize(final String returnType, final String codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(returnType, codeSnippet);
    PostOptimizationCompoundAssignmentNormalizer.exec(program);
    LongCastNormalizer.exec(program);
    return new Result(returnType, codeSnippet, getMainMethodSource(program));
  }
}
