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
  private final OutboxTable outboxes;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  JobRunner(JobEventTable table, OutboxTable outboxes) {
    this.table = table;
    this.outboxes = outboxes;
  }

  /**
   * Return fresh Js that knows how to request the specific permutation recompile for the given box.
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
        recompile(job, outboxes);
      }
    });
    job.getLogger().log(Type.TRACE, "added job to queue");
  }

  private static void recompile(Job job, OutboxTable outboxes) {
    job.getLogger().log(Type.INFO, "starting job: " + job.getId());
    job.getOutbox().recompile(job);
  }
}
