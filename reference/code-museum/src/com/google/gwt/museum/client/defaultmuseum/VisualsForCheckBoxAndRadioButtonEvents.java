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
import com.google.gwt.event.dom.client.HandlesAllKeyEvents;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Visual testing of rich text box events.
 */
public class VisualsForCheckBoxAndRadioButtonEvents extends AbstractIssue {
  private VerticalPanel p = new VerticalPanel();

  @SuppressWarnings("deprecation")
  @Override
  public Widget createIssue() {
    p.setWidth("500px");
    p.setBorderWidth(1);
    // TextBox
    final CheckBox b = new CheckBox("My Checkbox");
    b.setTitle("text box");
    p.add(b);
    EventReporter<Boolean, Object> handler = new EventReporter<Boolean, Object>(
        p);
    p.add(new Button("change value with event", new ClickHandler() {
      public void onClick(ClickEvent event) {
        b.setValue(false, true);
      }
    }));
    b.addKeyboardListener(handler);
    HandlesAllKeyEvents.addHandlers(b, handler);
    b.addFocusHandler(handler);
    b.addBlurHandler(handler);
    b.addFocusListener(handler);
    b.addValueChangeHandler(handler);

    // Rich text box:
    final RadioButton radio = new RadioButton("A", "With events");
    p.add(radio);
    final RadioButton radioPrime = new RadioButton("A", "No events");
    p.add(radioPrime);
    b.setTitle("Radio Button");
    p.add(radio);
    handler = new EventReporter<Boolean, Object>(p);
    radio.addKeyboardListener(handler);

    HandlesAllKeyEvents.addHandlers(radio, handler);
    radio.addBlurHandler(handler);
    radio.addFocusHandler(handler);
    radio.addClickHandler(handler);
    radio.addClickListener(handler);
    radio.addValueChangeHandler(handler);
    p.add(new Button("change value with event", new ClickHandler() {
      public void onClick(ClickEvent event) {
        radio.setValue(true, true);
      }
    }));

    return p;
  }

  @Override
  public String getInstructions() {
    return "Click on CheckBox and Radio Button, use change value events,see that correct events are firing";
  }

  @Override
  public String getSummary() {
    return "CheckBox and RadioButton test";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
