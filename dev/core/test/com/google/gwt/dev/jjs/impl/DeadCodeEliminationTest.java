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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests {@link DeadCodeElimination}.
 */
public class DeadCodeEliminationTest extends OptimizerTestBase {
  /*
   * TODO: this class needs more tests, and more sophisticated cases. Especially
   * to ensure we converge in a single pass.
   */

  @Override
  public void setUp() throws Exception {
    addSnippetClassDecl("static volatile boolean b;");
    addSnippetClassDecl("static volatile boolean b1;");
    addSnippetClassDecl("static volatile int i;");
    addSnippetClassDecl("static volatile long l;");
    addSnippetClassDecl("static volatile float f;");
    addSnippetClassDecl("static volatile double d;");
    addSnippetClassDecl("static volatile String s;");
    addSnippetClassDecl("static volatile Object o;");

    runMethodInliner = false;
    runSpecializer = false;
  }

  public void testConditionalOptimizations() throws Exception {
    optimize("int", "return true ? 3 : 4;").into("return 3;");
    optimize("int", "return false ? 3 : 4;").into("return 4;");

    optimize("boolean", "return b ? true : b1;").into("return b || b1;");
    optimize("boolean", "return b ? false : b1;").into("return !b && b1;");
    optimize("boolean", "return b ? b1 : true;").into("return !b || b1;");
    optimize("boolean", "return b ? b1 : false;").into("return b && b1;");
  }

  public void testConditionalOptimizations_exactType() throws Exception {
    addSnippetClassDecl("static class A {};");
    addSnippetClassDecl("static class B extends A {};");
    optimize("int", "return new A() == new B() ? 3 : 4;")
        .intoString("return (new EntryPoint$A(), new EntryPoint$B(), 4);");
    optimize("int", "return null == new B() ? 3 : 4;")
        .intoString("return (new EntryPoint$B(), 4);");
  }

  public void testInstanceOf_exactNonNullTypes() throws Exception {
    addSnippetClassDecl("static class A {};");
    addSnippetClassDecl("static class B extends A {};");
    Result result = optimize("void",
        "boolean test;",
        "test = new A() instanceof A;",
        "test = new B() instanceof B;",
        "test = new A() instanceof B;",
        "test = new B() instanceof A;");
    result.intoString(
        "boolean test;",
        "test = (new EntryPoint$A(), true);",
        "test = (new EntryPoint$B(), true);",
        "test = (new EntryPoint$A(), false);",
        "test = (new EntryPoint$B(), true);");
  }

  public void testInstanceOf_nullability() throws Exception {
    addSnippetClassDecl("static class A {};");
    addSnippetClassDecl("static class B extends A {};");
    addSnippetClassDecl("static class C {};");
    Result result = optimize("void",
        "A a = new A();",
        "boolean test;",
        "test = null instanceof B;",
        "test = a instanceof A;");
    result.intoString(
        "EntryPoint$A a = new EntryPoint$A();",
        "boolean test;",
        "test = false;",
        "test = a != null;");
  }

  public void testSwitchOverConstant_noMatchingCase() throws Exception {
    optimize("int", "switch (0) { case 1: return 1; } return 0;")
        .into("return 0;");
  }

  public void testSwitchOverConstant_MatchingCase() throws Exception {
    optimize("int",
        "switch (1) { case 1: return 1; } return 0;")
        .into("return 1;");

    // The if isn't really the focus of the optimization, but the `into`
    // string will not compile if it is invalid Java.  It makes the opt form
    // valid java.
    optimize("int",
        "int j = 1;",
        "if (b) {",
        "  switch (1) {",
        "    case 0:",
        "    case 1:",
        "    case 2:",
        "      j = 5;",
        "    case 3:",
        "      return 1;",
        "    default:",
        "      return j;",
        "  }",
        "}",
        "return -1;")
        .into(
            "int j = 1;",
            "if (b) {",
            "  switch (1) {",
            "    case 1:",  // All of the other cases and the default are gone
            "      j = 5;", // this is a dead-store but is currently retained
            "      return 1;",
            "  }",
            "}",
            "return -1;");
  }

