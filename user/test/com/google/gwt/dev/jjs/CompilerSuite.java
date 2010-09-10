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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.client.impl.StackTraceLineNumbersTest;
import com.google.gwt.dev.jjs.scriptonly.ScriptOnlyTest;
import com.google.gwt.dev.jjs.test.AnnotationsTest;
import com.google.gwt.dev.jjs.test.AutoboxTest;
import com.google.gwt.dev.jjs.test.BlankInterfaceTest;
import com.google.gwt.dev.jjs.test.ClassCastTest;
import com.google.gwt.dev.jjs.test.ClassObjectTest;
import com.google.gwt.dev.jjs.test.CompilerTest;
import com.google.gwt.dev.jjs.test.CoverageTest;
import com.google.gwt.dev.jjs.test.EnhancedForLoopTest;
import com.google.gwt.dev.jjs.test.EnumsTest;
import com.google.gwt.dev.jjs.test.EnumsWithNameObfuscationTest;
import com.google.gwt.dev.jjs.test.GenericCastTest;
import com.google.gwt.dev.jjs.test.HostedTest;
import com.google.gwt.dev.jjs.test.InitialLoadSequenceTest;
import com.google.gwt.dev.jjs.test.InnerClassTest;
import com.google.gwt.dev.jjs.test.InnerOuterSuperTest;
import com.google.gwt.dev.jjs.test.JStaticEvalTest;
import com.google.gwt.dev.jjs.test.JavaAccessFromJavaScriptTest;
import com.google.gwt.dev.jjs.test.JsStaticEvalTest;
import com.google.gwt.dev.jjs.test.JsniConstructorTest;
import com.google.gwt.dev.jjs.test.JsoTest;
import com.google.gwt.dev.jjs.test.MemberShadowingTest;
import com.google.gwt.dev.jjs.test.MethodBindTest;
import com.google.gwt.dev.jjs.test.MethodCallTest;
import com.google.gwt.dev.jjs.test.MethodInterfaceTest;
import com.google.gwt.dev.jjs.test.MiscellaneousTest;
import com.google.gwt.dev.jjs.test.NativeLongTest;
import com.google.gwt.dev.jjs.test.ObjectIdentityTest;
import com.google.gwt.dev.jjs.test.RunAsyncFailureTest;
import com.google.gwt.dev.jjs.test.RunAsyncMetricsIntegrationTest;
import com.google.gwt.dev.jjs.test.RunAsyncTest;
import com.google.gwt.dev.jjs.test.SingleJsoImplTest;
import com.google.gwt.dev.jjs.test.UnstableGeneratorTest;
import com.google.gwt.dev.jjs.test.VarargsTest;
import com.google.gwt.dev.jjs.test.singlejso.TypeHierarchyTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * The complete compiler suite.
 */
public class CompilerSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for com.google.gwt.dev.jjs");

    // $JUnit-BEGIN$
    suite.addTestSuite(AnnotationsTest.class);
    suite.addTestSuite(AutoboxTest.class);
    suite.addTestSuite(BlankInterfaceTest.class);
    suite.addTestSuite(ClassCastTest.class);
    suite.addTestSuite(ClassObjectTest.class);
    suite.addTestSuite(CompilerTest.class);
    suite.addTestSuite(CoverageTest.class);
    suite.addTestSuite(EnhancedForLoopTest.class);
    suite.addTestSuite(EnumsTest.class);
    suite.addTestSuite(EnumsWithNameObfuscationTest.class);
    suite.addTestSuite(GenericCastTest.class);
    suite.addTestSuite(HostedTest.class);
    suite.addTestSuite(InitialLoadSequenceTest.class);
    suite.addTestSuite(InnerClassTest.class);
    suite.addTestSuite(InnerOuterSuperTest.class);
    suite.addTestSuite(JavaAccessFromJavaScriptTest.class);
    suite.addTestSuite(JsniConstructorTest.class);
    suite.addTestSuite(JsoTest.class);
    suite.addTestSuite(JsStaticEvalTest.class);
    suite.addTestSuite(JStaticEvalTest.class);
    suite.addTestSuite(MemberShadowingTest.class);
    suite.addTestSuite(MethodBindTest.class);
    suite.addTestSuite(MethodCallTest.class);
    suite.addTestSuite(MethodInterfaceTest.class);
    suite.addTestSuite(MiscellaneousTest.class);
    suite.addTestSuite(NativeLongTest.class);
    suite.addTestSuite(ObjectIdentityTest.class);
    suite.addTestSuite(RunAsyncFailureTest.class);
    suite.addTestSuite(RunAsyncMetricsIntegrationTest.class);
    suite.addTestSuite(RunAsyncTest.class);
    suite.addTestSuite(ScriptOnlyTest.class);
    suite.addTestSuite(SingleJsoImplTest.class);
    suite.addTestSuite(StackTraceLineNumbersTest.class);
    suite.addTestSuite(TypeHierarchyTest.class);
    suite.addTestSuite(UnstableGeneratorTest.class);
    suite.addTestSuite(VarargsTest.class);
    // $JUnit-END$

    return suite;
  }

}
