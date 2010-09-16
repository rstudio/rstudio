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
package com.google.gwt.junit.client.impl;

import java.io.Serializable;

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
 */
public class JUnitResult implements Serializable {

  // Computed at the server, via HTTP header.
  private transient String agent;

  /**
   * If non-null, check points that were encountered during the run.
   */
  private String[] checkPoints;

  /**
   * If non-null, an exception that occurred during the run.
   */
  private ExceptionWrapper exceptionWrapper;

  // Computed at the server, via HTTP header.
  private transient String host;

  public String getAgent() {
    return agent;
  }

  public String[] getCheckPoints() {
    return checkPoints;
  }

  public Throwable getException() {
    return (exceptionWrapper == null) ? null : exceptionWrapper.getException();
  }

  public String getHost() {
    return host;
  }

  public void setAgent(String agent) {
    this.agent = agent;
  }

  public void setCheckPoints(String[] checkPoints) {
    this.checkPoints = checkPoints;
  }

  public void setException(Throwable exception) {
    this.exceptionWrapper = new ExceptionWrapper(exception);
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String toString() {
    return "TestResult {" + toStringInner() + "}";
  }

  protected String toStringInner() {
    return "exceptionWrapper: " + exceptionWrapper + ", agent: " + agent
        + ", host: " + host;
  }
}