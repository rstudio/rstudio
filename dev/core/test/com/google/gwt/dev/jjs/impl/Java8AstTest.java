/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import java.util.Collections;

/**
 * Tests that {@link com.google.gwt.dev.jjs.impl.GwtAstBuilder} correctly builds the AST for
 * features introduced in Java 8.
 */
public class Java8AstTest extends FullCompileTestBase {

  @Override
  public void setUp() throws Exception {
    sourceLevel = SourceLevel.JAVA8;
    super.setUp();
    addAll(LAMBDA_METAFACTORY);

    addAll(JavaResourceBase.createMockJavaResource("test.Runnable",
        "package test;",
        "public interface Runnable {",
        "  void run();",
        "}"
    ));
    addAll(JavaResourceBase.createMockJavaResource("test.Lambda",
        "package test;",
        "public interface Lambda<T> {",
        "  T run(int a, int b);",
        "}"
    ));
    addAll(JavaResourceBase.createMockJavaResource("test.Lambda2",
        "package test;",
        "public interface Lambda2<T> {",
        "  boolean run(T a, T b);",
        "}"
    ));
    addAll(JavaResourceBase.createMockJavaResource("test.AcceptsLambda",
        "package test;",
        "public class AcceptsLambda<T> {",
        "  public T accept(Lambda<T> foo) {",
        "    return foo.run(10, 20);",
        "  }",
        "  public boolean accept2(Lambda2<String> foo) {",
        "    return foo.run(\"a\", \"b\");",
        "  }",
        "}"
    ));

    addAll(JavaResourceBase.createMockJavaResource("test.Pojo",
        "package test;",
        "public class Pojo {",
        "  public Pojo(int x, int y) {",
        "  }",
        "  public Integer fooInstance(int a, int b) {",
        "    return a + b;",
        "  }",
        "}"
    ));

    addAll(JavaResourceBase.createMockJavaResource("test.DefaultInterface",
        "package test;",
        "public interface DefaultInterface {",
        "  void method1();",
        "  default int method2() { return 42; }",
        "}"
    ));

    addAll(JavaResourceBase.createMockJavaResource("test.DefaultInterfaceImpl",
        "package test;",
        "public class DefaultInterfaceImpl implements DefaultInterface {",
        "  public void method1() {}",
        "}"
    ));

    addAll(JavaResourceBase.createMockJavaResource("test.DefaultInterfaceImpl2",
        "package test;",
        "public class DefaultInterfaceImpl2 implements DefaultInterface {",
        "  public void method1() {}",
        "  public int method2() { return DefaultInterface.super.method2(); }",
        "}"
    ));
  }

