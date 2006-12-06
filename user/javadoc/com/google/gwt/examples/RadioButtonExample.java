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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;

public class RadioButtonExample implements EntryPoint {

  public void onModuleLoad() {
    // Make some radio buttons, all in one group.
    RadioButton rb0 = new RadioButton("myRadioGroup", "foo");
    RadioButton rb1 = new RadioButton("myRadioGroup", "bar");
    RadioButton rb2 = new RadioButton("myRadioGroup", "baz");

    // Check 'baz' by default.
    rb2.setChecked(true);

    // Add them to the root panel.
    FlowPanel panel = new FlowPanel();
    panel.add(rb0);
    panel.add(rb1);
    panel.add(rb2);
    RootPanel.get().add(panel);
  }
}