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
import com.google.gwt.valuestore.shared.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * In charge of requesting and displaying the appropriate entity lists when the
 * user goes to an {@link EntityListPlace}.
 */
public final class ListRequester implements PlaceChanged.Handler {

  private final SimplePanel panel;
  private final TableEntityListView entitiesView;
  private final ExpenseRequestFactory requests;
  private final Renderer<EntityListPlace> listNameFilter;
  private final Places places;

  public ListRequester(Places places, SimplePanel panel,
      TableEntityListView entitiesView, ExpenseRequestFactory requests,
      Renderer<EntityListPlace> renderer) {
    this.places = places;
    this.panel = panel;
    this.entitiesView = entitiesView;
    this.requests = requests;
    this.listNameFilter = renderer;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof EntityListPlace)) {
      return;
    }
    EntityListPlace newPlace = (EntityListPlace) event.getNewPlace();
    final String name = listNameFilter.render(newPlace);

    final ExpensesEntityKey<?> key = newPlace.getKey();

    // TODO Would be simpler if every entity key knew its find method
    key.accept(new ExpensesEntityVisitor() {

      public void visit(EmployeeKey employeeKey) {
        List<Property<EmployeeKey, ?>> columns = getEmployeeColumns();
        EntityListPresenter<EmployeeKey> presenter = new EntityListPresenter<EmployeeKey>(
            name, entitiesView, columns, places);
        requests.employeeRequest().findAllEmployees().forProperties(columns).to(
            presenter).fire();
      }

      public void visit(ReportKey reportKey) {
        List<Property<ReportKey, ?>> columns = getReportColumns();
        EntityListPresenter<ReportKey> presenter = new EntityListPresenter<ReportKey>(
            name, entitiesView, columns, places);
        requests.reportRequest().findAllReports().forProperties(columns).to(
            presenter).fire();
      }
    });

    if (entitiesView.getParent() == null) {
      panel.clear();
      panel.add(entitiesView);
    }
  }

  private List<Property<EmployeeKey, ?>> getEmployeeColumns() {
    List<Property<EmployeeKey, ?>> columns = new ArrayList<Property<EmployeeKey, ?>>();
    columns.add(EmployeeKey.get().getUserName());
    columns.add(EmployeeKey.get().getDisplayName());
    return columns;
  }

  private List<Property<ReportKey, ?>> getReportColumns() {
    List<Property<ReportKey, ?>> columns = new ArrayList<Property<ReportKey, ?>>();
    columns.add(ReportKey.get().getCreated());
    columns.add(ReportKey.get().getPurpose());
    return columns;
  }
}