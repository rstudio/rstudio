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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.app.place.PlaceChanged;
import com.google.gwt.sample.expenses.client.place.EntityListPlace;
import com.google.gwt.sample.expenses.client.place.Places;
import com.google.gwt.sample.expenses.shared.EmployeeKey;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.sample.expenses.shared.ExpensesEntityKey;
import com.google.gwt.sample.expenses.shared.ExpensesEntityVisitor;
import com.google.gwt.sample.expenses.shared.ReportKey;
import com.google.gwt.user.client.ui.Renderer;
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.HashMap;
import java.util.Map;

/**
 * In charge of requesting and displaying the appropriate entity lists when the
 * user goes to an {@link EntityListPlace}.
 */
public final class ListRequester implements PlaceChanged.Handler {

  private final SimplePanel panel;
  private final ExpenseRequestFactory requests;
  private final Renderer<EntityListPlace> placeRenderer;
  private final Places places;

  // TODO This dependency on view classes prevents testing this class in JRE.
  // Get a factory in here or something
  private final Map<EntityListPlace, ValuesListViewTable<?>> viewMap = new HashMap<EntityListPlace, ValuesListViewTable<?>>();

  public ListRequester(Places places, SimplePanel panel,
      ExpenseRequestFactory requests, Renderer<EntityListPlace> renderer) {
    this.places = places;
    this.panel = panel;
    this.requests = requests;
    this.placeRenderer = renderer;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof EntityListPlace)) {
      return;
    }
    final EntityListPlace newPlace = (EntityListPlace) event.getNewPlace();
    ExpensesEntityKey<?> key = newPlace.getKey();

    key.accept(new ExpensesEntityVisitor() {

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