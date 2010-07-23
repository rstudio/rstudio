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

import com.google.gwt.app.client.NotificationMole;
import com.google.gwt.app.place.PlacePickerView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.requestfactory.client.LoginWidget;
import com.google.gwt.sample.expenses.gwt.client.place.ListScaffoldPlace;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The outermost UI of the application.
 */
public class ScaffoldShell extends Composite {
  interface Binder extends UiBinder<Widget, ScaffoldShell> {
  }
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField SimplePanel details;
  @UiField DivElement error;
  @UiField LoginWidget loginWidget;
  @UiField SimplePanel master;
  @UiField NotificationMole mole;
  @UiField PlacePickerView<ListScaffoldPlace> placesBox;

  public ScaffoldShell() {
    initWidget(BINDER.createAndBindUi(this));
  }

  /**
   * @return the panel to hold the details
   */
  public SimplePanel getDetailsPanel() {
    return details;
  }

  /**
   * @return the login widget
   */
  public LoginWidget getLoginWidget() {
    return loginWidget;
  }
  
  /**
   * @return the panel to hold the master list
   */
  public SimplePanel getMasterPanel() {
    return master;
  }

  /**
   * @return the notification mole for loading feedback
   */
  public NotificationMole getMole() {
    return mole;
  }

  /**
   * @return the navigator
   */
  public PlacePickerView<ListScaffoldPlace> getPlacesBox() {
    return placesBox;
  }

  /**
   * @param string
   */
  public void setError(String string) {
    error.setInnerText(string);
  }
}
