/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.sample.expenses.gwt.client.place.ListScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;

/**
 * Finds the activity to run for a particular {@link ScaffoldPlace} in the top
 * half of the {@link ScaffoldShell}.
 */
public final class ScaffoldMasterActivities implements
    ActivityMapper<ScaffoldPlace> {

  private final ActivityMapper<ListScaffoldPlace> listActivities;

  private Activity last = null;

  public ScaffoldMasterActivities(
      ActivityMapper<ListScaffoldPlace> listActivities) {
    this.listActivities = listActivities;
  }

  public Activity getActivity(ScaffoldPlace place) {
    if (place instanceof ListScaffoldPlace) {
      last = listActivities.getActivity((ListScaffoldPlace) place);
    }

    return last;
  }
}
