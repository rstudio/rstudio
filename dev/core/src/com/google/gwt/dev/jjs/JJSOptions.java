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
package com.google.gwt.dev.jjs;

/**
 * Controls options for the {@link JavaToJavaScriptCompiler}.
 */
public class JJSOptions {

  private boolean aggressivelyOptimize = true;
  private boolean enableAssertions = false;
  private JsOutputOption output = JsOutputOption.OBFUSCATED;
  private boolean validateOnly = false;

  public JJSOptions() {
  }

  public JJSOptions(JJSOptions other) {
    copyFrom(other);
  }

  public void copyFrom(JJSOptions other) {
    this.aggressivelyOptimize = other.aggressivelyOptimize;
    this.enableAssertions = other.enableAssertions;
    this.output = other.output;
    this.validateOnly = other.validateOnly;
  }

  /**
   * Returns the output format setting.
   */
  public JsOutputOption getOutput() {
    return output;
  }

  /**
   * Returns true if the compiler should aggressively optimize.
   */
  public boolean isAggressivelyOptimize() {
    return aggressivelyOptimize;
  }

  /**
   * Returns true if the compiler should generate code to check assertions.
   */
  public boolean isEnableAssertions() {
    return enableAssertions;
  }

  /**
   * Returns true if the compiler should run in validation mode, not producing
   * any output.
   */
  public boolean isValidateOnly() {
    return validateOnly;
  }

  /**
   * Sets whether or not the compiler should aggressively optimize.
   */
  public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
    this.aggressivelyOptimize = aggressivelyOptimize;
  }

  /**
   * Sets whether or not the compiler should generate code to check assertions.
   */
  public void setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
  }

  /**
   * Sets the compiler output option.
   */
  public void setOutput(JsOutputOption output) {
    this.output = output;
  }

  /**
   * Sets whether or not the compiler should run in validation mode.
   */
  public void setValidateOnly(boolean validateOnly) {
    this.validateOnly = validateOnly;
  }
}
