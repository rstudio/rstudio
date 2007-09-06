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
package com.google.gwt.junit.client;

import com.google.gwt.junit.client.impl.ExceptionWrapper;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Map;
import java.util.HashMap;

/**
 * The result of a single trial-run of a single benchmark method. Each Trial
 * contains the results of running a benchmark method with one set of
 * values for its parameters. TestResults for a method will contain Trials
 * for all permutations of the parameter values. For test methods without
 * parameters, there is only 1 trial result.
 *
 * @skip
 */
public class Trial implements IsSerializable {

  ExceptionWrapper exceptionWrapper;

  double runTimeMillis;

  // Deserialized from exceptionWrapper on the server-side
  transient Throwable exception;

  /**
   * @gwt.typeArgs <java.lang.String,java.lang.String>
   */
  Map<String, String> variables;

  /**
   * Creates a new Trial.
   *
   * @param runTimeMillis    The amount of time spent executing the test
   * @param exceptionWrapper The wrapped getException thrown by the the last
   *                         test, or <code>null</code> if the last test
   *                         completed successfully.
   */
  public Trial(Map<String, String> variables, double runTimeMillis,
      ExceptionWrapper exceptionWrapper) {
    this.variables = variables;
    this.runTimeMillis = runTimeMillis;
    this.exceptionWrapper = exceptionWrapper;
  }

  public Trial() {
    this.variables = new HashMap<String, String>();
  }

  public Throwable getException() {
    return exception;
  }

  public ExceptionWrapper getExceptionWrapper() {
    return exceptionWrapper;
  }

  public double getRunTimeMillis() {
    return runTimeMillis;
  }

  /**
   * Returns the names and values of the variables used in the test. If there
   * were no variables, the map is empty.
   */
  public Map<String, String> getVariables() {
    return variables;
  }

  public void setException(Throwable exception) {
    this.exception = exception;
  }

  public void setExceptionWrapper(ExceptionWrapper exceptionWrapper) {
    this.exceptionWrapper = exceptionWrapper;
  }

  public void setRunTimeMillis(double runTimeMillis) {
    this.runTimeMillis = runTimeMillis;
  }

  @Override
  public String toString() {
    return "variables: " + variables + ", exceptionWrapper: " + exceptionWrapper
        + ", runTimeMillis: " + runTimeMillis;
  }
}
