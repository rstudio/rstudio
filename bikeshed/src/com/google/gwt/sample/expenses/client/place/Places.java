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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.app.place.PlaceController;
import com.google.gwt.bikeshed.cells.client.ActionCell;
import com.google.gwt.sample.expenses.shared.EmployeeKey;
import com.google.gwt.sample.expenses.shared.ExpensesEntityKey;
import com.google.gwt.sample.expenses.shared.ReportKey;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Object with knowledge of the places of the ExpensesScaffold app.
 */
public class Places {
  private final PlaceController<AbstractExpensesPlace> controller;

  private final List<EntityListPlace> listPlaces;

  public Places(PlaceController<AbstractExpensesPlace> controller) {
    this.controller = controller;

    ArrayList<EntityListPlace> places = new ArrayList<EntityListPlace>();
    places.add(new EntityListPlace(EmployeeKey.get()));
    places.add(new EntityListPlace(ReportKey.get()));
    listPlaces = Collections.unmodifiableList(places);
  }

  public <K extends ExpensesEntityKey<K>> ActionCell.Delegate<Values<K>> getDetailsGofer() {
    return new ActionCell.Delegate<Values<K>>() {
      public void execute(Values<K> object) {
        goToDetailsFor(object);
      }
    };
  }

  public <K extends ExpensesEntityKey<K>> ActionCell.Delegate<Values<K>> getEditorGofer() {
    return new ActionCell.Delegate<Values<K>>() {
      public void execute(Values<K> object) {
        goToEditorFor(object);
      }
    };
  }

  public List<EntityListPlace> getListPlaces() {
    return listPlaces;
  }

  private void goToDetailsFor(Values<? extends ExpensesEntityKey<?>> e) {
    controller.goTo(new EntityDetailsPlace(e));
  }

  private void goToEditorFor(Values<? extends ExpensesEntityKey<?>> e) {
    controller.goTo(new EditEntityPlace(e));
  }
}
