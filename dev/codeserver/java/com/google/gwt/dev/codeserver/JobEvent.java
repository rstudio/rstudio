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

import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefSchema;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedMap;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * The status of a compile job submitted to Super Dev Mode.
 *
 * <p>JobEvent objects are deeply immutable, though they describe a Job that changes.
 */
public final class JobEvent {

  private final String jobId;

  private final String inputModuleName;
  private final ImmutableSortedMap<String, String> bindings;
  private final Status status;
  private final String message;

  private final CompileDir compileDir;
  private final CompileStrategy compileStrategy;
  private final ImmutableList<String> arguments;

  private JobEvent(Builder builder) {
    this.jobId = Preconditions.checkNotNull(builder.jobId);
    this.inputModuleName = Preconditions.checkNotNull(builder.inputModuleName);
    this.bindings = ImmutableSortedMap.copyOf(builder.bindings);
    this.status = Preconditions.checkNotNull(builder.status);
    this.message = builder.message == null ? status.defaultMessage : builder.message;

    // The following fields may be null.
    this.compileDir = builder.compileDir;
    this.compileStrategy = builder.compileStrategy;
    this.arguments = ImmutableList.copyOf(builder.args);

    // Any new fields added should allow nulls for backward compatibility.
  }

  /**
   * The id of the job being compiled. Unique within the same CodeServer process.
   * This should be considered an opaque string.
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * The module name sent to the GWT compiler to start the compile.
   */
  public String getInputModuleName() {
    return inputModuleName;
  }

  /**
   * The binding properties sent to the GWT compiler.
   */
  public SortedMap<String, String> getBindings() {
    // Can't return ImmutableSortedMap here because it's repackaged and this is a public API.
    return bindings;
  }

  /**
   * The last reported status of the job.
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Returns a line of text describing the job's current status.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the directory where the GWT module is being compiled, or null if not available.
   * (Not available for jobs that are WAITING.)
   */
  public CompileDir getCompileDir() {
    return compileDir;
  }

  /**
   * Returns the strategy used to perform the compile or null if not available.
   * (Normally available for finished compiles.)
   */
  public CompileStrategy getCompileStrategy() {
    return compileStrategy;
  }

  /**
   * The arguments passed to Super Dev Mode at startup, or null if not available.
   */
  public ImmutableList<String> getArguments() {
    return arguments;
  }

  /**
   * Defines the lifecycle of a job.
   */
  public enum Status {
    WAITING("waiting", "Waiting for the compiler to start"),
    COMPILING("compiling", "Compiling"),
    SERVING("serving", "Compiled output is ready"),
    GONE("gone", "Compiled output is no longer available"),
    ERROR("error", "Compile failed with an error");

    final String jsonName;
    final String defaultMessage;

    Status(String jsonName, String defaultMessage) {
      this.jsonName = jsonName;
      this.defaultMessage = defaultMessage;
    }
  }

  /**
   * The approach taken to do the compile.
   */
  public enum CompileStrategy {
    FULL("full"), // Compiled all the source.
    INCREMENTAL("incremental"), // Only recompiled the source files that changed.
    SKIPPED("skipped"); // Did not compile anything since nothing changed

    final String jsonName;

    CompileStrategy(String jsonName) {
      this.jsonName = jsonName;
    }

    /**
     * The string to use for serialization.
     */
    String getJsonName() {
      return jsonName;
    }
  }

  /**
   * Creates a JobEvent.
   * This is public to allow external tests of code that implements {@link JobChangeListener}.
   * Normally all JobEvents are created in the code server.
   */
  public static class Builder {
    private String jobId;

    private String inputModuleName;
    private Map<String, String> bindings = ImmutableMap.of();
    private Status status;
    private String message;
    private CompileDir compileDir;
    private CompileStrategy compileStrategy;
    private List<String> args = ImmutableList.of();

    /**
     * A unique id for this job. Required.
     */
    public void setJobId(String jobId) {
      Preconditions.checkArgument(Job.isValidJobId(jobId), "invalid job id: " + jobId);
      this.jobId = jobId;
    }

    /**
     * The name of the module as passed to the compiler. Required.
     */
    public void setInputModuleName(String inputModuleName) {
      Preconditions.checkArgument(ModuleDef.isValidModuleName(inputModuleName),
          "invalid module name: " + jobId);
      this.inputModuleName = inputModuleName;
    }

    /**
     * The bindings passed to the compiler.
     * Optional, but may not be null. (Defaults to the empty map.)
     */
    public void setBindings(Map<String, String> bindings) {
      for (String name : bindings.keySet()) {
        if (!ModuleDefSchema.isValidPropertyName(name)) {
          throw new IllegalArgumentException("invalid property name: " + name);
        }
      }
      this.bindings = bindings;
    }

    /**
     * The job's current status. Required.
     */
    public void setStatus(Status status) {
      this.status = status;
    }

    /**
     * A message to describing the job's current state.
     * It should be a single line of text.
     * Optional. If null, a default message will be used.
     */
    public void setMessage(String message) {
      if (message != null) {
        Preconditions.checkArgument(!message.contains("\n"),
            "JobEvent messages should be a single line of text");
      }
      this.message = message;
    }

    /**
     * The directory where the GWT compiler will write its output.
     * Optional. (Not available until the compile starts.)
     */
    public void setCompileDir(CompileDir compileDir) {
      this.compileDir = compileDir;
    }

    /**
     * The strategy used to perform the compile.
     * Optional.
     */
    public void setCompileStrategy(CompileStrategy compileStrategy) {
      this.compileStrategy = compileStrategy;
    }

    /**
     * The arguments passed to {@link Options#parseArgs} at startup.
     * Optional but may not be null. If not set, defaults to the empty list.
     */
    public void setArguments(List<String> args) {
      this.args = Preconditions.checkNotNull(args);
    }

    public JobEvent build() {
      return new JobEvent(this);
    }
  }
}
