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
package com.google.gwt.junit.viewer.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Map;
import java.util.HashMap;

/**
 * A data object for Trial.
 */
public class Trial implements IsSerializable {

  String exception;

  double runTimeMillis;

  Map/*<String,String>*/ variables;

  public Trial() {
    this.variables = new HashMap();
  }

  public String getException() {
    return exception;
  }

  public double getRunTimeMillis() {
    return runTimeMillis;
  }

  /**
   * Returns the names and values of the variables used in the test. If there
   * were no variables, the map is empty.
   */
  public Map getVariables() {
    return variables;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public void setRunTimeMillis(double runTimeMillis) {
    this.runTimeMillis = runTimeMillis;
  }
}
