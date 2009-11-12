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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.StandardGeneratorContext;

/**
 * Abstract the process of determining all of the possible deferred binding
 * answers for a given type.
 */
public interface RebindPermutationOracle {

  /**
   * Called when the compiler is done with this oracle, so memory can be freed
   * up. After calling this method, the only legal method to call is
   * {@link #getAllPossibleRebindAnswers}.
   */
  void clear();

  /**
   * Always answers with at least one name.
   */
  String[] getAllPossibleRebindAnswers(TreeLogger logger, String sourceTypeName)
      throws UnableToCompleteException;

  /**
   * Returns the CompilationState.
   */
  CompilationState getCompilationState();

  /**
   * Returns the StandardGeneratorContext.
   */
  StandardGeneratorContext getGeneratorContext();
}
