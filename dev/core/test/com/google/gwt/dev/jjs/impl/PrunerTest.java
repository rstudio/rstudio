/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.util.arg.SourceLevel;

/**
 * Test for {@link Pruner}.
 */
public class PrunerTest extends OptimizerTestBase {
  @Override
  protected void setUp() throws Exception {
    sourceLevel = SourceLevel.JAVA8;
    super.setUp();
    runDeadCodeElimination = true;
  }

  public void testSmoke() throws Exception {
    addSnippetClassDecl("static int foo(int i) { return i; }");

    addSnippetClassDecl("static void unusedMethod() { }");
    addSnippetClassDecl("static void usedMethod() { }");
    addSnippetClassDecl("static class UnusedClass { }");
    addSnippetClassDecl("static class UninstantiatedClass { "
        + "int field; native int method() /*-{ return 1; }-*/; }");
    addSnippetClassDecl("static UninstantiatedClass uninstantiatedField;");
    addSnippetClassDecl("static int unusedField;");
    addSnippetClassDecl("static int unreadField;");
    addSnippetClassDecl("static int unassignedField;");
    addSnippetClassDecl("static UninstantiatedClass returnUninstantiatedClass() { return null; }");
    addSnippetClassDecl(
        "interface UsedInterface {",
        "  int unusedConstant = 2;",
        "  int usedConstant = 3;",
        "  void method2();",
        "}");
    addSnippetClassDecl("static class UsedClass implements UsedInterface {",
        "  int field2;",
        "  public void method2() { field2 = usedConstant; }",
        "  UsedClass(UninstantiatedClass c) { }",
        "  UsedClass(UninstantiatedClass c1, UninstantiatedClass c2) { }",
        "  UsedClass(UninstantiatedClass c1, int i, UninstantiatedClass c2) { field2 = i; }",
        "  UsedClass(UninstantiatedClass c1, int i, UninstantiatedClass c2, int j) " +
            "{ field2 = i + j; }",
        "}");
    addSnippetClassDecl(
        "static native void usedNativeMethod(UninstantiatedClass c, UsedClass c2)",
        "/*-{",
        "  c.@test.EntryPoint.UninstantiatedClass::field = 2;",
        "  c.@test.EntryPoint.UninstantiatedClass::method();",
        "  c2.@test.EntryPoint.UsedClass::field2++;",
        "  c2.@test.EntryPoint.UsedClass::method2();",
        "}-*/;");
    addSnippetClassDecl(
        "static native void unusedNativeMethod()",
        "/*-{",
        "}-*/;");
    addSnippetClassDecl("static void methodWithUninstantiatedParam(UninstantiatedClass c) { }");
    addSnippetClassDecl("interface UnusedInterface { void foo(); }");
    addSnippetClassDecl("interface Callback { void go(); }");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsConstructor");
    addSnippetClassDecl("@JsType interface Js { void doIt(Callback cb); }");
    addSnippetClassDecl("@JsType(isNative=true) static class JsProto { ",
        "public JsProto(int arg) {}",
        "}");
    addSnippetClassDecl("static class JsProtoImpl extends JsProto {",
        "public JsProtoImpl() { super(10); }",
        "}");

    addSnippetClassDecl("static class JsProtoImpl2 extends JsProto {",
        "@JsConstructor public JsProtoImpl2() { super(10); }",
        "}");

    addSnippetClassDecl("static class JsProtoImpl3 extends JsProto {",
        "public JsProtoImpl3() { super(10); }",
        "}");

    Result result;
    (result = optimize("void",
        "usedMethod();",
        "unreadField = 1;",  // should be pruned because it's not read.
        "foo(unassignedField);",
        "returnUninstantiatedClass();",
        "usedNativeMethod(null, null);",
        "foo(uninstantiatedField.field);",
        "uninstantiatedField.method();",
        "methodWithUninstantiatedParam(null);",
        "new UsedClass(null);",
        "new UsedClass(returnUninstantiatedClass(), returnUninstantiatedClass());",
        "new UsedClass(returnUninstantiatedClass(), 3, returnUninstantiatedClass());",
        "new UsedClass(returnUninstantiatedClass(), 3, returnUninstantiatedClass(), 4);",
        "UninstantiatedClass localUninstantiated = null;",
        "JsProtoImpl jsp = new JsProtoImpl();"
        )).intoString(
            "EntryPoint.usedMethod();",
            "EntryPoint.foo(EntryPoint.unassignedField);",
            "EntryPoint.returnUninstantiatedClass();",
            "EntryPoint.usedNativeMethod(null, null);",
            "EntryPoint.foo(null.nullField);",
            "null.nullMethod();",
            "EntryPoint.methodWithUninstantiatedParam();",
            "new EntryPoint$UsedClass();",
            "EntryPoint.returnUninstantiatedClass();",
            "EntryPoint.returnUninstantiatedClass();",
            "new EntryPoint$UsedClass();",
            "int lastArg;",
            "new EntryPoint$UsedClass((lastArg = (EntryPoint.returnUninstantiatedClass(), 3), EntryPoint.returnUninstantiatedClass(), lastArg));",
            "new EntryPoint$UsedClass((EntryPoint.returnUninstantiatedClass(), 3), (EntryPoint.returnUninstantiatedClass(), 4));",
            "new EntryPoint$JsProtoImpl();"
            );

    assertNotNull(result.findMethod("usedMethod"));
    // We do not assign to the field, but we use its default value.
    // Shouldn't be pruned.
    assertNotNull(result.findField("unassignedField"));
    assertNotNull(result.findMethod("usedNativeMethod"));
    assertNotNull(result.findMethod("returnUninstantiatedClass"));
    assertNotNull(result.findMethod("methodWithUninstantiatedParam"));
    assertNotNull(result.findClass("EntryPoint$UsedClass"));
    assertNotNull(result.findClass("EntryPoint$UsedInterface"));

    assertNull(result.findMethod("unusedMethod"));
    assertNull(result.findField("unusedField"));
    assertNull(result.findField("unreadField"));
    assertNull(result.findClass("EntryPoint$UnusedClass"));
    assertNull(result.findMethod("unusedNativeMethod"));
    assertNull(result.findField("uninstantiatedField"));
    assertNull(result.findClass("EntryPoint$UnusedInterface"));

    // Class is never instantiated. Should be pruned.
    assertNull(result.findClass("UninstantiatedClass"));

    assertEquals(
        "static null returnUninstantiatedClass(){\n" +
        "  return null;\n" +
        "}",
        result.findMethod("returnUninstantiatedClass").toSource());

    assertEquals(
        "static void methodWithUninstantiatedParam(){\n" +
        "}",
        result.findMethod("methodWithUninstantiatedParam").toSource());

    assertEquals(
        "[final null nullField, int field2]",
        ((JsniMethodBody) result.findMethod("usedNativeMethod").getBody())
            .getJsniFieldRefs().toString());
    assertEquals(
        "[public final null nullMethod(), public void method2()]",
        ((JsniMethodBody) result.findMethod("usedNativeMethod").getBody())
            .getJsniMethodRefs().toString());

    assertEquals(
        "interface EntryPoint$UsedInterface {\n" +
        "  final static int usedConstant\n\n" +
        "  private static final void $clinit(){\n" +
        "    final static int usedConstant = 3;\n" +
        "  }\n" +
        "\n" +
        "}",
        result.findClass("EntryPoint$UsedInterface").toSource());

    // Neither super ctor call, nor super call's param is pruned
    assertEquals(
        "public EntryPoint$JsProtoImpl(){\n" +
            "  this.EntryPoint$JsProto.EntryPoint$JsProto(10);\n" +
            "  this.$init();\n" +
            "}",
        findMethod(result.findClass("EntryPoint$JsProtoImpl"), "EntryPoint$JsProtoImpl").toSource());

    // Not JsType'd, and not instantiated, so should be pruned
    assertNull(result.findClass("EntryPoint$JsProtoImpl3"));

    // Should be rescued because of @JsType
    assertNotNull(result.findClass("EntryPoint$JsProtoImpl2"));
  }

