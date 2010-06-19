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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityManager;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.IsWidget;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.PlacePicker;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.gwt.client.place.ListScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.request.ExpensesEntityTypesProcessor;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.ui.ListActivitiesMapper;
import com.google.gwt.sample.expenses.gwt.ui.ScaffoldListPlaceRenderer;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.valuestore.shared.Record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mobile application for browsing the entities of the Expenses app.
 * 
 * TODO(jgw): Make this actually mobile-friendly.
 */
public class ScaffoldMobile implements EntryPoint {

  public void onModuleLoad() {

    /* App controllers and services */

    final HandlerManager eventBus = new HandlerManager(null);
    final ExpensesRequestFactory requestFactory = GWT.create(ExpensesRequestFactory.class);
    requestFactory.init(eventBus);
    final PlaceController<ScaffoldPlace> placeController = new PlaceController<ScaffoldPlace>(
        eventBus);

    /* Top level UI */

    final ScaffoldMobileShell shell = new ScaffoldMobileShell();

    /* Left side lets us pick from all the types of entities */

    PlacePicker<ListScaffoldPlace> placePicker = new PlacePicker<ListScaffoldPlace>(
        shell.getPlacesBox(), placeController, new ScaffoldListPlaceRenderer());
    placePicker.setPlaces(getTopPlaces());

    /*
     * The body is run by an ActivitManager that listens for PlaceChange events
     * and finds the corresponding Activity to run
     */

    final ActivityMapper<ScaffoldPlace> mapper = new ScaffoldMobileActivities(
        new ListActivitiesMapper(eventBus, requestFactory, placeController),
        requestFactory, placeController);
    final ActivityManager<ScaffoldPlace> activityManager = new ActivityManager<ScaffoldPlace>(
        mapper, eventBus);

    activityManager.setDisplay(new Activity.Display() {
      public void showActivityWidget(IsWidget widget) {
        shell.getBody().setWidget(widget == null ? null : widget.asWidget());
      }
    });

    /* Hide the loading message */

    Element loading = Document.get().getElementById("loading");
    loading.getParentElement().removeChild(loading);

    /* And show the user the shell */

    RootLayoutPanel.get().add(shell);
  }

  private List<ListScaffoldPlace> getTopPlaces() {
    final List<ListScaffoldPlace> rtn = new ArrayList<ListScaffoldPlace>();
    ExpensesEntityTypesProcessor.processAll(new ExpensesEntityTypesProcessor.EntityTypesProcessor() {
      public void processType(Class<? extends Record> recordType) {
        rtn.add(new ListScaffoldPlace(recordType));
      }
    });
    return Collections.unmodifiableList(rtn);
  }
}
