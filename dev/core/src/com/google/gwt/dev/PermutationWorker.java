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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.util.FileBackedObject;

/**
 * Represents a facility that can compile an individual {@link Permutation}.
 * Instances of PermutationWorker are expected to be created via
 * {@link PermutationWorkerFactory#getWorkers(int)}.
 */
interface PermutationWorker {

  /**
   * Compile a single permutation. The {@link com.google.gwt.dev.jjs.UnifiedAst}
   * will have been provided to {@link PermutationWorkerFactory#getWorkers}
   * method. The compiled PermutationResult will be returned via the
   * <code>resultFile</code> parameter.
   * 
   * @throws TransientWorkerException if the Permutation should be tried again
   *           on another worker
   * @throws UnableToCompleteException if the compile fails for any reason
   */
  void compile(TreeLogger logger, Permutation permutation,
      FileBackedObject<PermutationResult> resultFile) throws TransientWorkerException,
      UnableToCompleteException;

  /**
   * Returns a human-readable description of the worker instance. This may be
   * used for error reporting.
   */
  String getName();

  /**
   * Release any resources associated with the worker.
   */
  void shutdown();
}
