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
package com.google.gwt.sample.expenses.gwt.ui.report;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.sample.expenses.gwt.client.place.ReportScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;

/**
 * Maps {@link ReportScaffoldPlace} instances to the {@link Activity} to run.
 */
public class ReportActivitiesMapper implements
    ActivityMapper<ReportScaffoldPlace> {
  private final ExpensesRequestFactory requests;
  private final PlaceController<ScaffoldPlace> placeController;

  public ReportActivitiesMapper(ExpensesRequestFactory requests, PlaceController<ScaffoldPlace> placeController) {
    this.requests = requests;
    this.placeController = placeController;
  }

  public Activity getActivity(ReportScaffoldPlace place) {
    switch (place.getOperation()) {
      case DETAILS:
        return new ReportDetailsActivity(place.getId(), requests, placeController);
      case EDIT:
        return new ReportEditActivity(place.getId(), requests, placeController);
    }

    throw new IllegalArgumentException("Unknown operation "
        + place.getOperation());
  }
}
