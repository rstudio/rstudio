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

import com.google.gwt.app.place.AbstractRecordListActivity;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.RecordListView;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RecordListRequest;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecordChanged;
import com.google.gwt.view.client.Range;

/**
 * Activity that requests and displays all {@ReportRecord}
 * records.
 */
public final class ReportListActivity extends
    AbstractRecordListActivity<ReportRecord> {

  private static RecordListView<ReportRecord> defaultView;

  private static RecordListView<ReportRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new ReportListView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;

  /**
   * Creates an activity that uses the default singleton view instance.
   * 
   * @param proxyPlaceToListPlace
   */
  public ReportListActivity(ExpensesRequestFactory requests,
      PlaceController placeController) {
    this(requests, getDefaultView(), placeController);
  }

  /**
   * Creates an activity that uses the given view instance.
   */
  public ReportListActivity(ExpensesRequestFactory requests,
      RecordListView<ReportRecord> view, PlaceController placeController) {
    super(requests, placeController, view, ReportRecord.class);

    this.requests = requests;
  }

  @Override
  public void start(Display display, EventBus eventBus) {
    // TODO(rjrjr) this can move to super class when event bus gets smarter
    eventBus.addHandler(ReportRecordChanged.TYPE,
        new ReportRecordChanged.Handler() {
          public void onReportChanged(ReportRecordChanged event) {
            update(event.getWriteOperation(), event.getRecord());
          }
        });
    super.start(display, eventBus);
  }

  @Override
  protected RecordListRequest<ReportRecord> createRangeRequest(Range range) {
    return requests.reportRequest().findReportEntries(range.getStart(),
        range.getLength());
  }

  @Override
  protected void fireCountRequest(Receiver<Long> callback) {
    requests.reportRequest().countReports().fire(callback);
  }
}
