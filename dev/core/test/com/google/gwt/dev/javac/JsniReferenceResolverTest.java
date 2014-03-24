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

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;

/**
 * Test access to longs from JSNI.
 */
public class JsniReferenceResolverTest extends CheckerTestCase {

  /**
   * JSNI references to anonymous inner classes is deprecated.
   */
  public void testAnoymousJsniRef() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
      "class Buggy {",
      "  static void main() {",
      "    new Object() {",
      "      int foo = 3;",
      "    };",
      "  }",
      "  native void jsniMeth(Object o) /*-{",
      "    o.@Buggy$1::foo;",
      "  }-*/;",
      "}");
    shouldGenerateError(buggy, 8, "Referencing class 'Buggy$1': "
        + "unable to resolve class");
  }

  /**
   * JSNI references to anonymous inner classes is deprecated.
   */
  public void testAnoymousJsniRefNested() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static void main() {",
       "    new Object() {",
       "      class A {",
       "        int foo = 3;",
       "      };",
       "    };",
       "  }",
       "  native void jsniMeth(Object o) /*-{",
       "    o.@Buggy$1.A::foo;",
       "  }-*/;",
       "}");

    shouldGenerateError(buggy, 10, "Referencing class 'Buggy$1.A': "
        + "unable to resolve class");
  }

  public void testArrayBadMember() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy[][]::blah;",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        3,
        "Referencing member 'Buggy[][].blah': 'class' is the only legal reference for arrays and " +
        "primitive types");
  }

  public void testArrayClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy[][]::class;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy::class;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testClassAssignment() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy::class = null;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 3,
        "Illegal assignment to class literal 'Buggy.class'");
  }

  public void testCyclicReferences() {
    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
          "class Buggy {",
          "  static int anint = 3;",
          "  native void jsniMeth() /*-{",
          "    $wnd.alert(@Extra::along);",
          "  }-*/;",
          "}");

      MockJavaResource extra = JavaResourceBase.createMockJavaResource("Extra",
          "class Extra {",
          "  static long along = 3;",
          "  native void jsniMeth() /*-{",
          "    $wnd.alert(@Buggy::anint);",
          "  }-*/;",
          "}");

      shouldGenerateError(buggy, extra, 4, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }

    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
          "class Buggy {",
          "  Extra anExtra = new Extra();",
          "  static int anint = 3;",
          "  native void jsniMeth() /*-{",
          "    $wnd.alert(@Extra::along);",
          "  }-*/;",
          "}");

      MockJavaResource extra = JavaResourceBase.createMockJavaResource("Extra",
          "class Extra {",
          "  Buggy mattress = new Buggy();",
          "  static long along = 3;",
          "  native void jsniMeth() /*-{",
          "    $wnd.alert(@Buggy::anint);",
          "  }-*/;",
          "}");

      shouldGenerateError(buggy, extra, 5, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }
  }

  public void testDeprecationField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  @Deprecated static int bar;",
       "  native void jsniMethod() /*-{",
       "    @Buggy::bar;",
       "  }-*/;",
       "}");

    shouldGenerateWarning(buggy, 4,
        "Referencing field 'Buggy.bar': field 'Buggy.bar' is deprecated");
  }

  public void testDeprecationMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  @Deprecated static void foo(){}",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo();",
       "  }-*/;",
       "}");

    shouldGenerateWarning(buggy, 4,
        "Referencing method 'Buggy.foo': method 'Buggy.foo()' is deprecated");
  }

  public void testDeprecationSuppression() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "@Deprecated class D {",
       "  static int bar;",
       "}",
       "class Buggy {",
       "  @Deprecated static void foo(){}",
       "  @Deprecated static int bar;",
       "  @SuppressWarnings(\"deprecation\")",
       "  native void jsniMethod1() /*-{",
       "    @Buggy::foo();",
       "    @Buggy::bar;",
       "    @D::bar;",
       "  }-*/;",
       "  @SuppressWarnings({\"deprecation\", \"other\"})",
       "  native void jsniMethod2() /*-{",
       "    @Buggy::foo();",
       "    @Buggy::bar;",
       "    @D::bar;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);

    // Check inherited suppress warnings.
    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "@Deprecated class D {",
       "  static int bar;",
       "}",
       "@SuppressWarnings(\"deprecation\")",
       "class Buggy {",
       "  @Deprecated static void foo(){}",
       "  @Deprecated static int bar;",
       "  native void jsniMethod1() /*-{",
       "    @Buggy::foo();",
       "    @Buggy::bar;",
       "    @D::bar;",
       "  }-*/;",
       "  native void jsniMethod2() /*-{",
       "    @Buggy::foo();",
       "    @Buggy::bar;",
       "    @D::bar;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  /**
   * Test for issue 8093.
   */
  public void testBadSuppression1() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  private static final String RAWTYPES = \"rawtypes\";",
       "  @SuppressWarnings(RAWTYPES)",
       "  public void method1() {",
       "  }",
       "}");
    shouldGenerateWarning(buggy, 3,
        "Unable to analyze SuppressWarnings annotation, RAWTYPES not a string constant.");
  }

  /**
   * Test for issue 8093.
   */
  public void testBadSuppression2() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  private static final String UNCHECKED = \"unchecked\";",
       "  @SuppressWarnings({\"rawtypes\", UNCHECKED})",
       "  public void method1() {",
       "  }",
       "}");
    shouldGenerateWarning(buggy, 3,
        "Unable to analyze SuppressWarnings annotation, UNCHECKED not a string constant.");
  }

  public void testDeprecationType() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "@Deprecated class D {",
       "  static int bar;",
       "}",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @D::bar;",
       "  }-*/;",
       "}");

    shouldGenerateWarning(buggy, 6, "Referencing deprecated class 'D'");
  }

  public void testField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testFieldAccess() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "volatile long x = -1;",
       "native void jsniMeth() /*-{",
       "  $wnd.alert(\"x is: \"+this.@Buggy::x); }-*/;",
       "}");

    shouldGenerateError(buggy, 4,
        "Referencing field 'Buggy.x': type 'long' is not safe to access in JSNI code");
  }

  public void testFieldAssignment() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo = 4;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testFieldAssignmentStatic() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo = 4;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testFieldConstant() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static final int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testFieldConstantAssignment() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static final int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo = 4;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4,
        "Illegal assignment to compile-time constant 'Buggy.foo'");

    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static final String foo = \"asdf\";",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo = null;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4,
        "Illegal assignment to compile-time constant 'Buggy.foo'");

    // Not a compile-time constant.
    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static final Object foo = new Object();",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo = null;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testJsoStaticMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMeth(Object o) /*-{",
       "    @com.google.gwt.core.client.JavaScriptObject::createObject()();",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testJsoInstanceMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMeth(Object o) /*-{",
       "    new Object().@com.google.gwt.core.client.JavaScriptObject::toString()();",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        3,
        "Referencing method 'com.google.gwt.core.client.JavaScriptObject.toString()': references to instance methods in overlay types are illegal");
  }

  public void testJsoInterfaceMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  interface IFoo {",
       "    void foo();",
       "  }",
       "  static final class Foo extends com.google.gwt.core.client.JavaScriptObject implements IFoo{",
       "    protected Foo() { };",
       "    public void foo() { };",
       "  }",
       "  native void jsniMeth(Object o) /*-{",
       "    new Object().@Buggy.IFoo::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        10,
        "Referencing interface method 'Buggy.IFoo.foo()': implemented by 'Buggy$Foo'; references to instance methods in overlay types are illegal; use a stronger type or a Java trampoline method");
  }

  public void testJsoSubclassInstanceMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {",
       "    protected Foo() { };",
       "    void foo() { };",
       "  }",
       "  native void jsniMeth(Object o) /*-{",
       "    new Object().@Buggy.Foo::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        7,
        "Referencing method 'Buggy.Foo.foo()': references to instance methods in overlay types are illegal");
  }

  public void testJsoSubclassStaticMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {",
       "    protected Foo() { };",
       "    static void foo() { };",
       "  }",
       "  native void jsniMeth(Object o) /*-{",
       "    @Buggy.Foo::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testFieldStatic() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testFieldStaticQualified() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4,
        "Unnecessary qualifier on static field 'Buggy.foo'");
  }

  public void testFieldUnqualified() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4,
        "Missing qualifier on instance field 'Buggy.foo'");
  }

  public void testEnclosingClassField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
       "package some;",
       "class Buggy {",
       "  int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testEnclosingClassFieldNotFound() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
       "package some;",
       "class Buggy {",
       "  int foo = 3;",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::bar;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 5,
        "Referencing field 'Buggy.bar': unable to resolve field in class 'some.Buggy'");
  }

  public void testImportedClassField_PartialMatch() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "import other.pack.OtherPackageClass;",
      "class Buggy {",
      "  native void jsniMethod() /*-{",
      "    this.@PackageClass::f;",
      "  }-*/;",
      "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("other.pack.OtherPackageClass",
          "package other.pack;",
          "public class OtherPackageClass {",
          "  public int f;",
          "}");

      shouldGenerateError(
          buggy,
          otherPackageClass,
          5,
          "Referencing class 'PackageClass': unable to resolve class");
  }

  public void testImportedClassField_Precedence() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
        "package some;",
        "import other.pack.B;",
        "class Buggy {",
        "  class B {",
        "    int f;",
        "  }",
        "  native void jsniMethod() /*-{",
        "    this.@B::f;",
        "  }-*/;",
        "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("other.pack.B",
            "package other.pack;",
            "public class B {",
            "}");

    shouldGenerateNoError(buggy, otherPackageClass);
  }

  public void testImportedClassField_InnerClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "import other.pack.OtherPackageClass.Inner;",
      "class Buggy {",
      "  native void jsniMethod() /*-{",
      "    this.@Inner::f;",
      "  }-*/;",
      "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("other.pack.OtherPackageClass",
          "package other.pack;",
          "public class OtherPackageClass {",
          "  public class Inner {",
          "    public int f;",
          "  }",
          "}");

      shouldGenerateNoWarning(buggy, otherPackageClass);
  }

  public void testImportedClassField_InnerClassThroughOuter() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "import other.pack.OtherPackageClass;",
      "class Buggy {",
      "  native void jsniMethod() /*-{",
      "    this.@OtherPackageClass.Inner::f;",
      "  }-*/;",
      "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("other.pack.OtherPackageClass",
          "package other.pack;",
          "public class OtherPackageClass {",
          "  public class Inner {",
          "    public int f;",
          "  }",
          "}");

      shouldGenerateNoWarning(buggy, otherPackageClass);
  }

  public void testImportedClassField_InnerClassTest1() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "class Buggy {",
      "  public class Inner {",
      "    public int f;",
      "  }",
      "  native void jsniMethod() /*-{",
      "    this.@Inner::f;",
      "    this.@Buggy.Inner::f;",
      "    this.@some.Buggy.Inner::f;",
      "  }-*/;",
      "}");

      shouldGenerateNoWarning(buggy);
  }

  public void testImportedClassField_InnerClassTest2() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "class Buggy {",
      "  public class Inner {",
      "    public int f;",
      "    native void jsniMethod() /*-{",
      "      this.@Inner::f;",
      "      this.@Buggy.Inner::f;",
      "      this.@some.Buggy.Inner::f;",
      "    }-*/;",
      "  }",
      "}");

      shouldGenerateNoWarning(buggy);
  }

  public void testImportedClassField_InnerClassTest3() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "class Buggy {",
      "  public class OtherInner {",
      "    native void jsniMethod() /*-{",
      "      this.@Inner::f;",
      "      this.@Buggy.Inner::f;",
      "      this.@some.Buggy.Inner::f;",
      "    }-*/;",
      "  }",
      "  public class Inner {",
      "    public int f;",
      "  }",
      "}");

      shouldGenerateNoWarning(buggy);
  }

  public void testImportedClassField_InnerClassTest4() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "class Buggy {",
      "  public class OtherInner {",
      "    public class Inner {",
      "      public int f;",
      "    }",
      "    native void jsniMethod() /*-{",
      "      this.@OtherInner.Inner::f;",
      "      this.@Buggy.OtherInner.Inner::f;",
      "      this.@some.Buggy.OtherInner.Inner::f;",
      "    }-*/;",
      "  }",
      "}");

      shouldGenerateNoWarning(buggy);
  }

  public void testImportedClassField_InnerClassTest5() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "class Buggy {",
      "  public class OtherInner {",
      "    public class Inner {",
      "      public int f;",
      "    }",
      "    native void jsniMethod() /*-{",
      "      this.@Inner::f;",
      "    }-*/;",
      "  }",
      "}");

    shouldGenerateNoWarning(buggy);
  }

  public void testImplicitImport() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
        "package some;",
        "class Buggy {",
        "  native void jsniMethod(String s) /*-{",
        "    s.@String::length()();",
        "  }-*/;",
        "}");


    shouldGenerateNoError(buggy);
  }


  public void testImportedClassField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
      "package some;",
      "import other.pack.OtherPackageClass;",
      "class Buggy {",
      "  native void jsniMethod() /*-{",
      "    this.@OtherPackageClass::f;",
      "  }-*/;",
      "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("other.pack.OtherPackageClass",
          "package other.pack;",
          "public class OtherPackageClass {",
          "  public int f;",
          "}");

    shouldGenerateNoWarning(buggy, otherPackageClass);
  }

  public void testImportedClassField_CurrentPackage() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
        "package some;",
        "class Buggy {",
        "  native void jsniMethod() /*-{",
        "    this.@SamePackageClass::f;",
        "  }-*/;",
        "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("some.SamePackageClass",
            "package some;",
            "public class SamePackageClass {",
            "  public int f;",
            "}");

    shouldGenerateNoWarning(buggy, otherPackageClass);
  }

  public void testImportedStarClassField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
        "package some;",
        "import other.pack.*;",
        "class Buggy {",
        "  native void jsniMethod() /*-{",
        "    this.@OtherPackageClass::f;",
        "  }-*/;",
        "}");

    MockJavaResource otherPackageClass =
        JavaResourceBase.createMockJavaResource("other.pack.OtherPackageClass",
            "package other.pack;",
            "public class OtherPackageClass {",
            "  public int f;",
            "}");

    shouldGenerateNoWarning(buggy, otherPackageClass);
  }

  public void testInnerClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  static class Inner {",
       "    static long x = 3;",
       "  }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(@Buggy.Inner::x);",
       "  }-*/;",
       "}");

    shouldGenerateError(buggy, 6, "Referencing field 'Buggy.Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  public void testInnerClassDollar() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  static class Inner {",
       "    static long x = 3;",
       "  }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(@Buggy$Inner::x);",
       "  }-*/;",
       "}");

    shouldGenerateError(buggy, 6, "Referencing field 'Buggy$Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  public void testInnerNew() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  class Inner {",
       "    long x = 3;",
       "    Inner(boolean b) { };",
       "  }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(@Buggy.Inner::new(Z)(true).toString());",
       "  }-*/;",
       "}");

    // Cannot resolve, missing synthetic enclosing instance.
    shouldGenerateError(buggy, 7, "Referencing method 'Buggy.Inner.new(Z)': "
        + "unable to resolve method in class 'Buggy.Inner'");

    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  static class Inner {",
       "    long x = 3;",
       "    Inner(boolean b) { };",
       "  }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(@Buggy.Inner::new(Z)(this, true).toString());",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);

    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "public class Buggy {",
       "  class Inner {",
       "    long x = 3;",
       "    Inner(boolean b) { };",
       "  }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(@Buggy.Inner::new(LBuggy;Z)(this, true).toString());",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  /**
   * The proper behavior here is a close call. In Development Mode, Java arrays
   * are completely unusable in JavaScript, so the current reasoning is to allow
   * them.
   */
  public void testLongArray() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  long[] m() { return new long[] { -1 }; }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(this.@Buggy::m()()); }-*/;",
       "}");

    shouldGenerateNoError(buggy);
  }

  public void testLongParameter() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMeth(long x) /*-{ return; }-*/;",
       "}");

    shouldGenerateError(buggy, 2,
        "Parameter 'x': type 'long' is not safe to access in JSNI code");
  }

  public void testLongReturn() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native long jsniMeth() /*-{ return 0; }-*/;",
       "}");

    shouldGenerateError(buggy, 2,
        "Type 'long' may not be returned from a JSNI method");
  }

  public void testMalformedJsniRef() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 3,
        "Expected \":\" in JSNI reference\n>     @Buggy;\n" + "> ----------^");
  }

  public void testMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  void foo() { }",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testMethodArgument() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  void print(long x) { }",
       "  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;",
       "}");

    shouldGenerateError(
        buggy,
        3,
        "Parameter 1 of method 'Buggy.print': type 'long' may not be passed out of JSNI code");
  }

  public void testMethodAssignment() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  void foo() { }",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo() = null;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4, "Illegal assignment to method 'Buggy.foo'");
  }

  /**
   * Test JSNI references to methods defined in superclass/superinterfaces.
   */
  public void testMethodInheritance() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  interface A1 { void a1(); }",
       "  interface A2 extends A1 { void a2(); }",
       "  static abstract class C1 implements A2 { public abstract void c1(); }",
       "  native void jsniMeth(Object o) /*-{",
       "    o.@Buggy.A1::a1()();",
       "    o.@Buggy.A2::a1()();",
       "    o.@Buggy.A2::a2()();",
       "    o.@Buggy.C1::a1()();",
       "    o.@Buggy.C1::a2()();",
       "    o.@Buggy.C1::c1()();",
       "  }-*/;",
       "}");

    shouldGenerateNoWarning(buggy);
  }

  public void testMethodReturn() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  long m() { return -1; }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(this.@Buggy::m()()); }-*/;",
       "}");

    shouldGenerateError(
        buggy,
        4,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testMethodStatic() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static void foo() { }",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testMethodStaticQualified() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static void foo() { }",
       "  native void jsniMethod() /*-{",
       "    this.@Buggy::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4,
        "Unnecessary qualifier on static method 'Buggy.foo'");
  }

  public void testMethodUnqualified() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  void foo() { }",
       "  native void jsniMethod() /*-{",
       "    @Buggy::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 4,
        "Missing qualifier on instance method 'Buggy.foo'");
  }

  public void testNew() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static native Object main() /*-{",
       "    return @Buggy::new()();",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);

    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  Buggy(boolean b) { }",
       "  static native Object main() /*-{",
       "    return @Buggy::new(Z)(true);",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testNullField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static native Object main() /*-{",
       "    return @null::nullField;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);

    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static native Object main() /*-{",
       "    return @null::foo;",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        3,
        "Referencing field 'null.foo': 'nullField' is the only legal field reference for 'null'");
  }

  public void testNullMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static native Object main() /*-{",
       "    return @null::nullMethod()();",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);

    buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  static native Object main() /*-{",
       "    return @null::foo()();",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        3,
        "Referencing method 'null.foo()': 'nullMethod()' is the only legal method for 'null'");
  }

  public void testOverloadedMethodWithNoWarning() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  long m(int x) { return -1; }",
       "  int m(String x) { return -1; }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(this.@Buggy::m(Ljava/lang/String;)(\"hello\")); }-*/;",
       "}");

    shouldGenerateNoError(buggy);
  }

  public void testOverloadedMethodWithWarning() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  long m(int x) { return -1; }",
       "  int m(String x) { return -1; }",
       "  native void jsniMeth() /*-{",
       "    $wnd.alert(this.@Buggy::m(I)(10)); }-*/;",
       "}");

    shouldGenerateError(
        buggy,
        5,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testPrimitiveBadMember() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @boolean::blah;",
       "  }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        3,
        "Referencing member 'boolean.blah': "
            + "'class' is the only legal reference for arrays and primitive types");
  }

  public void testPrimitiveClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @boolean::class;",
       "  }-*/;",
       "}");
    shouldGenerateNoWarning(buggy);
  }

  public void testPrimitiveClassRemovedDeprecated() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Z::class;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 3,
        "Referencing class 'Z': unable to resolve class");
  }

  public void testRefInString() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "import com.google.gwt.core.client.UnsafeNativeLong;",
       "class Buggy {",
       "  void print(long x) { }",
       "  native void jsniMeth() /*-{ 'this.@Buggy::print(J)(0)'; }-*/;",
       "}");

    shouldGenerateNoError(buggy);
  }

  public void testAmbiguityResolution_NestedClasses() {
    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.A",
          "package some;",
          "class A {",
          "  static Object f;",
          "  static class B {",
          "    static Object f;",
          "    native void jsniMethod() /*-{",
          "      @CC::f;",
          "    }-*/;",
          "    static class CC {",
          "      static Object f;",
          "    }",
          "  }",
          "  static class B1 {",
          "    static Object f;",
          "    static class CC {",
          "    }",
          "  }",
          "}");

      shouldGenerateNoWarning(buggy);
    }

    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.A",
          "package some;",
          "class A {",
          "  static Object f;",
          "  static class B {",
          "    static Object f;",
          "    native void jsniMethod() /*-{",
          "      @CC::f;",
          "    }-*/;",
          "    static class CC {",
          "    }",
          "  }",
          "  static class B1 {",
          "    static Object f;",
          "    static class CC {",
          "      static Object f;",
          "    }",
          "  }",
          "}");

      // Note that the ambiguous CC is resolved to some.A.B.CC and not to A.B1.CC.
      shouldGenerateError(buggy, 7 ,
          "Referencing field 'CC.f': unable to resolve field in class 'some.A.B.CC'");
    }
  }

  public void testSuperFieldAccess() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::x; ",
        "   }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  public long x = -1;",
        "}");

    shouldGenerateError(buggy, extra,
        3,
        "Referencing field 'Buggy.x': unable to resolve field in class 'Buggy'");
  }

  public void testUnresolvedClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Foo::x;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 3,
        "Referencing class 'Foo': unable to resolve class");
  }

  public void testUnresolvedField() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy::x;",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 3,
        "Referencing field 'Buggy.x': unable to resolve field in class 'Buggy'");
  }

  public void testUnresolvedMethod() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  native void jsniMethod() /*-{",
       "    @Buggy::x(Ljava/lang/String);",
       "  }-*/;",
       "}");
    shouldGenerateError(buggy, 3, "Referencing method 'Buggy.x(Ljava/lang/String)': "
        + "unable to resolve method in class 'Buggy'");
  }

  public void testUnsafeAnnotation() {
    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
         "import com.google.gwt.core.client.UnsafeNativeLong;",
         "class Buggy {",
         "  void print(long x) { }",
         "  @UnsafeNativeLong",
         "  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;",
         "}");

      shouldGenerateNoError(buggy);
    }
  }

  public void testViolator() {
    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
         "class Buggy {",
         "  native void jsniMeth() /*-{",
         "    $wnd.alert(@Extra.Inner::x);",
         "  }-*/;",
         "}");

      MockJavaResource extra = JavaResourceBase.createMockJavaResource("Extra",
          "class Extra {",
          "  private static class Inner { ",
          "    private static int x = 3;",
          "  }",
          "}");

      shouldGenerateNoError(buggy, extra);
    }

    {
      MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
          "class Buggy {",
          "  native void jsniMeth() /*-{",
          "    $wnd.alert(@Extra.Inner::x);",
          "  }-*/;",
          "}");

      MockJavaResource extra = JavaResourceBase.createMockJavaResource("Extra",
          "class Extra {",
          "  private static class Inner { ",
          "    private static long x = 3;",
          "  }",
          "}");

      shouldGenerateError(
          buggy,
          extra,
          3,
          "Referencing field 'Extra.Inner.x': type 'long' is not safe to access in JSNI code");
    }
  }

  public void testWildcardMethodAccess() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  int m(String x) { return -1; }",
       "  native void jsniMeth() /*-{",
       "    this.@Buggy::m(*)(\"hello\"); }-*/;",
       "}");

    shouldGenerateNoError(buggy);
  }

  public void testWilcardMethodAccess_Ambiguous_withinClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy {",
       "  int m(String x) { return -1; }",
       "  int m(Integer x) { return -1; }",
       "  native void jsniMeth() /*-{",
       "    this.@Buggy::m(*)(\"hello\"); }-*/;",
       "}");
    shouldGenerateError(
        buggy,
        5,
        "Referencing method 'Buggy.m(*)': ambiguous wildcard match; "
            + "both 'Buggy.m(Ljava/lang/String;)' and "
            + "'Buggy.m(Ljava/lang/Integer;)' match");
  }

  public void testWildcardMethodAccess_Ambiguous_withSuperClass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy extends Extra{",
       "  public int m(String x) { return -1; }",
       "  native void jsniMeth() /*-{",
       "    this.@Buggy::m(*)(\"hello\"); }-*/;",
       "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Extra",
        "class Extra {",
        "  public int m(Integer x) { return -1; }",
        "}");

    shouldGenerateError(
        buggy,
        extra,
        4,
        "Referencing method 'Buggy.m(*)': ambiguous wildcard match; "
          + "both 'public Buggy.m(Ljava/lang/String;)' and "
          + "'public Extra.m(Ljava/lang/Integer;)' match");
  }

  public void testWildcardMethodAccess_NoConflict_OverrideFromSuperclass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
       "class Buggy extends Extra {",
       "  public int m(String x) { return -1; }",
       "  native void jsniMeth() /*-{",
       "    this.@Buggy::m(*)(\"hello\"); }-*/;",
       "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Extra",
        "class Extra {",
        "  public int m(String x) { return -1; }",
        "}");

    shouldGenerateNoError(buggy, extra);
  }

  public void testWildcardMethodAccess_Conflict_WithSuperclass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  protected int m(String x) { return -1; }",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  public int m(Object x) { return -1; }",
        "}");

    shouldGenerateError(buggy, extra,
        4,
        "Referencing method 'Buggy.m(*)': ambiguous wildcard match; "
            + "both 'protected Buggy.m(Ljava/lang/String;)' and "
            + "'public Super.m(Ljava/lang/Object;)' match");
  }

  public void testWildcardMethodAccess_Conflict_PrivateWithSuperclass() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  private int m(String x) { return -1; }",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  public int m(Object x) { return -1; }",
        "}");

    shouldGenerateError(buggy, extra,
        4,
        "Referencing method 'Buggy.m(*)': ambiguous wildcard match; "
            + "both 'private Buggy.m(Ljava/lang/String;)' and "
            + "'public Super.m(Ljava/lang/Object;)' match");
  }

  public void testWildcardSuperclassMethod_PackagePrivate_DifferentPackage() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
        "package some;",
        "import other.Super;",
        "class Buggy extends Super {",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("other.Super",
        "package other;",
        "public class Super {",
        "  int m(Object x) { return -1; }",
        "}");

    shouldGenerateError(buggy, extra,
        5,
        "Referencing method 'Buggy.m(*)': unable to resolve method in class 'some.Buggy'");
  }

  public void testWildcardSuperclassMethod_PackagePrivate_SamePackage() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  int m(Object x) { return -1; }",
        "}");

    shouldGenerateNoError(buggy, extra);
  }

  public void testWildcardSuperclassMethod_Private() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  private int m(Object x) { return -1; }",
        "}");

    shouldGenerateError(buggy, extra,
        3,
        "Referencing method 'Buggy.m(*)': unable to resolve method in class 'Buggy'");
  }

  public void testWildcardSuperclassMethod_Protected() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.Buggy",
        "package some;",
        "import other.Super;",
        "class Buggy extends Super {",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("other.Super",
        "package other;",
        "public class Super {",
        "  protected int m(Object x) { return -1; }",
        "}");

    shouldGenerateNoError(buggy, extra);
  }

  public void testWildcardSuperclassMethod_Public() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  public int m(Object x) { return -1; }",
        "}");

    shouldGenerateNoError(buggy, extra);
  }

  public void testWildcardMethodAccess_PotentialConflict() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("Buggy",
        "class Buggy extends Super {",
        "  int m(String x) { return -1; }",
        "  native void jsniMeth() /*-{",
        "    this.@Buggy::m(*)(\"hello\"); }-*/;",
        "}");

    MockJavaResource extra = JavaResourceBase.createMockJavaResource("Super",
        "class Super {",
        "  private int m(Object x) { return -1; }",
        "}");

    shouldGenerateNoError(buggy, extra);
  }
}
