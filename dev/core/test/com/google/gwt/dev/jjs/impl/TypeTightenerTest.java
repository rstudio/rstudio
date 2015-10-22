/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for {@link TypeTightener}.
 */
public class TypeTightenerTest extends OptimizerTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testTightenToNullType() throws Exception {
    Result result = optimize("void", "String s = null;");
    result.intoString("null s = null;");
  }

  public void testBinaryOperation() throws Exception {
    Result result = optimize("void", "String s = null; String x = s + s;");
    result.intoString("null s = null;", "String x = s + s;");
  }

  public void testBinaryOperation_nullField() throws Exception {
    addSnippetClassDecl(
        "static class A {",
        "  String name;",
        "}");
    Result result = optimize("void", "A a = null; a.name += null;");
    result.intoString("null a = null;", "null.nullField += null;");
  }

  public void testBinaryOperation_notNullable() throws Exception {
    optimize("void", "String other; String s = \"\"; s += null; if (s == null) { other = \"\"; } ")
        .intoString("null other;", "String s = \"\";", "s += null;");
  }

  public void testBinaryOperation_nullableButNotNull() throws Exception {
    optimize("void", "String s = null; s += null;")
        .intoString("String s = null;", "s += null;");
  }

  public void testCastOperation() throws Exception {
    addSnippetClassDecl("static class A { " + "String name; public void set() { name = \"A\";} }");
    addSnippetClassDecl("static class B extends A {" + "public void set() { name = \"B\";} }");
    addSnippetClassDecl("static class C extends A {" + "public void set() { name = \"C\";} }");

    Result result =
        optimize("void", "A b = new B();", "((A)b).set();", "((B)b).set();", "((C)b).set();");
    result.intoString("EntryPoint$B b = new EntryPoint$B();", "b.set();", "b.set();",
        "((null) b).nullMethod();");
  }

  public void testConditional() throws Exception {
    addSnippetClassDecl("static class D {}");
    addSnippetClassDecl("static class C extends D {}");
    addSnippetClassDecl("static class B extends C {}");
    addSnippetClassDecl("static class A extends C {}");
    Result result = optimize("void", "int x = 0;", "A a1 = new A();", "A a2 = new A();",
        "B b = new B();", "C c = new C();", "C c1 = (x > 0) ? a1 : b;", "C c2 = (x > 0) ? a1 : a2;",
        "D d = (x > 0) ? b : c;");
    result.intoString("int x = 0;", "EntryPoint$A a1 = new EntryPoint$A();",
        "EntryPoint$A a2 = new EntryPoint$A();", "EntryPoint$B b = new EntryPoint$B();",
        "EntryPoint$C c = new EntryPoint$C();", "EntryPoint$C c1 = x > 0 ? a1 : b;",
        "EntryPoint$A c2 = x > 0 ? a1 : a2;", "EntryPoint$C d = x > 0 ? b : c;");
  }

  public void testTightenLocalVariable() throws Exception {
    addSnippetClassDecl("static abstract class B {};");
    addSnippetClassDecl("static class C extends B{};");
    addSnippetClassDecl("static class D extends C{};");
    addSnippetClassDecl("static class E extends C{};");
    Result result = optimize("void",
        "B b;",
        "b = new C();", "b = new D();", "b = new E();");
    result.intoString("EntryPoint$C b;", "b = new EntryPoint$C();",
        "b = new EntryPoint$D();", "b = new EntryPoint$E();");
  }

  public void testTightenField() throws Exception {
    addSnippetClassDecl("static class A {};");
    addSnippetClassDecl("static class B extends A{};");
    addSnippetClassDecl("static class C extends B{};");
    addSnippetClassDecl("static class D extends B{};");
    addSnippetClassDecl("static class Test {private A a;"
        + "public void fun1() {a = new C();}"
        + "public void fun2() {a = new D();}"
        + "}");
    Result result = optimize("void", "");
    assertEquals("EntryPoint$B a",
        OptimizerTestBase.findField(result.findClass("EntryPoint$Test"), "a").toString());
  }

  public void testTightenParameterBasedOnAssignment() throws Exception {
    addSnippetClassDecl("static class A { }");
    addSnippetClassDecl("static class B extends A { }");
    addSnippetClassDecl("static class C extends B { }");
    addSnippetClassDecl("static class D extends B { }");
    addSnippetClassDecl("static void test(A a) { }");
    Result result = optimize("void", "C c = new C(); D d = new D(); test(c); test(d);");
    assertParameterTypes(result, "test", "EntryPoint$B");
  }

  public void testTightenParameterBasedOnLeafType() throws Exception {
    addSnippetClassDecl("abstract static class A { void makeSureFunIsCalled() {fun(this);} }");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static void fun(A a) {if (a == null) return;}");

    Result result = optimize("void", "");
    assertParameterTypes(result, "fun", "EntryPoint$B");
  }

  public void testTightenParameterBasedOnOverriddenMethods() throws Exception {
    addSnippetClassDecl("static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static class Test1 { public void fun(A a) {} }");
    addSnippetClassDecl("static class Test2 extends Test1 {public void fun (A a) {} }");
    Result result = optimize("void", "B b = new B();"
        + "Test1 test1 = new Test1();"
        + "test1.fun(b);");
    assertParameterTypes(result, "EntryPoint$Test1.fun(Ltest/EntryPoint$A;)V", "EntryPoint$B");
    assertParameterTypes(result, "EntryPoint$Test2.fun(Ltest/EntryPoint$A;)V", "EntryPoint$B");
  }

  public void testTightenParameterWithAccidentalOverrides() throws Exception {
    // Test the assumption that accidental overrides can be safely deleted if the super method
    // is still in place. That requires that the super method parameters not to be (over) tightened.
    addSnippetClassDecl("interface I { boolean m1(Object obj); boolean m2(Object obj);}");
    addSnippetClassDecl("static class A { public boolean m1(Object obj) { return obj == null; } }");
    addSnippetClassDecl(
        "static class B extends A {",
        "  public boolean m2(Object obj) { return obj == null; }",
        "}");
    addSnippetClassDecl("static class C extends B implements I {}");
    Result result = optimize("void",
        "B b = new B();",
        "b.m1(null);",
        "b.m2(b);",
        "C c = new C();",
        "c.m1(c);",
        "c.m2(c);");
    assertParameterTypes(result, "EntryPoint$A.m1(Ljava/lang/Object;)Z", "Object");
    // This one could be tighetened safely to EntryPoint$B but typetighener does not
    // tighten parameters in polymorphic method calls.
    assertParameterTypes(result, "EntryPoint$B.m2(Ljava/lang/Object;)Z", "Object");
  }

  public void testMethodBasedOnLeafType() throws Exception {
    addSnippetClassDecl("abstract static class A { void makeSureFunIsCalled() {fun(this);} }");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static A fun(A a) {return a;}");

    Result result = optimize("void", "");
    assertParameterTypes(result, "fun", "EntryPoint$B");
    assertReturnType(result, "fun", "EntryPoint$B");
  }

  public void testMethodBasedOnReturns() throws Exception {
    addSnippetClassDecl("static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static class C extends B {}");
    addSnippetClassDecl("static class D extends B {}");
    addSnippetClassDecl(
        "A fun(int a) { if(a<0) return new B(); else if(a==0) return new C(); return new D();}");
    Result result = optimize("void", "");
    assertReturnType(result, "fun", "EntryPoint$B");
  }

  public void testMethodBasedOnOverriders() throws Exception {
    addSnippetClassDecl("abstract static class A { abstract public A fun(); }");
    addSnippetClassDecl("static class B extends A { public B fun() {return new B();} }");
    addSnippetClassDecl("static class C extends A { public B fun() {return new B();} }");
    addSnippetClassDecl("static class D extends B { public D fun() {return new D();} }");
    Result result = optimize("void", "");
    assertReturnType(result, "EntryPoint$A.fun()Ltest/EntryPoint$A;", "EntryPoint$B");;
  }

  public void testTightenDependsOnTightenedMethods() throws Exception {
    addSnippetClassDecl("abstract static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static A test() { return new B(); }");
    // test should be tightened to be static EntryPoint$B test();

    addSnippetClassDecl("static class C {A a;}");
    addSnippetClassDecl("static void fun1(A a) {if (a == null) return;}");
    addSnippetClassDecl("static A fun2() { return test(); }"); // tighten method return type
    Result result = optimize("void",
        "A a = test();" // tighten local varialbe.
        + "C c = new C(); c.a = test();" // tighten field
        + "fun1(test());" // tighten parameter
        );
    assertReturnType(result, "fun2()Ltest/EntryPoint$A;", "EntryPoint$B");
    assertParameterTypes(result, "fun1", "EntryPoint$B");;
    assertEquals("EntryPoint$B a",
        OptimizerTestBase.findLocal(result.findMethod(MAIN_METHOD_NAME), "a").toString());
    assertEquals("EntryPoint$B a",
        OptimizerTestBase.findField(result.findClass("EntryPoint$C"), "a").toString());
  }

  public void testDoNotTighten_staticDispatch() throws Exception {
    addSnippetClassDecl("abstract static class A { public void m() {} }");
    addSnippetClassDecl("static class B extends A { public void m() { super.m(); }}");

    Result result = optimize("void",
        "new B().m();");

    assertForwardsTo(result.findMethod("test.EntryPoint$B.m()V"),
        result.findMethod("test.EntryPoint$A.m()V"));
  }

  public void testTightenInstanceOf_singleConcrete() throws Exception {
    addSnippetClassDecl("abstract static class A { public void m() {} }");
    addSnippetClassDecl("static class B extends A { public void m() { super.m(); }}");

    optimize("void"," A a= new B(); if (a instanceof B) { a.m(); }")
        .intoString("EntryPoint$B a = new EntryPoint$B();", "a.m();");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = false;
    while (TypeTightener.exec(program).didChange()) {
      MethodCallTightener.exec(program);
      DeadCodeElimination.exec(program);
      didChange = true;
    }
    return didChange;
  }
}
