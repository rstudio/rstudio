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

import java.io.File;

/**
 * This option specifies where the GWT compiler should write source code for a debugger
 * to read when debugging a GWT application.
 *
 * <p>The debugger will find the source code using a sourcemap. To convert a filename
 * in a sourcemap to a filename in the debug source directory, add "${module_name}/src/"
 * to the front. (The module name is after any renaming.)</p>
 */
public interface OptionSaveSourceOutput {

  /**
   * Returns the directory or jar where the GWT compiler should write the source code,
   * or null if the source files shouldn't be written.
   */
  File getSaveSourceOutput();

  /**
   * Sets the directory or jar where the GWT compiler should write the source code,
   * or null to skip writing it. If a jar exists then it will be overwritten.
   */
  void setSaveSourceOutput(File dest);
}
