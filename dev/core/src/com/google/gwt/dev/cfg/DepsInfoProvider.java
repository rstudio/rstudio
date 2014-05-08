/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.cfg;

import java.util.Set;

/**
 * An interface for classes that can answer type and module dependency queries.
 */
public interface DepsInfoProvider {

  /**
   * Returns the .gwt.xml file path for the given module referenced by name.
   */
  String getGwtXmlFilePath(String moduleName);

  /**
   * Returns a set of the names of modules that source included the given type referenced by name.
   */
  Set<String> getSourceModuleNames(String typeSourceName);

  /**
   * Returns a set of the names of modules in the transitive dependency tree for the given module
   * referenced by name.
   */
  Set<String> getTransitiveDepModuleNames(String targetModuleName);
}
