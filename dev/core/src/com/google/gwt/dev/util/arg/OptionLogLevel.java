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
package com.google.gwt.dev.util.arg;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Option to set the tree logger log level.
 */
public interface OptionLogLevel {

  /**
   * Returns the tree logger level.
   */
  TreeLogger.Type getLogLevel();

  /**
   * Sets the tree logger level.
   */
  void setLogLevel(TreeLogger.Type logLevel);
}
