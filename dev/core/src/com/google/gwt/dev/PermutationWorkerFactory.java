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
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a factory for implementations of an endpoint that will invoke
 * CompilePerms. Implementations of PermutationWorkerFactory should be
 * default-instantiable and will have {@link #init} called immediately after
 * construction.
 */
public abstract class PermutationWorkerFactory {

  /**
   * Coordinates the actions of a set of {@link PermutationWorker}s, running
   * each in its own thread.
   */
  private static class Manager {

    private static enum Result {
      SUCCESS, FAIL, WORKER_DEATH
    }

    /**
     * Runs a {@link PermutationWorker} on its own thread.
     */
    private class WorkerThread implements Runnable {
      private final PermutationWorker worker;

      public WorkerThread(PermutationWorker worker) {
        this.worker = worker;
      }

      public void run() {
        Result threadDeathResult = Result.FAIL;
        try {
          while (true) {
            Work work = workQueue.take();
            if (work == POISON_PILL) {
              return;
            }
            TreeLogger logger = work.getLogger();
            try {
              worker.compile(logger, work.getPerm(), work.getResultFile());
              logger.log(TreeLogger.DEBUG, "Successfully compiled permutation");
              resultsQueue.put(Result.SUCCESS);
            } catch (TransientWorkerException e) {
              logger.log(TreeLogger.DEBUG,
                  "Worker died, will retry Permutation", e);
              workQueue.add(work);
              threadDeathResult = Result.WORKER_DEATH;
              return;
            } catch (UnableToCompleteException e) {
              logger.log(TreeLogger.ERROR,
                  "Unrecoverable exception, shutting down", e);
              return;
            }
          }
        } catch (InterruptedException e) {
          return;
        } finally {
          // Record why I died.
          try {
            resultsQueue.put(threadDeathResult);
          } catch (InterruptedException ignored) {
          }
        }
      }
    }

    private static final Work POISON_PILL = new Work(null, null, null);

    public static void run(TreeLogger logger, List<Work> work,
        List<PermutationWorker> workers) throws UnableToCompleteException {
      new Manager().doRun(logger, work, workers);
    }

    /**
     * The queue of work to do.
     */
    BlockingQueue<Work> workQueue;

    /**
     * The queue of work to do.
     */
    BlockingQueue<Result> resultsQueue;

    private Manager() {
    }

    private void doRun(TreeLogger logger, List<Work> work,
        List<PermutationWorker> workers) throws UnableToCompleteException {

      // Initialize state.
      workQueue = new LinkedBlockingQueue<Work>(work);
      resultsQueue = new LinkedBlockingQueue<Result>();

      List<Thread> threads = new ArrayList<Thread>(workers.size());
      try {
        for (PermutationWorker worker : workers) {
          Thread thread = new Thread(new WorkerThread(worker), worker.getName());
          threads.add(thread);
          thread.start();
        }

        int workToDo = work.size();
        int aliveWorkers = workers.size();
        waitForWorkers : while (workToDo > 0 && aliveWorkers > 0) {
          Event blockedEvent = SpeedTracerLogger.start(CompilerEventType.BLOCKED);
          Result take = resultsQueue.take();
          blockedEvent.end();
          switch (take) {
            case SUCCESS:
              --workToDo;
              break;
            case FAIL:
              break waitForWorkers;
            case WORKER_DEATH:
              --aliveWorkers;
              break;
            default:
              throw new IncompatibleClassChangeError(Result.class.toString());
          }
        }

        workQueue.clear();
        for (int i = 0; i < aliveWorkers; ++i) {
          workQueue.add(POISON_PILL);
        }

        if (workToDo > 0) {
          logger.log(TreeLogger.ERROR,
              "Not all permutation were compiled , completed ("
                  + (work.size() - workToDo) + "/" + work.size() + ")");
          throw new UnableToCompleteException();
        }
      } catch (InterruptedException e) {
        logger.log(TreeLogger.ERROR,
            "Exiting without results due to interruption", e);
        throw new UnableToCompleteException();
      } finally {
        // Interrupt any outstanding threads.
        for (Thread thread : threads) {
          thread.interrupt();
        }
      }
    }
  }

  /**
   * Represents work to do.
   */
  private static class Work {
    private final TreeLogger logger;
    private final Permutation perm;
    private final FileBackedObject<PermutationResult> resultFile;

    public Work(TreeLogger logger, Permutation perm,
        FileBackedObject<PermutationResult> resultFile) {
      this.logger = logger;
      this.perm = perm;
      this.resultFile = resultFile;
    }

    public TreeLogger getLogger() {
      return logger;
    }

    public Permutation getPerm() {
      return perm;
    }

    public FileBackedObject<PermutationResult> getResultFile() {
      return resultFile;
    }
  }

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

  /**
   * Compiles all Permutations in a Precompilation and returns an array of Files
   * that can be consumed by Link using the system-default
   * PermutationWorkersFactories.
   */
  public static void compilePermutations(TreeLogger logger,
      Precompilation precompilation, int localWorkers,
      List<FileBackedObject<PermutationResult>> resultFiles)
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
      int localWorkers, List<FileBackedObject<PermutationResult>> resultFiles)
      throws UnableToCompleteException {
    assert permutations.length == resultFiles.size();
    assert Arrays.asList(precompilation.getPermutations()).containsAll(
        Arrays.asList(permutations));

    // Create the work.
    List<Work> work = new ArrayList<Work>(permutations.length);
    for (int i = 0; i < permutations.length; ++i) {
      Permutation perm = permutations[i];
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG,
            "Creating worker permutation " + perm.getId() + " of " + permutations.length);
      }
      work.add(new Work(logger, perm, resultFiles.get(i)));
    }

    // Create the workers.
    List<PermutationWorker> workers = new ArrayList<PermutationWorker>();
    try {
      createWorkers(logger, precompilation.getUnifiedAst(), work.size(),
          localWorkers, workers);

      // Get it done!
      Manager.run(logger, work, workers);
    } finally {
      Throwable caught = null;
      for (PermutationWorker worker : workers) {
        try {
          worker.shutdown();
        } catch (Throwable e) {
          caught = e;
        }
      }
      if (caught != null) {
        throw new RuntimeException(
            "One of the workers threw an exception while shutting down", caught);
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

    List<PermutationWorkerFactory> mutableFactories = new ArrayList<PermutationWorkerFactory>();
    String classes = System.getProperty(FACTORY_IMPL_PROPERTY,
        ThreadedPermutationWorkerFactory.class.getName() + ","
            + ExternalPermutationWorkerFactory.class.getName());
    if (logger.isLoggable(TreeLogger.SPAM)) {
      logger.log(TreeLogger.SPAM, "Factory impl property is " + classes);
    }

    String[] classParts = classes.split(",");
    for (String className : classParts) {
      try {
        Class<? extends PermutationWorkerFactory> clazz = Class.forName(
            className).asSubclass(PermutationWorkerFactory.class);
        PermutationWorkerFactory factory = clazz.newInstance();
        factory.init(logger);
        mutableFactories.add(factory);
        if (logger.isLoggable(TreeLogger.SPAM)) {
          logger.log(TreeLogger.SPAM, "Added PermutationWorkerFactory "
              + clazz.getName());
        }
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

    return Collections.unmodifiableList(mutableFactories);
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