  public void testSwitchOverConstant_NonConstant() throws Exception {
    // doesn't optimize when there is a non-constant switch expr
    // (though, in this case, it's easy to imagine that it could)
    String[] nonConstantSwitch = new String[] {
      "int j = 1;",
      "switch (j) {",
      "  case 0: ",
      "  case 1: ",
      "  case 2: ",
      "    j = 5; ",
      "  case 3: ",
      "    return 1; ",
      "  default: ",
      "    return j; ",
      "}"
    };
    optimize("int", nonConstantSwitch).into(nonConstantSwitch);
  }

  public void testIfOptimizations() throws Exception {
    optimize("int", "if (true) return 1; return 0;").into("return 1;");
    optimize("int", "if (false) return 1; return 0;").into("return 0;");
    optimize("int", "if (true) return 1; else return 2;").into("return 1;");
    optimize("int", "if (false) return 1; else return 2;").into("return 2;");

    optimize("int", "if (true) {} else return 4; return 0;").into("return 0;");

    addSnippetClassDecl("static boolean test() { return b; }");
    optimize("int", "if (test()) {} else {}; return 0;").into(
        "test(); return 0;");
  }

  public void testIfStatementToBoolean_NotOptimization() throws Exception {
    optimize("void", "if (!b) i = 1;").intoString(
        "EntryPoint.b || (EntryPoint.i = 1);");
    optimize("void", "if (!b) i = 1; else i = 2;").intoString(
        "EntryPoint.b ? (EntryPoint.i = 2) : (EntryPoint.i = 1);");
    optimize("int", "if (!b) { return 1;} else {return 2;}").into(
        "return b ? 2 : 1;");
  }

  public void testIfStatementToBoolean_ReturnLifting() throws Exception {
    optimize("int", "if (b) return 1; return 2;").into(
        "if (b) return 1; return 2;");
    optimize("int", "if (b) { return 1; }  return 2;").into(
        "if (b) { return 1; } return 2;");
    optimize("int", "if (b) { return 1;} else {return 2;}").into(
        "return b ? 1 : 2;");
    optimize("int", "if (b) return 1; else {return 2;}").into(
        "return b ? 1 : 2;");
    optimize("int", "if (b) return 1; else return 2;").into("return b ? 1 : 2;");
    optimize("void", "if (b) return; else return;").into(
        "if (b) return; else return;");
  }

  public void testIfStatementToBoolean_ThenElseOptimization() throws Exception {
    optimize("void", "if (b) i = 1; else i = 2;").intoString(
        "EntryPoint.b ? (EntryPoint.i = 1) : (EntryPoint.i = 2);");
    optimize("void", "if (b) {i = 1;} else {i = 2;}").intoString(
        "EntryPoint.b ? (EntryPoint.i = 1) : (EntryPoint.i = 2);");
  }

  public void testIfStatementToBoolean_ThenOptimization() throws Exception {
    optimize("void", "if (b) i = 1;").intoString(
        "EntryPoint.b && (EntryPoint.i = 1);");
    optimize("void", "if (b) {i = 1;}").intoString(
        "EntryPoint.b && (EntryPoint.i = 1);");
  }

  /**
   * BUG: JInstance was marked as not having side effects whereas it all depends on the
   * whether the expression on the left has side effects.
   *
   * Reproduces Issue:7818.
   */
  public void testInstanceOfOptimization() throws Exception {
    runMethodInliner = true;
    addSnippetClassDecl(
        "static class A  { "
            + "static int f1;"
            + "static A createA() { A.f1 = 1; return new A(); } "
            + "static boolean instanceofMulti() { return (createA() instanceof A); } "
            + "static boolean inlineable() { instanceofMulti(); return true;}"
            + "}");

    optimizeExpressions(false, "void", "A.inlineable()")
        .into("A.f1 = 1; new A();");
  }

  public void testCommuteMultiExpression() throws Exception {
    runMethodInliner = true;
    addSnippetClassDecl(
        "static class A  { "
            + "static int f1;"
            + "static A createA() { A.f1 = 1; return new A(); } "
            + "static boolean booleanWithSideEffects() { createA(); return true;}"
            + "static boolean booleanWithoutSideEffects() { return true;}"
            + "static int arithmeticWithSideEffects() { createA(); return 4;}"
            + "}");

    optimizeExpressions(false, "boolean", "true && A.booleanWithoutSideEffects()")
        .intoString("return true;");

    optimizeExpressions(false, "boolean", "true && A.booleanWithSideEffects()")
        .intoString("return (EntryPoint$A.f1 = 1, new EntryPoint$A(), true);");

    optimizeExpressions(false, "boolean", "false && A.booleanWithSideEffects()")
        .intoString("return false;");

    optimizeExpressions(false, "int", "3 + A.arithmeticWithSideEffects()")
        .intoString("return (EntryPoint$A.f1 = 1, new EntryPoint$A(), 7);");
  }

