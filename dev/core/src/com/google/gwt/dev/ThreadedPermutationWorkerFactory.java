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
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.FileBackedObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Create a single in-process PermutationWorker. This WorkerFactory is intended
 * to be used as a fall-back in case the other PermutationWorkers are unable to
 * complete.
 */
public class ThreadedPermutationWorkerFactory extends PermutationWorkerFactory {

  private static class ThreadedPermutationWorker implements PermutationWorker {
    private final UnifiedAst ast;
    private final int id;

    public ThreadedPermutationWorker(UnifiedAst ast, int id) {
      this.ast = ast;
      this.id = id;
    }

    public void compile(TreeLogger logger, Permutation permutation,
        FileBackedObject<PermutationResult> resultFile)
        throws TransientWorkerException, UnableToCompleteException {
      try {
        PermutationResult result = CompilePerms.compile(logger, permutation,
            ast);
        resultFile.set(logger, result);
      } catch (OutOfMemoryError e) {
        logger.log(TreeLogger.ERROR,
            "OutOfMemoryError: Increase heap size or lower "
                + MAX_THREADS_PROPERTY, e);
        throw new UnableToCompleteException();
      } catch (StackOverflowError e) {
        logger.log(TreeLogger.ERROR, "StackOverflowError: Increase stack size",
            e);
        throw new UnableToCompleteException();
      }
    }

    public String getName() {
      return "In-process PermutationWorker " + id;
    }

    public void shutdown() {
      // No-op
    }
  }

  /**
   * A Java system property that can be used to change the number of in-process
   * threads used.
   */
  public static final String MAX_THREADS_PROPERTY = "gwt.jjs.maxThreads";

  @Override
  public Collection<PermutationWorker> getWorkers(TreeLogger logger,
      UnifiedAst unifiedAst, int numWorkers) throws UnableToCompleteException {
    logger.log(TreeLogger.SPAM, "Creating ThreadedPermutationWorkers");

    numWorkers = Math.min(numWorkers, Integer.getInteger(MAX_THREADS_PROPERTY,
        1));

    if (numWorkers == 0) {
      return Collections.emptyList();
    }

    // The worker will deserialize a new copy
    List<PermutationWorker> toReturn = new ArrayList<PermutationWorker>(
        numWorkers);
    for (int i = 0; i < numWorkers; i++) {
      toReturn.add(new ThreadedPermutationWorker(unifiedAst, i));
    }
    return toReturn;
  }

  @Override
  public void init(TreeLogger logger) throws UnableToCompleteException {
    logger.log(TreeLogger.SPAM, "Initializing ThreadedPermutationWorkerFactory");
  }

  @Override
  public boolean isLocal() {
    return true;
  }
}
