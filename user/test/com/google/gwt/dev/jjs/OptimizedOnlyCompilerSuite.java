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

import com.google.gwt.dev.jjs.optimized.ArrayListOptimizationTest;
import com.google.gwt.dev.jjs.optimized.ArrayStoreOptimizationTest;
import com.google.gwt.dev.jjs.optimized.CastOptimizationTest;
import com.google.gwt.dev.jjs.optimized.JsOverlayMethodOptimizationTest;
import com.google.gwt.dev.jjs.optimized.SpecializationTest;
import com.google.gwt.dev.jjs.test.HasNoSideEffectsTest;
import com.google.gwt.dev.jjs.test.RunAsyncContentTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Compiler suite for tests not to be run in draft compile.
 */
public class OptimizedOnlyCompilerSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for com.google.gwt.dev.jjs");

    // $JUnit-BEGIN$
    suite.addTestSuite(ArrayListOptimizationTest.class);
    suite.addTestSuite(ArrayStoreOptimizationTest.class);
    suite.addTestSuite(CastOptimizationTest.class);
    suite.addTestSuite(JsOverlayMethodOptimizationTest.class);
    suite.addTestSuite(SpecializationTest.class);
    suite.addTestSuite(HasNoSideEffectsTest.class);
    // RunAsyncContentTest relies in string interning for its assertions which is now always off
    // in non optimzied compiles.
    suite.addTestSuite(RunAsyncContentTest.class);

    // $JUnit-END$

    return suite;
  }
}
