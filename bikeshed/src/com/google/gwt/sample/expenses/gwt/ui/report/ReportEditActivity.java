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

import com.google.gwt.app.place.AbstractRecordEditActivity;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.RecordEditView;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.client.place.ReportScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldRecordPlace.Operation;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.valuestore.shared.Value;

/**
 * An activity that requests all info on a report, allows the user to edit it,
 * and persists the results.
 */
public class ReportEditActivity extends
    AbstractRecordEditActivity<ReportRecord> {
  private static RecordEditView<ReportRecord> defaultView;

  private static RecordEditView<ReportRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new ReportEditView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;
  private final PlaceController<ScaffoldPlace> placeController;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public ReportEditActivity(Long id, ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    this(id, getDefaultView(), requests, placeController);
  }

  /**
   * Creates an activity that uses its own view instance.
   */
  public ReportEditActivity(Long id, RecordEditView<ReportRecord> view,
      ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    super(view, id, requests);
    this.requests = requests;
    this.placeController = placeController;
  }

  protected void exit() {
    placeController.goTo(new ReportScaffoldPlace(getId(), Operation.DETAILS));
  }

  @Override
  protected void fireFindRequest(Value<Long> id,
      Receiver<ReportRecord> callback) {
    requests.reportRequest().findReport(id).fire(callback);
  }
  
  @Override
  protected Class getRecordClass() {
    return ReportRecord.class;
  }

  protected void setRequestObject(ReportRecord record) {
    requestObject = requests.reportRequest().persist(record);
  }
}
