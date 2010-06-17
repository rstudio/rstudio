/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.emultest;

import com.google.gwt.emultest.benchmarks.LongBenchmark;
import com.google.gwt.emultest.benchmarks.java.lang.StringBufferBenchmark;
import com.google.gwt.emultest.benchmarks.java.lang.StringBufferImplBenchmark;
import com.google.gwt.emultest.benchmarks.java.util.ArrayListBenchmark;
import com.google.gwt.emultest.benchmarks.java.util.ArraySortBenchmark;
import com.google.gwt.emultest.benchmarks.java.util.HashMapBenchmark;
import com.google.gwt.emultest.benchmarks.java.util.TreeMapBenchmark;
import com.google.gwt.emultest.benchmarks.java.util.VectorBenchmark;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Test JRE emulations.
 */
public class EmulSuiteBenchmark {

  public static Test suite() {
    GWTTestSuite suite 
      = new GWTTestSuite("Benchmarks for com.google.gwt.emul.java");

    // $JUnit-BEGIN$
    // java.lang
    suite.addTestSuite(StringBufferBenchmark.class);
    suite.addTestSuite(StringBufferImplBenchmark.class);
    suite.addTestSuite(LongBenchmark.class);

    // java.util
    suite.addTestSuite(ArrayListBenchmark.class);
    suite.addTestSuite(ArraySortBenchmark.class);
    suite.addTestSuite(HashMapBenchmark.class);
    suite.addTestSuite(TreeMapBenchmark.class);
    suite.addTestSuite(VectorBenchmark.class);
    // $JUnit-END$

    return suite;
  }
}
