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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * <h1>PopupPanel.setAutoHideEnabled() and setModel()</h1>
 * 
 * <p>
 * Verify that all states of the {@link PopupPanel} (combinations of modal and
 * autoHide) work and can be change seemlessly.
 * </p>
 */
public class Issue2855 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    // Create the popup panel
    final PopupPanel popup = new PopupPanel();

    // Add buttons to call getters and setters
    Button toggleAutoHide = new Button("4. Toggle AutoHide",
        new ClickHandler() {
          public void onClick(ClickEvent event) {
            popup.setAutoHideEnabled(!popup.isAutoHideEnabled());
          }
        });
    Button toggleModal = new Button("3. Toggle Modal", new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup.setModal(!popup.isModal());
      }
    });
    Button isAutoHide = new Button("isAutoHide?", new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.alert("AutoHide: " + popup.isAutoHideEnabled());
      }
    });
    Button isModal = new Button("isModal?", new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.alert("Modal: " + popup.isModal());
      }
    });
    Button closeButton = new Button("Close", new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup.hide();
      }
    });
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(toggleModal);
    vPanel.add(toggleAutoHide);
    vPanel.add(isModal);
    vPanel.add(isAutoHide);
    vPanel.add(closeButton);
    popup.setWidget(vPanel);

    // Add control buttons
    Button showPopup = new Button("1. Show Popup", new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup.center();
      }
    });
    Button clickable = new Button("2/4. Click Me", new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.alert("You got me!");
      }
    });
    VerticalPanel layout = new VerticalPanel();
    layout.add(showPopup);
    layout.add(clickable);
    return layout;
  }

  @Override
  public String getInstructions() {
    String text = "Perform the following steps:<br>"
        + "1. Show the popup<br>"
        + "2. Click the 'Click Me' button and verify an alert box appears<br>"
        + "3. Click the 'Toggle Modal' button<br>"
        + "4. Click the 'Click Me' button and verify an alert box doesn't appear<br>"
        + "5. Click the 'Toggle AutoHide' button<br>"
        + "6. Click on the screen and verify that the popup closes";
    return text;
  }

  @Override
  public String getSummary() {
    return "PopupPanel.setAutoHideEnabled() and setModel() tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