  public void testCleanupVariableOfNonReferencedType() throws Exception {
    runDeadCodeElimination = false;
    addSnippetClassDecl("static class A {}");
    addSnippetClassDecl("static boolean fun(A a) { return a == null; }");
    Result result = optimize("void", "fun(null);");
    assertNull(result.findClass("EntryPoint$A"));
    assertEquals("test.EntryPoint.fun(Ltest/EntryPoint$A;)Z", result.findMethod("fun").toString());
    assertTrue(result.findMethod("fun").getParams().get(0).getType().isNullType());
  }

  /**
   * Test for issue 2478.
   */
  public void testPrunerThenEqualityNormalizer() throws Exception {
    runDeadCodeElimination = false;
    addSnippetClassDecl("static int foo(int i) { return i; }");

    addSnippetClassDecl("static class UninstantiatedClass { "
        + "int field[]; native int method() /*-{ return 1; }-*/; }");
    addSnippetClassDecl("static UninstantiatedClass uninstantiatedField;");
    Result result;
    (result = optimize("int",
        "int i = 0;",
        "if (uninstantiatedField.field[i] == 0) { i = 2; }",
        "return i;"
    )).intoString(
        "int i = 0;",
        "if (null.nullField[i] == 0) {",
        "  i = 2;",
        "}",
        "return i;"
    );

    EqualityNormalizer.exec(result.getOptimizedProgram());
  }

