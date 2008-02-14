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
package com.google.gwt.dev.linker;

import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Represents a unique compilation of the module. Multiple permutations may
 * result in identical JavaScript.
 */
public interface CompilationResult {
  /**
   * Returns the JavaScript compilation. The exact form and function of the
   * JavaScript should be considered opaque.
   */
  String getJavaScript();

  /**
   * Provides values for {@link SelectionProperty} instances that are not
   * explicitly set during the compilation phase. This method will return
   * multiple mappings, one for each permutation that resulted in the
   * compilation.
   */
  SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap();
}