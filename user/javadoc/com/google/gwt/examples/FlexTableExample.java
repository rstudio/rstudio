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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.RootPanel;

public class FlexTableExample implements EntryPoint {

  public void onModuleLoad() {
    // Tables have no explicit size -- they resize automatically on demand.
    FlexTable t = new FlexTable();

    // Put some text at the table's extremes.  This forces the table to be
    // 3 by 3.
    t.setText(0, 0, "upper-left corner");
    t.setText(2, 2, "bottom-right corner");

    // Let's put a button in the middle...
    t.setWidget(1, 0, new Button("Wide Button"));

    // ...and set it's column span so that it takes up the whole row.
    t.getFlexCellFormatter().setColSpan(1, 0, 3);

    RootPanel.get().add(t);
  }
}
