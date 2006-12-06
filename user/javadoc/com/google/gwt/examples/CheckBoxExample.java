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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class CheckBoxExample implements EntryPoint {

  public void onModuleLoad() {
    // Make a new check box, and select it by default.
    CheckBox cb = new CheckBox("Foo");
    cb.setChecked(true);

    // Hook up a listener to find out when it's clicked.
    cb.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        boolean checked = ((CheckBox) sender).isChecked();
        Window.alert("It is " + (checked ? "" : "not") + "checked");
      }
    });

    // Add it to the root panel.
    RootPanel.get().add(cb);
  }
}