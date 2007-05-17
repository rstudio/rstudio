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
 * Tests the DockPanel widget.
 */
public class DockPanelTest extends GWTTestCase {

  private static class OnLoadTestLabel extends Label {
    boolean attachedWhenLoaded;

    public OnLoadTestLabel(String text) {
      super(text);
    }

    protected void onLoad() {
      // Crawl up the DOM, looking for the body.
      Element curElem = getElement();
      Element body = RootPanel.getBodyElement();
      while (curElem != null) {
        if (DOM.compare(curElem, body)) {
          attachedWhenLoaded = true;
        }
        curElem = DOM.getParent(curElem);
      }
    }
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAddRemove() {
    final DockPanel dock = new DockPanel();

    OnLoadTestLabel l0 = new OnLoadTestLabel("l0");
    OnLoadTestLabel l1 = new OnLoadTestLabel("l1");
    OnLoadTestLabel l2 = new OnLoadTestLabel("l2");
    OnLoadTestLabel l3 = new OnLoadTestLabel("l3");
    OnLoadTestLabel l4 = new OnLoadTestLabel("l4");
    OnLoadTestLabel l5 = new OnLoadTestLabel("l5");

    dock.add(l0, DockPanel.NORTH);
    dock.add(l1, DockPanel.NORTH);
    dock.add(l2, DockPanel.WEST);

    // Add the dock halfway through to make sure onLoad is called for its
    // children correctly in both cases.
    RootPanel.get().add(dock);
    dock.add(l3, DockPanel.EAST);
    dock.add(l5, DockPanel.CENTER);
    dock.add(l4, DockPanel.SOUTH);

    // Ensure that the CENTER element can be added only once.
    try {
      dock.add(new Label("garbage"), DockPanel.CENTER);
      fail("Expecting IllegalArgumentException (from adding CENTER twice)");
    } catch (IllegalArgumentException e) {
    }

    // Test the structure.
    Element table = dock.getElement();
    Element tbody = DOM.getFirstChild(table);

    assertEquals(DOM.getChildCount(tbody), 4);

    Element tr0 = DOM.getChild(tbody, 0);
    Element tr1 = DOM.getChild(tbody, 1);
    Element tr2 = DOM.getChild(tbody, 2);
    Element tr3 = DOM.getChild(tbody, 3);

    assertEquals(DOM.getChildCount(tr0), 1);
    assertEquals(DOM.getChildCount(tr1), 1);
    assertEquals(DOM.getChildCount(tr2), 3);
    assertEquals(DOM.getChildCount(tr3), 1);

    assertTrue(l0.attachedWhenLoaded);
    assertTrue(l1.attachedWhenLoaded);
    assertTrue(l2.attachedWhenLoaded);
    assertTrue(l3.attachedWhenLoaded);
    assertTrue(l4.attachedWhenLoaded);
    assertTrue(l5.attachedWhenLoaded);

    // Ensure that adding an existing child again moves it to the new slot.
    // (move l4 from SOUTH to NORTH)
    dock.add(l4, DockPanel.NORTH);
    assertTrue(((DockPanel.LayoutData)l4.getLayoutData()).direction == DockPanel.NORTH);
  }
}
