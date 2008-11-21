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
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Represents a factory for implementations of an endpoint that will invoke
 * CompilePerms. Implementations of PermutationWorkerFactory should be
 * default-instantiable and will have {@link #init} called immediately after
 * construction.
 */
public abstract class PermutationWorkerFactory {

  /**
   * This callable is responsible for compiling one permutation. It waits for an
   * available worker, uses it, and returns it to the pool when finished.
   */
  private static class CompileOnePermutation implements Callable<ResultStatus> {
    private final BlockingQueue<PermutationWorker> availableWorkers;
    private final TreeLogger logger;
    private final Permutation permutation;
    private final File resultFile;

    public CompileOnePermutation(TreeLogger logger, Permutation permutation,
        File resultFile, BlockingQueue<PermutationWorker> availableWorkers) {
      this.logger = logger;
      this.permutation = permutation;
      this.resultFile = resultFile;
      this.availableWorkers = availableWorkers;
    }

    public ResultStatus call() {
      // Find a free worker
      PermutationWorker worker;
      try {
        worker = availableWorkers.take();
      } catch (InterruptedException e) {
        logger.log(TreeLogger.DEBUG, "Worker interrupted", e);
        return ResultStatus.HARD_FAILURE;
      }

      if (worker == noMoreWorkersWorker) {
        // Shutting down
        return ResultStatus.HARD_FAILURE;
      }

      // Invoke the worker
      try {
        PermutationResult result = worker.compile(logger, permutation);
        Util.writeObjectAsFile(logger, resultFile, result);
        logger.log(TreeLogger.DEBUG, "Successfully compiled permutation");
        availableWorkers.add(worker);
        return ResultStatus.SUCCESS;
      } catch (TransientWorkerException e) {
        logger.log(TreeLogger.DEBUG, "Worker died, will retry Permutation", e);
        return ResultStatus.TRANSIENT_FAILURE;
      } catch (UnableToCompleteException e) {
        logger.log(TreeLogger.ERROR, "Unrecoverable exception, shutting down",
            e);
        return ResultStatus.HARD_FAILURE;
      }
    }
  }

  private static enum ResultStatus {
    /**
     * A failure bad enough to merit shutting down the compilation.
     */
    HARD_FAILURE,

    /**
     * A successful compile.
     */
    SUCCESS,

    /**
     * A worker died while processing this permutation, but it is worth trying
     * to compile it with another worker.
     */
    TRANSIENT_FAILURE
  };

  /**
   * The name of the system property used to define the workers.
   */
  public static final String FACTORY_IMPL_PROPERTY = "gwt.jjs.permutationWorkerFactory";

  /**
   * This value can be passed into {@link #setLocalWorkers(int)} to indicate
   * that a heuristic should be used to determine the total number of local
   * workers.
   */
  public static final int WORKERS_AUTO = 0;

  private static List<PermutationWorkerFactory> lazyFactories;

  private static final PermutationWorker noMoreWorkersWorker = new PermutationWorker() {

    public PermutationResult compile(TreeLogger logger, Permutation permutation)
        throws TransientWorkerException, UnableToCompleteException {
      throw new UnableToCompleteException();
    }

    public String getName() {
      return "Marker worker indicating no more workers";
    }

    public void shutdown() {
    }
  };

  /**
   * Compiles all Permutations in a Precompilation and returns an array of Files
   * that can be consumed by Link using the system-default
   * PermutationWorkersFactories.
   */
  public static void compilePermutations(TreeLogger logger,
      Precompilation precompilation, int localWorkers, File[] resultFiles)
      throws UnableToCompleteException {
    compilePermutations(logger, precompilation,
        precompilation.getPermutations(), localWorkers, resultFiles);
  }

  /**
   * Compiles a subset of the Permutations in a Precompilation and returns an
   * array of Files that can be consumed by Link using the system-default
   * PermutationWorkersFactories.
   * 
   * @param localWorkers Set the maximum number of workers that should be
   *          executed on the local system by the PermutationWorkerFactory. The
   *          value {@link #WORKERS_AUTO} will allow the
   *          PermutationWorkerFactory to apply a heuristic to determine the
   *          correct number of local workers.
   * @param resultFiles the output files to write into; must be the same length
   *          as permutations
   */
  public static void compilePermutations(TreeLogger logger,
      Precompilation precompilation, Permutation[] permutations,
      int localWorkers, File[] resultFiles) throws UnableToCompleteException {
    assert permutations.length == resultFiles.length;
    assert Arrays.asList(precompilation.getPermutations()).containsAll(
        Arrays.asList(permutations));

    // We may have a mixed collection of workers from different factories
    List<PermutationWorker> workers = new ArrayList<PermutationWorker>();

    /*
     * We can have errors below this point, there's a finally block to handle
     * cleanup of workers.
     */
    try {
      createWorkers(logger, precompilation.getUnifiedAst(),
          permutations.length, localWorkers, workers);
      ExecutorService executor = Executors.newFixedThreadPool(workers.size());

      // List of available workers.
      // The extra space is for inserting nulls at shutdown time
      BlockingQueue<PermutationWorker> availableWorkers = new ArrayBlockingQueue<PermutationWorker>(
          2 * workers.size());
      availableWorkers.addAll(workers);

      try {

        // Submit all tasks to the executor

        // The permutation compiles not yet finished
        Queue<CompileOnePermutation> tasksOutstanding = new LinkedList<CompileOnePermutation>();

        // The futures for the results of those compiles
        Queue<Future<ResultStatus>> resultFutures = new LinkedList<Future<ResultStatus>>();

        for (int i = 0; i < permutations.length; ++i) {
          TreeLogger permLogger = logger.branch(TreeLogger.DEBUG,
              "Worker permutation " + permutations[i].getId() + " of "
                  + permutations.length);
          CompileOnePermutation task = new CompileOnePermutation(permLogger,
              permutations[i], resultFiles[i], availableWorkers);
          tasksOutstanding.add(task);
          resultFutures.add(executor.submit(task));
        }

        // Count the number of dead workers
        int numDeadWorkers = 0;
        int successCount = 0;

        while (!resultFutures.isEmpty() && numDeadWorkers < workers.size()) {
          assert resultFutures.size() == tasksOutstanding.size();

          CompileOnePermutation task = tasksOutstanding.remove();
          Future<ResultStatus> future = resultFutures.remove();
          ResultStatus result;
          try {
            result = future.get();
          } catch (InterruptedException e) {
            logger.log(TreeLogger.ERROR,
                "Exiting without results due to interruption", e);
            throw new UnableToCompleteException();
          } catch (ExecutionException e) {
            logger.log(TreeLogger.ERROR, "A compilation failed", e);
            throw new UnableToCompleteException();
          }

          if (result == ResultStatus.SUCCESS) {
            ++successCount;
          } else if (result == ResultStatus.TRANSIENT_FAILURE) {
            // A worker died. Resubmit for the remaining workers.
            ++numDeadWorkers;
            tasksOutstanding.add(task);
            resultFutures.add(executor.submit(task));
          } else if (result == ResultStatus.HARD_FAILURE) {
            // Shut down.
            break;
          } else {
            throw new InternalCompilerException("Unknown result type");
          }
        }

        // Too many permutations is a coding error
        assert successCount <= permutations.length;

        if (successCount < permutations.length) {
          // Likely as not, all of the workers died
          logger.log(TreeLogger.ERROR, "Not all permutation were compiled "
              + successCount + " of " + permutations.length);
          throw new UnableToCompleteException();
        }
      } finally {
        // Shut down the executor
        executor.shutdown();

        // Inform any residual CompileOnePermutation's that there aren't any
        // more workers
        for (int i = 0; i < workers.size(); i++) {
          availableWorkers.add(noMoreWorkersWorker);
        }
      }
    } finally {
      // Shut down all workers
      for (PermutationWorker worker : workers) {
        worker.shutdown();
      }
    }
  }

  /**
   * Creates one or more implementations of worker factories. This will treat
   * the value of the {@value #FACTORY_IMPL_PROPERTY} system property as a
   * comma-separated list of type names.
   */
  private static synchronized List<PermutationWorkerFactory> createAll(
      TreeLogger logger) throws UnableToCompleteException {
    // NB: This is the much-derided FactoryFactory pattern

    logger = logger.branch(TreeLogger.TRACE,
        "Creating PermutationWorkerFactory instances");

    if (lazyFactories != null) {
      logger.log(TreeLogger.SPAM, "Using lazy instances");
      return lazyFactories;
    }

    List<PermutationWorkerFactory> mutableFactories = new ArrayList<PermutationWorkerFactory>();
    String classes = System.getProperty(FACTORY_IMPL_PROPERTY,
        ThreadedPermutationWorkerFactory.class.getName() + ","
            + ExternalPermutationWorkerFactory.class.getName());
    logger.log(TreeLogger.SPAM, "Factory impl property is " + classes);

    String[] classParts = classes.split(",");
    for (String className : classParts) {
      try {
        Class<? extends PermutationWorkerFactory> clazz = Class.forName(
            className).asSubclass(PermutationWorkerFactory.class);
        PermutationWorkerFactory factory = clazz.newInstance();
        factory.init(logger);
        mutableFactories.add(factory);
        logger.log(TreeLogger.SPAM, "Added PermutationWorkerFactory "
            + clazz.getName());
      } catch (ClassCastException e) {
        logger.log(TreeLogger.ERROR, className + " is not a "
            + PermutationWorkerFactory.class.getName());
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to find PermutationWorkerFactory named " + className);
      } catch (InstantiationException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to instantiate PermutationWorkerFactory " + className, e);
      } catch (IllegalAccessException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to instantiate PermutationWorkerFactory " + className, e);
      }
    }

    if (mutableFactories.size() == 0) {
      logger.log(TreeLogger.ERROR,
          "No usable PermutationWorkerFactories available");
      throw new UnableToCompleteException();
    }

    return lazyFactories = Collections.unmodifiableList(mutableFactories);
  }

  /**
   * Create as many workers as possible to service the Permutations.
   */
  private static void createWorkers(TreeLogger logger, UnifiedAst unifiedAst,
      int workersNeeded, int localWorkers, List<PermutationWorker> workers)
      throws UnableToCompleteException {
    if (localWorkers <= WORKERS_AUTO) {
      // TODO: something smarter?
      localWorkers = 1;
    }

    for (PermutationWorkerFactory factory : PermutationWorkerFactory.createAll(logger)) {
      if (workersNeeded <= 0) {
        break;
      }

      int wanted = factory.isLocal() ? Math.min(workersNeeded, localWorkers)
          : workersNeeded;
      if (wanted <= 0) {
        continue;
      }

      Collection<PermutationWorker> newWorkers = factory.getWorkers(logger,
          unifiedAst, wanted);

      workers.addAll(newWorkers);
      workersNeeded -= newWorkers.size();
      if (factory.isLocal()) {
        localWorkers -= newWorkers.size();
      }
    }

    if (workers.size() == 0) {
      logger.log(TreeLogger.ERROR, "No PermutationWorkers created");
      throw new UnableToCompleteException();
    }
  }

  /**
   * Return some number of PermutationWorkers.
   * 
   * @param unifiedAst a UnifiedAst
   * @param numWorkers the desired number of workers
   * @return a collection of PermutationWorkers, the size of which may be less
   *         than <code>numWorkers</code>
   */
  public abstract Collection<PermutationWorker> getWorkers(TreeLogger logger,
      UnifiedAst unifiedAst, int numWorkers) throws UnableToCompleteException;

  /**
   * Initialize the PermutationWorkerFactory.
   */
  public abstract void init(TreeLogger logger) throws UnableToCompleteException;

  /**
   * Indicates if the PermutationWorkers created by the factory consume
   * computational or memory resources on the local system, as opposed to the
   * per-permutation work being performed on a remote system.
   */
  public abstract boolean isLocal();
}
