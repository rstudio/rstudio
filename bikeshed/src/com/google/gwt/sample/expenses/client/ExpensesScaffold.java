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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.client.place.AbstractExpensesPlace;
import com.google.gwt.sample.expenses.client.place.EntityListPlace;
import com.google.gwt.sample.expenses.client.place.Places;
import com.google.gwt.sample.expenses.gen.ExpenseRequestFactoryImpl;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.valuestore.client.ValueStoreJsonImpl;

/**
 * Application for browsing the entities of the Expenses app.
 */
public class ExpensesScaffold implements EntryPoint {

  public void onModuleLoad() {

    // App controllers and services
    final HandlerManager eventBus = new HandlerManager(null);
    final ValueStoreJsonImpl valueStore = new ValueStoreJsonImpl(eventBus);
    final ExpenseRequestFactoryImpl requests = new ExpenseRequestFactoryImpl(valueStore);
    final PlaceController<AbstractExpensesPlace> placeController = new PlaceController<AbstractExpensesPlace>(
        eventBus);
    final Places places = new Places(placeController);

    // Renderers
    final EntityNameRenderer entityNamer = new EntityNameRenderer();
    final ListPlaceRenderer listPlaceNamer = new ListPlaceRenderer(entityNamer);
    
    
    // Top level UI
    final ExpensesScaffoldShell shell = new ExpensesScaffoldShell();
    
    // Left side
    PlacePicker<EntityListPlace> placePicker = new PlacePicker<EntityListPlace>(
        shell.getPlacesBox(), placeController, listPlaceNamer);
    placePicker.setPlaces(places.getListPlaces());

    // Shared view for entity lists. Perhaps real app would have
    // a separate view per type?
    final TableEntityListView entitiesView = new TableEntityListView();
    eventBus.addHandler(PlaceChanged.TYPE, new ListRequester(places,
        shell.getBody(), entitiesView, requests, listPlaceNamer));
    
    // Shared view for entity details. Again, perhaps real app should not share
    final HTML detailsView = new HTML();
    eventBus.addHandler(PlaceChanged.TYPE, new DetailsRequester(entityNamer,
        shell.getBody(), detailsView));

    // Hide the loading message
    Element loading = Document.get().getElementById("loading");
    loading.getParentElement().removeChild(loading);

    RootLayoutPanel.get().add(shell);
  }
}
