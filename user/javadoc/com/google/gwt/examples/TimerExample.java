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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;

public class TimerExample implements EntryPoint, ClickHandler {

  public void onModuleLoad() {
    Button b = new Button("Click and wait 5 seconds");
    b.addClickHandler(this);

    RootPanel.get().add(b);
  }

  public void onClick(ClickEvent event) {
    // Create a new timer that calls Window.alert().
    Timer t = new Timer() {
      @Override
      public void run() {
        Window.alert("Nifty, eh?");
      }
    };

    // Schedule the timer to run once in 5 seconds.
    t.schedule(5000);
  }
}
