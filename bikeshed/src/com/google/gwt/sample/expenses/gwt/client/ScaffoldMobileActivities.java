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
import com.google.gwt.app.place.Place;

/**
 * Instantiates activities for the mobile app.
 */
public final class ScaffoldMobileActivities implements ActivityMapper {

  private final ExpensesMasterActivities listActivityBuilder;
  private final ExpensesDetailsActivities detailsActivityBuilder;

  public ScaffoldMobileActivities(ExpensesMasterActivities listActivitiesBuilder,
      ExpensesDetailsActivities detailsActivityBuilder) {
    this.listActivityBuilder = listActivitiesBuilder;
    this.detailsActivityBuilder = detailsActivityBuilder;
  }

  public Activity getActivity(Place place) {
    Activity rtn = listActivityBuilder.getActivity(place);
    if (rtn == null) {
      rtn = detailsActivityBuilder.getActivity(place);
    }
    return rtn;
  }
}
