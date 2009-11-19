/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.StackLayoutPanel;

public class StackLayoutPanelExample implements EntryPoint {

  public void onModuleLoad() {
    // Create a three-item stack, with headers sized in EMs. 
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    p.add(new HTML("this"), new HTML("[this]"), 4);
    p.add(new HTML("that"), new HTML("[that]"), 4);
    p.add(new HTML("the other"), new HTML("[the other]"), 4);

    // Attach the LayoutPanel to the RootLayoutPanel. The latter will listen for
    // resize events on the window to ensure that its children are informed of
    // possible size changes.
    RootLayoutPanel rp = RootLayoutPanel.get();
    rp.add(p);
  }
}
