/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.user.client.ui.FiniteWidgetIterator.WidgetProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Test cases for {@link FiniteWidgetIterator}.
 */
public class FiniteWidgetIteratorTest extends GWTTestCase {

  /**
   * Implementation of {@link WidgetProvider}.
   */
  private static class WidgetProviderImpl implements WidgetProvider {

    private final Map<Integer, Widget> widgets = new HashMap<Integer, Widget>();

    /**
     * Construct a new {@link WidgetProviderImpl} with the specified widgets.
     */
    public WidgetProviderImpl(Widget... widgets) {
      if (widgets != null) {
        for (int i = 0; i < widgets.length; i++) {
          setWidget(i, widgets[i]);
        }
      }
    }

    /**
     * Set the widget at the specified index.
     * 
     * @param index the index
     * @param w the widget
     */
    public void setWidget(int index, Widget w) {
      widgets.put(index, w);
    }

    public Widget get(int index) {
      if (!widgets.containsKey(index)) {
        fail("A widget was not specified for index: " + index);
      }
      return widgets.get(index);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testHasNextEmpty() {
    FiniteWidgetIterator iterator = new FiniteWidgetIterator(new WidgetProviderImpl(), 0);
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // Expected.
    }
  }

  public void testNext() {
    Widget w0 = new Widget();
    Widget w1 = new Widget();
    Widget w2 = new Widget();
    FiniteWidgetIterator iterator = new FiniteWidgetIterator(new WidgetProviderImpl(w0, w1, w2), 3);

    assertTrue(iterator.hasNext());
    assertEquals(w0, iterator.next());

    assertTrue(iterator.hasNext());
    assertEquals(w1, iterator.next());

    assertTrue(iterator.hasNext());
    assertEquals(w2, iterator.next());

    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // Expected.
    }
  }

  /**
   * Test that the iterator skips null widgets at the start of the list.
   */
  public void testNullWidgetStart() {
    Widget w1 = new Widget();
    Widget w2 = new Widget();
    FiniteWidgetIterator iterator =
        new FiniteWidgetIterator(new WidgetProviderImpl(null, w1, w2), 3);

    assertTrue(iterator.hasNext());
    assertEquals(w1, iterator.next());

    assertTrue(iterator.hasNext());
    assertEquals(w2, iterator.next());

    assertFalse(iterator.hasNext());
  }

  /**
   * Test that the iterator skips null widgets at the end of the list.
   */
  public void testNullWidgetEnd() {
    Widget w0 = new Widget();
    Widget w1 = new Widget();
    FiniteWidgetIterator iterator =
        new FiniteWidgetIterator(new WidgetProviderImpl(w0, w1, null), 3);

    assertTrue(iterator.hasNext());
    assertEquals(w0, iterator.next());

    assertTrue(iterator.hasNext());
    assertEquals(w1, iterator.next());

    assertFalse(iterator.hasNext());
  }

  /**
   * Test that the iterator skips null widgets in the middle of the list.
   */
  public void testNullWidgetMiddle() {
    Widget w0 = new Widget();
    Widget w2 = new Widget();
    FiniteWidgetIterator iterator =
        new FiniteWidgetIterator(new WidgetProviderImpl(w0, null, w2), 3);

    assertTrue(iterator.hasNext());
    assertEquals(w0, iterator.next());

    assertTrue(iterator.hasNext());
    assertEquals(w2, iterator.next());

    assertFalse(iterator.hasNext());
  }

  public void testRemove() {
    Widget w0 = new Label();
    Widget w1 = new Label();
    Widget w2 = new Label();
    WidgetProviderImpl provider = new WidgetProviderImpl(w0, w1, w2);
    FiniteWidgetIterator iterator = new FiniteWidgetIterator(provider, 3);

    // Add the widgets to a panel.
    FlowPanel panel = new FlowPanel();
    panel.add(w0);
    panel.add(w1);
    panel.add(w2);

    // Remove before getting first widget.
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

    // Remove the widget.
    assertEquals(w0, iterator.next());
    assertEquals(panel, w0.getParent());
    iterator.remove();
    assertNull(w0.getParent());
    provider.setWidget(0, null); // Update the provider.

    // Try to remove again.
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

    // Get the next widget.
    assertEquals(w1, iterator.next());
  }
}
