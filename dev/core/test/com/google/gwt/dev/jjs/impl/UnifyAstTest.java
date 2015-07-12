/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.arg.SourceLevel;

/**
 * Test for {@link UnifyAst}.
 */
public class UnifyAstTest extends OptimizerTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sourceLevel = SourceLevel.JAVA8;
  }

  public void testPackageInfo_defaultPackagePresent() throws Exception {
    final MockJavaResource packageInfo =
        JavaResourceBase.createMockJavaResource("package-info");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A {",
            "}");

    addAll(packageInfo, A);
    Result result = optimize("void", "");

    assertNotNull(result.findClass("package-info"));
  }

  public void testPackageInfo_defaultPackageAbsent() throws Exception {
    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A {",
            "}");

    addAll(A);
    Result result = optimize("void", "");

    assertNull(result.findClass("package-info"));
  }

  public void testOverrides_base() throws Exception {
    addAll(A_A, A_I, A_J, A_B, B_C, B_D, B_E);
    Result result = optimize("void", "");

    assertOverrides(result, "a.B.m()V", "a.I.m()V", "a.A.m()V");
    assertOverrides(result, "a.B.m1()La/A;", "a.I.m1()La/A;", "a.A.m1()La/A;");
    assertOverrides(result, "a.B.pp()V", "a.I.pp()V", "a.A.pp()V");
    assertOverrides(result, "b.C.m1()La/A;", "a.A.m1()La/A;");

    // The method dispatched for b.C.m()V is actually a.A,m()V.
    // no artificial forwarding method was inserted.
    assertEquals("a.A.m()V", findMethod(result, "b.C.m()V").toString());
    // and for and b.D.m()V is actually a.B,m()V.
    assertEquals("a.B.m()V", findMethod(result, "b.D.m()V").toString());
  }

  public void testOverrides_differentReturnTypes() throws Exception {
    addAll(A_A, A_I, A_J, A_B, B_C, B_D, B_E);
    Result result = optimize("void", "");

    assertOverrides(result, "a.B.m1()La/A;", "a.I.m1()La/A;" ,"a.A.m1()La/A;");
    assertOverrides(result, "a.A.m1()La/A;");
    assertOverrides(result, "b.C.m1()La/A;", "a.A.m1()La/A;");
    assertOverrides(result, "b.D.m1()La/A;", "a.I.m1()La/A;", "a.A.m1()La/A;", "a.B.m1()La/A;");
    assertOverrides(result, "b.C.m1()Lb/C;");
    assertOverrides(result, "b.D.m1()Lb/D;");
    assertOverrides(result, "b.E.m1()Lb/C;", "b.C.m1()Lb/C;");
    assertOverrides(result, "b.E.m1()Lb/E;");
  }

  public void testOverrides_packagePrivate() throws Exception {
    addAll(A_A, A_I, A_J, A_B, B_C, B_D, B_E);
    Result result = optimize("void", "");

    assertOverrides(result, "a.B.pp()V", "a.A.pp()V", "a.I.pp()V");
    assertOverrides(result, "a.A.pp()V");
    assertOverrides(result, "a.I.pp()V");
    assertOverrides(result, "a.J.pp()V", "a.I.pp()V");
    assertOverrides(result, "b.C.pp()V");
    assertEquals("a.B.pp()V", findMethod(result, "b.D.pp()V").toString());
  }

  /**
   * There are three main scenarios in accidental overrides:
   * <ul>
   *   <li>
   *    (1) an interface that extends two or more different interfaces that declare or inherit a
   *        method with the same signature,
   *   </li>
   *   <li>
   *    (2) an a class that accidentally implements a method of an interface, and
   *   </li>
   *   <li>
   *    (3) an a abstract class that accidentally declares a method of an interface.
   *   </li>
   * </ul>
   */
  public void testAccidentalOverrides_interfaceCase() throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  void m();",
            "}");

    final MockJavaResource I2 =
        JavaResourceBase.createMockJavaResource("I2",
            "public interface I2 {",
            "  void m();",
            "}");

    final MockJavaResource I3 =
        JavaResourceBase.createMockJavaResource("I3",
            "public interface I3 extends I1, I2 {",
            "}");

    addAll(I1, I2, I3);
    Result result = optimize("void", "");
    JMethod I3_m = findMethod(result, "I3.m()V");
    assertTrue(I3_m.isAbstract());
    assertOverrides(result, "I3.m()V", "I1.m()V", "I2.m()V");
  }

  public void testAccidentalOverrides_abstractClassCase() throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  void m();",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public abstract class A {",
            "  public abstract void m();",
            "}");

    final MockJavaResource B =
        JavaResourceBase.createMockJavaResource("B",
            "public abstract class B extends A {",
            "}");

    final MockJavaResource C1 =
        JavaResourceBase.createMockJavaResource("C1",
            "public abstract class C1 implements I11 {",
            "}");

    final MockJavaResource C2 =
        JavaResourceBase.createMockJavaResource("C2",
            "public abstract class C2 extends B implements I11 {",
            "}");

    addAll(I1, I11, A, B, C1, C2);
    Result result = optimize("void", "");
    JMethod C1_m = findMethod(result, "C1.m()V");
    assertTrue(C1_m.isAbstract());
    assertOverrides(result, "C1.m()V", "I1.m()V");

    JMethod C2_m = findMethod(result, "C2.m()V");
    assertTrue(C2_m.isAbstract());
    assertOverrides(result, "C2.m()V", "A.m()V", "I1.m()V");
  }

  /**
   * If the method in the superclass is abstract and package private, the stub needs to be marked
   * as an override. (If the method was concrete and package private then a compiler error will
   * be emitted by JDT).
   */
  public void testAccidentalOverrides_abstractClassPackagePrivateMethod()
      throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  void m();",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public abstract class A {",
            "  abstract void m(); // m() is package private.",
            "}");

    final MockJavaResource B =
        JavaResourceBase.createMockJavaResource("B",
            "public abstract class B extends A {",
            "}");

    // C exposes package private method A.m()V.
    final MockJavaResource C =
        JavaResourceBase.createMockJavaResource("C",
            "public abstract class C extends B implements I11 {",
            "}");

    addAll(I1, I11, A, B, C);
    Result result = optimize("void", "");

    JMethod C_m = findMethod(result, "C.m()V");
    assertTrue(C_m.isAbstract());
    assertOverrides(result, "C.m()V", "A.m()V", "I1.m()V");
  }

  public void testAccidentalOverrides_concreteImplementationCase()
      throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  void m();",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A {",
            "  public void m() {};",
            "}");

    final MockJavaResource B =
        JavaResourceBase.createMockJavaResource("B",
            "public abstract class B extends A {",
            "}");

    final MockJavaResource C =
        JavaResourceBase.createMockJavaResource("C",
            "public abstract class C extends B implements I11 {",
            "}");

    addAll(I1, I11, A, B, C);
    Result result = optimize("void", "");

    JMethod C_m = findMethod(result, "C.m()V");
    assertFalse(C_m.isAbstract());
    assertForwardsTo(C_m, findMethod(result, "A.m()V"));
    assertOverrides(result, "C.m()V", "A.m()V", "I1.m()V");
  }

  public void testAccidentalOverrides_concreteImplementationInterfaceMethodDefault()
      throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  default void m() {}",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A {",
            "  public void m() {};",
            "}");

    final MockJavaResource B =
        JavaResourceBase.createMockJavaResource("B",
            "public abstract class B extends A {",
            "}");

    final MockJavaResource C =
        JavaResourceBase.createMockJavaResource("C",
            "public abstract class C extends B implements I11 {",
            "}");

    addAll(I1, I11, A, B, C);
    Result result = optimize("void", "");

    JMethod C_m = findMethod(result, "C.m()V");
    assertFalse(C_m.isAbstract());
    assertForwardsTo(C_m, findMethod(result, "A.m()V"));
    assertOverrides(result, "C.m()V", "A.m()V", "I1.m()V");
  }

  public void testDefaults_simpleCase()
      throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  default void m() {}",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A implements I11 {",
            "}");

    addAll(I1, I11, A);
    Result result = optimize("void", "");

    JMethod A_m = findMethod(result, "A.m()V");
    assertFalse(A_m.isAbstract());
    assertForwardsTo(A_m, findMethod(result, "I1.m()V"));
    assertOverrides(result, "A.m()V", "I1.m()V");
  }

  public void testDefaults_diamond()
      throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  default void m() {}",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource I12 =
        JavaResourceBase.createMockJavaResource("I12",
            "public interface I12 extends I1 {",
            "  default void m() {}",
            "}");

    final MockJavaResource A1 =
        JavaResourceBase.createMockJavaResource("A1",
            "public abstract class A1 implements I11, I12 {",
            "}");

    final MockJavaResource A2 =
        JavaResourceBase.createMockJavaResource("A2",
            "public abstract class A2 implements I12, I11 {",
            "}");

    addAll(I1, I11, I12, A1, A2);
    Result result = optimize("void", "");

    JMethod A1_m = findMethod(result, "A1.m()V");
    assertFalse(A1_m.isAbstract());
    assertForwardsTo(A1_m, findMethod(result, "I12.m()V"));
    assertOverrides(result, "A1.m()V", "I1.m()V", "I12.m()V");

    JMethod A2_m = findMethod(result, "A2.m()V");
    assertFalse(A2_m.isAbstract());
    assertForwardsTo(A2_m, findMethod(result, "I12.m()V"));
    assertOverrides(result, "A2.m()V", "I1.m()V", "I12.m()V");
  }

  public void testDefaults_diamondAbstractBaseClass()
      throws UnableToCompleteException {

    final MockJavaResource I1 =
        JavaResourceBase.createMockJavaResource("I1",
            "public interface I1 {",
            "  default void m() {}",
            "}");

    final MockJavaResource I11 =
        JavaResourceBase.createMockJavaResource("I11",
            "public interface I11 extends I1 {",
            "}");

    final MockJavaResource I12 =
        JavaResourceBase.createMockJavaResource("I12",
            "public interface I12 extends I1 {",
            "  default void m() {}",
            "}");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public abstract class A implements I1 {",
            "}");

    final MockJavaResource A1 =
        JavaResourceBase.createMockJavaResource("A1",
            "public abstract class A1 extends A implements I11, I12 {",
            "}");

    final MockJavaResource A2 =
        JavaResourceBase.createMockJavaResource("A2",
            "public abstract class A2 extends A implements I12, I11 {",
            "}");

    addAll(I1, I11, I12, A, A1, A2);
    Result result = optimize("void", "");

    JMethod A_m = findMethod(result, "A.m()V");
    assertFalse(A_m.isAbstract());
    assertForwardsTo(A_m, findMethod(result, "I1.m()V"));
    assertOverrides(result, "A.m()V", "I1.m()V");

    JMethod A1_m = findMethod(result, "A1.m()V");
    assertFalse(A1_m.isAbstract());
    assertForwardsTo(A1_m, findMethod(result, "I12.m()V"));
    assertOverrides(result, "A1.m()V", "A.m()V", "I1.m()V", "I12.m()V");

    JMethod A2_m = findMethod(result, "A2.m()V");
    assertFalse(A2_m.isAbstract());
    assertForwardsTo(A2_m, findMethod(result, "I12.m()V"));
    assertOverrides(result, "A2.m()V", "A.m()V", "I1.m()V", "I12.m()V");
  }

  /**
   * Regression test for specialization resolution.
   */
  public void testOverrides_specializations()
      throws UnableToCompleteException {

    final MockJavaResource SpecializedImpl =
        JavaResourceBase.createMockJavaResource("test.SpecializedImpl",
            "package test;",
            "import javaemul.internal.annotations.SpecializeMethod;",
            "public class SpecializedImpl<K> extends Impl<K> implements I {",
            "  @SpecializeMethod(params = {String.class}, target = \"putString\")",
            "  public void put(K k) { }",
            "  public void putString(String k) { }",
            "}");

    final MockJavaResource Impl =
        JavaResourceBase.createMockJavaResource("test.Impl",
            "package test;",
            "public class Impl<K> {",
            "  public void put(K k) { }",
            "  public void m() { }",
            "}");

    final MockJavaResource I =
        JavaResourceBase.createMockJavaResource("test.I",
            "package test;",
            "public interface I {",
            "  public void m();",
            "}");

    addAll(I, SpecializedImpl, Impl);
    Result result =
        optimize("void", "new SpecializedImpl<String>().put(\"2\");");

    JClassType testImplClass = (JClassType) result.findClass("test.SpecializedImpl");

    String mSignature = "m()V";
    JMethod testImplChild_putString = findMethod(result, "test.SpecializedImpl." + mSignature);
    assertSame(testImplChild_putString,
        result.getOptimizedProgram()
            .typeOracle.getInstanceMethodBySignature(testImplClass, mSignature));
  }

  public void testGetProperty_error_undefinedProperty()
      throws UnableToCompleteException {

    final MockJavaResource someClass =
        JavaResourceBase.createMockJavaResource("test.SomeClass",
            "package test;",
            "public class SomeClass {",
            "  public void m() { String a = System.getProperty(\"undefined\"); }",
            "}");

    shouldGenerateError(someClass, 3, "Property 'undefined' is not defined.");
  }

  public void testGetProperty_error_multivalued()
      throws UnableToCompleteException {

    final MockJavaResource someClass =
        JavaResourceBase.createMockJavaResource("test.SomeClass",
            "package test;",
            "public class SomeClass {",
            "  public void m() { String a = System.getProperty(\"multivalued\"); }",
            "}");

    shouldGenerateError(someClass, 3, "Property 'multivalued' is multivalued." +
        " Multivalued properties are not supported by System.getProperty().");
  }

  public void testGetPropertyWithDefault_error_multivalued()
      throws UnableToCompleteException {

    final MockJavaResource someClass =
        JavaResourceBase.createMockJavaResource("test.SomeClass",
            "package test;",
            "public class SomeClass {",
            "  public void m() { String a = System.getProperty(\"multivalued\", \"somevalue\"); }",
            "}");

    shouldGenerateError(someClass, 3, "Property 'multivalued' is multivalued." +
        " Multivalued properties are not supported by System.getProperty().");
  }

  public void testGetPropertyWithDefault_success_undefined()
      throws UnableToCompleteException {

    final MockJavaResource someClass =
        JavaResourceBase.createMockJavaResource("test.SomeClass",
            "package test;",
            "public class SomeClass {",
            "  public void m() { String a = System.getProperty(\"undefined\", \"somevalue\"); }",
            "}");

    shouldGenerateNoError(someClass);
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    return false;
  }

  @Override
  protected CompilerContext provideCompilerContext() {
    CompilerContext context = super.provideCompilerContext();
    context.getModule().getProperties().createConfiguration("multivalued", true);
    return context;
  }

  @Override
  protected Pass providePass() {
    return new Pass() {
      private Result result = null;
      @Override
      public boolean run(TreeLogger logger, MockJavaResource buggyResource,
          MockJavaResource extraResource) {
        addAll(buggyResource, extraResource);
        try {
          result = optimize(logger, "void", "new SomeClass().m();");
        } catch (UnableToCompleteException e) {
          return false;
        }
        return true;
      }

      @Override
      public boolean classAvailable(String className) {
        return result != null && result.findClass(className) != null;
      }

      @Override
      public String getTopErrorMessage(Type logLevel, MockJavaResource resource) {
        return (logLevel == Type.WARN ? "Warnings" : "Errors") +
            " in '" + resource.getPath() + "'";
      }
    };
  }

  private static final MockJavaResource A_A =
      JavaResourceBase.createMockJavaResource("a.A",
          "package a;",
          "public class A {",
          "  public void m() { }",
          "  public A m1() { return null; }",
          "  void pp() {}",
          "}");

  private static final MockJavaResource A_I =
      JavaResourceBase.createMockJavaResource("a.I",
          "package a;",
          "public interface I {",
          "  void m();",
          "  A m1();",
          "  void pp();",
          "}");

  private static final MockJavaResource A_J =
      JavaResourceBase.createMockJavaResource("a.J",
          "package a;",
          "public interface J extends I {",
          "  void pp();",
          "}");

  /**
   * a.B accidentally implements a.I.m() and a.I.m1() and
   * explicitly implements a.I.pp()
   *
   * a.B also overrides package private a.A.pp()V and makes it public.
   */
  private static final MockJavaResource A_B =
      JavaResourceBase.createMockJavaResource("a.B",
          "package a;",
          "public class B extends A implements a.I {",
          "  public void pp() {}",
          "}");

  private static final MockJavaResource B_C =
      JavaResourceBase.createMockJavaResource("b.C",
          "package b;",
          "public class C extends a.A {",
          "  public C m1() { return null; }",
          "  void pp() {}",
          "}");

  private static final MockJavaResource B_D =
      JavaResourceBase.createMockJavaResource("b.D",
          "package b;",
          "public class D extends a.B {",
          "  public D m1() { return null; }",
          "}");

  private static final MockJavaResource B_E =
      JavaResourceBase.createMockJavaResource("b.E",
          "package b;",
          "public class E extends b.C {",
          "  public E m1() { return null; }",
          "}");
}
