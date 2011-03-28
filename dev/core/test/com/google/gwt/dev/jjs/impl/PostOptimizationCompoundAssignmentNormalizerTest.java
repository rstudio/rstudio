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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests {@link PostOptimizationCompoundAssignmentNormalizer}.
 */
public class PostOptimizationCompoundAssignmentNormalizerTest
    extends OptimizerTestBase {

  public void testIntegralFloatCoercion() throws Exception {
    // long op= float
    optimize("void", "long x=2L; float d=3; x += d;").into(
        "long x=2L; float d=3; x = (long)((float)x + d);");
    // long op= long
     optimize("void", "long x=2L; long d=3L; x += d;").into(
        "long x=2L; long d=3L; x = x + d;");
    // don't touch int op= int
    optimize("void", "int x=2; int d=3; x += d;").into(
        "int x=2; int d=3; x += d;");
    // don't touch, integral types with lhs wider than rhs
    optimize("void", "int x=2; short d=3; x += d;").into(
        "int x=2; short d=3; x += d;");
    // different integral types, but should narrow result
    optimize("void", "int x=2; short d=3; d += x;").into(
        "int x=2; short d=3; d = (short)(d + x);");
    // integral with long, should break up
    optimize("void", "int x=2; long d=3L; x += d;").into(
        "int x=2; long d=3L; x = (int)((long)x + d);");
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
  
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    PostOptimizationCompoundAssignmentNormalizer.exec(program);
    LongCastNormalizer.exec(program);
    return true;
  }
}
