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

import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportKey;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import com.google.gwt.valuestore.shared.ValuesListView;

/**
 * An implementation of {@link ValuesListView.Delegate} that requests all
 * {@link ReportKey} records.
 */
public final class AllReportsRequester implements ValuesListView.Delegate {
  private final ValuesListViewTable<ReportKey> view;
  private final ExpensesRequestFactory requests;

  public AllReportsRequester(ExpensesRequestFactory requests,
      ValuesListViewTable<ReportKey> newView) {
    this.view = newView;
    this.requests = requests;
  }

  public void onRangeChanged(int start, int length) {
    // TODO use start and length
    requests.reportRequest().findAllReports().forProperties(
        view.getProperties()).to(view).fire();
  }
}