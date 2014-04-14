/*
 * Copyright 2009 Google Inc.
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
 * Option to early optimize the unified AST during a precompile. Does not
 * correspond to any command line option. This is used directly by compiler
 * clients to specify whether or not to optimize early.
 */
public interface OptionOptimizePrecompile {

  /**
   * Returns true if the compiler should lightly optimize the raw AST before any permutation work.
   */
  boolean isOptimizePrecompile();

  /**
   * Sets whether or not the compiler should lightly optimize the raw AST before any permutation
   * work.
   */
  void setOptimizePrecompile(boolean enabled);
}
