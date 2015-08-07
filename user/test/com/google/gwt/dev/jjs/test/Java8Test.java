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
    if (JUnitShell.getCompilerOptions().getSourceLevel().compareTo(SourceLevel.JAVA8) >= 0) {
      super.runTest();
    }
  }

  public void testLambdaNoCapture() {
    // Make sure we are using the right Java8Test if the source compatibility level is set to Java 8
    // or above.
    assertFalse(JUnitShell.getCompilerOptions().getSourceLevel().compareTo(SourceLevel.JAVA8) >= 0);
  }

  public void testLambdaCaptureLocal() {
  }

  public void testLambdaCaptureLocalWithInnerClass() {
  }

  public void testLambdaCaptureLocalAndField() {
  }

  public void testLambdaCaptureLocalAndFieldWithInnerClass() {
  }

  public void testCompileLambdaCaptureOuterInnerField() throws Exception {
  }

  public void testStaticReferenceBinding() throws Exception {
  }

  public static Integer foo(int x, int y) {
    return x + y;
  }

  public Integer fooInstance(int x, int y) {
    return x + y + 1;
  }

  public void testInstanceReferenceBinding() throws Exception {
  }

  public void testImplicitQualifierReferenceBinding() throws Exception {
  }

  public void testConstructorReferenceBinding() {
  }

  public void testStaticInterfaceMethod() {
  }

  public void testArrayConstructorReference() {
  }

  public void testArrayConstructorReferenceBoxed() {
  }

  public void testVarArgsReferenceBinding() {
  }

  public void testVarArgsPassthroughReferenceBinding() {
  }

  public void testVarArgsPassthroughReferenceBindingProvidedArray() {
  }

  public void testSuperReferenceExpression() {
  }

  public void testSuperReferenceExpressionWithVarArgs() {
  }

  public void testPrivateConstructorReference() {
  }

  public void testDefaultInterfaceMethod() {
  }

  public void testDefaultInterfaceMethodVirtualUpRef() {
  }

  public void testDefaultInterfaceMethodMultiple() {
  }

  public void testDefaultMethodReference() {
  }

  public void testDefenderMethodByInterfaceInstance() {
  }

  public void testThisRefInDefenderMethod() {
  }

  public void testClassImplementsTwoInterfacesWithSameDefenderMethod() {
  }

  public void testAbstractClassImplementsInterface() {
  }

  public void testSuperRefInDefenderMethod() {
  }

  public void testSuperThisRefsInDefenderMethod() {
  }

  public void testNestedInterfaceClass() {
  }

  public void testBaseIntersectionCast() {
  }

  public void testIntersectionCastWithLambdaExpr() {
  }

  public void testIntersectionCastPolymorphism() {
  }

  public void testLambdaCaptureParameter() {
  }

  public void testLambdaNestingCaptureLocal() {
  }

  public void testLambdaNestingInAnonymousCaptureLocal() {
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal() {
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal_withInterference() {
  }

  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocalAndField() {
  }

  public void testLambdaNestingInMultipleAnonymousCaptureLocal() {
  }

  public void testLambdaNestingCaptureField_InnerClassCapturingOuterClassVariable() {
  }

  public void testInnerClassCaptureLocalFromOuterLambda() {
  }

  public void testLambdaNestingCaptureField() {
  }

  public void testLambdaMultipleNestingCaptureFieldAndLocal() {
  }

  public void testLambdaMultipleNestingCaptureFieldAndLocalInnerClass() {
  }

  public void testMethodRefWithSameName() {
  }

  public void testMultipleDefaults_fromInterfaces_left() {
  }

  public void testMultipleDefaults_fromInterfaces_right() {
  }

  public void testMultipleDefaults_superclass_left() {
  }

  public void testMultipleDefaults_superclass_right() {
  }

  public void testInterfaceThis() {
  }
}