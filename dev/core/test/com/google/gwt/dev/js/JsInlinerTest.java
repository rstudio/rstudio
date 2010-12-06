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

  /**
   * Test that an array reference breaks argument ordering.
   */
  public void testOrderingArray() throws Exception {
    StringBuffer code = new StringBuffer();

    code.append("function clinit() { clinit = null; }");

    // callee references array[0] before evaluating argument
    code.append("function callee(arg) { var array; return array[0] + arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { callee((clinit(),2)); }");

    // bootstrap the program
    code.append("caller();");

    compare(code.toString(), code.toString());
  }

  /**
   * Test that a field reference breaks argument ordering.
   */
  public void testOrderingField() throws Exception {
    StringBuffer code = new StringBuffer();

    code.append("function clinit() {  clinit = null; }");

    // callee references field.x before evaluating argument
    code.append("function callee(arg) { var field; return field.x + arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { callee((clinit(),2)); }");

    // bootstrap the program
    code.append("caller();");

    compare(code.toString(), code.toString());
  }

  /**
   * Test that a global variable breaks argument ordering.
   */
  public void testOrderingGlobal() throws Exception {
    StringBuffer code = new StringBuffer();
    // A global variable x
    code.append("var x;");

    // clinit() sets up x
    code.append("function clinit() { x = 1; clinit = null; }");

    // callee references x before evaluating argument
    code.append("function callee(arg) { alert(x); return arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { callee((clinit(),2)); }");

    // bootstrap the program
    code.append("caller();");

    compare(code.toString(), code.toString());
  }

  /**
   * Test that a local variable does not break argument ordering.
   */
  public void testOrderingLocal() throws Exception {
    StringBuffer code = new StringBuffer();

    code.append("function clinit() { clinit = null; }");

    // callee references y before evaluating argument
    code.append("function callee(arg) { var y; y=2; return arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { return callee((clinit(),3)); }");

    // bootstrap the program
    code.append("caller();");

    StringBuffer expected = new StringBuffer();

    expected.append("function clinit() { clinit = null; }");
    expected.append("function caller() {var y; return y=2,clinit(),3;}");
    expected.append("caller();");
    compare(expected.toString(), code.toString());
  }

  /**
   * Test that a new expression breaks argument ordering.
   */
  public void testOrderingNew() throws Exception {
    StringBuffer code = new StringBuffer();
    // A static variable x
    code.append("var x;");

    // foo() uses x
    code.append("function foo() { alert('x = ' + x); }");

    // callee does "new foo" before evaluating its argument
    code.append("function callee(arg) { new foo(); return arg; }");

    // caller invokes callee with a multi that initializes x
    code.append("function caller() { callee((x=1,2)); }");

    // bootstrap the program
    code.append("caller();");

    compare(code.toString(), code.toString());
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
    assertEquals(expected, input);
  }
}
