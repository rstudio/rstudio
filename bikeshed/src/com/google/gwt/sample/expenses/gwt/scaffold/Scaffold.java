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
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.PlacePicker;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.gwt.place.ExpensesListPlace;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlace;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlaces;
import com.google.gwt.sample.expenses.gwt.request.ExpensesEntityTypesProcessor;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.ui.ExpensesKeyNameRenderer;
import com.google.gwt.sample.expenses.gwt.ui.ListPlaceRenderer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.valuestore.shared.Record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Application for browsing the entities of the Expenses app.
 */
public class Scaffold implements EntryPoint {

  public void onModuleLoad() {

    // App controllers and services
    final HandlerManager eventBus = new HandlerManager(null);
    final ExpensesRequestFactory requestFactory = GWT.create(ExpensesRequestFactory.class);
    requestFactory.init(eventBus);

    final PlaceController<ExpensesPlace> placeController = new PlaceController<ExpensesPlace>(
        eventBus);
    final ExpensesPlaces places = new ExpensesPlaces(placeController);

    // Renderers
    final ExpensesKeyNameRenderer entityNamer = new ExpensesKeyNameRenderer();
    final ListPlaceRenderer listPlaceNamer = new ListPlaceRenderer();

    // Top level UI
    final ScaffoldShell shell = new ScaffoldShell();

    // Left side
    PlacePicker<ExpensesListPlace> placePicker = new PlacePicker<ExpensesListPlace>(
        shell.getPlacesBox(), placeController, listPlaceNamer);
    List<ExpensesListPlace> topPlaces = getTopPlaces();
    placePicker.setPlaces(topPlaces);

    // Shows entity lists
    eventBus.addHandler(PlaceChanged.TYPE, new ScaffoldListRequester(
        shell.getBody(), new ScaffoldListViewBuilder(places, requestFactory,
            listPlaceNamer)));

    // Shared view for entity details.
    final HTML detailsView = new HTML(); // TODO Real app should not share?

    // Shows entity details
    eventBus.addHandler(PlaceChanged.TYPE, new ScaffoldDetailsRequester(
        entityNamer, shell.getBody(), detailsView,
        new ScaffoldDetailsViewBuilder()));

    // Hide the loading message
    Element loading = Document.get().getElementById("loading");
    loading.getParentElement().removeChild(loading);

    RootLayoutPanel.get().add(shell);
  }

  private List<ExpensesListPlace> getTopPlaces() {
    final List<ExpensesListPlace> rtn = new ArrayList<ExpensesListPlace>();
    ExpensesEntityTypesProcessor.processAll(new ExpensesEntityTypesProcessor.EntityTypesProcessor() {
      public void processType(Class<? extends Record> recordType) {
        rtn.add(new ExpensesListPlace(recordType));
      }
    });
    return Collections.unmodifiableList(rtn);
  }
}
