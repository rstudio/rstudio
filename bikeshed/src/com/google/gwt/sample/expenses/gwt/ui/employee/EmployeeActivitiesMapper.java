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
package com.google.gwt.sample.expenses.gwt.ui.employee;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.scaffold.place.EmployeeScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.scaffold.place.ScaffoldPlace;

/**
 * Maps {@link EmployeeScaffoldPlace} instances to the {@link Activity} to run.
 */
public class EmployeeActivitiesMapper implements
    ActivityMapper<EmployeeScaffoldPlace> {
  private final ExpensesRequestFactory requests;
  private final PlaceController<ScaffoldPlace> placeController;

  public EmployeeActivitiesMapper(ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    this.requests = requests;
    this.placeController = placeController;
  }

  public Activity getActivity(EmployeeScaffoldPlace place) {
    switch (place.getOperation()) {
      case DETAILS:
        return new EmployeeDetailsActivity(place.getId(), requests);

      case EDIT:
        return new EmployeeEditActivity(place.getId(), requests,
            placeController);
    }

    throw new IllegalArgumentException("Unknown operation "
        + place.getOperation());
  }
}
