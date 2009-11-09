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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple example of an 'about' dialog box.
 */
public class AboutDialog extends DialogBox {

  interface Binder extends UiBinder<Widget, AboutDialog> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField Button closeButton;

  public AboutDialog() {
    // Use this opportunity to set the dialog's caption.
    setText("About the Mail Sample");
    setWidget(binder.createAndBindUi(this));

    setAnimationEnabled(true);
    setGlassEnabled(true);
  }

  @Override
  protected void onPreviewNativeEvent(NativePreviewEvent preview) {
    super.onPreviewNativeEvent(preview);

    NativeEvent evt = preview.getNativeEvent();
    if (evt.getType().equals("keydown")) {
      // Use the popup's key preview hooks to close the dialog when either
      // enter or escape is pressed.
      switch (evt.getKeyCode()) {
        case KeyCodes.KEY_ENTER:
        case KeyCodes.KEY_ESCAPE:
          hide();
          break;
      }
    }
  }

  @UiHandler("closeButton")
  void onSignOutClicked(ClickEvent event) {
    hide();
  }
}
