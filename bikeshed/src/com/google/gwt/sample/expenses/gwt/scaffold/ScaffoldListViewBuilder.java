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
package com.google.gwt.sample.expenses.gwt.scaffold;

import com.google.gwt.sample.expenses.gwt.place.ExpensesListPlace;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlaces;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.ui.employee.AllEmployeesRequester;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeListView;
import com.google.gwt.sample.expenses.gwt.ui.report.AllReportsRequester;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportListView;
import com.google.gwt.user.client.ui.Renderer;
import com.google.gwt.valuestore.client.ValuesListViewTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Returns the view instances for {@link ExpensesListPlace}s, paired with
 * objects to request all records of the appropriate type.
 */
public class ScaffoldListViewBuilder {
  private final ExpensesRequestFactory requests;
  private final Renderer<ExpensesListPlace> placeRenderer;
  private final ExpensesPlaces places;
  private final Map<ExpensesListPlace, ValuesListViewTable<?>> viewMap = new HashMap<ExpensesListPlace, ValuesListViewTable<?>>();

  public ScaffoldListViewBuilder(ExpensesPlaces places,
      ExpensesRequestFactory requests, Renderer<ExpensesListPlace> renderer) {
    this.places = places;
    this.requests = requests;
    this.placeRenderer = renderer;
  }

  public ValuesListViewTable<?> getListView(final ExpensesListPlace newPlace) {
    // TODO Will these class references prevent customized apps that keep this
    // view builder around from stripping unsued entity types?
    if (!viewMap.containsKey(newPlace)) {
      if (newPlace.getType().equals(EmployeeRecord.class)) {
        EmployeeListView newView = new EmployeeListView(
            placeRenderer.render(newPlace), places);
        newView.setDelegate(new AllEmployeesRequester(requests, newView));
        viewMap.put(newPlace, newView);
      }

      if (newPlace.getType().equals(ReportRecord.class)) {
        ReportListView newView = new ReportListView(
            placeRenderer.render(newPlace), places);
        newView.setDelegate(new AllReportsRequester(requests, newView));
        viewMap.put(newPlace, newView);
      }
    }
    return viewMap.get(newPlace);
  }
}
