/*
 * Copyright 2013 Google Inc.
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
 * Option to request the SOYC Compile Report output in the new json format.
 * TODO(ocallau) Consider moving this to a format-like option and merge with OptionSoycEnabled
 */
public interface OptionJsonSoycEnabled extends OptionCompilerMetricsEnabled {

  /**
   * Returns true if the compiler should record and emit Compile Report information in json format.
   */
  boolean isJsonSoycEnabled();

  /**
   * Sets whether or not the compiler should record and emit Compile Report information in json
   * format.
   */
  void setJsonSoycEnabled(boolean value);
}