  public void testStringOptimizations() throws Exception {
    runMethodInliner = true;
    addSnippetClassDecl(
        "static class A  { ",
        "  final static String s1 = \"a\";",
        "  final static String s2 = \"a\";",
        "  final static String s3 = \"b\";",
        "  final static String s4 = null;",
        "}");

    // TODO(rluble): This test is not 100% meaninful as the JDT performs some optimizations for us.
    optimizeExpressions(false, "boolean", "\"a\".equals(\"a\")")
        .into("return true;");
    optimizeExpressions(false, "boolean", "\"a\" == \"a\"")
        .into("return true;");
    optimizeExpressions(false, "boolean", "\"a\" != \"b\"")
        .into("return true;");
    optimizeExpressions(false, "boolean", "A.s1.equals(A.s1)")
        .into("return true;");
    optimizeExpressions(false, "boolean", "A.s1 == A.s1")
        .into("return true;");
    optimizeExpressions(false, "boolean", "A.s1 == \"a\"")
        .into("return true;");
    optimizeExpressions(false, "boolean", "\"a\" != null")
        .into("return true;");
    optimizeExpressions(false, "boolean", "\"a\".equals(A.s2)")
        .into("return true;");
    optimizeExpressions(false, "boolean", "!\"a\".equals(A.s3)")
        .into("return true;");
    optimizeExpressions(false, "boolean", "\"a\" == A.s2")
        .into("return true;");
    optimizeExpressions(false, "boolean", "\"a\" != A.s3")
        .into("return true;");
    optimizeExpressions(false, "boolean", "A.s1.equals(A.s2)")
        .into("return true;");
    // Next two are not directly optimizable because of inserted clinits.
    // optimizeExpressions(false, "boolean", "\"a\" != A.s4")
    //     .into("return true;");
    // optimizeExpressions(false, "boolean", "A.s4 == null")
    //    .into("return true;");
  }

  public void testStringOptimizations_withSpecializer() throws Exception {
    runSpecializer = true;
    testStringOptimizations();
  }

  public void testDoOptimization() throws Exception {
    optimize("void", "do {} while (b);").intoString(
        "do;",
        "while (EntryPoint.b);");
    optimize("void", "do {} while (true);").intoString(
        "do;",
        "while (true);");
    optimize("void", "do {} while (false);").intoString("");
    optimize("void", "do { i++; } while (false);").intoString("++EntryPoint.i;");
    optimize("void", "do { break; } while (false);").intoString(
        "do {",
        "  break;",
        "} while (false);");
  }

  public void testMultiExpressionOptimization() throws Exception {
    runMethodInliner = true;
    addSnippetClassDecl(
        "static class A  { ",
        "  static int f;",
        "  static { if (4-f ==0) f=4; }",
        "  static boolean t() { return true; }",
        "  static boolean f() { return false; }",
        "  static boolean notInlineable() { if (4-f == 0) return true;return false;}",
        "}");

    addSnippetClassDecl(
        "static class B  { ",
        "  static boolean inlineableOr() { return A.t() || A.notInlineable(); }",
        "  static boolean inlineableAnd() { return A.t() && A.notInlineable(); }",
        "}");

    optimize("void", "B.inlineableAnd();")
        .intoString("EntryPoint$A.$clinit();\nEntryPoint$A.notInlineable();");
    optimize("void", "B.inlineableOr();")
        .intoString("EntryPoint$A.$clinit();");
  }

