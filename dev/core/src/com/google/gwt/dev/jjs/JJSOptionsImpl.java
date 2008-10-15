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

import java.io.Serializable;

/**
 * Concrete class to implement all JJS options.
 */
public class JJSOptionsImpl implements JJSOptions, Serializable {

  private boolean aggressivelyOptimize = true;
  private boolean enableAssertions;
  private JsOutputOption output = JsOutputOption.OBFUSCATED;

  public JJSOptionsImpl() {
  }

  public JJSOptionsImpl(JJSOptions other) {
    copyFrom(other);
  }

  public void copyFrom(JJSOptions other) {
    setAggressivelyOptimize(other.isAggressivelyOptimize());
    setEnableAssertions(other.isEnableAssertions());
    setOutput(other.getOutput());
  }

  public JsOutputOption getOutput() {
    return output;
  }

  public boolean isAggressivelyOptimize() {
    return aggressivelyOptimize;
  }

  public boolean isEnableAssertions() {
    return enableAssertions;
  }

  public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
    this.aggressivelyOptimize = aggressivelyOptimize;
  }

  public void setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
  }

  public void setOutput(JsOutputOption output) {
    this.output = output;
  }
}
