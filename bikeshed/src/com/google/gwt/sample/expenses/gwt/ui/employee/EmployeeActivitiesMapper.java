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
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.ProxyPlace;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;

/**
 * Maps {@link ProxyPlace} instances to the {@link Activity} to run.
 */
public class EmployeeActivitiesMapper {
  private final ExpensesRequestFactory requests;
  private final PlaceController placeController;

  public EmployeeActivitiesMapper(ExpensesRequestFactory requests,
      PlaceController placeController) {
    this.requests = requests;
    this.placeController = placeController;
  }

  public Activity getActivity(ProxyPlace place) {
    switch (place.getOperation()) {
      case DETAILS:
        return new EmployeeDetailsActivity((EmployeeRecord) place.getProxy(),
            requests, placeController);

      case EDIT:
        return new EmployeeEditActivity((EmployeeRecord) place.getProxy(),
            requests, placeController, false);
        
      case CREATE:
        return new EmployeeEditActivity((EmployeeRecord) place.getProxy(),
            requests, placeController, true);
    }

    throw new IllegalArgumentException("Unknown operation "
        + place.getOperation());
  }
}
