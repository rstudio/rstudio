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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.jdt.RebindPermutationOracle;

/**
 * A Compiler used to compile a GWT project into artifacts.
 */
public interface AbstractCompiler {
  /**
   * Performs a precompilation, returning an object that can then be used to
   * compile individual permutations..
   * 
   * @param logger the logger to use
   * @param module the module to compile
   * @param rpo the RebindPermutationOracle
   * @param declEntryPts the set of entry classes declared in a GWT module;
   *          these will be automatically rebound
   * @param additionalRootTypes additional classes that should serve as code
   *          roots; will not be rebound; may be <code>null</code>
   * @param options the compiler options
   * @param singlePermutation if true, do not pre-optimize the resulting AST or
   *          allow serialization of the result
   * @param precompilationMetrics if not null, the precompile with gather some
   *          diagnostics for the compile.
   * @return the unified AST used to drive permutation compiles
   * @throws UnableToCompleteException if an error other than
   *           {@link OutOfMemoryError} occurs
   */
  UnifiedAst precompile(TreeLogger logger, ModuleDef module, RebindPermutationOracle rpo,
      String[] declEntryPts, String[] additionalRootTypes, JJSOptions options,
      boolean singlePermutation, PrecompilationMetricsArtifact precompilationMetrics)
      throws UnableToCompleteException;
}
