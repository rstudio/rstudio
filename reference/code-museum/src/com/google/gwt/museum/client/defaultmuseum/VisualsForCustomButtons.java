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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.museum.client.common.SimpleLayout;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.Widget;

/**
 * Visual test for custom buttons.
 */
public class VisualsForCustomButtons extends AbstractIssue {

  @Override
  public Widget createIssue() {
    SimpleLayout l = new SimpleLayout();
    Tree t = new Tree();
    PushButton a = new PushButton("Simple-dum");
    t.add(a);
    PushButton a2 = new PushButton("Simple-dee");
    t.add(a2);
    PushButton aUp = new PushButton("A-up");
    aUp.getDownFace().setHTML("A-down");
    t.add(aUp);

    ToggleButton b = new ToggleButton("B");
    t.add(b);
    l.add(t);
    l.nextRow();
    EventReporter<Object, Object> handler = new EventReporter<Object, Object>();
    l.addFooter(handler);
    handler.addClickHandler(a, l);
    handler.addClickHandler(a2, l);
    handler.addClickHandler(aUp, l);
    handler.addClickHandler(b, l);

    return l;
  }

  @Override
  public String getInstructions() {
    return "Check for custom buttons";
  }

  @Override
  public String getSummary() {
    return "CustomButton visual test";
  }

  @Override
  public boolean hasCSS() {

    return false;
  }

}
