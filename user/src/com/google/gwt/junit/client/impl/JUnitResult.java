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

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.junit.client.TimeoutException;

import junit.framework.AssertionFailedError;

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

  /**
   * If non-null, an exception that occurred during the run.
   */
  SerializableThrowable thrown;

  // Computed at the server, via HTTP header.
  private transient String agent;

  // Computed at the server, via HTTP header.
  private transient String host;

  public String getAgent() {
    return agent;
  }

  public SerializableThrowable getException() {
    return thrown;
  }

  public boolean isAnyException() {
    return thrown != null;
  }

  @GwtIncompatible
  public boolean isExceptionOf(Class<?> expectedException) {
    try {
      return thrown == null ? false
          : expectedException.isAssignableFrom(Class.forName(thrown.getDesignatedType()));
    } catch (Exception e) {
      return false;
    }
  }

  public String getHost() {
    return host;
  }

  public void setAgent(String agent) {
    this.agent = agent;
  }

  public void setException(Throwable exception) {
    thrown = SerializableThrowable.fromThrowable(exception);
    // Try to improve exception message if there is no class metadata available
    if (!thrown.isExactDesignatedTypeKnown()) {
      improveDesignatedType(thrown, exception);
    }
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String toString() {
    return "TestResult {thrown: " + thrown + ", agent: " + agent + ", host: " + host + "}";
  }

  /**
   * Returns best effort type info by checking against some common exceptions for unit tests.
   */
  private static void improveDesignatedType(SerializableThrowable t, Throwable designatedType) {
    if (designatedType instanceof AssertionFailedError) {
      String className = "junit.framework.AssertionFailedError";
      t.setDesignatedType(className, AssertionFailedError.class == designatedType.getClass());
    } else if (designatedType instanceof TimeoutException) {
      String className = "com.google.gwt.junit.client.TimeoutException";
      t.setDesignatedType(className, TimeoutException.class == designatedType.getClass());
    }
  }
}