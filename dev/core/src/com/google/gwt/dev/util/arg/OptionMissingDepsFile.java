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
 * Option to set the missing dependencies file.
 * <p>
 * If provided the compiler will dump a record of missing dependencies to this file formatted like:
 * <fromModuleName>\t<fromModuleUrl>\t<toModuleName>\t<toModuleUrl>\t<humanReadableWarning>
 */
public interface OptionMissingDepsFile {

  /**
   * Returns the missing dependencies file.
   */
  File getMissingDepsFile();

  /**
   * Sets the missing dependency file.
   */
  void setMissingDepsFile(File missingDepsFile);
}
