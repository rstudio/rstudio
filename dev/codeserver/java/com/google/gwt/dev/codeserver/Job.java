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
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.codeserver.JobEvent.CompileStrategy;
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedMap;
import com.google.gwt.thirdparty.guava.common.util.concurrent.Futures;
import com.google.gwt.thirdparty.guava.common.util.concurrent.ListenableFuture;
import com.google.gwt.thirdparty.guava.common.util.concurrent.SettableFuture;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A request for Super Dev Mode to compile something.
 *
 * <p>Each job has a lifecycle where it goes through up to four states. See
 * {@link JobEvent.Status}.
 *
 * <p>Jobs are thread-safe.
 */
class Job {
  private static final ConcurrentMap<String, AtomicInteger> prefixToNextId =
      new ConcurrentHashMap<String, AtomicInteger>();

  // Primary key

  private final String id;

  // Input

  private final String inputModuleName;

  private final ImmutableSortedMap<String, String> bindingProperties;

  // Output

  private final SettableFuture<Result> result = SettableFuture.create();

  // Listeners

  private final Outbox outbox;
  private final RecompileListener recompileListener;
  private final JobChangeListener jobChangeListener;
  private final LogSupplier logSupplier;

  private JobEventTable table; // non-null when submitted

  // Miscellaneous

  private final ImmutableList<String> args;
  private final Set<String> tags;

  /**
   * The id to report to the recompile listener.
   */
  private int compileId = -1; // non-negative after the compile has started

  private CompileDir compileDir; // non-null after the compile has started
  private CompileStrategy compileStrategy; // non-null after the compile has started
  private String outputModuleName; // non-null after successful compile

  private Exception listenerFailure;

  /**
   * Creates a job to update an outbox.
   * @param bindingProperties  Properties that uniquely identify a permutation.
   *     (Otherwise, more than one permutation will be compiled.)
   * @param parentLogger  The parent of the logger that will be used for this job.
   */
  Job(Outbox box, Map<String, String> bindingProperties,
      TreeLogger parentLogger, Options options) {
    this.id = chooseNextId(box);
    this.outbox = box;
    this.inputModuleName = box.getInputModuleName();
    // TODO: we will use the binding properties to find or create the outbox,
    // then take binding properties from the outbox here.
    this.bindingProperties = ImmutableSortedMap.copyOf(bindingProperties);
    this.recompileListener = Preconditions.checkNotNull(options.getRecompileListener());
    this.jobChangeListener = Preconditions.checkNotNull(options.getJobChangeListener());
    this.args = Preconditions.checkNotNull(options.getArgs());
    this.tags = Preconditions.checkNotNull(options.getTags());
    this.logSupplier = new LogSupplier(parentLogger, id);
  }

  static boolean isValidJobId(String id) {
    return ModuleDef.isValidModuleName(id);
  }

  private static String chooseNextId(Outbox box) {
    String prefix = box.getId();
    prefixToNextId.putIfAbsent(prefix, new AtomicInteger(0));
    return prefix + "_" + prefixToNextId.get(prefix).getAndIncrement();
  }

  /**
   * A string uniquely identifying this job (within this process).
   *
   * <p>Note that the number doesn't have any particular relationship
   * with the output directory's name since jobs can be submitted out of order.
   */
  String getId() {
    return id;
  }

  /**
   * The module name that will be sent to the compiler.
   */
  String getInputModuleName() {
    return inputModuleName;
  }

  /**
   * The binding properties to use for this recompile.
   */
  ImmutableSortedMap<String, String> getBindingProperties() {
    return bindingProperties;
  }

  /**
   * The outbox that will serve the job's result (if successful).
   */
  Outbox getOutbox() {
    return outbox;
  }

  /**
   * Returns the logger for this job. (Creates it on first use.)
   */
  TreeLogger getLogger() {
    return logSupplier.get();
  }

  /**
   * Blocks until we have the result of this recompile.
   */
  Result waitForResult() {
    return Futures.getUnchecked(getFutureResult());
  }

  /**
   * Returns a Future that will contain the result of this recompile.
   */
  ListenableFuture<Result> getFutureResult() {
    return result;
  }

  Exception getListenerFailure() {
    return listenerFailure;
  }

  // === state transitions ===

  /**
   * Returns true if this job has been submitted to the JobRunner.
   * (That is, if {@link #onSubmitted} has ever been called.)
   */
  synchronized boolean wasSubmitted() {
    return table != null;
  }

  boolean isDone() {
    return result.isDone();
  }

  /**
   * Reports that this job has been submitted to the JobRunner.
   * Starts sending updates to the JobTable.
   * @throws IllegalStateException if the job was already started.
   */
  synchronized void onSubmitted(JobEventTable table) {
    if (wasSubmitted()) {
      throw new IllegalStateException("compile job has already started: " + id);
    }
    this.table = table;
    table.publish(makeEvent(Status.WAITING), getLogger());
  }

