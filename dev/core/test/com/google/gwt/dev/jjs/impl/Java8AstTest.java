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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.arg.SourceLevel;

/**
 * Tests that {@link com.google.gwt.dev.jjs.impl.GwtAstBuilder} correctly builds the AST for
 * features introduced in Java 8.
 */
public class Java8AstTest extends JJSTestBase {

  @Override
  public void setUp() {
    sourceLevel = SourceLevel.JAVA8;
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
        "public interface Lambda2<String> {",
        "  boolean run(String a, String b);",
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

  public void testCompileStaticReferenceBinding() throws Exception {
    addSnippetClassDecl("public static Integer foo(int x, int y) { return x + y; }");
    String lambda = "new AcceptsLambda<Integer>().accept(EntryPoint::foo);";
    assertEqualBlock(
        "(new AcceptsLambda()).accept(new Lambda$$foo__IILjava_lang_Integer_2$Type());",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "foo"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program,
        "test.Lambda$$foo__IILjava_lang_Integer_2$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, "Lambda$$foo__IILjava_lang_Integer_2$Type");
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
    assertEqualBlock(
        "(new AcceptsLambda()).accept(new Lambda$foo__IILjava_lang_Integer_2$Type(this));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);
    // created by JDT, should exist
    assertNotNull(getMethod(program, "foo"));

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program,
        "test.Lambda$foo__IILjava_lang_Integer_2$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, "Lambda$foo__IILjava_lang_Integer_2$Type");
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

  public void testCompileImplicitQualifierReferenceBinding() throws Exception {
    String lambda = "new AcceptsLambda<String>().accept2(String::equalsIgnoreCase);";
    assertEqualBlock(
        "(new AcceptsLambda()).accept2("
            + "new Lambda2$$equalsIgnoreCase__Ljava_lang_String_2Z$Type());",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program,
        "test.Lambda2$$equalsIgnoreCase__Ljava_lang_String_2Z$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass,
        "Lambda2$$equalsIgnoreCase__Ljava_lang_String_2Z$Type");
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
        "public final boolean run(Object arg0,Object arg1){return arg0.equalsIgnoreCase(arg1);}",
        formatSource(samMethod.toSource()));
  }

  public void testCompileConstructorReferenceBinding() throws Exception {
    String lambda = "new AcceptsLambda<Pojo>().accept(Pojo::new);";
    assertEqualBlock(
        "(new AcceptsLambda()).accept(new Lambda$$Pojo__IIV$Type());",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program, "test.Lambda$$Pojo__IIV$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass, "Lambda$$Pojo__IIV$Type");
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
    addSnippetClassDecl("int field1, field2;");
    addSnippetClassDecl(
        "class Pojo2 {",
        "  public Pojo2(int x, int y) {",
        "  }",
        "  public int someMethod() { ",
        "    return field1 + field2; ",
        "  }",
        "}"
    );

    String lambda = "new AcceptsLambda<Pojo2>().accept(Pojo2::new);";
    assertEqualBlock(
        "(new AcceptsLambda()).accept("
            + "new Lambda$$EntryPoint$Pojo2__Ltest_EntryPoint_2IIV$Type(this));",
        lambda
    );
    JProgram program = compileSnippet("void", lambda, false);

    // created by GwtAstBuilder
    JClassType lambdaInnerClass = (JClassType) getType(program,
        "test.Lambda$$EntryPoint$Pojo2__Ltest_EntryPoint_2IIV$Type");
    assertNotNull(lambdaInnerClass);

    // should have constructor taking this and x
    JMethod ctor = findMethod(lambdaInnerClass,
        "Lambda$$EntryPoint$Pojo2__Ltest_EntryPoint_2IIV$Type");
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

  private static final MockJavaResource LAMBDA_METAFACTORY =
      JavaResourceBase.createMockJavaResource("java.lang.invoke.LambdaMetafactory",
          "package java.lang.invoke;",
          "public class LambdaMetafactory {",
          "}");
}
