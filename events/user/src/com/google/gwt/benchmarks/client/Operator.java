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

/**
 * A mathematical operator used in {@link IntRange}s to indicate the stepping
 * function.
 */
public final class Operator {

  /**
   * The standard multiplication operator.
   */
  public static Operator MULTIPLY = new Operator("*");

  /**
   * The standard addition operator.
   */
  public static Operator ADD = new Operator("+");

  private String value;

  private Operator(String value) {
    this.value = value;
  }

  /**
   * Returns the textual representation of the <code>Operator</code>.
   * 
   * @return a non-null {@link String}
   */
  @Override
  public String toString() {
    return value;
  }
}
