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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class LazyPanelExample implements EntryPoint {

  private static class HelloLazyPanel extends LazyPanel {
    @Override
    protected Widget createWidget() {
      return new Label("Well hello there!");
    }
  }

  public void onModuleLoad() {
    final Widget lazy = new HelloLazyPanel();
    
    // Not strictly necessary, but keeps the empty outer div
    // from effecting layout before it is of any use
    lazy.setVisible(false);

    PushButton b = new PushButton("Click me");    
    b.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        lazy.setVisible(true);
      }
    });
    
    RootPanel root = RootPanel.get();
    root.add(b);
    root.add(lazy);
  }
}
