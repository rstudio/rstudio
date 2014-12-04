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

  public void testCastOperation() throws Exception {
    addSnippetClassDecl("static class A { " + "String name; public void set() { name = \"A\";} }");
    addSnippetClassDecl("static class B extends A {" + "public void set() { name = \"B\";} }");
    addSnippetClassDecl("static class C extends A {" + "public void set() { name = \"C\";} }");

    Result result =
        optimize("void", "A b = new B();", "((A)b).set();", "((B)b).set();", "((C)b).set();");
    result.intoString("EntryPoint$B b = new EntryPoint$B();\n" + "b.set();\n" + "b.set();\n"
        + "((null) b).nullMethod();");
  }

  public void testConditional() throws Exception {
    addSnippetClassDecl("static class D {}");
    addSnippetClassDecl("static class C extends D {}");
    addSnippetClassDecl("static class B extends C {}");
    addSnippetClassDecl("static class A extends C {}");
    Result result = optimize("void", "int x = 0;", "A a1 = new A();", "A a2 = new A();",
        "B b = new B();", "C c = new C();", "C c1 = (x > 0) ? a1 : b;", "C c2 = (x > 0) ? a1 : a2;",
        "D d = (x > 0) ? b : c;");
    result.intoString("int x = 0;\n" + "EntryPoint$A a1 = new EntryPoint$A();\n"
        + "EntryPoint$A a2 = new EntryPoint$A();\n" + "EntryPoint$B b = new EntryPoint$B();\n"
        + "EntryPoint$C c = new EntryPoint$C();\n" + "EntryPoint$C c1 = x > 0 ? a1 : b;\n"
        + "EntryPoint$A c2 = x > 0 ? a1 : a2;\n" + "EntryPoint$C d = x > 0 ? b : c;");
  }

  public void testTightenLocalVariable() throws Exception {
    addSnippetClassDecl("static abstract class B {};");
    addSnippetClassDecl("static class C extends B{};");
    addSnippetClassDecl("static class D extends C{};");
    addSnippetClassDecl("static class E extends C{};");
    Result result = optimize("void",
        "B b;",
        "b = new C();", "b = new D();", "b = new E();");
    result.intoString("EntryPoint$C b;\n" + "b = new EntryPoint$C();\n"
        + "b = new EntryPoint$D();\n" + "b = new EntryPoint$E();");
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
    assertEquals("static void test(EntryPoint$B a);\n",
        result.findMethod("test").toString());
  }

  public void testTightenParameterBasedOnLeafType() throws Exception {
    addSnippetClassDecl("abstract static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static void fun(A a) {if (a == null) return;}");

    Result result = optimize("void", "");
    assertEquals("static void fun(EntryPoint$B a);\n",
        result.findMethod("fun").toString());
  }

  public void testTightenParameterBasedOnOverriddens() throws Exception {
    addSnippetClassDecl("static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static class Test1 { public void fun(A a) {} }");
    addSnippetClassDecl("static class Test2 extends Test1 {public void fun (A a) {} }");
    Result result = optimize("void", "B b = new B();"
        + "Test1 test1 = new Test1();"
        + "test1.fun(b);");
    assertEquals("public void fun(EntryPoint$B a);\n",
        OptimizerTestBase.findMethod(result.findClass("EntryPoint$Test1"), "fun").toString());
    assertEquals("public void fun(EntryPoint$B a);\n",
        OptimizerTestBase.findMethod(result.findClass("EntryPoint$Test2"), "fun").toString());
  }

  public void testInstanceOf() throws Exception {
    addSnippetClassDecl("static class A {};");
    addSnippetClassDecl("static class B extends A {};");
    Result result = optimize("void", "A a = new A();", "B b = new B();", "boolean test;",
        "test = a instanceof A;", "test = b instanceof B;", "test = a instanceof B;",
        "test = b instanceof A;");
    result.intoString("EntryPoint$A a = new EntryPoint$A();\n"
        + "EntryPoint$B b = new EntryPoint$B();\n" + "boolean test;\n" + "test = true;\n"
        + "test = true;\n" + "test = a instanceof EntryPoint$B;\n" + "test = true;");
  }

  public void testMethodBasedOnLeafType() throws Exception {
    addSnippetClassDecl("abstract static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static A fun(A a) {return a;}");

    Result result = optimize("void", "");
    assertEquals("static EntryPoint$B fun(EntryPoint$B a);\n",
        result.findMethod("fun").toString());
  }

  public void testMethodBasedOnReturns() throws Exception {
    addSnippetClassDecl("static class A {}");
    addSnippetClassDecl("static class B extends A {}");
    addSnippetClassDecl("static class C extends B {}");
    addSnippetClassDecl("static class D extends B {}");
    addSnippetClassDecl(
        "A fun(int a) { if(a<0) return new B(); else if(a==0) return new C(); return new D();}");
    Result result = optimize("void", "");
    assertEquals("EntryPoint$B fun(int a);\n", result.findMethod("fun").toString());
  }

  public void testMethodBasedOnOverriders() throws Exception {
    addSnippetClassDecl("abstract static class A { abstract public A fun(); }");
    addSnippetClassDecl("static class B extends A { public B fun() {return new B();} }");
    addSnippetClassDecl("static class C extends A { public B fun() {return new B();} }");
    addSnippetClassDecl("static class D extends B { public D fun() {return new D();} }");
    Result result = optimize("void", "");
    assertEquals("public abstract EntryPoint$B fun();\n",
        OptimizerTestBase.findMethod(result.findClass("EntryPoint$A"), "fun").toString());
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
    assertEquals("static EntryPoint$B fun2();\n", result.findMethod("fun2").toString());
    assertEquals("EntryPoint$B a",
        OptimizerTestBase.findLocal(result.findMethod(MAIN_METHOD_NAME), "a").toString());
    assertEquals("EntryPoint$B a",
        OptimizerTestBase.findField(result.findClass("EntryPoint$C"), "a").toString());
    assertEquals("static void fun1(EntryPoint$B a);\n", result.findMethod("fun1").toString());
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = false;
    // TODO(jbrosenberg): remove loop when Pruner/CFA interaction is perfect.
    while (TypeTightener.exec(program).didChange()) {
      didChange = true;
    }
    return didChange;
  }
}
