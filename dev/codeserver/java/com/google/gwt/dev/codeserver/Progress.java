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

import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedMap;

/**
 * A snapshot of a {@link Job}'s current state, for progress dialogs.
 */
class Progress {

  /**
   * The id of the job being compiled. (Unique within the same CodeServer process.)
   */
  final String jobId;

  final String inputModuleName;
  final ImmutableSortedMap<String, String> bindings;
  final Status status;

  Progress(Job job, Status status) {
    this.jobId = job.getId();
    this.inputModuleName = job.getInputModuleName();
    this.bindings = job.getBindingProperties();
    this.status = status;
  }

  /**
   * Returns true if the job's progress should be shown in the progress view.
   * (For jobs that are GONE, their status is only available by request.)
   */
  public boolean isActive() {
    return status == Status.WAITING || status == Status.COMPILING || status == Status.SERVING;
  }

  JsonObject toJsonObject() {
    JsonObject out = new JsonObject();
    out.put("jobId", jobId);
    out.put("inputModule", inputModuleName);
    out.put("bindings", getBindingsJson());
    out.put("status", status.jsonName);
    return out;
  }

  private JsonObject getBindingsJson() {
    JsonObject out = new JsonObject();
    for (String name : bindings.keySet()) {
      out.put(name, bindings.get(name));
    }
    return out;
  }

  /**
   * Defines the lifecycle of a job.
   */
  static enum Status {
    WAITING("waiting"),
    COMPILING("compiling"),
    SERVING("serving"), // Output is available to HTTP requests
    GONE("gone"); // Output directory is no longer being served

    final String jsonName;

    Status(String jsonName) {
      this.jsonName = jsonName;
    }
  }

  /**
   * Returned when a compile is in progress.
   */
  static class Compiling extends Progress {

    /**
     * The number of steps finished, for showing progress.
     */
    final int finishedSteps;

    /**
     * The number of steps total, for showing progress.
     */
    final int totalSteps;

    /**
     * A message about the current step being executed.
     */
    final String stepMessage;

    Compiling(Job job, int finishedSteps, int totalSteps, String stepMessage) {
      super(job, Status.COMPILING);
      this.finishedSteps = finishedSteps;
      this.totalSteps = totalSteps;
      this.stepMessage = stepMessage;
    }

    @Override
    JsonObject toJsonObject() {
      JsonObject out = super.toJsonObject();
      out.put("finishedSteps", finishedSteps);
      out.put("totalSteps", totalSteps);
      out.put("stepMessage", stepMessage);
      return out;
    }
  }
}
