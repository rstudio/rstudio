/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple example of an 'about' dialog box.
 */
public class AboutDialog extends DialogBox {

  private static final String LOGO_IMAGE = "http://www.google.com/images/logo_sm.gif";

  public AboutDialog() {
    // Use this opportunity to set the dialog's caption.
    setText("About the Mail Sample");

    // Create a DockPanel to contain the 'about' label and the 'OK' button.
    DockPanel outer = new DockPanel();
    outer.setSpacing(4);

    outer.add(new Image(LOGO_IMAGE), DockPanel.WEST);

    // Create the 'OK' button, along with a listener that hides the dialog
    // when the button is clicked. Adding it to the 'south' position within
    // the dock causes it to be placed at the bottom.
    HorizontalPanel buttonPanel = new HorizontalPanel();
    buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
    buttonPanel.add(new Button("Close", new ClickListener() {
      public void onClick(Widget sender) {
        hide();
      }
    }));
    outer.add(buttonPanel, DockPanel.SOUTH);

    // Create the 'about' label. Placing it in the 'rest' position within the
    // dock causes it to take up any remaining space after the 'OK' button
    // has been laid out.
    HTML text = new HTML(
      "This sample application demonstrates the construction "
        + "of a complex user interface using GWT's built-in widgets.  Have a look "
        + "at the code to see how easy it is to build your own apps!");
    text.setStyleName("mail-AboutText");
    outer.add(text, DockPanel.CENTER);

    // Add a bit of spacing and margin to the dock to keep the components from
    // being placed too closely together.
    outer.setSpacing(8);

    setWidget(outer);
  }

  public boolean onKeyDownPreview(char key, int modifiers) {
    // Use the popup's key preview hooks to close the dialog when either
    // enter or escape is pressed.
    switch (key) {
      case KeyboardListener.KEY_ENTER:
      case KeyboardListener.KEY_ESCAPE:
        hide();
        break;
    }

    return true;
  }
}
