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
package com.google.gwt.benchmarks.client;

import com.google.gwt.benchmarks.client.impl.BenchmarkResults;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * The translatable implementation of {@link Benchmark}.
 */
public abstract class Benchmark extends GWTTestCase {

  private static native String getDocumentLocation() /*-{
    return $doc.location.toString();
  }-*/;

  /**
   * Collective test results.
   */
  private BenchmarkResults results;

  // CHECKSTYLE_OFF
  @Override
  protected BenchmarkResults __getOrCreateTestResult() {
    if (results == null) {
      results = new BenchmarkResults();
      results.setSourceRef(getDocumentLocation());
    }
    return results;
  }
  // CHECKSTYLE_ON

}
