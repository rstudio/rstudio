/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * TODO: document me.
 */
public class LinearPanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testHorizontalAddRemove() {
    HorizontalPanel hp = new HorizontalPanel();
    RootPanel.get().add(hp);

    HTML[] stuff = new HTML[3];
    stuff[0] = new HTML("foo");
    stuff[1] = new HTML("bar");
    stuff[2] = new HTML("baz");

    // Ensure that we can add & remove cleanly.
    hp.add(stuff[0]);
    hp.add(stuff[1]);
    hp.add(stuff[2]);

    assertTrue(hp.getWidgetCount() == 3);
    hp.remove(stuff[1]);
    assertTrue(hp.getWidgetCount() == 2);
    assertTrue(hp.getWidget(0) == stuff[0]);
    assertTrue(hp.getWidget(1) == stuff[2]);

    // Make sure the table structure is still correct (no stuff left hanging
    // around).
    Element elem = hp.getElement();
    Element body = DOM.getFirstChild(elem);
    Element tr = DOM.getFirstChild(body);
    assertTrue(DOM.getChildCount(tr) == 2);
  }

  public void testVerticalAddRemove() {
    VerticalPanel hp = new VerticalPanel();
    RootPanel.get().add(hp);

    HTML[] stuff = new HTML[3];
    stuff[0] = new HTML("foo");
    stuff[1] = new HTML("bar");
    stuff[2] = new HTML("baz");

    // Ensure that we can add & remove cleanly.
    hp.add(stuff[0]);
    hp.add(stuff[1]);
    hp.add(stuff[2]);

    assertTrue(hp.getWidgetCount() == 3);
    hp.remove(stuff[1]);
    assertTrue(hp.getWidgetCount() == 2);
    assertTrue(hp.getWidget(0) == stuff[0]);
    assertTrue(hp.getWidget(1) == stuff[2]);

    // Make sure the table structure is still correct (no stuff left hanging
    // around).
    Element elem = hp.getElement();
    Element body = DOM.getFirstChild(elem);
    assertTrue(DOM.getChildCount(body) == 2);
  }
}
