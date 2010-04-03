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
import com.google.gwt.sample.expenses.gwt.request.EmployeeKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKeyVisitor;
import com.google.gwt.sample.expenses.gwt.request.ReportKey;
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

  public ScaffoldDetailsRequester(ExpensesKeyNameRenderer entityNamer,
      SimplePanel simplePanel, HTML detailsView) {
    this.entityNamer = entityNamer;
    this.panel = simplePanel;
    this.detailsView = detailsView;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof ExpensesDetailsPlace)) {
      return;
    }
    ExpensesDetailsPlace newPlace = (ExpensesDetailsPlace) event.getNewPlace();
    final Values<? extends ExpensesKey<?>> entity = newPlace.getEntity();
    ExpensesKey<?> key = entity.getKey();

    // TODO make a pretty uibinder page, not least because we're rendering
    // user strings here, which is dangerous

    final String title = new StringBuilder("<h1>").append(
        entityNamer.render(key)).append("</h1>").toString();

    final StringBuilder list = new StringBuilder();

    // TODO would actually issue request to get details here, but
    // at the moment we know we already have them, such as they are.
    // And we haven't implemented the fake findEmployee call yet, ahem.

    key.accept(new ExpensesKeyVisitor() {

      @SuppressWarnings("unchecked")
      public void visit(EmployeeKey employeeKey) {
        // wow, this cast is nasty. Perhaps visitor should actually
        // be on the Values themselves?
        Values<EmployeeKey> eValues = (Values<EmployeeKey>) entity;
        String user = eValues.get(EmployeeKey.get().getUserName());
        list.append("<div>");
        list.append("<label>").append("User Name: ").append("</label>");
        list.append("<span>").append(user).append("</span>");
        list.append("</div>");

        list.append("<div>");
        String display = eValues.get(EmployeeKey.get().getDisplayName());
        list.append("<label>").append("Display Name: ").append("</label>");
        list.append("<span>").append(display).append("</span>");
        list.append("</div>");
      }

      @SuppressWarnings("unchecked")
      public void visit(ReportKey reportKey) {
        Values<ReportKey> rValues = (Values<ReportKey>) entity;
        String purpose = rValues.get(ReportKey.get().getPurpose());
        list.append("<div>");
        list.append("<label>").append("Purpose: ").append("</label>");
        list.append("<span>").append(purpose).append("</span>");
        list.append("</div>");

        list.append("<div>");
        String created = rValues.get(ReportKey.get().getCreated()).toString();
        list.append("<label>").append("Created: ").append("</label>");
        list.append("<span>").append(created).append("</span>");
        list.append("</div>");
      }
    });

    detailsView.setHTML(title + list.toString());

    if (detailsView.getParent() == null) {
      panel.clear();
      panel.add(detailsView);
    }
  }
}