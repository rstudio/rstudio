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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.sample.gaerequest.client.ReloadOnAuthenticationFailure;
import com.google.gwt.sample.mobilewebapp.client.event.AddTaskEvent;
import com.google.gwt.sample.mobilewebapp.client.event.EditingCanceledEvent;
import com.google.gwt.sample.mobilewebapp.client.event.GoHomeEvent;
import com.google.gwt.sample.mobilewebapp.client.event.ShowTaskEvent;
import com.google.gwt.sample.mobilewebapp.client.event.TaskSavedEvent;
import com.google.gwt.sample.mobilewebapp.client.place.AppPlaceHistoryMapper;
import com.google.gwt.sample.mobilewebapp.client.place.TaskEditPlace;
import com.google.gwt.sample.mobilewebapp.client.place.TaskListPlace;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.web.bindery.event.shared.UmbrellaException;

/**
 * The heart of the applicaiton, mainly concerned with bootstrapping.
 */
public class App {
  private static final String HISTORY_SAVE_KEY = "SAVEPLACE";

  private final Storage storage;

  /**
   * Where components of the app converse by posting and monitoring events.
   */
  private final EventBus eventBus;

  /**
   * Owns the current {@link Place} in the app. A Place is the embodiment of any
   * bookmarkable state.
   */
  private final PlaceController placeController;

  /**
   * The top of our UI.
   */
  private final MobileWebAppShell shell;

  private final AppPlaceHistoryMapper historyMapper;

  private final PlaceHistoryHandler historyHandler;

  private final ReloadOnAuthenticationFailure reloadOnAuthenticationFailure;

  public App(Storage storage, EventBus eventBus, PlaceController placeController,
      AppPlaceHistoryMapper historyMapper, PlaceHistoryHandler historyHandler,
      ReloadOnAuthenticationFailure reloadOnAuthenticationFailure, MobileWebAppShell shell) {

    this.storage = storage;
    this.eventBus = eventBus;
    this.placeController = placeController;
    this.historyMapper = historyMapper;
    this.historyHandler = historyHandler;
    this.reloadOnAuthenticationFailure = reloadOnAuthenticationFailure;
    this.shell = shell;
  }

  /**
   * Given a parent view to show itself in, start this App.
   * 
   * @param parentView where to show the app's widget
   */
  public void run(HasWidgets.ForIsWidget parentView) {
    parentView.add(shell);

    eventBus.addHandler(AddTaskEvent.TYPE, new AddTaskEvent.Handler() {
      @Override
      public void onAddTask(AddTaskEvent event) {
        placeController.goTo(TaskEditPlace.getTaskCreatePlace());
      }
    });

    eventBus.addHandler(ShowTaskEvent.TYPE, new ShowTaskEvent.Handler() {
      @Override
      public void onShowTask(ShowTaskEvent event) {
        TaskProxy task = event.getTask();
        placeController.goTo(TaskEditPlace.createTaskEditPlace(task.getId(), task));
      }
    });

    eventBus.addHandler(GoHomeEvent.TYPE, new GoHomeEvent.Handler() {
      @Override
      public void onGoHome(GoHomeEvent event) {
        placeController.goTo(new TaskListPlace(false));
      }
    });

    eventBus.addHandler(TaskSavedEvent.TYPE, new TaskSavedEvent.Handler() {
      @Override
      public void onTaskSaved(TaskSavedEvent event) {
        placeController.goTo(new TaskListPlace(true));
      }
    });

    eventBus.addHandler(EditingCanceledEvent.TYPE, new EditingCanceledEvent.Handler() {
      @Override
      public void onEditCanceled(EditingCanceledEvent event) {
        placeController.goTo(new TaskListPlace(false));
      }
    });

    GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void onUncaughtException(Throwable e) {
        while (e instanceof UmbrellaException) {
          e = ((UmbrellaException) e).getCauses().iterator().next();
        }
        Window.alert("An unexpected error occurred: " + e.getMessage());
        placeController.goTo(new TaskListPlace(false));
      }
    });

    // Check for authentication failures or mismatches
    reloadOnAuthenticationFailure.register(eventBus);

    initBrowserHistory(historyMapper, historyHandler, new TaskListPlace(true));
  }

  /**
   * Initialize browser history / bookmarking. If LocalStorage is available, use
   * it to make the user's default location in the app the last one seen.
   */
  private void initBrowserHistory(final AppPlaceHistoryMapper historyMapper,
      PlaceHistoryHandler historyHandler, TaskListPlace defaultPlace) {

    Place savedPlace = null;
    if (storage != null) {
      try {
        // wrap in try-catch in case stored value is invalid
        savedPlace = historyMapper.getPlace(storage.getItem(HISTORY_SAVE_KEY));
      } catch (Throwable t) {
        // ignore error and use the default-default
      }
    }
    if (savedPlace == null) {
      savedPlace = defaultPlace;
    }
    historyHandler.register(placeController, eventBus, savedPlace);

    /*
     * Go to the place represented in the URL. This is what makes bookmarks
     * work.
     */
    historyHandler.handleCurrentHistory();

    /*
     * Monitor the eventbus for place changes and note them in LocalStorage for
     * the next launch.
     */
    if (storage != null) {
      eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
        public void onPlaceChange(PlaceChangeEvent event) {
          storage.setItem(HISTORY_SAVE_KEY, historyMapper.getToken(event.getNewPlace()));
        }
      });
    }
  }
}
