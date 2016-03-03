/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.javac.UnitCacheSingleton;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes requests to compile modules using Super Dev Mode.
 *
 * <p>Guarantees that only one thread invokes the GWT compiler at a time and reports
 * progress on waiting jobs.
 *
 * <p>JobRunners are thread-safe.
 */
public class JobRunner {

  private final JobEventTable table;
  private final MinimalRebuildCacheManager minimalRebuildCacheManager;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  JobRunner(JobEventTable table, MinimalRebuildCacheManager minimalRebuildCacheManager) {
    this.table = table;
    this.minimalRebuildCacheManager = minimalRebuildCacheManager;
  }

  /**
   * Submits a cleaner job to be executed. (Waits for completion.)
   */
  void clean(final TreeLogger logger, final OutboxTable outboxTable) throws ExecutionException {
    try {
      TreeLogger branch = logger.branch(TreeLogger.INFO, "Cleaning disk caches.");
      executor.submit(new CleanerJob(branch, outboxTable)).get();
    } catch (InterruptedException e) {
      // Allow the JVM to shutdown.
    }
  }

  /**
   * Submits a recompile js creation job to be executed. (Waits for completion and returns JS.).
   */
  public String getRecompileJs(final TreeLogger logger, final Outbox box)
      throws ExecutionException {
    try {
      return executor.submit(new Callable<String>() {
        @Override
        public String call() throws Exception {
          return box.getRecompileJs(logger);
        }
      }).get();
    } catch (InterruptedException e) {
      // Allow the JVM to shutdown.
      return null;
    }
  }

  /**
   * Submits a job to be executed. (Returns immediately.)
   */
  synchronized void submit(final Job job) {
    if (table.wasSubmitted(job)) {
      throw new IllegalStateException("job already submitted: " + job.getId());
    }
    job.onSubmitted(table);
    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          recompile(job);
        } catch (Throwable t) {
          // Try to release the job so the HTTP request will return an error.
          // (But this might not work if the same exception is thrown while
          // sending the finished event.)
          if (!job.isDone()) {
            try {
              job.onFinished(new Job.Result(null, null, t));
              return;
            } catch (Throwable t2) {
              // fall through and log original exception
            }
          }
          // Assume everything is broken. Last-ditch attempt to report the error.
          t.printStackTrace();
        }
      }
    });
    job.getLogger().log(Type.TRACE, "added job to queue");
  }

  private static void recompile(Job job) {
    job.getLogger().log(Type.INFO, "starting job: " + job.getId());
    job.getOutbox().recompile(job);
  }

  /**
   * A callable for clearing both unit and minimalRebuild caches. It also forces the next recompile
   * even if no input files have changed.
   * <p>
   * By packaging it as a callable and running it in the ExecutorService any danger of clearing
   * caches at the same time as an active compile job is avoided.
   */
  private class CleanerJob implements Callable<Void> {

    private final OutboxTable outboxTable;
    private TreeLogger logger;

    public CleanerJob(final TreeLogger logger, final OutboxTable outboxTable) {
      this.logger = logger;
      this.outboxTable = outboxTable;
    }

    @Override
    public Void call() throws UnableToCompleteException {
      long beforeMs = System.nanoTime() / 1000000L;
      minimalRebuildCacheManager.deleteCaches();
      UnitCacheSingleton.clearCache();
      outboxTable.forceNextRecompileAll();
      long afterMs = System.nanoTime() / 1000000L;
      logger.log(TreeLogger.INFO, String.format("Cleaned in %sms.", (afterMs - beforeMs)));
      return null;
    }
  }
}
