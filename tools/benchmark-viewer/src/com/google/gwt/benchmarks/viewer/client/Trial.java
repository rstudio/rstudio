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
package com.google.gwt.benchmarks.viewer.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * A data object for Trial.
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
}
