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
 * Specifies a prefix that a debugger should add to the beginning of each
 * filename in a sourcemap to determine the file's full URL.
 * (It's saved to the sourceRoot field in SourceMap spec.)
 * If null, no prefix is saved and Java filenames are relative
 * to the sourcemap's URL.
 */
public interface OptionSourceMapFilePrefix {

  /**
   * Returns the prefix to be added (or null for no prefix).
   */
  String getSourceMapFilePrefix();

  /**
   * Sets the prefix. (Null will disable it.)
   */
  void setSourceMapFilePrefix(String path);
}
