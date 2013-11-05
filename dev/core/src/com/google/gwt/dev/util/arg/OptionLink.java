/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.arg;

/**
 * Whether the separate compiler should link compiled output and supplied precompiled libraries into
 * a usable result.<br />
 *
 * Normally the separate compiler does not link and instead outputs just a precompiled library.
 */
public interface OptionLink {

  /**
   * Sets whether the separate compiler should link compiled output and supplied precompiled
   * libraries into a usable result.
   */
  void setLink(boolean link);

  /**
   * Whether the separate compiler should link compiled output and supplied precompiled libraries
   * into a usable result.
   */
  boolean shouldLink();
}
