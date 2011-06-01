/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;

/**
 * The place in the app that show a task in an editable view.
 */
public class TaskEditPlace extends Place {

  /**
   * The tokenizer for this place.
   */
  @Prefix("edit")
  public static class Tokenizer implements PlaceTokenizer<TaskEditPlace> {

    private static final String NO_ID = "create";

    public TaskEditPlace getPlace(String token) {
      try {
        // Parse the task ID from the URL.
        Long taskId = Long.parseLong(token);
        return new TaskEditPlace(taskId, null);
      } catch (NumberFormatException e) {
        // If the ID cannot be parsed, assume we are creating a task.
        return TaskEditPlace.getTaskCreatePlace();
      }
    }

    public String getToken(TaskEditPlace place) {
      Long taskId = place.getTaskId();
      return (taskId == null) ? NO_ID : taskId.toString();
    }
  }

  /**
   * The singleton instance of this place used for creation.
   */
  private static TaskEditPlace singleton;

  /**
   * Create an instance of {@link TaskEditPlace} associated with the specified
   * task ID.
   * 
   * @param taskId the ID of the task to edit
   * @param task the task to edit, or null if not available
   * @return the place
   */
  public static TaskEditPlace createTaskEditPlace(Long taskId, TaskProxy task) {
    return new TaskEditPlace(taskId, task);
  }

  /**
   * Get the singleton instance of the {@link TaskEditPlace} used to create a
   * new task.
   * 
   * @return the place
   */
  public static TaskEditPlace getTaskCreatePlace() {
    if (singleton == null) {
      singleton = new TaskEditPlace(null, null);
    }
    return singleton;
  }

  private final TaskProxy task;
  private final Long taskId;

  /**
   * Construct a new {@link TaskEditPlace} for the specified task id.
   * 
   * @param taskId the ID of the task to edit
   * @param task the task to edit, or null if not available
   */
  private TaskEditPlace(Long taskId, TaskProxy task) {
    this.taskId = taskId;
    this.task = task;
  }

  /**
   * Get the task to edit.
   * 
   * @return the task to edit, or null if not available
   */
  public TaskProxy getTask() {
    return task;
  }

  /**
   * Get the ID of the task to edit.
   * 
   * @return the ID of the task, or null if creating a new task
   */
  public Long getTaskId() {
    return taskId;
  }
}
