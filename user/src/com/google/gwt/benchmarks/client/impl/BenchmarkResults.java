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
package com.google.gwt.benchmarks.client.impl;

import com.google.gwt.junit.client.impl.JUnitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the results of the execution of a single benchmark. A
 * BenchmarkResults is constructed transparently within a benchmark and reported
 * back to the JUnit RPC server, JUnitHost. It's then shared (via
 * JUnitMessageQueue) with JUnitShell and aggregated in BenchmarkReport with
 * other BenchmarkResults.
 * 
 * @skip
 * @see com.google.gwt.junit.client.impl.JUnitHost
 * @see com.google.gwt.junit.JUnitMessageQueue
 * @see com.google.gwt.junit.JUnitShell
 * @see com.google.gwt.benchmarks.BenchmarkReport
 */
public class BenchmarkResults extends JUnitResult {

  /**
   * The URL of the document on the browser (document.location). This is used to
   * locate the *cache.html document containing the generated JavaScript for the
   * test. In the case of Development Mode, this points (uselessly) to the
   * nocache file, because there is no generated JavaScript.
   *
   * Apparently, we can't get this value on the server-side because of the
   * goofy way HTTP_REFERER is set by different browser implementations of
   * XMLHttpRequest.
   */
  private String sourceRef;

  private List<Trial> trials = new ArrayList<Trial>();

  public String getSourceRef() {
    return sourceRef;
  }

  public List<Trial> getTrials() {
    return trials;
  }

  public void setSourceRef(String sourceRef) {
    this.sourceRef = sourceRef;
  }

  @Override
  public String toString() {
    return "BenchmarkResults {" + toStringInner() + "}";
  }

  @Override
  protected String toStringInner() {
    return super.toStringInner() + ", trials: " + trials + ", sourceRef: "
        + sourceRef;
  }

}