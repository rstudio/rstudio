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
package com.google.gwt.dev;

import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.ResourceLoader;

/**
 * An interface for classes that can answer build target option questions.
 */
public interface BuildTargetOptions {

  /**
   * Returns the property object whose values should be considered "final" when deciding which
   * generators to run during library compilation.
   */
  Properties getFinalProperties();

  /**
   * Returns the directory path into which to dump a copy of any generated source.
   */
  String getGenDir();

  /**
   * Returns the directory path the compiler is free to use to write temporary files which are not
   * part of final link output.
   */
  String getOutputDir();

  /**
   * Returns the resource loader to use when finding and loading compilable source, build resources
   * such as PNG and CSS files and public resources such as HTML files.
   */
  ResourceLoader getResourceLoader();

  /**
   * Returns the directory path into which the compiler should write linked output.
   */
  String getWarDir();
}
