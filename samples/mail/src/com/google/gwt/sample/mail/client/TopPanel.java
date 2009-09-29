/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.mail.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * The top panel, which contains the 'welcome' message and various links.
 */
public class TopPanel extends Composite {

  interface Binder extends UiBinder<Widget, TopPanel> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField Anchor signOutLink;
  @UiField Anchor aboutLink;

  public TopPanel() {
    initWidget(binder.createAndBindUi(this));
  }

  @UiHandler("aboutLink")
  void onAboutClicked(ClickEvent event) {
    // When the 'About' item is selected, show the AboutDialog.
    // Note that showing a dialog box does not block -- execution continues
    // normally, and the dialog fires an event when it is closed.
    AboutDialog dlg = new AboutDialog();
    dlg.show();
    dlg.center();
  }

  @UiHandler("signOutLink")
  void onSignOutClicked(ClickEvent event) {
    Window.alert("If this were implemented, you would be signed out now.");
  }
}
