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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class HTMLExample implements EntryPoint {

  public void onModuleLoad() {
    // Create a Label and an HTML widget.
    Label lbl = new Label("This is just text.  It will not be interpreted "
      + "as <html>.");

    HTML html = new HTML(
      "This is <b>HTML</b>.  It will be interpreted as such if you specify "
        + "the <span style='font-family:fixed'>asHTML</span> flag.", true);

    // Add them to the root panel.
    VerticalPanel panel = new VerticalPanel();
    panel.add(lbl);
    panel.add(html);
    RootPanel.get().add(panel);
  }
}
