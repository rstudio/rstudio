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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskEditView;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;

/**
 * A factory for retrieving objects used throughout the application.
 */
public interface ClientFactory {

  /**
   * Get the event bus.
   * 
   * @return the {@link EventBus} used throughout the app
   */
  EventBus getEventBus();

  /**
   * Get the local {@link Storage} object if supported.
   * 
   * @return the local {@link Storage} object
   */
  Storage getLocalStorageIfSupported();

  /**
   * Get the {@link PlaceController}.
   * 
   * @return the place controller
   */
  PlaceController getPlaceController();

  /**
   * Get the RequestFactory used to query the server.
   * 
   * @return the request factory
   */
  MobileWebAppRequestFactory getRequestFactory();

  /**
   * Get the UI shell.
   * 
   * @return the shell
   */
  MobileWebAppShell getShell();

  /**
   * Get an implementation of {@link TaskEditView}.
   */
  TaskEditView getTaskEditView();

  /**
   * Get an implementation of {@link TaskListView}.
   */
  TaskListView getTaskListView();
}
