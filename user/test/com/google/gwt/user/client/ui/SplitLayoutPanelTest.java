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

/**
 * Tests for {@link SplitLayoutPanel}.
 */
public class SplitLayoutPanelTest extends WidgetTestBase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
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
}
