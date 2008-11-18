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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Focusing on the document body while a modal {@link PopupPanel} is visible
 * causes Internet Explorer to disappear to the back of the UI stack. Also,
 * modal PopupPanels do not prevent the user from focusing on input elements.
 */
public class Issue2390 extends AbstractIssue {
  @Override
  public Widget createIssue() {
    // A label indicating where to click to focus the body
    Label label = new Label("Click to the right of this box while the popup "
        + "is visible >>>");
    label.getElement().getStyle().setProperty("border", "1px solid red");

    // Create a modal PopupPanel
    final PopupPanel popup = new PopupPanel(false, true);
    popup.setWidget(new Button("Hide Popup", new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup.hide();
      }
    }));

    // Create a button to show the PopupPanel
    Button showPopupButton = new Button("Show Popup", new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup.center();
      }
    });

    // Create a bunch of input elements to test
    // TODO(jlabanca): divide this out into a separate issue 2707
    CheckBox checkBox = new CheckBox("CheckBox");
    RadioButton radio1 = new RadioButton("grouping", "RadioButton1");
    RadioButton radio2 = new RadioButton("grouping", "RadioButton2");
    ListBox list = new ListBox();
    list.addItem("test1");
    list.addItem("test2");
    list.addItem("test3");
    list.addItem("test4");
    TextBox textBox = new TextBox();

    // Combine all of the elements into a panel
    VerticalPanel layout = new VerticalPanel();
    layout.add(label);
    layout.add(showPopupButton);
    layout.add(textBox);
    layout.add(checkBox);
    layout.add(radio1);
    layout.add(radio2);
    layout.add(list);
    return layout;
  }

  @Override
  public String getInstructions() {
    return "First, make sure you have another program (such as another "
        + "instance of the IE) running on the system.  Click the button below "
        + "to show the PopupPanel, then click to right of the red box.  The "
        + "browser should not be sent to the back of the OS UI stack because the "
        + "document body is blurred.  Also, make sure that the user cannot "
        + "interact with the input elements while the popup is visible.";
  }

  @Override
  public String getSummary() {
    return "IE dissappears when clicking outside a modal PopupPanel";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
