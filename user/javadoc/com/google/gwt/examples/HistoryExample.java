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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class HistoryExample implements EntryPoint, ValueChangeHandler<String> {

  private Label lbl = new Label();

  public void onModuleLoad() {
    // Create three hyperlinks that change the application's history.
    Hyperlink link0 = new Hyperlink("link to foo", "foo");
    Hyperlink link1 = new Hyperlink("link to bar", "bar");
    Hyperlink link2 = new Hyperlink("link to baz", "baz");

    // If the application starts with no history token, redirect to a new
    // 'baz' state.
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem("baz");
    }

    // Add widgets to the root panel.
    VerticalPanel panel = new VerticalPanel();
    panel.add(lbl);
    panel.add(link0);
    panel.add(link1);
    panel.add(link2);
    RootPanel.get().add(panel);

    // Add history listener
    History.addValueChangeHandler(this);

    // Now that we've setup our listener, fire the initial history state.
    History.fireCurrentHistoryState();
  }

  public void onValueChange(ValueChangeEvent<String> event) {
    // This method is called whenever the application's history changes. Set
    // the label to reflect the current history token.
    lbl.setText("The current history token is: " + event.getValue());
  }
}
