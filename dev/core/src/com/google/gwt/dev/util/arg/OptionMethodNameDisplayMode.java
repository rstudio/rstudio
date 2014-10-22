/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.util.arg;

/**
 * Option to set the what is mapping mode for function names in chrome dev tools.
 */
public interface OptionMethodNameDisplayMode {
  /**
   * Specifies which level of displayNames the GWT compiler outputs for chrome dev tools.
   */
  public enum Mode {
    /**
     * Emit no extra information.
     */
    NONE,
    /**
     * Use the method name as displayName.
     */
    ONLY_METHOD_NAME,
    /**
     * Use the class and method name as displayName.
     */
    ABBREVIATED,
    /**
     * Use the full qualified class and method name as displayName.
     */
    FULL
  }

  Mode getMethodNameDisplayMode();

  void setMethodNameDisplayMode(Mode methodNameDisplayMode);
}
