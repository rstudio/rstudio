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

import static com.google.gwt.dom.client.Style.Unit.PCT;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class LayoutPanelExample implements EntryPoint {

  public void onModuleLoad() {
    // Attach two child widgets to a LayoutPanel, laying them out horizontally,
    // splitting at 50%.
    Widget childOne = new HTML("left"), childTwo = new HTML("right");
    LayoutPanel p = new LayoutPanel();
    p.add(childOne);
    p.add(childTwo);

    Layer layerOne = p.getLayer(childOne), layerTwo = p.getLayer(childTwo);
    layerOne.setLeftWidth(0, PCT, 50, PCT);
    layerTwo.setRightWidth(0, PCT, 50, PCT);

    // Note the explicit call to layout(). This is required for the layout to
    // take effect.
    p.layout();

    // Attach the LayoutPanel to the RootLayoutPanel. The latter will listen for
    // resize events on the window to ensure that its children are informed of
    // possible size changes.
    RootLayoutPanel rp = RootLayoutPanel.get();
    rp.add(p);

    // The RootLayoutPanel also requires that its layout() method be explicitly
    // called for the initial layout to take effect.
    rp.layout();
  }
}