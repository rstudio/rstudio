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
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.impl.StandardCompilationResult;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.PerfLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compiles a set of permutations, possibly in parallel in multiple threads.
 */
public class PermutationCompiler {

  /**
   * A Result for a permutation that failed to compile.
   */
  private static final class FailedResult extends Result {
    private Throwable exception;

    public FailedResult(Permutation perm, Throwable exception) {
      super(perm);
      this.exception = exception;
    }

    public Throwable getException() {
      return exception;
    }
  }

  /**
   * Represents the task of compiling a single permutation.
   */
  private static final class PermutationTask implements Callable<String> {
    private static void logProperties(TreeLogger logger,
        StaticPropertyOracle propOracle) {
      Property[] props = propOracle.getOrderedProps();
      String[] values = propOracle.getOrderedPropValues();
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger = logger.branch(TreeLogger.DEBUG, "Setting properties", null);
        for (int i = 0; i < props.length; i++) {
          String name = props[i].getName();
          String value = values[i];
          logger.log(TreeLogger.TRACE, name + " = " + value, null);
        }
      }
    }

    private final JavaToJavaScriptCompiler jjs;
    private final TreeLogger logger;
    private final Permutation perm;

    public PermutationTask(TreeLogger logger, JavaToJavaScriptCompiler jjs,
        Permutation perm) {
      this.logger = logger;
      this.jjs = jjs;
      this.perm = perm;
    }

    public String call() throws Exception {
      PerfLogger.start("Permutation #" + (perm.getNumber() + 1));
      try {
        TreeLogger branch = logger.branch(TreeLogger.TRACE,
            "Analyzing permutation #" + (perm.getNumber() + 1));
        logProperties(branch, perm.getPropertyOracle());
        return jjs.compile(branch, perm.getRebindOracle());
      } finally {
        PerfLogger.end();
      }
    }

