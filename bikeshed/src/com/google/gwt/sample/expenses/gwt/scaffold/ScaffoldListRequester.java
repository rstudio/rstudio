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

import com.google.gwt.app.place.PlaceChanged;
import com.google.gwt.sample.expenses.gwt.place.ExpensesListPlace;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlaces;
import com.google.gwt.sample.expenses.gwt.request.EmployeeKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKeyVisitor;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportKey;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeListView;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportListView;
import com.google.gwt.user.client.ui.Renderer;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.valuestore.client.ValuesListViewTable;

import java.util.HashMap;
import java.util.Map;

/**
 * In charge of requesting and displaying the appropriate record lists in the
 * appropriate view when the user goes to an {@link ExpensesListPlace} in the
 * Scaffold app.
 */
public final class ScaffoldListRequester implements PlaceChanged.Handler {

  private final SimplePanel panel;
  private final ExpensesRequestFactory requests;
  private final Renderer<ExpensesListPlace> placeRenderer;
  private final ExpensesPlaces places;

  // TODO This dependency on view classes prevents testing this class in JRE.
  // Get a factory in here or something
  private final Map<ExpensesListPlace, ValuesListViewTable<?>> viewMap = new HashMap<ExpensesListPlace, ValuesListViewTable<?>>();

  public ScaffoldListRequester(ExpensesPlaces places, SimplePanel panel,
      ExpensesRequestFactory requests, Renderer<ExpensesListPlace> renderer) {
    this.places = places;
    this.panel = panel;
    this.requests = requests;
    this.placeRenderer = renderer;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof ExpensesListPlace)) {
      return;
    }
    final ExpensesListPlace newPlace = (ExpensesListPlace) event.getNewPlace();
    ExpensesKey<?> key = newPlace.getKey();

    key.accept(new ExpensesKeyVisitor() {

      public void visit(EmployeeKey employeeKey) {
        ValuesListViewTable<?> view = viewMap.get(newPlace);
        if (null == view) {
          view = new EmployeeListView(placeRenderer.render(newPlace), places,
              requests);
          viewMap.put(newPlace, view);
        }
      }

      public void visit(ReportKey reportKey) {
        ValuesListViewTable<?> view = viewMap.get(newPlace);
        if (null == view) {
          view = new ReportListView(placeRenderer.render(newPlace), places,
              requests);
          viewMap.put(newPlace, view);
        }
      }
    });

    ValuesListViewTable<?> entitiesView = viewMap.get(newPlace);
    if (entitiesView.getParent() == null) {
      panel.clear();
      panel.add(entitiesView);
    }
  }
}