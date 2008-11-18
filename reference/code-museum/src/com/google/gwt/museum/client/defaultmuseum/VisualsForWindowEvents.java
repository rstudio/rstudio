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
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * Simple visual test for checking up on window events with Handlers.
 */
public class VisualsForWindowEvents extends AbstractIssue implements
    CloseHandler<Window>, Window.ClosingHandler, Window.ScrollHandler,
    ResizeHandler {
  static int numResizes = 0;

  private ArrayList<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();
  private FlowPanel messages = new FlowPanel();

  @Override
  public Widget createIssue() {
    FlowPanel out = new FlowPanel();

    FlowPanel body = new FlowPanel();
    FlowPanel buttons = new FlowPanel();

    Button addHandlersButton = new Button("Add window handlers.");
    addHandlersButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        setupWindowHandlers();
      }
    });

    Button removeHandlersButton = new Button("Remove window handlers.");
    removeHandlersButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        removeWindowHandlers();
      }
    });

    Button clearButton = new Button("Clear events");
    clearButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        messages.clear();
      }
    });

    buttons.add(addHandlersButton);
    buttons.add(removeHandlersButton);
    buttons.add(clearButton);

    body.add(buttons);
    body.add(messages);

    out.add(body);

    return out;
  }

  @Override
  public String getInstructions() {
    return "Click on the button to add all the window handlers. "
        + "Then click the other button to remove them all. "
        + "With handlers enabled, you should get messages for scrolling or "
        + "resizing the window, a \"do you want to navigate away?\" message"
        + "if you're about to navigate away, and an alert when you do navigate"
        + "away or close the window.";
  }

  @Override
  public String getSummary() {
    return "window events";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  public void onClose(CloseEvent<Window> event) {
    Window.alert("Closing the window.");
  }

  public void onResize(ResizeEvent event) {
    Window.setTitle("Got resize " + numResizes++);
  }

  public void onWindowClosing(ClosingEvent event) {
    event.setMessage("Are you sure you want to navigate away?");
  }

  public void onWindowScroll(ScrollEvent event) {
    addMessage("Got a window scroll!");
  }

  private void addMessage(String msg) {
    messages.add(new Label(msg));
  }

  private void removeWindowHandlers() {
    for (HandlerRegistration reg : registrations) {
      reg.removeHandler();
    }
  }

  private void setupWindowHandlers() {
    registrations.add(Window.addCloseHandler(this));
    registrations.add(Window.addResizeHandler(this));
    registrations.add(Window.addWindowClosingHandler(this));
    registrations.add(Window.addWindowScrollHandler(this));
  }
}