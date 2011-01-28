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
package com.google.gwt.dev.javac;

/**
 * Test access to longs from JSNI.
 */
public class JsniCheckerTest extends CheckerTestCase {

  /**
   * JSNI references to anonymous inner classes is deprecated.
   */
  public void testAnoymousJsniRef() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static void main() {\n");
    code.append("    new Object() {\n");
    code.append("      int foo = 3;\n");
    code.append("    };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    o.@Buggy$1::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 8, "Referencing class 'Buggy$1': "
        + "JSNI references to anonymous classes are illegal");
  }

  /**
   * JSNI references to anonymous inner classes is deprecated.
   */
  public void testAnoymousJsniRefNested() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static void main() {\n");
    code.append("    new Object() {\n");
    code.append("      class A {\n");
    code.append("        int foo = 3;\n");
    code.append("      };\n");
    code.append("    };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    o.@Buggy$1.A::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 10, "Referencing class 'Buggy$1.A': "
        + "JSNI references to anonymous classes are illegal");
  }

  public void testArrayBadMember() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy[][]::blah;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing member 'Buggy[][].blah': 'class' is the only legal reference for array types");
  }

  public void testArrayClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy[][]::class;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::class;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testClassAssignment() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::class = null;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 3,
        "Illegal assignment to class literal 'Buggy.class'");
  }

  public void testCyclicReferences() {
    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  static int anint = 3;\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra::along);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  static long along = 3;\n");
      extra.append("  native void jsniMeth() /*-{\n");
      extra.append("    $wnd.alert(@Buggy::anint);\n");
      extra.append("  }-*/;\n");
      extra.append("}\n");

      shouldGenerateError(buggy, extra, 4, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }

    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  Extra anExtra = new Extra();\n");
      buggy.append("  static int anint = 3;\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra::along);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  Buggy mattress = new Buggy();\n");
      extra.append("  static long along = 3;\n");
      extra.append("  native void jsniMeth() /*-{\n");
      extra.append("    $wnd.alert(@Buggy::anint);\n");
      extra.append("  }-*/;\n");
      extra.append("}\n");

      shouldGenerateError(buggy, extra, 5, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }
  }

  public void testDeprecationField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  @Deprecated static int bar;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 4, "Referencing deprecated field 'Buggy.bar'");
  }

  public void testDeprecationMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  @Deprecated static void foo(){}\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 4, "Referencing deprecated method 'Buggy.foo'");
  }

  public void testDeprecationSuppression() {
    StringBuffer code = new StringBuffer();
    code.append("@Deprecated class D {\n");
    code.append("  static int bar;\n");
    code.append("}\n");
    code.append("class Buggy {\n");
    code.append("  @Deprecated static void foo(){}\n");
    code.append("  @Deprecated static int bar;\n");
    code.append("  @SuppressWarnings(\"deprecation\")\n");
    code.append("  native void jsniMethod1() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("    @Buggy::bar;\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("  @SuppressWarnings({\"deprecation\", \"other\"})\n");
    code.append("  native void jsniMethod2() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("    @Buggy::bar;\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    // Check inherited suppress warnings.
    code = new StringBuffer();
    code.append("@Deprecated class D {\n");
    code.append("  static int bar;\n");
    code.append("}\n");
    code.append("@SuppressWarnings(\"deprecation\")\n");
    code.append("class Buggy {\n");
    code.append("  @Deprecated static void foo(){}\n");
    code.append("  @Deprecated static int bar;\n");
    code.append("  native void jsniMethod1() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("    @Buggy::bar;\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("  native void jsniMethod2() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("    @Buggy::bar;\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testDeprecationType() {
    StringBuffer code = new StringBuffer();
    code.append("@Deprecated class D {\n");
    code.append("  static int bar;\n");
    code.append("}\n");
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 6, "Referencing deprecated class 'D'");
  }

  public void testField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    this.@Buggy::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testFieldAccess() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("volatile long x = -1;\n");
    code.append("native void jsniMeth() /*-{\n");
    code.append("  $wnd.alert(\"x is: \"+this.@Buggy::x); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 4,
        "Referencing field 'Buggy.x': type 'long' is not safe to access in JSNI code");
  }

  public void testFieldAssignment() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    this.@Buggy::foo = 4;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testFieldAssignmentStatic() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo = 4;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testFieldConstant() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static final int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testFieldConstantAssignment() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static final int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo = 4;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4,
        "Illegal assignment to compile-time constant 'Buggy.foo'");

    code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static final String foo = \"asdf\";\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo = null;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4,
        "Illegal assignment to compile-time constant 'Buggy.foo'");

    // Not a compile-time constant.
    code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static final Object foo = new Object();\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo = null;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testJsoStaticMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    @com.google.gwt.core.client.JavaScriptObject::createObject()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testJsoInstanceMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    new Object().@com.google.gwt.core.client.JavaScriptObject::toString()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing method 'com.google.gwt.core.client.JavaScriptObject.toString()': references to instance methods in overlay types are illegal");
  }

  public void testJsoInterfaceMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  interface IFoo {\n");
    code.append("    void foo();\n");
    code.append("  }\n");
    code.append("  static final class Foo extends com.google.gwt.core.client.JavaScriptObject implements IFoo{\n");
    code.append("    protected Foo() { };\n");
    code.append("    public void foo() { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    new Object().@Buggy.IFoo::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        10,
        "Referencing interface method 'Buggy.IFoo.foo()': implemented by 'Buggy$Foo'; references to instance methods in overlay types are illegal; use a stronger type or a Java trampoline method");
  }

  public void testJsoSubclassInstanceMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {\n");
    code.append("    protected Foo() { };\n");
    code.append("    void foo() { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    new Object().@Buggy.Foo::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        7,
        "Referencing method 'Buggy.Foo.foo()': references to instance methods in overlay types are illegal");
  }

  public void testJsoSubclassStaticMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {\n");
    code.append("    protected Foo() { };\n");
    code.append("    static void foo() { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    @Buggy.Foo::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testFieldStatic() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testFieldStaticQualified() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    this.@Buggy::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4,
        "Unnecessary qualifier on static field 'Buggy.foo'");
  }

  public void testFieldUnqualified() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  int foo = 3;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4,
        "Missing qualifier on instance field 'Buggy.foo'");
  }

  public void testInnerClass() {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    static long x = 3;\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::x);\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 6, "Referencing field 'Buggy.Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  public void testInnerClassDollar() {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    static long x = 3;\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy$Inner::x);\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 6, "Referencing field 'Buggy$Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  public void testInnerNew() {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("    Inner(boolean b) { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::new(Z)(true).toString());\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    // Cannot resolve, missing synthetic enclosing instance.
    shouldGenerateError(code, 7, "Referencing method 'Buggy.Inner.new(Z)': "
        + "unable to resolve method");

    code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("    Inner(boolean b) { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::new(Z)(this, true).toString());\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("    Inner(boolean b) { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::new(LBuggy;Z)(this, true).toString());\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  /**
   * The proper behavior here is a close call. In Development Mode, Java arrays
   * are completely unusable in JavaScript, so the current reasoning is to allow
   * them.
   */
  public void testLongArray() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long[] m() { return new long[] { -1 }; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testLongParameter() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMeth(long x) /*-{ return; }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 2,
        "Parameter 'x': type 'long' is not safe to access in JSNI code");
  }

  public void testLongReturn() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native long jsniMeth() /*-{ return 0; }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 2,
        "Type 'long' may not be returned from a JSNI method");
  }

  public void testMalformedJsniRef() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 3,
        "Expected \":\" in JSNI reference\n>     @Buggy;\n" + "> ----------^");
  }

  public void testMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void foo() { }\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    this.@Buggy::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testMethodArgument() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void print(long x) { }\n");
    code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        3,
        "Parameter 1 of method 'Buggy.print': type 'long' may not be passed out of JSNI code");
  }

  public void testMethodAssignment() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void foo() { }\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    this.@Buggy::foo() = null;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4, "Illegal assignment to method 'Buggy.foo'");
  }

  /**
   * Test JSNI references to methods defined in superclass/superinterfaces.
   */
  public void testMethodInheritance() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  interface A1 { void a1(); }\n");
    code.append("  interface A2 extends A1 { void a2(); }\n");
    code.append("  static abstract class C1 implements A2 { public abstract void c1(); }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    o.@Buggy.A1::a1()();\n");
    code.append("    o.@Buggy.A2::a1()();\n");
    code.append("    o.@Buggy.A2::a2()();\n");
    code.append("    o.@Buggy.C1::a1()();\n");
    code.append("    o.@Buggy.C1::a2()();\n");
    code.append("    o.@Buggy.C1::c1()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateNoWarning(code);
  }

  public void testMethodReturn() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m() { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        4,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testMethodStatic() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static void foo() { }\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testMethodStaticQualified() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static void foo() { }\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    this.@Buggy::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4,
        "Unnecessary qualifier on static method 'Buggy.foo'");
  }

  public void testMethodUnqualified() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void foo() { }\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 4,
        "Missing qualifier on instance method 'Buggy.foo'");
  }

  public void testNew() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @Buggy::new()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  Buggy(boolean b) { }\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @Buggy::new(Z)(true);\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testNullField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @null::nullField;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @null::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing field 'null.foo': 'nullField' is the only legal field reference for 'null'");
  }

  public void testNullMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @null::nullMethod()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @null::foo()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing method 'null.foo()': 'nullMethod()' is the only legal method for 'null'");
  }

  public void testOverloadedMethodWithNoWarning() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m(int x) { return -1; }\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(Ljava/lang/String;)(\"hello\")); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testOverloadedMethodWithWarning() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m(int x) { return -1; }\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(I)(10)); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        5,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testPrimitiveBadMember() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @boolean::blah;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing member 'boolean.blah': 'class' is the only legal reference for primitive types");
  }

  public void testPrimitiveClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @boolean::class;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testPrimitiveClassDeprecated() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Z::class;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateWarning(code, 3,
        "Referencing primitive type 'Z': this is deprecated, use 'boolean' instead");
  }

  public void testRefInString() {
    {
      StringBuffer code = new StringBuffer();
      code.append("import com.google.gwt.core.client.UnsafeNativeLong;");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  native void jsniMeth() /*-{ 'this.@Buggy::print(J)(0)'; }-*/;\n");
      code.append("}\n");

      shouldGenerateNoError(code);
    }
  }

  public void testUnresolvedClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Foo::x;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 3,
        "Referencing class 'Foo': unable to resolve class");
  }

  public void testUnresolvedField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::x;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 3,
        "Referencing field 'Buggy.x': unable to resolve field");
  }

  public void testUnresolvedMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::x(Ljava/lang/String);\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 3,
        "Referencing method 'Buggy.x(Ljava/lang/String)': unable to resolve method");
  }

  public void testUnsafeAnnotation() {
    {
      StringBuffer code = new StringBuffer();
      code.append("import com.google.gwt.core.client.UnsafeNativeLong;");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  @UnsafeNativeLong\n");
      code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
      code.append("}\n");

      shouldGenerateNoError(code);
    }
  }

  public void testViolator() {
    {
      StringBuffer okay = new StringBuffer();
      okay.append("class Buggy {\n");
      okay.append("  native void jsniMeth() /*-{\n");
      okay.append("    $wnd.alert(@Extra.Inner::x);\n");
      okay.append("  }-*/;\n");
      okay.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  private static class Inner { \n");
      extra.append("    private static int x = 3;\n");
      extra.append("  }\n");
      extra.append("}\n");

      shouldGenerateNoError(okay, extra);
    }

    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra.Inner::x);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  private static class Inner { \n");
      extra.append("    private static long x = 3;\n");
      extra.append("  }\n");
      extra.append("}\n");

      shouldGenerateError(
          buggy,
          extra,
          3,
          "Referencing field 'Extra.Inner.x': type 'long' is not safe to access in JSNI code");
    }
  }

  public void testWildcardRef() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(*)(\"hello\")); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }
}
