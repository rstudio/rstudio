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

import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.PlaceHistoryHandler;
import com.google.gwt.app.place.PlaceHistoryHandlerWithFactory;
import com.google.gwt.app.place.PlaceTokenizer;
import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.app.place.ProxyListPlacePicker;
import com.google.gwt.app.place.ProxyPlace;
import com.google.gwt.app.place.ProxyPlaceToListPlace;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;

/**
 * Responsible for object creation. A prime candidate to be replaced by a code
 * generated dependency injector. May we suggest {@link http
 * ://code.google.com/p/google-gin/}?
 */
public class ScaffoldFactory {

  /*
   * App wide communication
   */
  private final EventBus eventBus = new HandlerManager(null);

  /*
   * Server RPC
   */
  private final ExpensesRequestFactory requestFactory = GWT.create(ExpensesRequestFactory.class);

  /*
   * Top level UI
   */
  private final ScaffoldShell shell = new ScaffoldShell();
  private final ScaffoldMobileShell mobileShell = new ScaffoldMobileShell();

  /*
   * Navigation within the app
   */

  /**
   * Defines where the user is right now.
   */
  private final PlaceController placeController = new PlaceController(eventBus);

  /**
   * Monitors {@link #placeController}, makes browser history and bookmarking
   * work.
   */
  private final PlaceHistoryHandlerWithFactory<ScaffoldFactory> placeHistoryHandler = GWT.create(ScaffoldPlaceHistoryHandler.class);

  private final ProxyPlace.Tokenizer proxyPlaceTokenizer = new ProxyPlace.Tokenizer(
      requestFactory);
  private final ProxyListPlace.Tokenizer proxyListPlaceTokenizer = new ProxyListPlace.Tokenizer(
      requestFactory);

  /**
   * Drives the list of proxied types on the left side of the screen.
   */
  private final ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace(
      requestFactory);

  private final ProxyListPlacePicker proxylistPlacePicker = new ProxyListPlacePicker(
      placeController, proxyPlaceToListPlace);

  public ScaffoldFactory() {
    requestFactory.init(eventBus);
    placeHistoryHandler.setFactory(this);
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public ProxyListPlacePicker getListPlacePicker() {
    return proxylistPlacePicker;
  }

  public ScaffoldMobileShell getMobileShell() {
    return mobileShell;
  }

  public PlaceController getPlaceController() {
    return placeController;
  }

  public PlaceHistoryHandler getPlaceHistoryHandler() {
    return placeHistoryHandler;
  }

  public PlaceTokenizer<ProxyListPlace> getProxyListPlaceTokenizer() {
    return proxyListPlaceTokenizer;
  }

  public PlaceTokenizer<ProxyPlace> getProxyPlaceTokenizer() {
    return proxyPlaceTokenizer;
  }

  public ProxyPlaceToListPlace getProxyPlaceToListPlace() {
    return proxyPlaceToListPlace;
  }

  public ExpensesRequestFactory getRequestFactory() {
    return requestFactory;
  }

  public ScaffoldShell getShell() {
    return shell;
  }
}
