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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HandlesAllKeyEvents;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Visual testing of rich text box events.
 */
public class VisualsForTextEvents extends AbstractIssue {
  private VerticalPanel p = new VerticalPanel();

  @SuppressWarnings("deprecation")
  @Override
  public Widget createIssue() {
    p.setWidth("500px");
    p.setBorderWidth(1);
    // TextBox
    final TextBox b = new TextBox();
    b.setTitle("text box");
    p.add(b);
    EventReporter<String, Object> handler = new EventReporter<String, Object>(p);
    p.add(new Button("change value with event", new ClickHandler() {
      public void onClick(ClickEvent event) {
        b.setValue("emily", true);
      }
    }));
    b.addKeyboardListener(handler);
    HandlesAllKeyEvents.addHandlers(b, handler);
    b.addChangeListener(handler);
    b.addFocusHandler(handler);
    b.addBlurHandler(handler);
    b.addFocusListener(handler);
    b.addValueChangeHandler(handler);
    b.addMouseListener(handler);
    // Rich text box:
    RichTextArea rich = new RichTextArea();
    rich.setTitle("rich text box");
    p.add(rich);
    handler = new EventReporter<String, Object>(p);
    rich.addKeyboardListener(handler);
    HandlesAllKeyEvents.addHandlers(rich,handler);

    rich.addBlurHandler(handler);
    rich.addFocusHandler(handler);
    rich.addClickHandler(handler);
    rich.addClickListener(handler);

    return p;
  }

  @Override
  public String getInstructions() {
    return "type and click into text boxes, see that correct events show up";
  }

  @Override
  public String getSummary() {
    return "Text widget event tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
