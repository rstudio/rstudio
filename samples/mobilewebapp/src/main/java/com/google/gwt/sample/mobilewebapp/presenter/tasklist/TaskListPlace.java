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
package com.google.gwt.sample.mobilewebapp.presenter.tasklist;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * The place in the app that shows a list of tasks.
 */
public class TaskListPlace extends Place {

  /**
   * The tokenizer for this place. TaskList doesn't have any state, so we don't
   * have anything to encode.
   */
  @Prefix("tl")
  public static class Tokenizer implements PlaceTokenizer<TaskListPlace> {

    public TaskListPlace getPlace(String token) {
      return new TaskListPlace(true);
    }

    public String getToken(TaskListPlace place) {
      return "";
    }
  }

  private final boolean taskListStale;

  /**
   * Construct a new {@link TaskListPlace}.
   * 
   * @param taskListStale true if the task list is stale and should be cleared
   */
  public TaskListPlace(boolean taskListStale) {
    this.taskListStale = taskListStale;
  }

  /**
   * Check if the task list is stale and should be cleared.
   * 
   * @return true if stale, false if not
   */
  public boolean isTaskListStale() {
    return taskListStale;
  }
}
