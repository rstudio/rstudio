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
 * Whether recompiles should process only changed files and construct JS output by
 * linking old and new JS on a per class basis.
 */
public interface OptionIncrementalCompile {

  /**
   * Whether monolithic recompiles should process only changed files and construct JS output by
   * linking old and new JS on a per class basis.
   */
  boolean isIncrementalCompileEnabled();

  /**
   * Sets whether or not monolithic recompiles should process only changed files.
   */
  void setIncrementalCompileEnabled(boolean enabled);
}
