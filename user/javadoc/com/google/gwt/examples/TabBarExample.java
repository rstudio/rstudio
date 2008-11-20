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
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabBar;

public class TabBarExample implements EntryPoint {

  public void onModuleLoad() {
    // Create a tab bar with three items.
    TabBar bar = new TabBar();
    bar.addTab("foo");
    bar.addTab("bar");
    bar.addTab("baz");

    // Hook up a tab listener to do something when the user selects a tab.
    bar.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        // Let the user know what they just did.
        Window.alert("You clicked tab " + event.getSelectedItem());
      }
    });

    // Just for fun, let's disallow selection of 'bar'.
    bar.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
      public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
        if (event.getItem().intValue() == 1) {
          event.cancel();
        }
      }
    });

    // Add it to the root panel.
    RootPanel.get().add(bar);
  }
}