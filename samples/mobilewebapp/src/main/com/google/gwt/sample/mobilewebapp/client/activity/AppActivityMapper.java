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
package com.google.gwt.sample.mobilewebapp.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.Provider;
import com.google.gwt.sample.mobilewebapp.client.place.TaskEditPlace;
import com.google.gwt.sample.mobilewebapp.client.place.TaskListPlace;

/**
 * A mapping of places to activities used by this application.
 */
public class AppActivityMapper implements ActivityMapper {

  private final ClientFactory clientFactory;
  private final Provider<Boolean> isTaskListIncluded;

  public AppActivityMapper(ClientFactory clientFactory, Provider<Boolean> isTaskListIncluded) {
    this.isTaskListIncluded = isTaskListIncluded;
    this.clientFactory = clientFactory;
  }

  public Activity getActivity(Place place) {
    if (place instanceof TaskListPlace) {
      // The list of tasks.
      if (!isTaskListIncluded.get()) {
        // Do not start a task list activity if the task list is always visible.
        return new TaskListActivity(clientFactory, (TaskListPlace) place);
      }
    } else if (place instanceof TaskEditPlace) {
      // Editable view of a task.
      return new TaskEditActivity(clientFactory, (TaskEditPlace) place);
    }
    return null;
  }

}
