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

import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.PCT;
import static com.google.gwt.dom.client.Style.Unit.PX;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;

public class LayoutExample implements EntryPoint {

  public void onModuleLoad() {
    // The following is a very simple example, which constructs a layout around
    // a parent element, and attaches two child elements that split their
    // parent's space vertically. It then goes on to animate from the first
    // state to a horizontal stacking that uses EM units rather than
    // percentages.
    Document doc = Document.get();
    Element parent = doc.createDivElement();
    doc.getBody().appendChild(parent);

    Layout layout = new Layout(parent);
    layout.onAttach();

    Element topChild = doc.createDivElement(), bottomChild = doc
        .createDivElement();
    Layer topLayer = layout.attachChild(topChild);
    Layer bottomLayer = layout.attachChild(bottomChild);

    // Stack the two children vertically, meeting at 50%.
    topLayer.setLeftRight(0, PX, 0, PX);
    bottomLayer.setLeftRight(0, PX, 0, PX);
    topLayer.setTopHeight(0, PCT, 50, PCT);
    bottomLayer.setBottomHeight(0, PCT, 50, PCT);
    layout.layout();

    // Update the two children to stack horizontally, meeting at 10em.
    // Also have them animate for 500ms.
    topLayer.setTopBottom(0, PX, 0, PX);
    bottomLayer.setTopBottom(0, PX, 0, PX);
    topLayer.setLeftWidth(0, EM, 10, EM);
    bottomLayer.setLeftRight(10, EM, 0, EM);
    layout.layout(500);
  }
}
