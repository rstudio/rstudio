/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tests for {@link SplitLayoutPanel}.
 */
public class SplitLayoutPanelTest extends DockLayoutPanelTest {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    @Override
    public void addChild(HasWidgets container, Widget child) {
      ((SplitLayoutPanel) container).addNorth(child, 10);
    }
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new SplitLayoutPanel(), new Adder(), true);
  }

  public void testReplaceCenterWidget() {
    SplitLayoutPanel p = new SplitLayoutPanel();
    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");

    // center: l1
    p.addWest(l0, 64);
    p.add(l1);
    assertEquals(l1, p.getCenter());

    // center: l2
    p.remove(l1);
    p.add(l2);
    assertEquals(l2, p.getCenter());
  }

  public void testSetWidgetMinSizeCenter() {
    SplitLayoutPanel p = new SplitLayoutPanel();
    Label west = new Label("west");
    Label center = new Label("center");

    p.addWest(west, 100);
    p.add(center);

    // Should be ignored gracefully.
    p.setWidgetMinSize(center, 10);
  }

  public void testSplitterOrder() {
    SplitLayoutPanel p = new SplitLayoutPanel();
    WidgetCollection children = p.getChildren();

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");
    Label l3 = new Label("tintin");
    Label l4 = new Label("toto");

    p.addWest(l0, 64);
    assertEquals(l0, children.get(0));
    assertEquals(SplitLayoutPanel.HSplitter.class, children.get(1).getClass());

    p.addNorth(l1, 64);
    assertEquals(l1, children.get(2));
    assertEquals(SplitLayoutPanel.VSplitter.class, children.get(3).getClass());

    p.addEast(l2, 64);
    assertEquals(l2, children.get(4));
    assertEquals(SplitLayoutPanel.HSplitter.class, children.get(5).getClass());

    p.addSouth(l3, 64);
    assertEquals(l3, children.get(6));
    assertEquals(SplitLayoutPanel.VSplitter.class, children.get(7).getClass());

    p.add(l4);
    assertEquals(l4, children.get(8));
  }

  public void testSplitterSize() {
    SplitLayoutPanel p = new SplitLayoutPanel(5);
    assertEquals(5, p.getSplitterSize());
    WidgetCollection children = p.getChildren();

    p.addWest(new Label("foo"), 64);
    assertEquals("5px",
        children.get(1).getElement().getStyle().getWidth().toLowerCase(Locale.ROOT));
  }

  public void testRemoveInsert() {
    SplitLayoutPanel p = new SplitLayoutPanel();
    WidgetCollection children = p.getChildren();

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");

    p.addWest(l0, 64);
    p.add(l1);
    assertEquals(l0, children.get(0));
    assertEquals(SplitLayoutPanel.HSplitter.class, children.get(1).getClass());
    assertEquals(l1, children.get(2));

    p.remove(l0);
    p.insertWest(l2, 64, l1);
    assertEquals(l2, children.get(0));
    assertEquals(SplitLayoutPanel.HSplitter.class, children.get(1).getClass());
    assertEquals(l1, children.get(2));
  }

  public void testRemoveOutOfOrder() {
    SplitLayoutPanel p = new SplitLayoutPanel();
    WidgetCollection children = p.getChildren();

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");

    p.addWest(l0, 64);
    p.addWest(l1, 64);
    assertEquals(l0, children.get(0));
    assertEquals(SplitLayoutPanel.HSplitter.class, children.get(1).getClass());
    assertEquals(l1, children.get(2));
    assertEquals(SplitLayoutPanel.HSplitter.class, children.get(3).getClass());

    SplitLayoutPanel.HSplitter splitter0 = (SplitLayoutPanel.HSplitter) children.get(1);

    // Remove the second element and make sure the correct splitter is removed.
    p.remove(l1);
    assertEquals(2, children.size());
    assertEquals(l0, children.get(0));
    assertEquals(splitter0, children.get(1));
  }

  @DoNotRunWith({Platform.HtmlUnitLayout})
  public void testResize() {
    SplitLayoutPanel p = new SplitLayoutPanel();
    RootLayoutPanel.get().add(p);

    // RootLayoutPanel handles setting the size automatically, but it's deferred to the document
    // onLoad event, so we'd have to run the whole rest of this test in a deferred command to use
    // that. (Otherwise, Element#getClientHeight always returns 0, since nothing's been rendered.)
    // Setting some absolute heights on the panel manually lets us run the test synchronously.
    p.setHeight("1000px");
    p.setWidth("1000px");

    Label north = new Label("north");
    Label south = new Label("south");
    Label east = new Label("east");
    Label west = new Label("west");

    p.addNorth(north, 100);
    p.addSouth(south, 200);
    p.addEast(east, 300);
    p.addWest(west, 400);
    p.forceLayout();

    WidgetCollection children = p.getChildren();
    Widget northSplitter = children.get(1);
    assertEquals(SplitLayoutPanel.VSplitter.class, northSplitter.getClass());
    Widget southSplitter = children.get(3);
    assertEquals(SplitLayoutPanel.VSplitter.class, southSplitter.getClass());
    Widget eastSplitter = children.get(5);
    assertEquals(SplitLayoutPanel.HSplitter.class, eastSplitter.getClass());
    Widget westSplitter = children.get(7);
    assertEquals(SplitLayoutPanel.HSplitter.class, westSplitter.getClass());

    // Dragging the north splitter down 10px should increase the north size from 100px -> 110px.
    assertEquals(100, north.getOffsetHeight());
    dragSplitter(northSplitter, 0, 10);
    p.forceLayout();
    assertEquals(110, north.getOffsetHeight());

    // Dragging the south splitter up 10px should increase the south size from 200px -> 210px.
    assertEquals(200, south.getOffsetHeight());
    dragSplitter(southSplitter, 0, -10);
    p.forceLayout();
    assertEquals(210, south.getOffsetHeight());

    // Dragging the east splitter right 10px should decrease the east size from 300px -> 290px.
    assertEquals(300, east.getOffsetWidth());
    dragSplitter(eastSplitter, 10, 0);
    p.forceLayout();
    assertEquals(290, east.getOffsetWidth());

    // Dragging the west splitter left 10px should decrease the west size from 400px -> 390px.
    assertEquals(400, west.getOffsetWidth());
    dragSplitter(westSplitter, -10, 0);
    p.forceLayout();
    assertEquals(390, west.getOffsetWidth());
  }

  /**
   * Test that forcing layout will call onResize only once.
   */
  public void testForceLayoutNoRedundantOnResize() {
    final List<Boolean> called = new ArrayList<>();
    SplitLayoutPanel panel = new SplitLayoutPanel();
    SimpleLayoutPanel child = new SimpleLayoutPanel() {
      @Override
      public void onResize() {
        super.onResize();
        called.add(true);
      }
    };
    panel.addWest(child,123.4);
    panel.forceLayout();
    assertEquals(1,called.size());
  }

  @Override
  protected DockLayoutPanel createDockLayoutPanel() {
    return new SplitLayoutPanel();
  }

  /** Simulates a mouse drag on a splitter widget by creating and dispatching native events. */
  private void dragSplitter(Widget splitter, int offsetX, int offsetY) {
    // Even though the actual splitters are at various places in the document, all these mouse
    // drags start at 0,0. The panel calculates the "offset" between the actual position and the
    // mouse position, and compensates for that; if that logic is correct, then the actual
    // positions shouldn't matter, only the relative movement since the mouse down event.
    //
    // This is important in some cases where the browser window is scrolled -- see issue 4755.
    NativeEvent mouseDown = Document.get().createMouseDownEvent(
        0, /* detail */
        0, 0, /* screen X, Y */
        0, 0, /* client X, Y */
        false, false, false, false, /* modifier keys */
        NativeEvent.BUTTON_LEFT);
    splitter.getElement().dispatchEvent(mouseDown);

    NativeEvent mouseMove = Document.get().createMouseMoveEvent(
        0,
        offsetX, offsetY,
        offsetX, offsetY,
        false, false, false, false,
        NativeEvent.BUTTON_LEFT);
    splitter.getElement().dispatchEvent(mouseMove);

    NativeEvent mouseUp = Document.get().createMouseUpEvent(
        0,
        offsetX, offsetY,
        offsetX, offsetY,
        false, false, false, false,
        NativeEvent.BUTTON_LEFT);
    splitter.getElement().dispatchEvent(mouseUp);
  }
}
