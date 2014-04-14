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
package com.google.gwt.dev.jjs;

/**
 * Possibly options for JavaScript output format.
 */
public enum JsOutputOption {
  /**
   * Formatted output with huge but unambiguous identifiers.
   */
  DETAILED,
  /**
   * Compressed output, using tiny unreadable identifiers (default).
   */
  OBFUSCATED,
  /**
   * Formatted output with human-readable identifiers.
   */
  PRETTY;

  public boolean shouldMinimize() {
    return this == OBFUSCATED;
  }
}
