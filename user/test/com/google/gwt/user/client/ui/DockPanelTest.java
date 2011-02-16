/*
 * Copyright 2008 Google Inc.
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
@SuppressWarnings("deprecation")
public class DockPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((DockPanel) container).add(child, DockPanel.NORTH);
    }
  }

  private static class OnLoadTestLabel extends Label {
    boolean attachedWhenLoaded;

    public OnLoadTestLabel(String text) {
      super(text);
    }

    @Override
    protected void onLoad() {
      // Crawl up the DOM, looking for the body.
      Element curElem = getElement();
      Element body = RootPanel.getBodyElement();
      while (curElem != null) {
        if (curElem == body) {
          attachedWhenLoaded = true;
        }
        curElem = DOM.getParent(curElem);
      }
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
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
    assertTrue(((DockPanel.LayoutData) l4.getLayoutData()).direction == DockPanel.NORTH);
  }

  public void testAddAsIsWidget() {
    DockPanel panel = createDockPanel();
    Widget widget = new Label("foo");
    
    panel.add(widget, DockPanel.NORTH);
    
    assertLogicalPaternity(panel,widget);
    assertPhysicalPaternity(panel,widget);
  }
  
  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new DockPanel(), new Adder(), true);
  }

  public void testDebugId() {
    DockPanel dock = new DockPanel();
    Label north1 = new Label("n1");
    dock.add(north1, DockPanel.NORTH);
    Label north2 = new Label("n2");
    dock.add(north2, DockPanel.NORTH);
    Label south1 = new Label("s1");
    dock.add(south1, DockPanel.SOUTH);
    Label south2 = new Label("s2");
    dock.add(south2, DockPanel.SOUTH);
    Label east1 = new Label("e1");
    dock.add(east1, DockPanel.EAST);
    Label east2 = new Label("e2");
    dock.add(east2, DockPanel.EAST);
    Label west1 = new Label("w1");
    dock.add(west1, DockPanel.WEST);
    Label west2 = new Label("w2");
    dock.add(west2, DockPanel.WEST);
    Label center = new Label("c");
    dock.add(center, DockPanel.CENTER);
    dock.ensureDebugId("myDock");

    // Check the body ids
    UIObjectTest.assertDebugId("myDock", dock.getElement());
    UIObjectTest.assertDebugId("myDock-north1",
        DOM.getParent(north1.getElement()));
    UIObjectTest.assertDebugId("myDock-north2",
        DOM.getParent(north2.getElement()));
    UIObjectTest.assertDebugId("myDock-south1",
        DOM.getParent(south1.getElement()));
    UIObjectTest.assertDebugId("myDock-south2",
        DOM.getParent(south2.getElement()));
    UIObjectTest.assertDebugId("myDock-east1",
        DOM.getParent(east1.getElement()));
    UIObjectTest.assertDebugId("myDock-east2",
        DOM.getParent(east2.getElement()));
    UIObjectTest.assertDebugId("myDock-west1",
        DOM.getParent(west1.getElement()));
    UIObjectTest.assertDebugId("myDock-west2",
        DOM.getParent(west2.getElement()));
    UIObjectTest.assertDebugId("myDock-center",
        DOM.getParent(center.getElement()));
  }

  /**
   * Asserts that <b>panel</b> is the logical parent of <b>expectedChild</b>.
   * 
   * @param panel the parent panel
   * @param expectedChild the expected child of <b>panel</b>
   */
  private void assertLogicalPaternity(DockPanel panel, Widget expectedChild) {
    assertSame("The parent and the panel must be the same", panel,
        expectedChild.getParent());
    assertTrue("The child must be in the childen collection of the panel",
        panel.getChildren().contains(expectedChild));
  }
  
  /**
   * Asserts that <b>expectedChild</b> is the first physical child of
   * <b>parent</b>.
   * 
   * @param parent the parent panel
   * @param expectedChild the expected child of <b>panel</b>
   */
  private void assertPhysicalPaternity(Widget parent,
      Widget expectedChild) {
    Element panelElement = parent.getElement();
    Element childElement = expectedChild.getElement();
    assertTrue("The parent's Element of the child must be the panel's Element", DOM.isOrHasChild(panelElement, childElement));
  }

  private DockPanel createDockPanel() {
    return new DockPanel();
  }
}
