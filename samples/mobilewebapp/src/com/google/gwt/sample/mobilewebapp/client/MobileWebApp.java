/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryHandler.DefaultHistorian;
import com.google.gwt.place.shared.PlaceHistoryHandler.Historian;
import com.google.gwt.sample.mobilewebapp.client.activity.AppActivityMapper;
import com.google.gwt.sample.mobilewebapp.client.place.AppPlaceHistoryMapper;
import com.google.gwt.sample.mobilewebapp.client.place.TaskListPlace;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class MobileWebApp implements EntryPoint {

  static final String HISTORY_SAVE_KEY = "SAVEPLACE";
  final Storage storage = Storage.getLocalStorageIfSupported();

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    /*
     * Initialize the ClientFactory, which includes common resources used
     * throughout the app. We bundle them all into a ClientFactory so we only
     * have to pass one argument around.
     */
    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();
    PlaceController placeController = clientFactory.getPlaceController();

    /*
     * Add the main application shell to the RootLayoutPanel. The shell includes
     * the header bar at the top and a content area.
     */
    MobileWebAppShell shell = clientFactory.getShell();
    RootLayoutPanel.get().add(shell);

    /*
     * Start ActivityManager for the main widget with our ActivityMapper. The
     * ActivityManager starts an activity based on the current place.
     */
    ActivityMapper activityMapper = new AppActivityMapper(clientFactory);
    ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
    activityManager.setDisplay(shell);

    /*
     * Start PlaceHistoryHandler with our PlaceHistoryMapper. The
     * PlaceHistoryHandler chooses the correct place based on the history token
     * in the URL.
     */
    final AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);
    Historian historian = (Historian) GWT.create(DefaultHistorian.class);

    PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper, historian);
    Place savedPlace = null;
    if (storage != null) {
      try {
        // wrap in try-catch in case stored value is invalid
        savedPlace = historyMapper.getPlace(storage.getItem(HISTORY_SAVE_KEY));
      } catch (Throwable t) {
        // ignore error
      }
    }
    if (savedPlace == null) {
      savedPlace = new TaskListPlace(true);
    }
    historyHandler.register(placeController, eventBus, savedPlace);

    // Go to the place represented in the URL else default place.
    historyHandler.handleCurrentHistory();

    /* Hook up storage to listen to event bus */
    eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
      public void onPlaceChange(PlaceChangeEvent event) {
        Place newPlace = event.getNewPlace();
        if (storage != null) {
          storage.setItem(HISTORY_SAVE_KEY, historyMapper.getToken(newPlace));
        }
      }
    });
  }
}