  public void testInterface() throws Exception {
    runDeadCodeElimination = false;
    addSnippetClassDecl("interface I { int bar(); }");
    addSnippetClassDecl("static class A implements I { public int bar() { return 1; } }");
    Result result =
        optimize("void", "A a = new A(); a.bar();");
    assertNotNull(result.findClass("EntryPoint$I"));
    assertNotNull(result.findClass("EntryPoint$A"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$A"), "bar"));
    assertNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I"), "bar"));
  }

  public void testDefaultInterface() throws Exception {
    runDeadCodeElimination = false;
    addSnippetClassDecl("interface I {default int bar() {return 1;} "
        + "default int foo() {return 1;} }");
    addSnippetClassDecl("static class A implements I { }");
    addSnippetClassDecl("interface I2 {default int foo() {return fun();}"
        + "default int fun() {return 1;}"
        + "default int goo() {return 0;}"
        + "static int s1() { return s2();}"
        + "static int s2() { return 0;}"
        + "static int s3() { return 1;}"
        + "}");
    Result result = optimize("void", "A a = new A(); a.bar(); "
        + "I2 i = new I2() {}; i.foo(); I2.s1();");
    assertNotNull(result.findClass("EntryPoint$A"));
    assertNotNull(result.findClass("EntryPoint$I"));
    assertNotNull(result.findClass("EntryPoint$I2"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I"), "bar"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$A"), "bar"));
    assertNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I"), "foo"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I2"), "foo"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I2"), "fun"));
    assertNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I2"), "goo"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I2"), "s1"));
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I2"), "s2"));
    assertNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$I2"), "s3"));
  }

  public void testPruneParamsAndLocalsInMethodBody() throws Exception {
    runDeadCodeElimination = false;
    addSnippetClassDecl("interface I {default int bar() {int a = 0; return 1;} "
        + "static int fun(int b) {int a = 0; return 1;} }");
    Result result = optimize("void", "I i = new I() {}; i.bar(); I.fun(0);");
    assertNotNull(result.findClass("EntryPoint$I"));
    JMethod bar = OptimizerTestBase.findMethod(result.findClass("EntryPoint$I"), "bar");
    assertNotNull(bar);
    assertEquals("public int bar(){\n" +
        "  0;\n" +
        "  return 1;\n" +
        "}", bar.toSource());
    JMethod fun = OptimizerTestBase.findMethod(result.findClass("EntryPoint$I"), "fun");
    assertNotNull(fun);
    assertEquals("public static int fun(){\n" +
        "  0;\n" +
        "  return 1;\n" +
        "}", fun.toSource());
  }

  public void testJsFunction_notPruneUnusedJsFunction() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction interface MyJsFunctionInterface {",
        "int foo (int a);",
        "}");
    addSnippetClassDecl(
        "interface MyPlainInterface {",
        "int foo (int a);",
        "}");
    addSnippetClassDecl(
        "@JsFunction interface MyJsFunctionInterfaceUnused {",
        "int foo (int a);",
        "}");
    Result result;
    (result = optimize("void",
        "MyJsFunctionInterface a = new MyJsFunctionInterface() {"
            + "@Override public int foo (int a) { return 1; }"
            + "};"
            + "MyPlainInterface b = new MyPlainInterface() {"
            + "@Override public int foo (int a) { return 1; }"
            + "};"
        )).intoString(
            "new EntryPoint$1();\n" +
            "new EntryPoint$2();"
            );

    assertNotNull(result.findClass("EntryPoint$MyJsFunctionInterface"));
    assertNotNull(result.findClass("EntryPoint$MyPlainInterface"));
    assertNotNull(result.findClass("EntryPoint$MyJsFunctionInterfaceUnused"));

    // Function in JsFunction interface may be implicitly called in JS, should not be pruned.
    assertNotNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$1"), "foo"));
    assertNull(OptimizerTestBase.findMethod(result.findClass("EntryPoint$2"), "foo"));
  }

  public void testJsFunction_unrelatedjsFunctionCast() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction interface MyJsFunctionInterface {",
        "int foo (int a);",
        "}");
    addSnippetClassDecl(
        "@JsFunction interface MyOtherJsFunctionInterface {",
        "int bar (int a);",
        "}");
    addSnippetClassDecl(
        "interface MyPlainInterface {",
        "int goo (int a);",
        "}");
    Result result;
    (result = optimize("int",
        "MyJsFunctionInterface a = new MyJsFunctionInterface() {"
            + "@Override public int foo (int a) { return 1; }"
            + "};"
            + "return ((MyOtherJsFunctionInterface) a).bar(0) + ((MyPlainInterface) a).goo(0);"
        )).intoString(
            "EntryPoint$MyJsFunctionInterface a = new EntryPoint$1();\n" +
            "return ((EntryPoint$MyOtherJsFunctionInterface) a).bar(0) +" // SAM is not pruned.
            + " ((EntryPoint$MyPlainInterface) a).nullMethod();"
            );

    // JsFunction can be cross casted, or casted from JavaScript function
    // so the JsFunction interface and its SAM function should not be pruned.
    assertNotNull(result.findClass("EntryPoint$MyOtherJsFunctionInterface"));
    assertNotNull(OptimizerTestBase.findMethod(
        result.findClass("EntryPoint$MyOtherJsFunctionInterface"), "bar"));
    assertNull(result.findClass("EntryPoint$MyPlainInterface"));
 }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = false;
    // TODO(jbrosenberg): remove loop when Pruner/CFA interaction is perfect.
    while (Pruner.exec(program, true).didChange()) {
      didChange = true;
    }
    return didChange;
  }
}