  /**
   * Reports that we started to compile the job.
   */
  synchronized void onStarted(int compileId, CompileDir compileDir) {
    if (table == null || !table.isActive(this)) {
      throw new IllegalStateException("compile job is not active: " + id);
    }
    this.compileId = compileId;
    this.compileDir = compileDir;

    try {
      recompileListener.startedCompile(inputModuleName, compileId, compileDir);
    } catch (Exception e) {
      getLogger().log(TreeLogger.Type.WARN, "recompile listener threw exception", e);
      listenerFailure = e;
    }

    publish(makeEvent(Status.COMPILING));
  }

  /**
   * Reports that this job has made progress while compiling.
   * @throws IllegalStateException if the job is not running.
   */
  synchronized void onProgress(String stepMessage) {
    checkIsCompiling("onProgress");
    publish(makeEvent(Status.COMPILING, stepMessage));
  }

  synchronized void setCompileStrategy(CompileStrategy strategy) {
    checkIsCompiling("setCompileStrategy");
    if (compileStrategy != null) {
      throw new IllegalStateException("setCompileStrategy can only be set once per job");
    }
    this.compileStrategy = strategy;
    // Not bothering to send an event just for this change, so it will be included
    // in the next event.
  }

  /**
   * Reports that this job has finished.
   * @throws IllegalStateException if the job is not running.
   */
  synchronized void onFinished(Result newResult) {
    if (table == null || !table.isActive(this)) {
      throw new IllegalStateException("compile job is not active: " + id);
    }

    // Report that we finished unless the listener messed up already.
    if (listenerFailure == null) {
      try {
        recompileListener.finishedCompile(inputModuleName, compileId, newResult.isOk());
      } catch (Exception e) {
        getLogger().log(TreeLogger.Type.WARN, "recompile listener threw exception", e);
        listenerFailure = e;
      }
    }

    result.set(newResult);
    outputModuleName = newResult.outputModuleName;
    if (newResult.isOk()) {
      publish(makeEvent(Status.SERVING));
    } else {
      publish(makeEvent(Status.ERROR));
    }
  }

  /**
   * Reports that this job's output is no longer available.
   */
  synchronized void onGone() {
    if (table == null || !table.isActive(this)) {
      throw new IllegalStateException("compile job is not active: " + id);
    }
    publish(makeEvent(Status.GONE));
  }

  private JobEvent makeEvent(Status status) {
    return makeEvent(status, null);
  }

  private JobEvent makeEvent(Status status, String message) {
    JobEvent.Builder out = new JobEvent.Builder();
    out.setJobId(getId());
    out.setInputModuleName(getInputModuleName());
    out.setBindings(getBindingProperties());
    out.setStatus(status);
    out.setMessage(message);
    out.setOutputModuleName(outputModuleName);
    out.setCompileDir(compileDir);
    out.setCompileStrategy(compileStrategy);
    out.setArguments(args);
    out.setTags(tags);
    out.setMetricMap(getMetricMapSnapshot());
    return out.build();
  }

  private Map<String, Long> getMetricMapSnapshot() {
    TreeLogger logger = getLogger();
    if (logger instanceof AbstractTreeLogger) {
      return ((AbstractTreeLogger)logger).getMetricMap().getSnapshot();
    }
    return ImmutableMap.of(); // not found
  }

  /**
   * Makes an event visible externally.
   */
  private void publish(JobEvent event) {
    if (listenerFailure == null) {
      try {
        jobChangeListener.onJobChange(event);
      } catch (Exception e) {
        getLogger().log(Type.WARN, "JobChangeListener threw exception", e);
        listenerFailure = e;
      }
    }
    table.publish(event, getLogger());
  }

  private void checkIsCompiling(String methodName) {
    if (table == null || table.getPublishedEvent(this).getStatus() != Status.COMPILING) {
      throw new IllegalStateException(methodName + " called for a job that isn't compiling: " + id);
    }
  }

  /**
   * Creates a child logger on first use.
   */
  static class LogSupplier {
    private final TreeLogger parent;
    private final String jobId;
    private TreeLogger child;

    LogSupplier(TreeLogger parent, String jobId) {
      this.parent = parent;
      this.jobId = jobId;
    }

    synchronized TreeLogger get() {
      if (child == null) {
        child = parent.branch(Type.INFO, "Job " + jobId);
        if (child instanceof AbstractTreeLogger) {
          ((AbstractTreeLogger)child).resetMetricMap();
        }
      }
      return child;
    }
  }

  /**
   * The result of a recompile.
   */
  static class Result {

    /**
     * non-null if successful
     */
    final CompileDir outputDir;

    /**
     * non-null if successful.
     */
    final String outputModuleName;

    /**
     * non-null for an error
     */
    final Throwable error;

    Result(CompileDir outputDir, String outputModuleName, Throwable error) {
      assert (outputDir == null) != (error == null);
      this.outputDir = outputDir;
      this.outputModuleName = outputModuleName;
      this.error = error;
    }

    boolean isOk() {
      return error == null;
    }
  }
}
