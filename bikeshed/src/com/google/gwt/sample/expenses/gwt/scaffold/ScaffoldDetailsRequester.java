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
import com.google.gwt.sample.expenses.gwt.place.ExpensesDetailsPlace;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKey;
import com.google.gwt.sample.expenses.gwt.ui.ExpensesKeyNameRenderer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.valuestore.shared.Values;

/**
 * In charge of requesting and displaying details of a particular record in the
 * appropriate view when the user goes to an {@link ExpensesDetailsPlace} in the
 * Scaffold app.
 */
public final class ScaffoldDetailsRequester implements PlaceChanged.Handler {

  private final ExpensesKeyNameRenderer entityNamer;
  private final SimplePanel panel;
  private final HTML detailsView;
  private final ScaffoldDetailsViewBuilder detailsBuilder;

  public ScaffoldDetailsRequester(ExpensesKeyNameRenderer entityNamer,
      SimplePanel simplePanel, HTML detailsView,
      ScaffoldDetailsViewBuilder detailsBuilder) {
    this.entityNamer = entityNamer;
    this.panel = simplePanel;
    this.detailsView = detailsView;
    this.detailsBuilder = detailsBuilder;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof ExpensesDetailsPlace)) {
      return;
    }

    ExpensesDetailsPlace newPlace = (ExpensesDetailsPlace) event.getNewPlace();
    final Values<? extends ExpensesKey<?>> values = newPlace.getEntity();
    ExpensesKey<?> key = values.getKey();

    final String title = new StringBuilder("<h1>").append(
        entityNamer.render(key)).append("</h1>").toString();

    // TODO would actually issue request to get details here, but
    // at the moment we know we already have them, such as they are.
    // And we haven't implemented the fake findEmployee call yet, ahem.

    StringBuilder list = new StringBuilder();
    detailsBuilder.appendHtmlDescription(list, values);
    detailsView.setHTML(title + list.toString());

    if (detailsView.getParent() == null) {
      panel.clear();
      panel.add(detailsView);
    }
  }
}