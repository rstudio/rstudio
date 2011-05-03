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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskEditView;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskEditView;
import com.google.gwt.sample.mobilewebapp.client.desktop.DesktopTaskListView;
import com.google.gwt.sample.mobilewebapp.client.desktop.MobileWebAppShellDesktop;

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

  public ClientFactoryImpl() {
    DefaultRequestTransport requestTransport = new DefaultRequestTransport();
    requestFactory = GWT.create(MobileWebAppRequestFactory.class);
    requestFactory.initialize(eventBus, requestTransport);

    // Initialize local storage.
    String storageUrlValue = Window.Location.getParameter(STORAGE_URL_ARG);
    if (storageUrlValue == null || storageUrlValue.startsWith("t")) {
      localStorage = Storage.getLocalStorageIfSupported();
    } else {
      localStorage = null;
    }
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

  /**
   * Create the application UI shell.
   * 
   * @return the UI shell
   */
  protected MobileWebAppShell createShell() {
    return new MobileWebAppShellDesktop(this);
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
}
