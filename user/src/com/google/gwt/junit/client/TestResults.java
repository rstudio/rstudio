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
package com.google.gwt.junit.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;
import java.util.ArrayList;

/**
 * Encapsulates the results of the execution of a single benchmark. A TestResult
 * is constructed transparently within a benchmark and reported back to the
 * JUnit RPC server, JUnitHost. It's then shared (via JUnitMessageQueue) with
 * JUnitShell and aggregated in BenchmarkReport with other TestResults.
 * 
 * @skip
 * @see com.google.gwt.junit.client.impl.JUnitHost
 * @see com.google.gwt.junit.JUnitMessageQueue
 * @see com.google.gwt.junit.JUnitShell
 * @see com.google.gwt.junit.benchmarks.BenchmarkReport
 */
public class TestResults implements IsSerializable {

  // Computed at the server, via HTTP header.
  private String agent;

  private String host;

  /**
   * The URL of the document on the browser (document.location). This is used to
   * locate the *cache.html document containing the generated JavaScript for the
   * test. In the case of hosted mode, this points (uselessly) to the nocache
   * file, because there is no generated JavaScript.
   * 
   * Apparently, we can't get this value on the server-side because of the goofy
   * way HTTP_REFERER is set by different browser implementations of
   * XMLHttpRequest.
   */
  private String sourceRef;

  private List<Trial> trials;

  public TestResults() {
    trials = new ArrayList<Trial>();
  }

  public String getAgent() {
    return agent;
  }

  public String getHost() {
    return host;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public List<Trial> getTrials() {
    return trials;
  }

  public void setAgent(String agent) {
    this.agent = agent;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setSourceRef(String sourceRef) {
    this.sourceRef = sourceRef;
  }

  public void setTrials(List<Trial> trials) {
    this.trials = trials;
  }

  @Override
  public String toString() {
    return "trials: " + trials + ", sourceRef: " + sourceRef + ", agent: "
        + agent + ", host: " + host;
  }
}