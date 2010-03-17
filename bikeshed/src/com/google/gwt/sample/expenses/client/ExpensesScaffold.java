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
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.PlacePicker;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.client.place.ExpensesScaffoldPlace;
import com.google.gwt.sample.expenses.client.place.Places;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.user.client.ui.RootLayoutPanel;


/**
 * Application for browsing the entities of the Expenses app.
 */
public class ExpensesScaffold implements EntryPoint {

  public void onModuleLoad() {
    final ExpenseRequestFactory requests = GWT.create(ExpenseRequestFactory.class);
    final HandlerManager eventBus = new HandlerManager(null);
    final PlaceController<ExpensesScaffoldPlace> placeController = new PlaceController<ExpensesScaffoldPlace>(
        eventBus);

    final ExpensesScaffoldShell shell = new ExpensesScaffoldShell();

    PlacePicker<ExpensesScaffoldPlace> placePicker = new PlacePicker<ExpensesScaffoldPlace>(
        shell.getPlacesBox(), placeController);
    placePicker.setPlaces(Places.getListPlacesAndNames());

    // TODO Shouldn't create this until it's actually needed
    final TableEntityListView entitiesView = new TableEntityListView();

    eventBus.addHandler(PlaceChanged.TYPE, new ListRequester(shell.getBody(),
        entitiesView, requests));

    Element loading = Document.get().getElementById("loading");
    loading.getParentElement().removeChild(loading);

    RootLayoutPanel.get().add(shell);
  }
}
