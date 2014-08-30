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
import com.google.gwt.dev.codeserver.Progress.Status;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains the progress of all the jobs that have been submitted to the JobRunner.
 */
class ProgressTable {

  /**
   * The progress of each known job, by job id.
   */
  private final Map<String, Progress> progressById = Maps.newHashMap();

  /**
   * A set of submitted job ids that are still active, in the order they were submitted.
   * Jobs that are waiting, compiling, or serving are considered active.
   */
  private final Set<String> activeJobIds = new LinkedHashSet<String>();

  /**
   * The set of compiling job ids. Since compiling is single-threaded, this set should
   * contain zero or one entries.
   */
  private final Set<String> compilingJobIds = new LinkedHashSet<String>();

  /**
   * Publishes the progress of a job after it changed. (This replaces any previous progress.)
   */
  synchronized void publish(Progress progress, TreeLogger logger) {
    String id = progress.jobId;

    progressById.put(id, progress);

    // Update indexes

    if (progress.isActive()) {
      activeJobIds.add(id);
    } else {
      activeJobIds.remove(id);
    }

    if (progress.status == Status.COMPILING) {
      compilingJobIds.add(id);
      assert compilingJobIds.size() <= 1;
    } else {
      compilingJobIds.remove(id);
    }

    logger.log(Type.TRACE, "job's progress set to " + progress.status + ": " + id);
  }

  /**
   * Returns true if the job's status was ever published.
   */
  synchronized boolean wasSubmitted(Job job) {
    return progressById.containsKey(job.getId());
  }

  synchronized boolean isActive(Job job) {
    return activeJobIds.contains(job.getId());
  }

  /**
   * Returns the progress of the job that's currently being compiled, or null if idle.
   */
  synchronized Progress getProgressForCompilingJob() {
    if (compilingJobIds.isEmpty()) {
      return null;
    }

    String id = compilingJobIds.iterator().next();
    Progress progress = progressById.get(id);
    assert progress != null;
    return progress;
  }

  /**
   * Returns the progress of all active jobs, in the order submitted.
   * TODO: hook this up.
   */
  synchronized ImmutableList<Progress> getProgressForActiveJobs() {
    ImmutableList.Builder<Progress> builder = ImmutableList.builder();
    for (String id : activeJobIds) {
      Progress p = progressById.get(id);
      assert p != null;
      builder.add(p);
    }
    return builder.build();
  }
}
