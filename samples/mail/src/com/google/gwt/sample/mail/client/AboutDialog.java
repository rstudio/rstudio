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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple example of an 'about' dialog box.
 */
public class AboutDialog extends DialogBox {

  public AboutDialog() {
    // Use this opportunity to set the dialog's caption.
    setText("About the Mail Sample");

    // Create a VerticalPanel to contain the 'about' label and the 'OK' button.
    VerticalPanel outer = new VerticalPanel();

    // Create the 'about' text and set a style name so we can style it with CSS.

    HTML text = new HTML("This sample application demonstrates the "
        + "construction of a complex user interface using GWT's built-in "
        + "widgets.  Have a look at the code to see how easy it is to build "
        + "your own apps!");
    text.setStyleName("mail-AboutText");
    outer.add(text);

    // Create the 'OK' button, along with a listener that hides the dialog
    // when the button is clicked.
    outer.add(new Button("Close", new ClickListener() {
      public void onClick(Widget sender) {
        hide();
      }
    }));

    setWidget(outer);
  }

  @Override
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
