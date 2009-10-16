/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;

/**
 * Safety checks for JsInliner.
 */
public class JsInlinerTest extends OptimizerTestBase {

  private static class FixStaticRefsVisitor extends JsModVisitor {

    public static void exec(JsProgram program) {
      (new FixStaticRefsVisitor()).accept(program);
    }

    @Override
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      JsName name = x.getName();
      if (name != null) { 
        name.setStaticRef(x);
      }
    }
  }

  public void testInlineArrayLiterals() throws Exception {
    String input = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1([], 10); } b1();";
    compare(input, input);
  }

  public void testInlineFunctionLiterals() throws Exception {
    String input = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1(function (){}, 10); } b1();";
    compare(input, input);
    String input2 = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1(function blah(){}, 10); } b1();";
    compare(input2, input2);
  }
  
  public void testInlineObjectLiterals() throws Exception {
    String input = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1({}, 10); } b1();";
    compare(input, input);
  }
  /**
   * A test for mutually-recursive functions. Setup:
   *
   * <pre>
   * a -> b, c
   * b -> a, c
   * c -> a, c
   * </pre>
   */
  public void testMutualRecursion() throws Exception {
    String input = "function a1() { return ex ? b1() : c1() }"
        + "function b1() { return ex2 ? a1(): c1(); }"
        + "function c1() { return ex2? a1():c1(); } c1()";
    String expected = "function a1() { return ex ? (ex2 ? a1() : c1()) : c1() }"
        + "function c1() { return ex2 ? a1() :c1(); } c1()";
    compare(expected, input);
  }

  public void testSelfRecursion() throws Exception {
    String input = "function a1() { return blah && b1() }"
        + "function b1() { return bar && a1()}" + "function c() { a1() } c()";

    String expected = "function a1() { return blah && bar && a1() }"
        + "function c() { a1() } c()";

    compare(expected, input);
  }

  private void compare(String expected, String input) throws Exception {
    input = optimize(input, JsSymbolResolver.class, FixStaticRefsVisitor.class,
        JsInliner.class, JsUnusedFunctionRemover.class);
    expected = optimize(expected);
    System.err.println("Input vs ");
    assertEquals(expected, input);
  }
}
