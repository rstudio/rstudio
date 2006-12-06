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
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ImageExample implements EntryPoint {

  private Label lbl = new Label();

  public void onModuleLoad() {
    // Create an image, not yet referencing a URL.
    Image image = new Image();

    // Hook up a load listener, so that we can find out when it loads (or
    // fails to, as the case may be).
    image.addLoadListener(new LoadListener() {
      public void onLoad(Widget sender) {
        lbl.setText("Done loading.");
      }
    
      public void onError(Widget sender) {
        lbl.setText("An error occurred while loading.");
      }
    });

    // Point the image at a real URL.
    lbl.setText("Loading...");
    image.setUrl("http://www.google.com/images/logo.gif");

    // Add the image & label to the root panel.
    VerticalPanel panel = new VerticalPanel();
    panel.add(lbl);
    panel.add(image);
    RootPanel.get().add(panel);
  }
}