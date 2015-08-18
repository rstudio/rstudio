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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.common.InliningMode;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.List;

/**
 * Safety checks for JsInliner.
 */
public class JsInlinerTest extends OptimizerTestBase {

  private static class FixStaticRefsVisitor extends JsModVisitor {

    /**
     * Called reflectively.
     */
    @SuppressWarnings("unused")
    public static void exec(JsProgram program) {
      (new FixStaticRefsVisitor()).accept(program);
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      JsName name = x.getName();
      if (name != null) {
        name.setStaticRef(x);
      }
    }
  }

  public void testInlineArrayLiterals() throws Exception {
    String input = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1([], 10); } b1();";
    verifyNoChange(input);
  }

  public void testInlineFunctionLiterals() throws Exception {
    String input = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1(function (){}, 10); } b1();";
    verifyNoChange(input);
    String input2 = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1(function blah(){}, 10); } b1();";
    verifyNoChange(input2);
  }

  public void testInlineObjectLiterals() throws Exception {
    String input = "function a1(arg, x) { arg.x = x; return arg; }"
        + "function b1() { var x=a1({}, 10); } b1();";
    verifyNoChange(input);
  }

  public void testInlineSmallFunctions() throws Exception {
    String input, expected;
    // Always make more than one call, because there are special heuristics for functions that
    // are called only once.

    // Inline empty function
    input = Joiner.on('\n').join(
        "function setP(t, p) {}",
        "function b1(o) {  setP(o, 1); setP(o, 2);  } b1({});");
    expected = Joiner.on('\n').join(
        "function b1(o){}",
        "b1({});");
    verifyOptimized(expected, input);

    // Inline a array assignment.
    input = Joiner.on('\n').join(
        "function set(arr, p,  v) { arr[p] = v; }",
        "function b1(arr) {  set(arr, \"X\", 1); set(arr, \"Y\", 1);  } b1({});");
    expected = Joiner.on('\n').join(
        "function b1(arr){arr['X']=1;arr['Y']=1}",
        "b1({});");
    verifyOptimized(expected, input);

    // Inline a devirtualized setter
    input = Joiner.on('\n').join(
        "function setP(t, p) {t.a=p; }",
        "function b1(o) {  setP(o, 1); setP(o, 2);  } b1({});");
    expected = Joiner.on('\n').join(
        "function b1(o){o.a=1; o.a=2;}",
        "b1({});");
    verifyOptimized(expected, input);

    // Inline a devirtualized getter
    input = Joiner.on('\n').join(
        "function getP(t) {return t.a; }",
        "function b1(o) {  getP(o) == getP(o);  } b1({});");
    expected = Joiner.on('\n').join(
        "function b1(o){o.a==o.a;}",
        "b1({});");
    verifyOptimized(expected, input);
  }

  public void testWithVardeclaration() throws Exception {
    String input, expected;
    // Always make more than one call, because there are special heuristics for functions that
    // are called only once.

    input = Joiner.on('\n').join(
        "function a(o) { $wnd.blah(o); }",
        "function f(t,u,b,d) {var a = t;  return a.u;}",
        "function b1(o) { a(f(o,1,2,3)); a(f(o,1,2,5));  } b1({});");
    expected = Joiner.on('\n').join(
        "function d(a){$wnd.blah(a)}",
        "function e(a){var b,c;d((b=a,b.u));d((c=a,c.u))}",
        "e({})");
    verifyOptimizedObfuscated(expected, input);
  }

  public void testInliningAnnotations() throws Exception {
    String input, expected;
    // Always make more than one call, because there are special heuristics for functions that
    // are called only once.

    // Test FORCE_INLINE
    input = Joiner.on('\n').join(
        "function uniqueId_forceInline(id) {return jsinterop.closure.getUniqueId(id);}",
        "function b1() { uniqueId_forceInline('a'); uniqueId_forceInline('b');  } b1();");
    expected = Joiner.on('\n').join(
        "function a(){jsinterop.closure.getUniqueId('a');jsinterop.closure.getUniqueId('b')}",
        "a();");
    verifyOptimizedObfuscated(expected, input);

    // Test DO_NOT_INLINE
    input = Joiner.on('\n').join(
        "function uniqueId_doNotInline(id) {return jsinterop.closure.getUniqueId(id);}",
        "function b1() { uniqueId_doNotInline('a'); uniqueId_doNotInline('b');  } b1();");
    expected = Joiner.on('\n').join(
        "function b(a){return jsinterop.closure.getUniqueId(a)}",
        "function c(){b('a');b('b')}",
        "c();");
    verifyOptimizedObfuscated(expected, input);
  }

  public void testCheckerError() throws Exception {
    String input = Joiner.on('\n').join(
        "function m_forceInline(a,b,c) {a[c]=b;}",
        "function b1(a) { m_forceInline(a++,a++,a++);} b1();");
    assertCheckerError(input,
        "Function m_forceInline is marked as @ForceInline but it could not be inlined");
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
    verifyOptimized(expected, input);
  }

  /**
   * Test that a global array reference breaks argument ordering.
   */
  public void testOrderingArrayGlobal() throws Exception {
    StringBuilder code = new StringBuilder();

    code.append("var array; ");
    code.append("function clinit() { clinit = null; }");

    // callee references array[0] before evaluating argument
    code.append("function callee(arg) { return array[0] + arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { callee((clinit(),2)); }");

    // bootstrap the program
    code.append("caller();");

    verifyNoChange(code.toString());
  }

  /**
   * Test that a local reference does not break argument ordering.
   */
  public void testOrderingArrayLocal() throws Exception {
    StringBuilder code = new StringBuilder();

    code.append("function clinit() { clinit = null; }");

    // callee references array[0] before evaluating argument
    code.append("function callee(arg) { var array; return array[0] + arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { callee((clinit(),2)); }");

    // bootstrap the program
    code.append("caller();");

    StringBuilder expected = new StringBuilder();
    expected.append("function clinit() { clinit = null; }");
    expected.append("function caller() { var array; array[0] + (clinit(), 2); }");
    expected.append("caller();");

    verifyOptimized(expected.toString(), code.toString());
  }

  /**
   * Test that a field reference breaks argument ordering.
   */
  public void testOrderingField() throws Exception {
    StringBuilder code = new StringBuilder();

    code.append("function clinit() {  clinit = null; }");

    // callee references field.x before evaluating argument
    code.append("function callee(arg) { var field; return field.x + arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { callee((clinit(),2)); }");

    // bootstrap the program
    code.append("caller();");

    verifyNoChange(code.toString());
  }

  /**
   * Test that a global variable breaks argument ordering.
   */
  public void testOrderingGlobal() throws Exception {
    StringBuilder code = new StringBuilder();
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

    verifyNoChange(code.toString());
  }

  /**
   * Test that a local variable does not break argument ordering.
   */
  public void testOrderingLocal() throws Exception {
    StringBuilder code = new StringBuilder();

    code.append("function clinit() { clinit = null; }");

    // callee references y before evaluating argument
    code.append("function callee(arg) { var y; y=2; return arg; }");

    // caller invokes callee with a multi that runs clinit()
    code.append("function caller() { return callee((clinit(),3)); }");

    // bootstrap the program
    code.append("caller();");

    StringBuilder expected = new StringBuilder();

    expected.append("function clinit() { clinit = null; }");
    expected.append("function caller() {var y; return y=2,clinit(),3;}");
    expected.append("caller();");
    verifyOptimized(expected.toString(), code.toString());
  }

  /**
   * Test that a new expression breaks argument ordering.
   */
  public void testOrderingNew() throws Exception {
    StringBuilder code = new StringBuilder();
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

    verifyNoChange(code.toString());
  }

  public void testSelfRecursion() throws Exception {
    String input = "function a1() { return blah && b1() }"
        + "function b1() { return bar && a1()}" + "function c() { a1() } c()";

    String expected = "function a1() { return blah && bar && a1() }"
        + "function c() { a1() } c()";

    verifyOptimized(expected, input);
  }

  /*
   * This is inspired by issue 5936:
   * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=5936
   */
  public void testPreserveNameScopeWithDoubleInliningAndObfuscation() throws Exception {
    StringBuilder code = new StringBuilder();

    code.append("function getA(){"
                + "var s;"
                + "s = getB();"
                + "return s;"
                + "}");

    code.append("function getB(){"
                + "var t;"
                + "t = 't';"
                + "t = t + '';"
                + "return t;"
                + "}");

    code.append("function start(y){"
                + "getA();"
                + "if (y != 10) {$wnd.alert('y != 10');}"
                + "}");

    code.append("var x = 10; start(x);");

    StringBuilder expected = new StringBuilder();
    expected.append("function c(a){var b;b='t';if(a!=10){$wnd.alert('y != 10')}}");
    expected.append("var d=10;c(d);");

    verifyOptimizedObfuscated(expected.toString(), code.toString());
  }

  private void verifyNoChange(String input) throws Exception {
    verifyOptimized(input, input);
  }

  private void verifyOptimized(String expected, String input) throws Exception {
    String actual = optimizeToSource(input, JsSymbolResolver.class, FixStaticRefsVisitor.class,
        JsInlinerProxy.class, JsUnusedFunctionRemover.class);
    String expectedAfterParse = optimizeToSource(expected);
    assertEquals(expectedAfterParse, actual);
  }

  private void verifyOptimizedObfuscated(String expected, String input) throws Exception {
    String actual = optimizeToSource(input, JsSymbolResolver.class, FixStaticRefsVisitor.class,
        JsInlinerProxy.class, JsUnusedFunctionRemover.class, JsObfuscateNamer.class);
    String expectedAfterParse = optimizeToSource(expected);
    assertEquals(expectedAfterParse, actual);
  }

  private void assertCheckerError(String input, String error) throws Exception {
    JsProgram optimizedProgram = optimize(input, JsSymbolResolver.class, FixStaticRefsVisitor.class,
        JsInlinerProxy.class, JsUnusedFunctionRemover.class);
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError(error, null);
    UnitTestTreeLogger testLogger = builder.createLogger();
    try {
      JsForceInliningChecker.check(testLogger, JavaToJavaScriptMap.EMPTY, optimizedProgram);
      fail("JsForceInliningChecker should have thrown an exception");
    } catch (UnableToCompleteException expected) {
    }
    testLogger.assertCorrectLogEntries();
  }

  /**
   * A Proxy class to call JsInlner, due to its lack of a single parameter exec method.
   */
  private static class JsInlinerProxy {
    /**
     * Static entry point used by JavaToJavaScriptCompiler.
     */
    public static OptimizerStats exec(JsProgram program) {
      final List<JsNode> inlineableFunctions = Lists.newArrayList();
      new JsVisitor() {
        @Override
        public void endVisit(JsFunction x, JsContext ctx) {
          inlineableFunctions.add(x);
          JsName functionName = x.getName();
          if (functionName == null) {
            return;
          }
          if (functionName.getIdent().endsWith("_forceInline")) {
            x.setInliningMode(InliningMode.FORCE_INLINE);
          } else if (functionName.getIdent().endsWith("_doNotInline")) {
            x.setInliningMode(InliningMode.DO_NOT_INLINE);
          }
        }
      }.accept(program);
      return JsInliner.exec(program, inlineableFunctions);
    }
  }

}
