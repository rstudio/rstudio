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

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * The result of a single trial-run of a single benchmark method. Each Trial
 * contains the results of running a benchmark method with one set of values for
 * its parameters. TestResults for a method will contain Trials for all
 * permutations of the parameter values. For test methods without parameters,
 * there is only 1 trial result.
 * 
 * @skip
 */
public class Trial implements IsSerializable {

  private double runTimeMillis;

  private Map<String, String> variables = new HashMap<String, String>();

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

  public void setRunTimeMillis(double runTimeMillis) {
    this.runTimeMillis = runTimeMillis;
  }

  @Override
  public String toString() {
    return "variables: " + variables + ", runTimeMillis: " + runTimeMillis;
  }
}
