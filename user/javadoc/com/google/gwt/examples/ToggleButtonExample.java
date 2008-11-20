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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ToggleButton;

public class ToggleButtonExample implements EntryPoint {
  public void onModuleLoad() {
    // Make a new button that does something when you click it.
    final ToggleButton toggleButton = new ToggleButton("Up", "Down");
    toggleButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (toggleButton.isDown()) {
          Window.alert("I have been toggled down");
        } else {
          Window.alert("I have been toggled up");
        }
      }
    });

    // In a real application, you would have to have css styles defined for
    // gwt-ToggleButton-up,gwt-ToggleButton-up-hovering,gwt-ToggleButton-up-disabled,
    // gwt-ToggleButton-down,.gwt-ToggleButton-down-hovering,.gwt-ToggleButton-down-disabled

    // Add the ToggleButton to the root panel.
    RootPanel.get().add(toggleButton);
  }
}