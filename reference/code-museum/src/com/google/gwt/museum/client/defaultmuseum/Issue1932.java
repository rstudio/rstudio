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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * DOM.eventGetClientX/Y incorrect with HTML margin/borders Firefox 2/Safari 3.
 */
public class Issue1932 extends AbstractIssue {
  /**
   * A set of options used to set the page margins and borders.
   */
  private class ControlPanel extends Composite {
    private final Grid grid = new Grid(2, 3);

    private final TextBox borderBox = new TextBox();

    private final TextBox marginBox = new TextBox();

    public ControlPanel() {
      initWidget(grid);

      // Add an option to set the margin
      marginBox.setText("10px");
      grid.setHTML(0, 0, "<b>Margin:</b>");
      grid.setWidget(0, 1, marginBox);
      grid.setWidget(0, 2, new Button("Set", new ClickHandler() {
        public void onClick(ClickEvent event) {
          updateMargin();
        }
      }));

      // Add an option to set the border
      borderBox.setText("5px solid #DDDDDD");
      grid.setHTML(1, 0, "<b>Border:</b>");
      grid.setWidget(1, 1, borderBox);
      grid.setWidget(1, 2, new Button("Set", new ClickHandler() {
        public void onClick(ClickEvent event) {
          updateBorder();
        }
      }));
    }

    /**
     * Update the border on the HTML element.
     */
    public void updateBorder() {
      htmlElement.getStyle().setProperty("border", borderBox.getText());
    }

    /**
     * Update the margin on the HTML element.
     */
    public void updateMargin() {
      htmlElement.getStyle().setProperty("margin", marginBox.getText());
    }
  }

  /**
   * The HTML element of the page.
   */
  private Element htmlElement = null;

  @Override
  public Widget createIssue() {
    // Setup the page size and cursor
    htmlElement = DOM.getParent(RootPanel.getBodyElement());

    // Create a crosshair to show the current position
    final SimplePanel positioner = new SimplePanel();
    positioner.setPixelSize(30, 30);
    positioner.getElement().getStyle().setProperty("borderLeft",
        "1px solid red");
    positioner.getElement().getStyle().setProperty("borderTop", "1px solid red");
    positioner.getElement().getStyle().setProperty("cursor", "crosshair");

    // Create an area to echo position information.
    final HTML echo = new HTML();

    // Create a target box to test inside
    final Label sandbox = new Label();
    sandbox.sinkEvents(Event.ONMOUSEMOVE);
    sandbox.setPixelSize(300, 300);
    sandbox.getElement().getStyle().setProperty("border", "3px solid blue");
    sandbox.getElement().getStyle().setProperty("cursor", "crosshair");

    // Keep the crosshair under the cursor
    Event.addNativePreviewHandler(new NativePreviewHandler() {
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        // Ignore events outside of the sandbox
        NativeEvent nativeEvent = event.getNativeEvent();
        Element target = Element.as(nativeEvent.getEventTarget());
        if (!sandbox.getElement().isOrHasChild(target)
            && !positioner.getElement().isOrHasChild(target)) {
          positioner.removeFromParent();
          return;
        }
                
        switch (Event.as(nativeEvent).getTypeInt()) {
          case Event.ONMOUSEMOVE:
            int absX = nativeEvent.getClientX() + Window.getScrollLeft();
            int absY = nativeEvent.getClientY() + Window.getScrollTop();
            RootPanel.get().add(positioner, absX, absY);

            echo.setHTML("event.clientX: " + nativeEvent.getClientX() + "<br>"
                + "event.clientY: " + nativeEvent.getClientY() + "<br>"
                + "absolute left: " + positioner.getAbsoluteLeft() + "<br>"
                + "absolute top: " + positioner.getAbsoluteTop());
            break;
        }
      }
    });

    // Combine the control panel and return
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(new ControlPanel());
    vPanel.add(echo);
    vPanel.add(sandbox);
    return vPanel;
  }

  @Override
  public String getInstructions() {
    return "Move the cursor inside the blue box below and verify that the "
        + "point of the red positioner lines up directly beneath the center of "
        + "the cursor (crosshair). Also confirm that event.clientX/Y == absolute "
        + "left/top. The buttons may not work on Safari 2 because Safari 2 has "
        + "issues when you attempt to modify the HTML element programatically.";
  }

  @Override
  public String getSummary() {
    return "DOM.eventGetClientX/Y incorrect with HTML margin/border in Firefox "
        + "2 and Safari 2";
  }

  @Override
  public boolean hasCSS() {
    return true;
  }
}