  public void testOptimizeStringCalls() throws Exception {
    // Note: we're limited here by the methods declared in the mock String in
    // JJSTestBase#addBuiltinClasses

    // String.length
    optimize("int", "return \"abc\".length();").intoString("return 3;");
    optimize("int", "return s.length();").intoString("return EntryPoint.s.length();");

    // String.charAt
    optimize("char", "return \"abc\".charAt(1);").intoString("return 'b';");
    optimize("char", "return s.charAt(1);").intoString("return EntryPoint.s.charAt(1);");

    // String.toString
    optimize("String", "return \"a\".toString();").intoString("return \"a\";");
    optimize("String", "return s.toString();").intoString("return EntryPoint.s.toString();");
    optimize("String", "return o.toString();").intoString("return EntryPoint.o.toString();");

    // String.hashCode: never optimized
    optimize("int", "return \"abc\".hashCode();").intoString("return \"abc\".hashCode();");
    optimize("int", "return s.hashCode();").intoString("return EntryPoint.s.hashCode();");

    // String.equals
    optimize("boolean", "return \"a\".equals(\"a\");").intoString("return true;");
    optimize("boolean", "return \"a\".equals(\"b\");").intoString("return false;");
    optimize("boolean", "return s.equals(\"a\");")
        .intoString("return EntryPoint.s.equals(\"a\");");

    // String concat
    optimize("String", "return \"a\" + \"a\";").intoString("return \"aa\";");
    optimize("String", "return \"a\" + 1;").intoString("return \"a1\";");
    optimize("String", "return \"a\" + '1';").intoString("return \"a1\";");
    optimize("String", "return \"a\" +  1L;").intoString("return \"a1\";");
  }

  public void testSubtractFromZero() throws Exception {
    optimize("int", "return 0 - i;").intoString("return -EntryPoint.i;");
    optimize("long", "return 0 - l;").intoString("return -EntryPoint.l;");
    // Verify that float/double subtracts from zero aren't replaced, since they
    // are needed for obscure IEEE754 functionality -- specifically, converting
    // 0.0 - v into -v means the sign of the result is the opposite of the input
    // rather than always being positive.
    optimize("float", "return 0.0F - f;").intoString("return 0.0 - EntryPoint.f;");
    optimize("double", "return 0.0 - d;").intoString("return 0.0 - EntryPoint.d;");
  }

  public void testFloatingPoint() throws Exception {
    // Internally we represent float literals as double, so here we make sure that 1.1f is
    // is printed as a double with the right precision.
    optimize("float", "return 1.1f;").intoString("return " + String.format("%.16g", (double) 1.1f) +
        ";");
  }

  public void testMultiExpression_RedundantClinitRemoval() throws Exception {
    addSnippetClassDecl(
        "static class A  { "
            + "static int f1;"
            + "static int f2;"
            + "static { f1 = 1; }"
            + "static void m1() { } "
            + "}" +
        "static class B extends A  { "
            + "static int f3;"
            + "static int f4;"
            + "static { f3 = 1; }"
            + "static void m2() { } "
            + "}");

    optimizeExpressions(true, "void", "A.m1()", "A.m1()").intoString("EntryPoint$A.$clinit();\n"
        + "EntryPoint$A.m1();\n"
        + "EntryPoint$A.m1();");
    optimizeExpressions(true, "void", "B.m2()", "A.m1()").intoString("EntryPoint$B.$clinit();\n"
        + "EntryPoint$B.m2();\n"
        + "EntryPoint$A.m1();");
    optimizeExpressions(true, "void", "A.m1()", "B.m2()").intoString("EntryPoint$A.$clinit();\n"
        + "EntryPoint$A.m1();\n"
        + "EntryPoint$B.$clinit();\n"
        + "EntryPoint$B.m2();");
  }

  private boolean runMethodInliner;
  private boolean runSpecializer;

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    if (runMethodInliner) {
      MethodInliner.exec(program);
    }
    if (runSpecializer) {
      Finalizer.exec(program); // required so that method is marked final
      MakeCallsStatic.exec(program, false); // required so that method is static
      TypeTightener.exec(program); // required so that the parameter types are tightened
      MethodCallSpecializer.exec(program);
    }

    OptimizerStats result = DeadCodeElimination.exec(program, method);
    if (result.didChange()) {
      // Make sure we converge in one pass.
      //
      // TODO(rluble): It does not appear to be true in general unless we iterate until a
      // fixpoint in exec().
      //
      // Example:
      //
      //     Constructor( ) { deadcode }
      //     m( new Constructor(); }
      //
      // If m is processed first, it will see the constructor as having side effects.
      // Then the constructor will become empty enabling m() become empty in the next round.
      //
      assertFalse(DeadCodeElimination.exec(program, method).didChange());
    }
    return result.didChange();
  }
}
