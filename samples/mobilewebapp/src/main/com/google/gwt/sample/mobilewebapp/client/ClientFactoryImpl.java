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
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.sample.gaerequest.client.GaeAuthRequestTransport;
import com.google.gwt.sample.gaerequest.client.ReloadOnAuthenticationFailure;
import com.google.gwt.sample.mobilewebapp.client.activity.AppActivityMapper;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskEditView;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListActivity;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskReadView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskEditView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskListView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskReadView;
import com.google.gwt.sample.mobilewebapp.client.desktop.MobileWebAppShellDesktop;
import com.google.gwt.sample.mobilewebapp.client.place.AppPlaceHistoryMapper;
import com.google.gwt.sample.mobilewebapp.shared.MobileWebAppRequestFactory;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.requestfactory.shared.RequestTransport;

/**
 * Default implementation of {@link ClientFactory}. Used by desktop version.
 */
class ClientFactoryImpl implements ClientFactory {

  /**
   * The URL argument used to enable or disable local storage.
   */
  private static final String STORAGE_URL_ARG = "storage";

  private final EventBus eventBus = new SimpleEventBus();
  private final PlaceController placeController = new PlaceController(eventBus);
  private final MobileWebAppRequestFactory requestFactory;
  private MobileWebAppShell shell;
  private final Storage localStorage;
  private TaskEditView taskEditView;
  private TaskListView taskListView;
  private final ActivityManager activityManager;

  private final AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);

  /**
   * The stock GWT class that ties the PlaceController to browser history,
   * configured by our custom {@link #historyMapper}.
   */
  private final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);

  private TaskReadView taskReadView;

  public ClientFactoryImpl() {
    RequestTransport requestTransport = new GaeAuthRequestTransport(eventBus);
    requestFactory = GWT.create(MobileWebAppRequestFactory.class);
    requestFactory.initialize(eventBus, requestTransport);

    // Initialize local storage.
    String storageUrlValue = Window.Location.getParameter(STORAGE_URL_ARG);
    if (storageUrlValue == null || storageUrlValue.startsWith("t")) {
      localStorage = Storage.getLocalStorageIfSupported();
    } else {
      localStorage = null;
    }

    /*
     * ActivityMapper determines an Activity to run for a particular place.
     */
    ActivityMapper activityMapper = new AppActivityMapper(this, getIsTaskListIncludedProvider());
    /*
     * Owns a panel in the window, in this case the entire {@link #shell}.
     * Monitors the {@link #eventBus} for {@link PlaceChangeEvent}s posted by
     * the {@link #placeController}, and chooses what {@link Activity} gets to
     * take over the panel at the current place. Configured by an {@link
     * AppActivityMapper}.
     */
    activityManager = new ActivityManager(activityMapper, eventBus);
  }

  public App getApp() {
    return new App(getLocalStorageIfSupported(), getEventBus(), getPlaceController(),
        historyMapper, historyHandler, new ReloadOnAuthenticationFailure(), getShell());
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public Storage getLocalStorageIfSupported() {
    return localStorage;
  }

  public PlaceController getPlaceController() {
    return placeController;
  }

  public MobileWebAppRequestFactory getRequestFactory() {
    return requestFactory;
  }

  public MobileWebAppShell getShell() {
    if (shell == null) {
      shell = createShell();
    }
    return shell;
  }

  public TaskEditView getTaskEditView() {
    if (taskEditView == null) {
      taskEditView = createTaskEditView();
    }
    return taskEditView;
  }

  public TaskListView getTaskListView() {
    if (taskListView == null) {
      taskListView = createTaskListView();
    }
    return taskListView;
  }

  public TaskReadView getTaskReadView() {
    if (taskReadView == null) {
      taskReadView = createTaskReadView();
    }
    return taskReadView;
  }

  public void init() {
    activityManager.setDisplay(getShell());
  }

  /**
   * Create the application UI shell.
   * 
   * @return the UI shell
   */
  protected MobileWebAppShell createShell() {
    return new MobileWebAppShellDesktop(eventBus, placeController, getTaskListView(),
        getTaskEditView(), getTaskReadView());
  }

  /**
   * Create a {@link TaskEditView}.
   * 
   * @return a new {@link TaskEditView}
   */
  protected TaskEditView createTaskEditView() {
    return new DesktopTaskEditView();
  }

  /**
   * Create a {@link TaskListView}.
   * 
   * @return a new {@link TaskListView}
   */
  protected TaskListView createTaskListView() {
    return new DesktopTaskListView();
  }

  protected TaskReadView createTaskReadView() {
    return new DesktopTaskReadView();
  }

  /**
   * Returns provider that indicates whether the task list is always visible.
   * The default implementation returned by this method always indicates false.
   * 
   * @return provider that always provides false
   */
  protected Provider<Boolean> getIsTaskListIncludedProvider() {
    return new Provider<Boolean>() {
      @Override
      public Boolean get() {
        return false;
      }
    };
  }

  protected Provider<TaskListActivity> getTaskListActivityProvider() {
    return new Provider<TaskListActivity>() {
      @Override
      public TaskListActivity get() {

        /*
         * TODO (rjrjr) the false arg is needed by MobileWebAppShellTablet,
         * which shouldn't be using activities at all
         */
        return new TaskListActivity(ClientFactoryImpl.this, false);
      }
    };
  }
}
