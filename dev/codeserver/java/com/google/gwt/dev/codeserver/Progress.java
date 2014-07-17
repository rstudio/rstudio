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

/**
 * A snapshot of the compiler's current state, for progress dialogs.
 */
abstract class Progress {

  /**
   * Returns the json representation of this progress snapshot.
   */
  abstract JsonObject toJsonObject();

  /**
   * Returned when no compile is running.
   */
  static final Progress IDLE = new Progress() {

    @Override
    JsonObject toJsonObject() {
      JsonObject out = new JsonObject();
      out.put("status", "idle");
      return out;
    }
  };

  /**
   * Returned when a compile is in progress.
   */
  static class Compiling extends Progress {

    /**
     * The module being compiled.
     */
    final String module;

    /**
     * Identifies the currently running compile.
     * (It's unique within the same CodeServer process and module.)
     */
    final int compileId;

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

    Compiling(String module, int compileId, int finishedSteps, int totalSteps, String stepMessage) {
      this.module = module;
      this.compileId = compileId;
      this.finishedSteps = finishedSteps;
      this.totalSteps = totalSteps;
      this.stepMessage = stepMessage;
    }

    @Override
    JsonObject toJsonObject() {
      JsonObject out = new JsonObject();
      out.put("status", "compiling");
      out.put("module", module);
      out.put("compileId", compileId);
      out.put("finishedSteps", finishedSteps);
      out.put("totalSteps", totalSteps);
      out.put("stepMessage", stepMessage);
      return out;
    }
  }
}
