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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * In charge of requesting and displaying the appropriate record lists in the
 * appropriate view when the user goes to an {@link ExpensesListPlace} in the
 * Scaffold app.
 */
public final class ScaffoldListRequester implements PlaceChanged.Handler {

  private final SimplePanel panel;
  private final ScaffoldListViewBuilder builder;

  public ScaffoldListRequester(SimplePanel panel,
      ScaffoldListViewBuilder builder) {
    this.panel = panel;
    this.builder = builder;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof ExpensesListPlace)) {
      return;
    }
    final ExpensesListPlace newPlace = (ExpensesListPlace) event.getNewPlace();

    Widget view = builder.getListView(newPlace).asWidget();

    if (null == view) {
      throw new RuntimeException("Unable to locate a view for " + newPlace);
    }

    if (view.getParent() == null) {
      panel.clear();
      panel.add(view);
    }
  }
}