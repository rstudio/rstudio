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
 * This option specifies whether the GWT compiler should save Java source code for a debugger
 * to read when debugging a GWT application.
 *
 * <p>By default, source code will be written to "extras". The {@link OptionSaveSourceOutput} option
 * can be used to redirect it to a different directory or jar.</p>
 *
 * <p>The debugger will find the source code using a sourcemap. Only source code that's referred
 * to by a sourcemap will be saved. To convert a filename in a sourcemap to a filename in extras,
 * add "${module_name}/src/" to the front. (The module name is after any renaming.)</p>
 */
public interface OptionSaveSource {

  /**
   * Returns false if source code should not be saved.
   */
  boolean shouldSaveSource();

  /**
   * If set to true and sourcemaps are on, source code will be saved.
   */
  void setSaveSource(boolean enabled);
}
