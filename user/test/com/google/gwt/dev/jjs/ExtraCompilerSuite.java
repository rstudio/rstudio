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
import com.google.gwt.dev.jjs.test.BasicJsInteropTest;
import com.google.gwt.dev.jjs.test.BlankInterfaceTest;
import com.google.gwt.dev.jjs.test.CompilerMiscRegressionTest;
import com.google.gwt.dev.jjs.test.CompilerTest;
import com.google.gwt.dev.jjs.test.CoverageTest;
import com.google.gwt.dev.jjs.test.EnumsWithNameObfuscationTest;
import com.google.gwt.dev.jjs.test.GwtIncompatibleTest;
import com.google.gwt.dev.jjs.test.HostedTest;
import com.google.gwt.dev.jjs.test.JStaticEvalTest;
import com.google.gwt.dev.jjs.test.JsStaticEvalTest;
import com.google.gwt.dev.jjs.test.MemberShadowingTest;
import com.google.gwt.dev.jjs.test.MiscellaneousTest;
import com.google.gwt.dev.jjs.test.NativeDevirtualizationTest;
import com.google.gwt.dev.jjs.test.UnstableGeneratorTest;
import com.google.gwt.dev.jjs.test.UnusedImportsTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Compiler test suite for special cases and regressions.
 */
public class ExtraCompilerSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for com.google.gwt.dev.jjs");

    // $JUnit-BEGIN$
    suite.addTestSuite(BasicJsInteropTest.class);
    suite.addTestSuite(BlankInterfaceTest.class);
    suite.addTestSuite(CompilerTest.class);
    suite.addTestSuite(CompilerMiscRegressionTest.class);
    suite.addTestSuite(CoverageTest.class);
    suite.addTestSuite(EnumsWithNameObfuscationTest.class);
    suite.addTestSuite(GwtIncompatibleTest.class);
    suite.addTestSuite(HostedTest.class);
    suite.addTestSuite(JsStaticEvalTest.class);
    suite.addTestSuite(JStaticEvalTest.class);
    suite.addTestSuite(MemberShadowingTest.class);
    suite.addTestSuite(MiscellaneousTest.class);
    suite.addTestSuite(NativeDevirtualizationTest.class);
    suite.addTestSuite(ScriptOnlyTest.class);
    suite.addTestSuite(StackTraceLineNumbersTest.class);
    suite.addTestSuite(UnusedImportsTest.class);
    suite.addTestSuite(UnstableGeneratorTest.class);
    // $JUnit-END$

    return suite;
  }
}
