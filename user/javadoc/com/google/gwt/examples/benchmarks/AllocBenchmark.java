/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.examples.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;

/**
 * Provides profile statistics on allocation times for different kinds of
 * objects.
 *
 */
public class AllocBenchmark extends Benchmark {

  private static final int numAllocs = 1000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.examples.Benchmarks";
  }

  /**
   * Allocates java.lang.Object in a for loop 1,000 times.
   *
   * The current version of the compiler lifts the declaration of obj outside
   * of this loop and also does constant folding of numAllocs.
   * Also, this loop allocs the GWT JS mirror for java.lang.Object
   * <em>NOT</em> an empty JS object, for example.
   *
   */
  public void testJavaObjectAlloc() {
    for ( int i = 0; i < numAllocs; ++i ) {
      Object obj = new Object();
    }
  }

  /**
   * Compares GWT mirror allocations of java.lang.Object to an empty JS object.
   */
  public native void testJsniObjectAlloc1() /*-{
    for (var i = 0; i < @com.google.gwt.examples.benchmarks.AllocBenchmark::numAllocs; ++i ) {
      var obj = {}; // An empty JS object alloc
    }
  }-*/;

  /**
   * Like version 1, but also folds the constant being used in the iteration.
   */
  public native void testJsniObjectAlloc2() /*-{
    for (var i = 0; i < 1000; ++i ) {
      var obj = {}; // An empty JS object alloc
    }
  }-*/;

  /**
   * Like version 2, but hoists the variable declaration from the loop.
   */
  public native void testJsniObjectAlloc3() /*-{
    var obj;
    for (var i = 0; i < 1000; ++i ) {
      obj = {}; // An empty JS object alloc
    }
  }-*/;

  /**
   * Like version 3, but counts down (and in a slightly different range).
   */
  public native void testJsniObjectAlloc4() /*-{
    var obj;
    for (var i = 1000; i > 0; --i ) {
      obj = {}; // An empty JS object alloc
    }
  }-*/;
}

