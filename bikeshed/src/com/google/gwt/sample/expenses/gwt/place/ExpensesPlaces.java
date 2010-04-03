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
package com.google.gwt.sample.expenses.gwt.place;

import com.google.gwt.app.place.PlaceController;
import com.google.gwt.bikeshed.cells.client.ActionCell;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKey;
import com.google.gwt.valuestore.shared.Values;

/**
 * Object with knowledge of the places of an app.
 */
public class ExpensesPlaces {
  private final PlaceController<ExpensesPlace> controller;

  public ExpensesPlaces(PlaceController<ExpensesPlace> controller) {
    this.controller = controller;
  }

  public <K extends ExpensesKey<K>> ActionCell.Delegate<Values<K>> getDetailsGofer() {
    return new ActionCell.Delegate<Values<K>>() {
      public void execute(Values<K> object) {
        goToDetailsFor(object);
      }
    };
  }

  public <K extends ExpensesKey<K>> ActionCell.Delegate<Values<K>> getEditorGofer() {
    return new ActionCell.Delegate<Values<K>>() {
      public void execute(Values<K> object) {
        goToEditorFor(object);
      }
    };
  }

  private void goToDetailsFor(Values<? extends ExpensesKey<?>> e) {
    controller.goTo(new ExpensesDetailsPlace(e));
  }

  private void goToEditorFor(Values<? extends ExpensesKey<?>> e) {
    controller.goTo(new ExpensesEditorPlace(e));
  }
}