  public void testCompileLambdaNoCapture() throws Exception {
    String lambda = "new AcceptsLambda<Integer>().accept((a,b) -> a + b);";
    assertEqualBlock(
        "(new AcceptsLambda()).accept(new EntryPoint$lambda$0$Type());",
        lambda
       );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "lambda$0"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // no fields
    assertEquals(0, lambdaInnerClass.getFields().size());

    // should have constructor taking no args
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(0, ctor.getParams().size());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));
    // should implement run method
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){return EntryPoint.lambda$0(arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileLambdaCaptureLocal() throws Exception {
    String lambda = "int x = 42; new AcceptsLambda<Integer>().accept((a,b) -> x + a + b);";
    assertEqualBlock(
        "int x=42;(new AcceptsLambda()).accept(new EntryPoint$lambda$0$Type(x));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "lambda$0"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking x
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(1, ctor.getParams().size());
    assertEquals(JPrimitiveType.INT, ctor.getOriginalParamTypes().get(0));

    // should have 1 field to store the local
    assertEquals(1, lambdaInnerClass.getFields().size());
    assertEquals(JPrimitiveType.INT, lambdaInnerClass.getFields().get(0).getType());

    // should contain assignment statement of ctor param to field
    assertEquals("{this.x_0=x_0;}", formatSource(ctor.getBody().toSource()));
    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda as static function
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){" +
            "return EntryPoint.lambda$0(this.x_0,arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileLambdaCaptureLocalWithBlockInLambda() throws Exception {
    String lambda =
        "int x = 42; "
        + "new AcceptsLambda<Integer>().accept((a,b) -> { int temp = x; return temp + a + b; });";
    assertEqualBlock(
        "int x=42;(new AcceptsLambda()).accept(new EntryPoint$lambda$0$Type(x));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "lambda$0"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking x
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(1, ctor.getParams().size());
    assertEquals(JPrimitiveType.INT, ctor.getOriginalParamTypes().get(0));

    // should have 1 field to store the local
    assertEquals(1, lambdaInnerClass.getFields().size());
    assertEquals(JPrimitiveType.INT, lambdaInnerClass.getFields().get(0).getType());

    // should contain assignment statement of ctor param to field
    assertEquals("{this.x_0=x_0;}", formatSource(ctor.getBody().toSource()));
    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda as static function
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){" +
            "return EntryPoint.lambda$0(this.x_0,arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  // test whether local capture and outer scope capture work together
  public void testCompileLambdaCaptureLocalAndField() throws Exception {
    addSnippetClassDecl("private int y = 22;");
    String lambda = "int x = 42; new AcceptsLambda<Integer>().accept((a,b) -> x + y + a + b);";
    assertEqualBlock(
        "int x=42;(new AcceptsLambda()).accept(new EntryPoint$lambda$0$Type(this,x));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "lambda$0"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(2, ctor.getParams().size());
    assertEquals(lambdaInnerClass.getEnclosingType(), ctor.getOriginalParamTypes().get(0));
    assertEquals(JPrimitiveType.INT, ctor.getOriginalParamTypes().get(1));

    // should have 2 field to store the outer and local
    assertEquals(2, lambdaInnerClass.getFields().size());
    assertEquals(lambdaInnerClass.getEnclosingType(),
        lambdaInnerClass.getFields().get(0).getType());
    assertEquals(JPrimitiveType.INT, lambdaInnerClass.getFields().get(1).getType());

    // should contain assignment statement of ctor params to field
    assertEquals("{this.$$outer_0=$$outer_0;this.x_1=x_1;}",
        formatSource(ctor.getBody().toSource()));
    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){" +
            "return this.$$outer_0.lambda$0(this.x_1,arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  // make sure nested scoping of identically named variables works
  public void testCompileLambdaCaptureOuterInnerField() throws Exception {
    addSnippetClassDecl("private int y = 22;");
    addSnippetClassDecl("class Foo { " +
          "int y = 42;" +
          "void m() {" +
          "new AcceptsLambda<Integer>().accept((a,b) -> EntryPoint.this.y + y + a + b); }" +
        " }");
    String lambda = "new Foo().m();";
    assertEqualBlock(
        "(new EntryPoint$Foo(this)).m();",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    JMethod lambdaMethod = findMethod(program.getFromTypeMap("test.EntryPoint$Foo"), "lambda$0");
    assertNotNull(lambdaMethod);
    assertEquals("{return Integer.valueOf(this.this$01.y+this.y+a_0+b_1);}",
        formatSource(lambdaMethod.getBody().toSource()));
    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$Foo$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$Foo$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(1, ctor.getParams().size());
    assertEquals(lambdaInnerClass.getEnclosingType(), ctor.getOriginalParamTypes().get(0));

    // should have 1 field to store the outer
    assertEquals(1, lambdaInnerClass.getFields().size());
    assertEquals(lambdaInnerClass.getEnclosingType(),
        lambdaInnerClass.getFields().get(0).getType());

    // should contain assignment statement of ctor params to field
    assertEquals("{this.$$outer_0=$$outer_0;}", formatSource(ctor.getBody().toSource()));
    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){return this.$$outer_0.lambda$0(arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testLambdaCaptureParameter() throws Exception {
    addSnippetClassDecl("interface ClickHandler {\n" +
        "    int onClick(int a);\n" +
        "  }\n" +
        "  private int addClickHandler(ClickHandler clickHandler) {\n" +
        "    return clickHandler.onClick(1);\n" +
        "  }\n" +
        "  private int addClickHandler(int a) {\n" +
        "    return addClickHandler(x->{int temp = a; return temp;});\n" +
        "  }\n");
    JProgram program = compileSnippet("int", "return addClickHandler(2);", false);
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking the outer variable from parameter
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(1, ctor.getParams().size());
    assertEquals(JPrimitiveType.INT, ctor.getOriginalParamTypes().get(0));

    // should have 1 field to store the outer
    assertEquals(1, lambdaInnerClass.getFields().size());
    assertEquals(JPrimitiveType.INT,
        lambdaInnerClass.getFields().get(0).getType());

    // should contain assignment statement of ctor params to field
    assertEquals("{this.a_0=a_0;}", formatSource(ctor.getBody().toSource()));
    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(
        program.getFromTypeMap("test.EntryPoint$ClickHandler")));

    JMethod samMethod = findMethod(lambdaInnerClass, "onClick");
    assertEquals("public final int onClick(int a){return EntryPoint.lambda$0(this.a_0,a);}",
        formatSource(samMethod.toSource()));
  }

  public void testLambdaNestingCaptureLocal() throws Exception {
    addSnippetClassDecl("interface Inner {\n" +
        "    void f();\n" +
        "  }\n");
    addSnippetClassDecl(
        "  interface Outer {\n" +
        "     void accept(Inner t);\n" +
        "   }\n");
    addSnippetClassDecl(
        "  public static void call(Outer a) {\n" +
        "    a.accept(() -> {});\n" +
        "  }\n");
    String nestedLambda = "boolean[] success = new boolean[] {false};\n"
        + "call( sam1 -> { call(sam2 -> {success[0] = true;}); });";
    assertEqualBlock("boolean[]success=new boolean[]{false};"
        + "EntryPoint.call(new EntryPoint$lambda$1$Type(success));", nestedLambda);
    JProgram program = compileSnippet("void", nestedLambda, false);
    JClassType lambdaInnerClass1 = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    JClassType lambdaInnerClass2 = (JClassType) getType(program, "test.EntryPoint$lambda$1$Type");
    JClassType lambdaInnerClass3 = (JClassType) getType(program, "test.EntryPoint$lambda$2$Type");
    assertNotNull(lambdaInnerClass1);
    assertNotNull(lambdaInnerClass2);
    assertNotNull(lambdaInnerClass3);

    // check constructors
    JMethod ctor1 = findMethod(lambdaInnerClass1, "EntryPoint$lambda$0$Type");
    assertTrue(ctor1 instanceof JConstructor);
    assertEquals(0, ctor1.getParams().size());

    JMethod ctor2 = findMethod(lambdaInnerClass2, "EntryPoint$lambda$1$Type");
    assertTrue(ctor2 instanceof JConstructor);
    assertEquals(1, ctor2.getParams().size());
    assertEquals("boolean[]", ctor2.getOriginalParamTypes().get(0).getName());

    JMethod ctor3 = findMethod(lambdaInnerClass3, "EntryPoint$lambda$2$Type");
    assertTrue(ctor3 instanceof JConstructor);
    assertEquals(1, ctor3.getParams().size());
    assertEquals("boolean[]", ctor3.getOriginalParamTypes().get(0).getName());

    // check fields
    assertEquals(0, lambdaInnerClass1.getFields().size());

    assertEquals(1, lambdaInnerClass2.getFields().size());
    assertEquals("boolean[]",
        lambdaInnerClass2.getFields().get(0).getType().getName());

    assertEquals(1, lambdaInnerClass3.getFields().size());
    assertEquals("boolean[]",
        lambdaInnerClass3.getFields().get(0).getType().getName());

    // check constructor body
    assertEquals("{this.success_0=success_0;}", formatSource(ctor2.getBody().toSource()));
    assertEquals("{this.success_0=success_0;}", formatSource(ctor3.getBody().toSource()));

    // check super interface
    assertTrue(lambdaInnerClass1.getImplements().contains(
        program.getFromTypeMap("test.EntryPoint$Inner")));
    assertTrue(lambdaInnerClass2.getImplements().contains(
        program.getFromTypeMap("test.EntryPoint$Outer")));
    assertTrue(lambdaInnerClass3.getImplements().contains(
        program.getFromTypeMap("test.EntryPoint$Outer")));

    // check samMethod
    JMethod samMethod1 = findMethod(lambdaInnerClass2, "accept");
    JMethod samMethod2 = findMethod(lambdaInnerClass3, "accept");
    assertEquals(
        "public final void accept(EntryPoint$Inner t){EntryPoint.lambda$1(this.success_0,t);}",
        formatSource(samMethod1.toSource()));
    assertEquals(
        "public final void accept(EntryPoint$Inner t){EntryPoint.lambda$2(this.success_0,t);}",
        formatSource(samMethod2.toSource()));

    // check lambda method
    JMethod lambdaMethod1 = findMethod(program, "lambda$1");
    JMethod lambdaMethod2 = findMethod(program, "lambda$2");
    assertEquals(
        "private static void lambda$1(boolean[]success_0,EntryPoint$Inner sam1_1)"
        + "{{EntryPoint.call(new EntryPoint$lambda$2$Type(success_0));}}",
        formatSource(lambdaMethod1.toSource()));
    assertEquals(
        "private static void lambda$2(boolean[]success_0,EntryPoint$Inner sam2_1)"
        + "{{success_0[0]=true;}}",
        formatSource(lambdaMethod2.toSource()));
  }

  public void testLambdaNestingInAnonymousCaptureLocal() throws Exception {
    String lambda =
        "int x = 42;\n" +
        "new Runnable() { public void run() { Lambda<Integer> l = (a, b) -> x + a + b; l.run(1, 2); } }.run();";
    assertEqualBlock("int x=42;(new EntryPoint$1(this,x)).run();", lambda);
    JProgram program = compileSnippet("void", lambda, false);
    JClassType outerClass = (JClassType) getType(program, "test.EntryPoint$1");

    // check that anonymous class implementation uses synthetic field val$x2 to initialize lambda
    // synthetic class
    assertEquals(
        "public void run(){Lambda l=new EntryPoint$1$lambda$0$Type(this.val$x2);l.run(1,2);}",
        formatSource(findMethod(outerClass, "run").toSource()));
  }

  public void testLambdaNestingInMultipleAnonymousCaptureLocal() throws Exception {
    addSnippetClassDecl("interface I { int foo(Integer i); }");
    // checks that lambda has access to local variable and arguments when placed in local anonymous
    // class with multiple nesting
    String snippet =
        "int[] x = new int[] {42};\n" +
        "int result = new I(){\n" +
        "  public int foo(Integer i1){\n" +
        "    return new I(){\n" +
        "      public int foo(Integer i2){\n" +
        "        return new I(){\n" +
        "          public int foo(Integer i3){\n" +
        "            Lambda<Integer> l = (a, b) -> x[0] = x[0] + a + b + i1 + i2 + i3;\n" +
        "            return l.run(1, 2);\n" +
        "          }\n" +
        "        }.foo(3);\n" +
        "      }\n" +
        "    }.foo(2);\n" +
        "  }\n" +
        "}.foo(1);\n";

    JProgram program = compileSnippet("void", snippet, false);
    JClassType outer1 = (JClassType) getType(program, "test.EntryPoint$1");
    JClassType outer2 = (JClassType) getType(program, "test.EntryPoint$1$1");
    JClassType outer3 = (JClassType) getType(program, "test.EntryPoint$1$1$1");

    assertNotNull(outer1);
    assertNotNull(outer2);
    assertNotNull(outer3);

    JMethod outer1Method = findMethod(outer1, "foo");
    JMethod outer2Method = findMethod(outer2, "foo");
    JMethod outer3Method = findMethod(outer3, "foo");
    assertNotNull(outer3Method);
    assertEquals(
        "public int foo(Integer i1){"
        + "return(new EntryPoint$1$1(this,this.val$x2,i1)).foo(Integer.valueOf(2));"
        + "}",
        formatSource(outer1Method.toSource()));
    assertEquals(
        "public int foo(Integer i2){"
        + "return(new EntryPoint$1$1$1(this,this.val$x2,this.val$i13,i2)).foo(Integer.valueOf(3));"
        + "}",
        formatSource(outer2Method.toSource()));
    // checks that lambda scope initialized similar to anonymous class
    assertEquals(
        "public int foo(Integer i3){"
        + "Lambda l=new EntryPoint$1$1$1$lambda$0$Type(this.val$x2,this.val$i13,this.val$i24,i3);"
        + "return((Integer)l.run(1,2)).intValue();"
        + "}",
        formatSource(outer3Method.toSource()));
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal() throws Exception {
    // checks that lambda has access to local variable and arguments when placed in mixed scopes
    // Local Class -> Local Class -> Local Anonymous -> lambda -> Local Anonymous
    addSnippetClassDecl("interface I { int foo(Integer i); }");
    addSnippetClassDecl("class A {\n" +
      "int a() {\n" +
      "  int[] x = new int[] {42};\n" +
      "  class B {\n" +
      "    void b() {\n" +
      "      I i = new I(){\n" +
      "        public int foo(Integer arg){\n" +
      "          Runnable r = () ->{\n" +
      "            new Runnable() {\n" +
      "              public void run() {\n" +
      "                Lambda<Integer> l = (a, b) -> x[0] = x[0] + a + b + arg;\n" +
      "                x[0] = l.run(1, 2);\n" +
      "              }\n" +
      "            }.run();\n" +
      "          };\n" +
      "          r.run();\n" +
      "          return x[0];\n" +
      "        }\n" +
      "      };\n" +
      "      i.foo(1);\n" +
      "    }\n" +
      "  }\n" +
      "  B b = new B();\n" +
      "  b.b();\n" +
      "  return x[0];\n" +
      "}\n" +
    "}\n");

    String snippet = "A a = new A();";
    JProgram program = compileSnippet("void", snippet, false);
    JClassType lambdaOuter = (JClassType) getType(program, "test.EntryPoint$A$1B$1$1");
    assertNotNull(lambdaOuter);

    JMethod lambdaOuterMethod = findMethod(lambdaOuter, "run");
    assertNotNull(lambdaOuterMethod);
    // checks that lambda initialization properly uses synthetic fields from outer class
    assertEquals(
        "public void run(){"
        + "Lambda l=new EntryPoint$A$1B$1$1$lambda$0$Type(this.val$x2,this.val$arg3);"
        + "this.val$x2[0]=((Integer)l.run(1,2)).intValue();"
        + "}",
        formatSource(lambdaOuterMethod.toSource()));
  }

  public void testLambdaNestingCaptureField() throws Exception {
    addSnippetClassDecl("interface Inner {\n" +
        "    void f();\n" +
        "  }\n");
    addSnippetClassDecl(
        "  interface Outer {\n" +
        "     void accept(Inner t);\n" +
        "   }\n");
    addSnippetClassDecl(
        "  static class A {\n" +
        "    public boolean[] success = new boolean[] {false};\n" +
        "    public void call(Outer a) {\n" +
        "      a.accept(() -> {});\n" +
        "    }\n" +
        "  }\n");
    String nestedLambda = "A a = new A();\n"
        + "a.call( sam1 -> { a.call(sam2 -> {a.success[0] = true;}); });";
    assertEqualBlock("EntryPoint$A a=new EntryPoint$A();a.call(new EntryPoint$lambda$0$Type(a));",
        nestedLambda);
    JProgram program = compileSnippet("void", nestedLambda, false);
    JClassType lambdaInnerClass1 = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    JClassType lambdaInnerClass2 = (JClassType) getType(program, "test.EntryPoint$lambda$1$Type");
    assertNotNull(lambdaInnerClass1);
    assertNotNull(lambdaInnerClass2);

    // check constructors
    JMethod ctor1 = findMethod(lambdaInnerClass1, "EntryPoint$lambda$0$Type");
    assertTrue(ctor1 instanceof JConstructor);
    assertEquals(1, ctor1.getParams().size());
    assertEquals("test.EntryPoint$A", ctor1.getOriginalParamTypes().get(0).getName());

    JMethod ctor2 = findMethod(lambdaInnerClass2, "EntryPoint$lambda$1$Type");
    assertTrue(ctor2 instanceof JConstructor);
    assertEquals(1, ctor2.getParams().size());
    assertEquals("test.EntryPoint$A", ctor2.getOriginalParamTypes().get(0).getName());

    // check fields
    assertEquals(1, lambdaInnerClass1.getFields().size());
    assertEquals("test.EntryPoint$A", lambdaInnerClass2.getFields().get(0).getType().getName());

    assertEquals(1, lambdaInnerClass2.getFields().size());
    assertEquals("test.EntryPoint$A", lambdaInnerClass2.getFields().get(0).getType().getName());

    // check constructor body
    assertEquals("{this.a_0=a_0;}", formatSource(ctor2.getBody().toSource()));
    assertEquals("{this.a_0=a_0;}", formatSource(ctor2.getBody().toSource()));

    // check super interface
    assertTrue(lambdaInnerClass1.getImplements().contains(
        program.getFromTypeMap("test.EntryPoint$Outer")));
    assertTrue(lambdaInnerClass2.getImplements().contains(
        program.getFromTypeMap("test.EntryPoint$Outer")));

    // check samMethod
    JMethod samMethod1 = findMethod(lambdaInnerClass1, "accept");
    JMethod samMethod2 = findMethod(lambdaInnerClass2, "accept");
    assertEquals(
        "public final void accept(EntryPoint$Inner t){EntryPoint.lambda$0(this.a_0,t);}",
        formatSource(samMethod1.toSource()));
    assertEquals(
        "public final void accept(EntryPoint$Inner t){EntryPoint.lambda$1(this.a_0,t);}",
        formatSource(samMethod2.toSource()));

    // check lambda method
    JMethod lambdaMethod1 = findMethod(program, "lambda$0");
    JMethod lambdaMethod2 = findMethod(program, "lambda$1");
    assertEquals(
        "private static void lambda$0(EntryPoint$A a_0,EntryPoint$Inner sam1_1)"
        + "{{a_0.call(new EntryPoint$lambda$1$Type(a_0));}}",
        formatSource(lambdaMethod1.toSource()));
    assertEquals(
        "private static void lambda$1(EntryPoint$A a_0,EntryPoint$Inner sam2_1)"
        + "{{a_0.success[0]=true;}}",
        formatSource(lambdaMethod2.toSource()));
  }

  public void testCompileStaticReferenceBinding() throws Exception {
    addSnippetClassDecl("public static Integer foo(int x, int y) { return x + y; }");
    String lambda = "new AcceptsLambda<Integer>().accept(EntryPoint::foo);";

    String generatedInnerClassName = "test.EntryPoint$0methodref$foo$Type";
    String simpleLambdaInnerClassName = generatedInnerClassName.substring("test.".length());

    assertEqualBlock(
        "(new AcceptsLambda()).accept(new " + simpleLambdaInnerClassName + "());", lambda);
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "foo"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, generatedInnerClassName);
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass,
        simpleLambdaInnerClassName);
    assertTrue(ctor instanceof JConstructor);
    // no ctor args
    assertEquals(0, ctor.getParams().size());

    // no fields
    assertEquals(0, lambdaInnerClass.getFields().size());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){return EntryPoint.foo(arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileInstanceReferenceBinding() throws Exception {
    addSnippetClassDecl("public Integer foo(int x, int y) { return x + y; }");
    String lambda = "new AcceptsLambda<Integer>().accept(this::foo);";

    String generatedInnerClassName = "test.EntryPoint$0methodref$foo$Type";
    String simpleLambdaInnerClassName = generatedInnerClassName.substring("test.".length());

    assertEqualBlock(
        "(new AcceptsLambda()).accept(new " + simpleLambdaInnerClassName + "(this));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "foo"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, generatedInnerClassName);
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass,  simpleLambdaInnerClassName);
    assertTrue(ctor instanceof JConstructor);
    // instance capture
    assertEquals(1, ctor.getParams().size());
    assertEquals(lambdaInnerClass.getEnclosingType(), ctor.getOriginalParamTypes().get(0));

    // should have 1 field to store the captured instance
    assertEquals(1, lambdaInnerClass.getFields().size());
    assertEquals(lambdaInnerClass.getEnclosingType(),
        lambdaInnerClass.getFields().get(0).getType());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){return this.$$outer_0.foo(arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileInstanceReferenceBindingMultiple() throws Exception {
    addSnippetClassDecl("Pojo instance1 = new Pojo(1, 2);");
    addSnippetClassDecl("Pojo instance2 = new Pojo(3, 4);");
    String reference =
        "new AcceptsLambda<Integer>().accept(instance1::fooInstance);\n" +
        "new AcceptsLambda<Integer>().accept(instance2::fooInstance);";

    String generatedInnerClassName = "test.EntryPoint$0methodref$fooInstance$Type";
    String simpleLambdaInnerClassName1 = generatedInnerClassName.substring("test.".length());
    String simpleLambdaInnerClassName2 = "EntryPoint$1methodref$fooInstance$Type";

    assertEqualBlock(
        "(new AcceptsLambda()).accept(new " + simpleLambdaInnerClassName1 + "(this.instance1));\n"
        + "(new AcceptsLambda()).accept(new " + simpleLambdaInnerClassName2 + "(this.instance2));",
        reference);
    JProgram program = compileSnippet("void", reference, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, generatedInnerClassName);
    assertNotNull(lambdaInnerClass);
    assertEquals(1, Collections.frequency(program.getDeclaredTypes(), lambdaInnerClass));

    // should have constructor taking the instance
    JMethod ctor = findMethod(lambdaInnerClass, simpleLambdaInnerClassName1);
    assertTrue(ctor instanceof JConstructor);
    // instance capture
    assertEquals(1, ctor.getParams().size());
    assertEquals(lambdaInnerClass.getEnclosingType(), ctor.getOriginalParamTypes().get(0));

    // should have 1 field to store the captured instance
    assertEquals(1, lambdaInnerClass.getFields().size());
    assertEquals(lambdaInnerClass.getEnclosingType(),
        lambdaInnerClass.getFields().get(0).getType());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){return this.$$outer_0.fooInstance(arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileInstanceReferenceBindingMultipleWithSameMethodSignature() throws Exception {
    addSnippetClassDecl(
        "static class TestMF_A {",
        "  public String getId() {",
        "    return \"A\";",
        "  }",
        "}",

        "static class TestMF_B {",
        "  public String getId() {",
        "    return \"B\";",
        "  }",
        "}",

        "interface Function<T> {",
        "  T apply();",
        "}",

        "private String f(Function<String> arg) {",
        "    return arg.apply();",
        "  }");

    String reference = Joiner.on('\n').join(
        "TestMF_A a = new TestMF_A();",
        "TestMF_B b = new TestMF_B();",
        "f(a::getId);",
        "f(b::getId);"
    );

    String generatedInnerClassNameForA = "test.EntryPoint$0methodref$getId$Type";
    String simpleLambdaInnerClassNameForA =
        generatedInnerClassNameForA.substring("test.".length());

    String generatedInnerClassNameForB = "test.EntryPoint$1methodref$getId$Type";
    String simpleLambdaInnerClassNameForB =
        generatedInnerClassNameForB.substring("test.".length());

    assertEqualBlock(
        "EntryPoint$TestMF_A a=new EntryPoint$TestMF_A();"
            + "EntryPoint$TestMF_B b=new EntryPoint$TestMF_B();"
            + "this.f(new " + simpleLambdaInnerClassNameForA + "(a));"
            + "this.f(new " + simpleLambdaInnerClassNameForB + "(b));",
        reference);
    JProgram program = compileSnippet("void", reference, false);

    // created by GwtAstBuilder
    JClassType innerClassA = (JClassType) getType(program, generatedInnerClassNameForA);
    JClassType innerClassB = (JClassType) getType(program, generatedInnerClassNameForB);
    assertNotNull(innerClassA);
    assertNotNull(innerClassB);

    // should have constructor taking this and x
    JMethod ctorA = findMethod(innerClassA, simpleLambdaInnerClassNameForA);
    assertTrue(ctorA instanceof JConstructor);
    // instance capture
    assertEquals(1, ctorA.getParams().size());
    assertEquals("test.EntryPoint$TestMF_A", ctorA.getOriginalParamTypes().get(0).getName());
    JMethod ctorB = findMethod(innerClassB, simpleLambdaInnerClassNameForB);
    assertTrue(ctorB instanceof JConstructor);
    // instance capture
    assertEquals(1, ctorB.getParams().size());
    assertEquals("test.EntryPoint$TestMF_B", ctorB.getOriginalParamTypes().get(0).getName());

    // should have 1 field to store the captured instance
    assertEquals(1, innerClassA.getFields().size());
    assertEquals("test.EntryPoint$TestMF_A",
        innerClassA.getFields().get(0).getType().getName());
    assertEquals(1, innerClassB.getFields().size());
    assertEquals("test.EntryPoint$TestMF_B",
        innerClassB.getFields().get(0).getType().getName());

    // should extends EntryPoint$Function
    assertTrue(
        innerClassA.getImplements().contains(program.getFromTypeMap("test.EntryPoint$Function")));
    assertTrue(
        innerClassB.getImplements().contains(program.getFromTypeMap("test.EntryPoint$Function")));

    // should implement apply method
    JMethod samMethodA = findMethod(innerClassA, "apply");
    assertEquals(
        "public final Object apply(){return this.$$outer_0.getId();}",
        formatSource(samMethodA.toSource()));
    JMethod samMethodB = findMethod(innerClassB, "apply");
    assertEquals(
        "public final Object apply(){return this.$$outer_0.getId();}",
        formatSource(samMethodB.toSource()));
  }

  public void testCompileStaticReferenceBindingMultiple() throws Exception {
    addSnippetClassDecl(
        "static class TestMF_A {",
        "  public static String getId() {",
        "    return \"A\";",
        "  }",
        "}",

        "static class TestMF_B {",
        "  public static String getId() {",
        "    return \"B\";",
        "  }",
        "}",

        "interface Function<T> {",
        "  T apply();",
        "}",

        "private String f(Function<String> arg) {",
        "    return arg.apply();",
        "  }");
    String reference = "f(TestMF_A::getId);\n" + "f(TestMF_B::getId);";

    String generatedInnerClassNameForA = "test.EntryPoint$0methodref$getId$Type";
    String simpleLambdaInnerClassNameForA =
        generatedInnerClassNameForA.substring("test.".length());

    String generatedInnerClassNameForB = "test.EntryPoint$1methodref$getId$Type";
    String simpleLambdaInnerClassNameForB =
        generatedInnerClassNameForB.substring("test.".length());

    assertEqualBlock("this.f(new " + simpleLambdaInnerClassNameForA + "());" +
            "this.f(new " + simpleLambdaInnerClassNameForB + "());",
        reference);
    JProgram program = compileSnippet("void", reference, false);

    // created by GwtAstBuilder
    JClassType innerClassA = (JClassType) getType(program, generatedInnerClassNameForA);
    JClassType innerClassB = (JClassType) getType(program, generatedInnerClassNameForB);
    assertNotNull(innerClassA);
    assertNotNull(innerClassB);

    // should extends EntryPoint$Function
    assertTrue(
        innerClassA.getImplements().contains(program.getFromTypeMap("test.EntryPoint$Function")));
    assertTrue(
        innerClassB.getImplements().contains(program.getFromTypeMap("test.EntryPoint$Function")));

    // should implement apply method
    JMethod samMethodA = findMethod(innerClassA, "apply");
    assertEquals(
        "public final Object apply(){return EntryPoint$TestMF_A.getId();}",
        formatSource(samMethodA.toSource()));
    JMethod samMethodB = findMethod(innerClassB, "apply");
    assertEquals(
        "public final Object apply(){return EntryPoint$TestMF_B.getId();}",
        formatSource(samMethodB.toSource()));
  }

  public void testCompileImplicitQualifierReferenceBinding() throws Exception {
    String lambda = "new AcceptsLambda<String>().accept2(String::equalsIgnoreCase);";

    String generatedInnerClassName = "test.EntryPoint$0methodref$equalsIgnoreCase$Type";
    String simpleLambdaInnerClassName =
        generatedInnerClassName.substring("test.".length());

    assertEqualBlock(
        "(new AcceptsLambda()).accept2(new " +  simpleLambdaInnerClassName + " ());",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, generatedInnerClassName);
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, simpleLambdaInnerClassName);
    assertTrue(ctor instanceof JConstructor);
    // no instance capture
    assertEquals(0, ctor.getParams().size());

    // no fields
    assertEquals(0, lambdaInnerClass.getFields().size());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda2")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final boolean run(Object arg0,Object arg1)"
            + "{return((String)arg0).equalsIgnoreCase((String)arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileConstructorReferenceBinding() throws Exception {
    String lambda = "new AcceptsLambda<Pojo>().accept(Pojo::new);";

    String generatedInnerClassName = "test.EntryPoint$0methodref$ctor$Type";
    String simpleLambdaInnerClassName =
        generatedInnerClassName.substring("test.".length());

    assertEqualBlock(
        "(new AcceptsLambda()).accept(new " + simpleLambdaInnerClassName + "());",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, generatedInnerClassName);
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, simpleLambdaInnerClassName);
    assertTrue(ctor instanceof JConstructor);
    // no instance capture
    assertEquals(0, ctor.getParams().size());

    // no fields
    assertEquals(0, lambdaInnerClass.getFields().size());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){return new Pojo(arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileConstructorReferenceBindingWithEnclosingInstanceCapture()
      throws Exception {
    addSnippetClassDecl(
        "int field1, field2;",

        "class Pojo2 {",
        "  public Pojo2(int x, int y) {",
        "  }",
        "  public int someMethod() { ",
        "    return field1 + field2; ",
        "  }",
        "}"
    );

    String lambda = "new AcceptsLambda<Pojo2>().accept(Pojo2::new);";

    String generatedInnerClassName = "test.EntryPoint$0methodref$ctor$Type";
    String simpleLambdaInnerClassName =
        generatedInnerClassName.substring("test.".length());

    assertEqualBlock("(new AcceptsLambda()).accept(new " + simpleLambdaInnerClassName + "(this));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, generatedInnerClassName);
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, simpleLambdaInnerClassName);
    assertTrue(ctor instanceof JConstructor);
    // one instance capture
    assertEquals(1, ctor.getParams().size());

    // one field for instance
    assertEquals(1, lambdaInnerClass.getFields().size());

    // should extends test.Lambda
    assertTrue(lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.Lambda")));

    // should implement run method and invoke lambda via captured instance
    JMethod samMethod = findMethod(lambdaInnerClass, "run");
    assertEquals(
        "public final Object run(int arg0,int arg1){"
            + "return new EntryPoint$Pojo2(this.test_EntryPoint,arg0,arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testIntersectionCast() throws Exception {
    addSnippetClassDecl("static class A {void print() {} }");
    addSnippetClassDecl("interface I1 {}");
    addSnippetClassDecl("interface I2 {}");
    addSnippetClassDecl("interface I3 {}");
    addSnippetClassDecl("static class B extends A implements I1 {}");
    addSnippetClassDecl("static class C extends A implements I1, I2, I3 {}");
    String cast1 = "B b = new B(); ((A & I1) b).print();";
    assertEqualBlock("EntryPoint$B b=new EntryPoint$B();((EntryPoint$A)(EntryPoint$I1)b).print();",
        cast1);
    String cast2 = "C c = new C(); ((A & I1 & I2 & I3)c).print();";
    assertEqualBlock("EntryPoint$C c=new EntryPoint$C();"
        + "((EntryPoint$A)(EntryPoint$I1)(EntryPoint$I2)(EntryPoint$I3)c).print();", cast2);
  }

  public void testIntersectionCastOfLambda() throws Exception {
    addSnippetClassDecl("interface I1 { public void foo(); }");
    addSnippetClassDecl("interface I2 { }");
    String lambda = "Object o = (I2 & I1) () -> {};";
    assertEqualBlock("Object o=(EntryPoint$I2)(EntryPoint$I1)new EntryPoint$lambda$0$Type();",
        lambda);

    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "lambda$0"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // no fields
    assertEquals(0, lambdaInnerClass.getFields().size());

    // should have constructor taking no args
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(0, ctor.getParams().size());

    // should implements I1 and I2
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I1")));
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I2")));
    // should implement foo method
    JMethod samMethod = findMethod(lambdaInnerClass, "foo");
    assertEquals("public final void foo(){EntryPoint.lambda$0();}",
        formatSource(samMethod.toSource()));
  }

  public void testMultipleIntersectionCastOfLambda() throws Exception {
    addSnippetClassDecl("interface I1 { public void foo(); }");
    addSnippetClassDecl("interface I2 { }");
    addSnippetClassDecl("interface I3 { }");
    String lambda = "I2 o = (I3 & I2 & I1) () -> {};";
    assertEqualBlock(
        "EntryPoint$I2 o=(EntryPoint$I3)(EntryPoint$I2)(EntryPoint$I1)new EntryPoint$lambda$0$Type();",
        lambda);

    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "lambda$0"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);

    // no fields
    assertEquals(0, lambdaInnerClass.getFields().size());

    // should have constructor taking no args
    JMethod ctor = findMethod(lambdaInnerClass, "EntryPoint$lambda$0$Type");
    assertTrue(ctor instanceof JConstructor);
    assertEquals(0, ctor.getParams().size());

    // should extends java.lang.Object, implements I1, I2 and I3
    assertEquals("java.lang.Object", lambdaInnerClass.getSuperClass().getName());
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I1")));
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I2")));
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I3")));
    // should implement foo method
    JMethod samMethod = findMethod(lambdaInnerClass, "foo");
    assertEquals("public final void foo(){EntryPoint.lambda$0();}",
        formatSource(samMethod.toSource()));
  }

  public void testIntersectionCastOfLambdaWithClassType() throws Exception {
    addSnippetClassDecl("interface I1 { public void foo(); }");
    addSnippetClassDecl("class A { }");
    String lambda = "Object o = (A & I1) () -> {};";
    assertEqualBlock("Object o=(EntryPoint$A)(EntryPoint$I1)new EntryPoint$lambda$0$Type();",
        lambda);

    JProgram program = compileSnippet("void", lambda, false);

    assertNotNull(getMethod(program, "lambda$0"));

    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);
    assertEquals("java.lang.Object", lambdaInnerClass.getSuperClass().getName());
    assertEquals(1, lambdaInnerClass.getImplements().size());
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I1")));
    // should implement foo method
    JMethod samMethod = findMethod(lambdaInnerClass, "foo");
    assertEquals("public final void foo(){EntryPoint.lambda$0();}",
        formatSource(samMethod.toSource()));
  }

  public void testIntersectionCastOfLambdaOneAbstractMethod() throws Exception {
    addSnippetClassDecl("interface I1 { public void foo(); }");
    addSnippetClassDecl("interface I2 extends I1{ public void foo();}");
    String lambda = "Object o = (I1 & I2) () -> {};";
    // (I1 & I2) is resolved to I2 by JDT.
    assertEqualBlock("Object o=(EntryPoint$I2)new EntryPoint$lambda$0$Type();",
        lambda);

    JProgram program = compileSnippet("void", lambda, false);

    assertNotNull(getMethod(program, "lambda$0"));

    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);
    assertEquals("java.lang.Object", lambdaInnerClass.getSuperClass().getName());
    assertEquals(1, lambdaInnerClass.getImplements().size()); // only implements I2.
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I2")));
    // should implement foo method
    JMethod samMethod = findMethod(lambdaInnerClass, "foo");
    assertEquals("public final void foo(){EntryPoint.lambda$0();}",
        formatSource(samMethod.toSource()));
  }

  public void testIntersectionCastMultipleAbstractMethods() throws Exception {
    addSnippetClassDecl("interface I1 { public void foo(); }");
    addSnippetClassDecl("interface I2 { public void bar(); public void fun();}");
    String lambda = "Object o = (I1 & I2) () -> {};";
    assertEqualBlock("Object o=(EntryPoint$I1)(EntryPoint$I2)new EntryPoint$lambda$0$Type();",
        lambda);

    JProgram program = compileSnippet("void", lambda, false);

    assertNotNull(getMethod(program, "lambda$0"));

    JClassType lambdaInnerClass = (JClassType) getType(program, "test.EntryPoint$lambda$0$Type");
    assertNotNull(lambdaInnerClass);
    assertEquals("java.lang.Object", lambdaInnerClass.getSuperClass().getName());
    assertEquals(1, lambdaInnerClass.getImplements().size());
    assertTrue(
        lambdaInnerClass.getImplements().contains(program.getFromTypeMap("test.EntryPoint$I1")));
    // should implement foo method
    JMethod samMethod = findMethod(lambdaInnerClass, "foo");
    assertEquals("public final void foo(){EntryPoint.lambda$0();}",
        formatSource(samMethod.toSource()));
  }

  private static final MockJavaResource LAMBDA_METAFACTORY =
      JavaResourceBase.createMockJavaResource("java.lang.invoke.LambdaMetafactory",
          "package java.lang.invoke;",
          "public class LambdaMetafactory {",
          "}");

  public void testDefaultInterfaceMethod() throws Exception {
    JProgram program = compileSnippet("void", "(new DefaultInterfaceImpl()).method2();", false);

    // created by GwtAstBuilder
    JInterfaceType intf = (JInterfaceType) getType(program, "test.DefaultInterface");
    // should have an actual method with body on it
    JMethod defaultMethod = findMethod(intf, "method2");
    assertNotNull(defaultMethod);
    assertNotNull(defaultMethod.getBody());
    assertEquals(1, ((JMethodBody) defaultMethod.getBody()).getBlock().getStatements().size());
  }

  public void testDefaultInterfaceMethodSuperResolution() throws Exception {
    JProgram program = compileSnippet("void", "new DefaultInterfaceImpl2();", false);
    // created by GwtAstBuilder
    JClassType clazz = (JClassType) getType(program, "test.DefaultInterfaceImpl2");
    JMethod defaultMethod = findMethod(clazz, "method2");
    assertNotNull(defaultMethod);
    assertNotNull(defaultMethod.getBody());
    assertEquals("{return super.method2();}",
        formatSource(defaultMethod.getBody().toSource()));
  }

  /**
   * Regression test for issue 9190.
   */
  public void testMethodHandlerLambdaFromDifferentCompilationUnits() throws Exception {
    MockJavaResource interfaceF =
        JavaResourceBase.createMockJavaResource("test.F",
            "package test;",
            "@FunctionalInterface",
            "public interface F {",
            "  boolean eval();",
            "}");
    MockJavaResource classOne =
        JavaResourceBase.createMockJavaResource("test.ClassOne",
            "package test;",
            "public class ClassOne {",
            "  public boolean m() { return true; }",
            "  public static boolean evaluateF(F f) { return f.eval(); }",
            "  public boolean evaluateM() {",
            "    ClassOne a = new ClassOne();",
            "    return evaluateF(this::m);",
            "  }",
            "}");

    String entryPointClass = Joiner.on('\n').join(
        "package test;",
        "public class EntryPoint {",
        "  public static void onModuleLoad() {",
        "    ClassOne classOne = new ClassOne();",
        "    ClassOne.evaluateF(classOne::m);",
        "    new ClassOne().evaluateM();",
        "  }",
        "}");

    addAll(interfaceF, classOne);

    // Compiling the snippet to Javascript makes sure JavaScript generation does not trigger the
    // assertion in {@link GenerateJavaScriptAST.CreateNamesAndScopesVisitor#recordSymbol}.
    //
    // Also just performing the compile asserts that the AST is well formed.
    compileSnippetToJS(entryPointClass);
  }

  @Override
  protected void optimizeJava() {
  }
}
