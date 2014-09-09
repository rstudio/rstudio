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
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains the current status of each {@link Job}.
 * (That is, the most recently reported event.)
 */
class JobEventTable {

  /**
   * The most recent event sent by each job.
   */
  private final Map<String, JobEvent> eventsByJobId = Maps.newHashMap();

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
   * Returns the event that's currently published for the given job.
   */
  synchronized JobEvent getPublishedEvent(Job job) {
    return eventsByJobId.get(job.getId());
  }

  /**
   * Publishes the progress of a job after it changed. (This replaces any previous progress.)
   */
  synchronized void publish(JobEvent event, TreeLogger logger) {
    String id = event.getJobId();

    eventsByJobId.put(id, event);

    // Update indexes

    if (isActive(event.getStatus())) {
      activeJobIds.add(id);
    } else {
      activeJobIds.remove(id);
    }

    if (event.getStatus() == Status.COMPILING) {
      compilingJobIds.add(id);
      assert compilingJobIds.size() <= 1;
    } else {
      compilingJobIds.remove(id);
    }

    logger.log(Type.TRACE, "job's progress set to " + event.getStatus() + ": " + id);
  }

  private static boolean isActive(Status status) {
    return status == Status.WAITING || status == Status.COMPILING || status == Status.SERVING;
  }

  /**
   * Returns true if the job's status was ever published.
   */
  synchronized boolean wasSubmitted(Job job) {
    return eventsByJobId.containsKey(job.getId());
  }

  synchronized boolean isActive(Job job) {
    return activeJobIds.contains(job.getId());
  }

  /**
   * Returns an event indicating the current status of the job that's currently being compiled,
   * or null if idle.
   */
  synchronized JobEvent getCompilingJobEvent() {
    if (compilingJobIds.isEmpty()) {
      return null;
    }

    String id = compilingJobIds.iterator().next();
    JobEvent event = eventsByJobId.get(id);
    assert event != null;
    return event;
  }

  /**
   * Returns an event indicating the current status of each active job, in the order they were
   * submitted.
   * TODO: hook this up.
   */
  synchronized ImmutableList<JobEvent> getActiveEvents() {
    ImmutableList.Builder<JobEvent> builder = ImmutableList.builder();
    for (String id : activeJobIds) {
      JobEvent p = eventsByJobId.get(id);
      assert p != null;
      builder.add(p);
    }
    return builder.build();
  }
}
