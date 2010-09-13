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
package com.google.gwt.user.client.ui;

import java.util.Iterator;

/**
 * Base tests for classes that extend {@link Panel}
 * 
 * TODO: add circular containment test.
 * 
 * @param <T> the panel type
 */
public abstract class PanelTestBase<T extends Panel> extends WidgetTestBase {
  
  public void testIsWidget() {
    T panel = createPanel();
    IsWidgetImpl l = new IsWidgetImpl(new Label("l"));
    panel.add(l);
    Iterator<Widget> iterator = panel.iterator();
    assertSame(l.w, iterator.next());
    assertFalse(iterator.hasNext());
    
    panel.remove(l);
    assertFalse(panel.iterator().hasNext());
  }
  
  public void testIndexedPanel() {
    T panel = createPanel();
    if (!(panel instanceof IndexedPanel)) {
      return;
    }
    
    assertTrue("Expect all IndexedPanels to implement ForIsWidget",
        panel instanceof IndexedPanel.ForIsWidget);
    
    IndexedPanel.ForIsWidget w = (IndexedPanel.ForIsWidget) panel;
    
    IsWidgetImpl l1 = new IsWidgetImpl(new Label("l1"));
    IsWidgetImpl l2 = new IsWidgetImpl(new Label("l2"));
    IsWidgetImpl l3 = new IsWidgetImpl(new Label("l3"));
    IsWidgetImpl l4 = new IsWidgetImpl(new Label("l4"));
    
    panel.add(l1);
    panel.add(l2);
    panel.add(l3);
    panel.add(l4);
    
    assertEquals(4, w.getWidgetCount());
    assertEquals(0, w.getWidgetIndex(l1.w));
    assertEquals(0, w.getWidgetIndex(l1));
    assertEquals(1, w.getWidgetIndex(l2));
    assertEquals(2, w.getWidgetIndex(l3));
    assertEquals(3, w.getWidgetIndex(l4));
    
    assertTrue(w.remove(2));
    assertEquals(3, w.getWidgetCount());
    assertEquals(0, w.getWidgetIndex(l1.w));
    assertEquals(0, w.getWidgetIndex(l1));
    assertEquals(1, w.getWidgetIndex(l2));
    assertEquals(-1, w.getWidgetIndex(l3));
    assertEquals(2, w.getWidgetIndex(l4));

    assertTrue(w.remove(2));
    assertEquals(2, w.getWidgetCount());
    assertEquals(0, w.getWidgetIndex(l1.w));
    assertEquals(0, w.getWidgetIndex(l1));
    assertEquals(1, w.getWidgetIndex(l2));
    assertEquals(-1, w.getWidgetIndex(l3));
    assertEquals(-1, w.getWidgetIndex(l4));

    assertTrue(w.remove(0));
    assertEquals(1, w.getWidgetCount());
    assertEquals(-1, w.getWidgetIndex(l1.w));
    assertEquals(-1, w.getWidgetIndex(l1));
    assertEquals(0, w.getWidgetIndex(l2));
    assertEquals(-1, w.getWidgetIndex(l3));
    assertEquals(-1, w.getWidgetIndex(l4));

    assertTrue(w.remove(0));
    assertEquals(0, w.getWidgetCount());
    assertFalse(panel.iterator().hasNext());
  }

  public void testInsertPanel() {
    T panel = createPanel();
    if (!(panel instanceof InsertPanel)) {
      return;
    }
    
    assertTrue("Expect all InsertPanel to implement ForIsWidget",
        panel instanceof InsertPanel.ForIsWidget);
    
    InsertPanel.ForIsWidget w = (InsertPanel.ForIsWidget) panel;
    
    IsWidgetImpl l1 = new IsWidgetImpl(new Label("l1"));
    IsWidgetImpl l2 = new IsWidgetImpl(new Label("l2"));
    IsWidgetImpl l3 = new IsWidgetImpl(new Label("l3"));
    IsWidgetImpl l4 = new IsWidgetImpl(new Label("l4"));

    w.add(l1);
    w.add(l2.w);

    w.insert(l3, 1);
    w.insert(l4.w, 3);
    w.insert(l1, 3);
    w.insert(l2.w, 0);
    
    Widget[] expected = new Widget[] {
        l2.w, l3.w, l1.w,  l4.w,
    };
    
    Iterator<Widget> iterator = panel.iterator();
    for (Widget e : expected) {
      Widget next = iterator.next();
      assertSame("Expected " + e + ", saw " + next, e, next);
    }
    assertFalse(iterator.hasNext());
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(createPanel(),
        new HasWidgetsTester.DefaultWidgetAdder(), supportsMultipleWidgets());
  }

  public void testOnAttach() {
    // Used to call onDetach when not attached.
    Widget someWidget = new TextBox();
    Panel panel1 = createPanel(); // new and unattached
    Panel panel2 = createPanel(); // new and unattached
    panel1.add(someWidget);
    panel2.add(someWidget);

    // Make sure that the RootPanel does not throw an exception.
    RootPanel.get().setParent(null);
    RootPanel.get().setParent(null);
  }

  public void testRemoveWithError() {
    // Create a widget that will throw an exception onUnload.
    BadWidget badWidget = new BadWidget();
    badWidget.setFailOnUnload(true);

    // Add the widget to a panel.
    Panel panel = createPanel();
    panel.add(badWidget);
    assertFalse(badWidget.isAttached());

    // Attach the widget.
    RootPanel.get().add(panel);
    assertTrue(badWidget.isAttached());

    // Remove the widget from the panel.
    try {
      panel.remove(badWidget);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertFalse(badWidget.isAttached());
    assertNull(badWidget.getParent());
    assertNull(badWidget.getElement().getParentElement());

    // Detach the panel to ensure that it doesn't throw an exception.
    RootPanel.get().remove(panel);
  }

  protected abstract T createPanel();

  /**
   * Check if the panel in test supports multiple (unbounded) widgets.
   * 
   * @return true if multiple widgets are supported
   */
  protected boolean supportsMultipleWidgets() {
    return true;
  }
}
