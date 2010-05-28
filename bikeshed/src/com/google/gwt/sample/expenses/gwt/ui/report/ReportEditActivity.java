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

import com.google.gwt.app.place.AbstractActivity;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.scaffold.place.ScaffoldPlace;
import com.google.gwt.user.client.ui.Label;

/**
 * An activity that requests all info on a report, allows the user to edit it,
 * and persists the results.
 */
public class ReportEditActivity extends AbstractActivity {

  public ReportEditActivity(String id, ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    // TODO Auto-generated constructor stub
  }

  public void start(Callback callback) {
    callback.onStarted(new Label("tbd"));
  }

}
