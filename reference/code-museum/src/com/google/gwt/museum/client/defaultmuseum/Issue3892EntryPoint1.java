/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

/**
 * Only a single GWT application can preview native events.
 */
public class Issue3892EntryPoint1 extends AbstractIssue {

  public static final String BUTTON_1_ID = "Issue3892Button1";
  public static final String BUTTON_2_ID = "Issue3892Button2";
  public static final String BUTTON_3_ID = "Issue3892Button3";

  /**
   * The main grid used for layout.
   */
  private Grid grid = new Grid(1, 3);

  @Override
  public Widget createIssue() {
    Window.alert("Module 1 loaded");

    // Setup the grid.
    grid.setHTML(0, 0, "<b>Test<b>");
    grid.setHTML(0, 1, "<b>Description<b>");
    grid.setHTML(0, 2, "<b>Expected Results<b>");
    addTest(BUTTON_1_ID, "Event is not cancelled by any module.",
        "The event will fire in the button.", false);
    addTest(BUTTON_2_ID, "Module 1 cancels event.",
        "The event will not fire in the button.", true);
    addTest(BUTTON_3_ID, "Module 2 cancels event.",
        "The event will not fire in the button.", true);

    // Add the event preview.
    Event.addNativePreviewHandler(new NativePreviewHandler() {
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        if (event.getTypeInt() == Event.ONCLICK) {
          Element target = event.getNativeEvent().getEventTarget().cast();
          if (BUTTON_2_ID.equals(target.getId())) {
            event.cancel();
            Window.alert("Click handled by module 1 and cancelled");
          } else {
            Window.alert("Click handled by module 1");
          }
        }
      }
    });

    return grid;
  }

  @Override
  public String getInstructions() {
    return "After all three modules have loaded (indicated by alert boxes), "
        + "click the buttons and verify that you see the expected results. "
        + "For each test, all three modules should preview the event (even if "
        + "one of the modules cancels the event).";
  }

  @Override
  public String getSummary() {
    return "Only a single GWT application can preview native events";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  /**
   * Add a test button to the grid.
   * 
   * @param buttonId the ID of the button
   * @param description the test description
   * @param results the expected result of the test
   * @param isCancelled true if one of the modules will cancel the event
   */
  private void addTest(String buttonId, String description, String results,
      final boolean isCancelled) {
    int row = grid.getRowCount();
    grid.resizeRows(row + 1);

    // Add the test button.
    Button button = new Button("Run Test", new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (isCancelled) {
          Window.alert("[Error] Event should have been cancelled");
        } else {
          Window.alert("[Success] Event successfully fired");
        }
      }
    });
    button.getElement().setId(buttonId);
    grid.setWidget(row, 0, button);

    // Add the description and expected results.
    grid.setText(row, 1, description);
    grid.setText(row, 2, results);
  }
}
