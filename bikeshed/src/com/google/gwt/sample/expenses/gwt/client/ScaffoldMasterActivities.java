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

import com.google.gwt.app.place.AbstractRecordListActivity;
import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.sample.expenses.gwt.client.place.ListScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlaceToRecordType;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldRecordPlace;
import com.google.gwt.sample.expenses.gwt.ui.ListActivitiesMapper;
import com.google.gwt.valuestore.shared.Record;

/**
 * Finds the activity to run for a particular {@link ScaffoldPlace} in the top
 * half of the {@link ScaffoldShell}.
 */
public final class ScaffoldMasterActivities implements
    ActivityMapper<ScaffoldPlace> {

  private final ListActivitiesMapper listActivities;

  private AbstractRecordListActivity<?> last = null;
  private Class<? extends Record> lastType = null;

  public ScaffoldMasterActivities(ListActivitiesMapper listActivities) {
    this.listActivities = listActivities;
  }

  public Activity getActivity(ScaffoldPlace place) {
    if (place instanceof ListScaffoldPlace) {
      ListScaffoldPlace listPlace = (ListScaffoldPlace) place;
      last = listActivities.getActivity(listPlace);
      lastType = listPlace.getType();
    }

    if (last != null && place instanceof ScaffoldRecordPlace) {
      Class<? extends Record> thisType = place.acceptFilter(new ScaffoldPlaceToRecordType());
      if (lastType.equals(thisType)) {
        last.select(((ScaffoldRecordPlace) place).getId());
      }
    }
    return last;
  }
}
