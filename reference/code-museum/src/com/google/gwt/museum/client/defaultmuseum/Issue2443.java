/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Dragging a {@link DialogBox} to the right edge of the screen creates a
 * horizontal scroll bar. The {@link DialogBox} should wrap the the text in
 * order to avoid creating the scroll bar.
 */
public class Issue2443 extends AbstractIssue {
  /**
   * The DialogBox to test.
   */
  private DialogBox dialogBox = null;

  @Override
  public Widget createIssue() {
    // Create the DialogBox
    dialogBox = new DialogBox(false, false);
    dialogBox.setText("Dialog Box");

    String message = "This text should wrap when the "
        + "DialogBox is dragged to the right edge of the screen.  ";
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(new Label(message));
    vPanel.add(new Button("Close", new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    }));
    dialogBox.setWidget(vPanel);

    // Create a button to display the dialog box
    Button showButton = new Button("Show DialogBox", new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.center();
      }
    });
    return showButton;
  }

  @Override
  public String getInstructions() {
    return "Move the DialogBox to the right edge of the screen.  The DialogBox "
        + "should wrap its text as best it can to avoid creating a horizontal "
        + "scroll bar.";
  }

  @Override
  public String getSummary() {
    return "DialogBox does not resize naturally";
  }

  @Override
  public boolean hasCSS() {
    return true;
  }
}