    public Permutation getPermutation() {
      return perm;
    }
  }

  /**
   * Contains the results of an attempt to compile.
   */
  private abstract static class Result {
    private final Permutation perm;

    public Result(Permutation perm) {
      this.perm = perm;
    }

    public Permutation getPermutation() {
      return perm;
    }
  }

  /**
   * A Result for a permutation that succeeded.
   */
  private static final class SuccessResult extends Result {
    private final String js;

    public SuccessResult(Permutation perm, String js) {
      super(perm);
      this.js = js;
    }

    public String getJs() {
      return js;
    }
  }

  /**
   * Implements a memory-sensitive worker thread to compile permutations.
   */
  private class WorkerThread implements Runnable {
    private PermutationTask currentTask;

    private final Runnable outOfMemoryRetryAction = new Runnable() {
      public void run() {
        currentTask.logger.log(
            TreeLogger.WARN,
            "Not enough memory to run another concurrent permutation, reducing thread count; "
                + "increasing the amount of memory by using the -Xmx flag "
                + "at startup may result in faster compiles");
        tasks.add(currentTask);
      }
    };

    public void run() {
      while (true) {
        currentTask = tasks.poll();
        if (currentTask == null) {
          // Nothing left to do.
          tryToExitNonFinalThread(null);

          // As the last thread, I must inform the main thread we're all done.
          results.add(FINISHED_RESULT);
          exitFinalThread();
        }

        if (!hasEnoughMemory()) {
          /*
           * Not enough memory to run, but if there are multiple threads, we can
           * try again with fewer threads.
           */
          tryToExitNonFinalThread(outOfMemoryRetryAction);
        }

        boolean definitelyFinalThread = (threadCount.get() == 1);
        try {
          String result = currentTask.call();
          results.add(new SuccessResult(currentTask.getPermutation(), result));
        } catch (OutOfMemoryError e) {
          if (definitelyFinalThread) {
            // OOM on the final thread, this is a truly unrecoverable failure.
            currentTask.logger.log(TreeLogger.ERROR, "Out of memory", e);
            results.add(new FailedResult(currentTask.getPermutation(),
                new UnableToCompleteException()));
            exitFinalThread();
          }

          /*
           * Try the task again with fewer threads, it may not OOM this time.
           */
          tryToExitNonFinalThread(outOfMemoryRetryAction);

          /*
           * Okay, so we actually are the final thread. However, we weren't the
           * final thread at the beginning of the compilation, so it's possible
           * that a retry may now succeed with only one active thread. Let's
           * optimistically retry one last time, and if this doesn't work, it's
           * a hard failure.
           */
          outOfMemoryRetryAction.run();
        } catch (Throwable e) {
          // Unexpected error compiling, this is unrecoverable.
          results.add(new FailedResult(currentTask.getPermutation(), e));
          throw new ThreadDeath();
        }
      }
    }

    private void exitFinalThread() {
      assert threadCount.compareAndSet(1, 0);
      throw new ThreadDeath();
    }

    /**
     * Returns <code>true</code> if there is enough estimated memory to run
     * another permutation, or if this is the last live worker thread and we
     * have no choice.
     */
    private boolean hasEnoughMemory() {
      if (threadCount.get() == 1) {
        // I'm the last thread, so I have to at least try.
        return true;
      }

      if (astMemoryUsage >= getPotentialFreeMemory()) {
        return true;
      }

      // Best effort memory reclaim.
      System.gc();
      return astMemoryUsage < getPotentialFreeMemory();
    }

    /**
     * Exits this thread if and only if it's not the last running thread,
     * performing the specified action before terminating.
     * 
     * @param actionOnExit
     */
    private void tryToExitNonFinalThread(Runnable actionOnExit) {
      int remainingThreads = threadCount.decrementAndGet();
      if (remainingThreads == 0) {
        // We are definitely the last thread.
        threadCount.incrementAndGet();
        return;
      }

      // We are definitely not the last thread, and have removed our count.
      if (actionOnExit != null) {
        actionOnExit.run();
      }
      throw new ThreadDeath();
    }
  }

  /**
   * A marker Result that tells the main thread all work is done.
   */
  private static final Result FINISHED_RESULT = new Result(null) {
  };

  private static long getPotentialFreeMemory() {
    long used = Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory();
    assert (used > 0);
    long potentialFree = Runtime.getRuntime().maxMemory() - used;
    assert (potentialFree >= 0);
    return potentialFree;
  }

  /**
   * Holds an estimate of how many bytes of memory a new concurrent compilation
   * will consume.
   */
  protected final long astMemoryUsage;

  /**
   * A queue of results being sent from worker threads to the main thread.
   */
  protected final BlockingQueue<Result> results = new LinkedBlockingQueue<Result>();

  /**
   * A queue of tasks being sent to the worker threads.
   */
  protected final ConcurrentLinkedQueue<PermutationTask> tasks = new ConcurrentLinkedQueue<PermutationTask>();

  /**
   * Tracks the number of live worker threads.
   */
  protected final AtomicInteger threadCount = new AtomicInteger();

  private final TreeLogger logger;

  public PermutationCompiler(TreeLogger logger, JavaToJavaScriptCompiler jjs,
      Permutation[] perms) {
    this.logger = logger.branch(TreeLogger.DEBUG, "Compiling " + perms.length
        + " permutations");
    this.astMemoryUsage = jjs.getAstMemoryUsage();
    for (Permutation perm : perms) {
      tasks.add(new PermutationTask(this.logger, jjs, perm));
    }
  }

  public void go(StandardLinkerContext linkerContext)
      throws UnableToCompleteException {
    int initialThreadCount = computeInitialThreadCount();
    Thread[] workerThreads = new Thread[initialThreadCount];
    for (int i = 0; i < initialThreadCount; ++i) {
      workerThreads[i] = new Thread(new WorkerThread());
    }
    threadCount.set(initialThreadCount);
    for (Thread thread : workerThreads) {
      thread.start();
    }
    try {
      while (true) {
        Result result = results.take();
        if (result == FINISHED_RESULT) {
          assert threadCount.get() == 0;
          return;
        } else if (result instanceof SuccessResult) {
          finishPermuation(logger, linkerContext, (SuccessResult) result);
        } else if (result instanceof FailedResult) {
          FailedResult failedResult = (FailedResult) result;
          throw logAndTranslateException(failedResult.getException());
        }
        // Allow GC.
        result = null;
      }

    } catch (InterruptedException e) {
      throw new RuntimeException("Unexpected interruption", e);
    } finally {
      for (Thread thread : workerThreads) {
        if (thread.isAlive()) {
          thread.interrupt();
        }
      }
    }
  }

  private int computeInitialThreadCount() {
    /*
     * Don't need more threads than the number of permutations.
     */
    int result = tasks.size();

    /*
     * Computation is mostly CPU bound, so don't use more threads than
     * processors.
     */
    result = Math.min(Runtime.getRuntime().availableProcessors(), result);

    /*
     * Allow user-defined override as an escape valve.
     */
    result = Math.min(result, Integer.getInteger("gwt.jjs.maxThreads", result));

    if (result == 1) {
      return 1;
    }

    // More than one thread would definitely be faster at this point.

    if (JProgram.isTracingEnabled()) {
      logger.log(TreeLogger.INFO,
          "Parallel compilation disabled due to gwt.jjs.traceMethods being enabled");
      return 1;
    }

    int desiredThreads = result;

    /*
     * Need to do some memory estimation to figure out how many concurrent
     * threads we can safely run.
     */
    long potentialFreeMemory = getPotentialFreeMemory();
    int extraMemUsageThreads = (int) (potentialFreeMemory / astMemoryUsage);
    logger.log(TreeLogger.TRACE,
        "Extra threads constrained by estimated memory usage: "
            + extraMemUsageThreads + " = " + potentialFreeMemory + " / "
            + astMemoryUsage);
    int memUsageThreads = extraMemUsageThreads + 1;

    if (memUsageThreads < desiredThreads) {
      long currentMaxMemory = Runtime.getRuntime().maxMemory();
      // Convert to megabytes.
      currentMaxMemory /= 1024 * 1024;

      long suggestedMaxMemory = currentMaxMemory * 2;

      logger.log(TreeLogger.WARN, desiredThreads
          + " threads could be run concurrently, but only " + memUsageThreads
          + " threads will be run due to limited memory; "
          + "increasing the amount of memory by using the -Xmx flag "
          + "at startup (java -Xmx" + suggestedMaxMemory
          + "M ...) may result in faster compiles");
    }

    result = Math.min(memUsageThreads, desiredThreads);
    return result;
  }

  private void finishPermuation(TreeLogger logger,
      StandardLinkerContext linkerContext, SuccessResult result)
      throws UnableToCompleteException {
    Permutation perm = result.getPermutation();
    StandardCompilationResult compilation = linkerContext.getCompilation(
        logger, result.getJs());
    StaticPropertyOracle propOracle = perm.getPropertyOracle();
    Property[] orderedProps = propOracle.getOrderedProps();
    String[] orderedPropValues = propOracle.getOrderedPropValues();
    Map<SelectionProperty, String> unboundProperties = new HashMap<SelectionProperty, String>();
    for (int i = 0; i < orderedProps.length; i++) {
      SelectionProperty key = linkerContext.getProperty(orderedProps[i].getName());
      if (key.tryGetValue() != null) {
        /*
         * The view of the Permutation doesn't include properties with defined
         * values.
         */
        continue;
      }
      unboundProperties.put(key, orderedPropValues[i]);
    }
    compilation.addSelectionPermutation(unboundProperties);
  }

  private UnableToCompleteException logAndTranslateException(Throwable e) {
    if (e instanceof UnableToCompleteException) {
      return (UnableToCompleteException) e;
    } else {
      logger.log(TreeLogger.ERROR, "Unexpected compiler failure", e);
      return new UnableToCompleteException();
    }
  }
}
