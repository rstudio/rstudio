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

import java.io.File;

/**
 * Option to set the module path prefix.
 */
public interface OptionModulePathPrefix {

  /**
   * Sets the path to be prefixed to the module output.
   */
  void setModulePathPrefix(String prefix);

  /**
   * Returns the path of the webserver root context.
   */
  File getModuleBaseDir();
}
