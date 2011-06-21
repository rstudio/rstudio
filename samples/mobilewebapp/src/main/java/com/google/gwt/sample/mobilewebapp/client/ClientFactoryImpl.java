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
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.sample.gaerequest.client.GaeAuthRequestTransport;
import com.google.gwt.sample.gaerequest.client.ReloadOnAuthenticationFailure;
import com.google.gwt.sample.mobilewebapp.client.activity.AppActivityMapper;
import com.google.gwt.sample.mobilewebapp.client.activity.AppPlaceHistoryMapper;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskEditView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskListView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskReadView;
import com.google.gwt.sample.mobilewebapp.client.desktop.MobileWebAppShellDesktop;
import com.google.gwt.sample.mobilewebapp.client.ui.PieChart;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditView;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskReadView;
import com.google.gwt.sample.mobilewebapp.presenter.taskchart.TaskChartPresenter;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListView;
import com.google.gwt.sample.mobilewebapp.shared.MobileWebAppRequestFactory;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
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
  private final TaskProxyLocalStorage taskProxyLocalStorage;
  private TaskEditView taskEditView;
  private TaskListView taskListView;
  private ActivityManager activityManager;

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
    taskProxyLocalStorage = new TaskProxyLocalStorage(localStorage);
  }

  public App getApp() {
    return new App(getLocalStorageIfSupported(), eventBus, getPlaceController(),
        getActivityManager(), historyMapper, historyHandler, new ReloadOnAuthenticationFailure(),
        getShell());
  }

  @Override
  public EventBus getEventBus() {
    return eventBus;
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

  public TaskProxyLocalStorage getTaskProxyLocalStorage() {
    return taskProxyLocalStorage;
  }

  public TaskReadView getTaskReadView() {
    if (taskReadView == null) {
      taskReadView = createTaskReadView();
    }
    return taskReadView;
  }

  /**
   * ActivityMapper determines an Activity to run for a particular place,
   * configures the {@link #getActivityManager()}
   */
  protected ActivityMapper createActivityMapper() {
    return new AppActivityMapper(this);
  }

  /**
   * Create the application UI shell.
   * 
   * @return the UI shell
   */
  protected MobileWebAppShell createShell() {
    PieChart pieChart = PieChart.createIfSupported();
    TaskChartPresenter presenter = null;
    if (pieChart != null) {
      presenter = new TaskChartPresenter(pieChart);
      presenter.start(getEventBus());
    }
    return new MobileWebAppShellDesktop(eventBus, presenter, placeController, getTaskListView(),
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
   * Owns a panel in the window, in this case the entire {@link #shell}.
   * Monitors the {@link #eventBus} for
   * {@link com.google.gwt.place.shared.PlaceChangeEvent PlaceChangeEvent}s posted by the
   * {@link #placeController}, and chooses what
   * {@link com.google.gwt.activity.shared.Activity Activity} gets to take
   * over the panel at the current place. Configured by the
   * {@link #createActivityMapper()}.
   */
  protected ActivityManager getActivityManager() {
    if (activityManager == null) {
      activityManager = new ActivityManager(createActivityMapper(), eventBus);
    }
    return activityManager;
  }

  private Storage getLocalStorageIfSupported() {
    return localStorage;
  }
}
