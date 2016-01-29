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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Dummy test case. Java8Test is super sourced so that GWT can be compiled by Java 6.
 *
 * NOTE: Make sure this class has the same test methods of its supersourced variant.
 */
@DoNotRunWith(Platform.Devel)
public class Java8Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.Java8Test";
  }

  @Override
  public void runTest() throws Throwable {
    // Only run these tests if -sourceLevel 8 (or greater) is enabled.
    if (isGwtSourceLevel8()) {
      super.runTest();
    }
  }

  public void testLambdaNoCapture() {
    // Make sure we are using the right Java8Test if the source compatibility level is set to Java 8
    // or above.
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaCaptureLocal() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaCaptureLocalWithInnerClass() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaCaptureLocalAndField() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaCaptureLocalAndFieldWithInnerClass() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testCompileLambdaCaptureOuterInnerField() throws Exception {
    assertFalse(isGwtSourceLevel8());
  }

  public void testStaticReferenceBinding() throws Exception {
    assertFalse(isGwtSourceLevel8());
  }

  public void testInstanceReferenceBinding() throws Exception {
    assertFalse(isGwtSourceLevel8());
  }

  public void testImplicitQualifierReferenceBinding() throws Exception {
    assertFalse(isGwtSourceLevel8());
  }

  public void testConstructorReferenceBinding() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testStaticInterfaceMethod() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testArrayConstructorReference() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testArrayConstructorReferenceBoxed() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testVarArgsReferenceBinding() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testVarArgsPassthroughReferenceBinding() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testVarArgsPassthroughReferenceBindingProvidedArray() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testSuperReferenceExpression() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testSuperReferenceExpressionWithVarArgs() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testPrivateConstructorReference() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testDefaultInterfaceMethod() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testDefaultInterfaceMethodVirtualUpRef() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testInterfaceWithDefaultMethodsInitialization() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testDefaultInterfaceMethodMultiple() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testDefaultMethodReference() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testDefenderMethodByInterfaceInstance() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testDefaultMethod_staticInitializer() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testThisRefInDefenderMethod() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testClassImplementsTwoInterfacesWithSameDefenderMethod() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testAbstractClassImplementsInterface() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testSuperRefInDefenderMethod() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testSuperThisRefsInDefenderMethod() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testNestedInterfaceClass() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testBaseIntersectionCast() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testIntersectionCastWithLambdaExpr() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testIntersectionCastPolymorphism() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaCaptureParameter() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingCaptureLocal() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingInAnonymousCaptureLocal() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal_withInterference() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocalAndField() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingInMultipleAnonymousCaptureLocal() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingCaptureField_InnerClassCapturingOuterClassVariable() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testInnerClassCaptureLocalFromOuterLambda() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaNestingCaptureField() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaMultipleNestingCaptureFieldAndLocal() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testLambdaMultipleNestingCaptureFieldAndLocalInnerClass() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testMethodRefWithSameName() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testMultipleDefaults_fromInterfaces_left() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testMultipleDefaults_fromInterfaces_right() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testMultipleDefaults_superclass_left() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testMultipleDefaults_superclass_right() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testInterfaceThis() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testMethodReference_generics() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testNativeJsTypeWithStaticInitializer() {
    assertFalse(isGwtSourceLevel8());
  }

  public void testJsVarargsLambda() {
    assertFalse(isGwtSourceLevel8());
  }

  private boolean isGwtSourceLevel8() {
    return JUnitShell.getCompilerOptions().getSourceLevel().compareTo(SourceLevel.JAVA8) >= 0;
  }
}