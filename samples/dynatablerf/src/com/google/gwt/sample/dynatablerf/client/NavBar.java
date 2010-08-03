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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.sample.dynatablerf.client.events.NavigationEvent;
import com.google.gwt.sample.dynatablerf.client.events.NavigationEvent.Direction;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

class NavBar extends Composite {
  interface Binder extends UiBinder<Widget, NavBar> {
  }

  @UiField
  Button gotoFirst;

  @UiField
  Button gotoNext;

  @UiField
  Button gotoPrev;

  @UiField
  DivElement status;

  @UiConstructor
  public NavBar() {
    Binder binder = GWT.create(Binder.class);
    initWidget(binder.createAndBindUi(this));
  }

  public HandlerRegistration addNavigationEventHandler(
      NavigationEvent.Handler handler) {
    return addHandler(handler, NavigationEvent.TYPE);
  }

  @UiHandler("gotoFirst")
  void onFirst(ClickEvent event) {
    fireEvent(new NavigationEvent(Direction.START));
  }

  @UiHandler("gotoNext")
  void onNext(ClickEvent event) {
    fireEvent(new NavigationEvent(Direction.FORWARD));
  }

  @UiHandler("gotoPrev")
  void onPrev(ClickEvent event) {
    fireEvent(new NavigationEvent(Direction.BACKWARD));
  }
}