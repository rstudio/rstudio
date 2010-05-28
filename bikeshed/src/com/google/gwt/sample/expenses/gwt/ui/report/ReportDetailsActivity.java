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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.client.place.ReportScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldRecordPlace.Operation;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.valuestore.shared.Value;
import com.google.gwt.valuestore.ui.RecordDetailsView;

/**
 * An {@link com.google.gwt.app.place.Activity Activity} that requests and
 * displays detailed information on a given report.
 */
public class ReportDetailsActivity extends AbstractActivity implements
    RecordDetailsView.Delegate {
  private static RecordDetailsView<ReportRecord> defaultView;

  private static RecordDetailsView<ReportRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new ReportDetailsView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;
  private final PlaceController<ScaffoldPlace> placeController;
  private final RecordDetailsView<ReportRecord> view;
  private String id;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public ReportDetailsActivity(String id, ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    this(id, requests, placeController, getDefaultView());
  }

  /**
   * Creates an activity that uses its own view instance.
   */
  public ReportDetailsActivity(String id, ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController,
      RecordDetailsView<ReportRecord> view) {
    this.placeController = placeController;
    this.id = id;
    this.requests = requests;
    view.setDelegate(this);
    this.view = view;
  }

  public void editClicked() {
    placeController.goTo(new ReportScaffoldPlace(view.getValue(),
        Operation.EDIT));
  }

  public void start(final Display display) {
    Receiver<ReportRecord> callback = new Receiver<ReportRecord>() {
      public void onSuccess(ReportRecord record) {
        view.setValue(record);
        display.showActivityWidget(view);
      }
    };
    requests.reportRequest().findReport(Value.of(id)).to(callback).fire();
  }
}
