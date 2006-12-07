/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.json.client;

/**
 * Represents a JSON number. Numbers are represented by <code>double</code>s.
 */
public class JSONNumber extends JSONValue {

  private double value;

  /**
   * Creates a new JSONNumber from the double value.
   */
  public JSONNumber(double value) {
    this.value = value;
  }

  /**
   * Gets the double value that this JSONNumber represents.
   */
  public double getValue() {
    return value;
  }

  /**
   * Returns <code>this</code>, as this is a JSONNumber.
   */
  public JSONNumber isNumber() {
    return this;
  }

  /**
   * Returns the JSON representation of this number.
   */
  public String toString() {
    return new Double(value).toString();
  }
}
