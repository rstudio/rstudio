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
package com.google.gwt.cell.client;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

import java.util.Set;

/**
 * Base class for testing {@link Cell}.
 *
 * @param <T> the cell type
 */
public abstract class CellTestBase<T> extends GWTTestCase {

  /**
   * A mock cell used for testing.
   *
   * @param <T> the cell type
   */
  static class MockCell<T> extends AbstractCell<T> {

    private final boolean isSelectable;
    private Context lastContext;
    private T lastEventValue;
    private Element lastParentElement;
    private final T updateValue;

    public MockCell(boolean isSelectable, T updateValue,
        String... consumedEvents) {
      super(consumedEvents);
      this.isSelectable = isSelectable;
      this.updateValue = updateValue;
    }

    public void assertLastEventValue(T expected) {
      assertEquals(expected, lastEventValue);
    }

    public void assertLastParentElement(Element expected) {
      assertEquals(expected, lastParentElement);
    }
    
    @Override
    public boolean dependsOnSelection() {
      return isSelectable;
    }

    public Context getLastContext() {
      return lastContext;
    }

    @Override
    public boolean handlesSelection() {
      return isSelectable;
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, T value,
        NativeEvent event, ValueUpdater<T> valueUpdater) {
      lastContext = context;
      lastParentElement = parent;
      lastEventValue = value;
      if (valueUpdater != null) {
        valueUpdater.update(updateValue);
      }
    }

    @Override
    public void render(Context context, T value, SafeHtmlBuilder sb) {
      lastContext = context;
      if (value != null) {
        sb.appendEscaped(String.valueOf(value));
      }
    }
  }

  /**
   * A mock {@link ValueUpdater} used for testing.
   */
  class MockValueUpdater implements ValueUpdater<T> {

    private T lastValue;

    public void assertLastValue(T expected) {
      assertEquals(expected, lastValue);
      lastValue = null;
    }

    public void update(T value) {
      assertNull(lastValue);
      assertNotNull(value);
      this.lastValue = value;
    }
  }

  /**
   * The default row value key used for all tests.
   */
  protected static final Object DEFAULT_KEY = new Object();

  @Override
  public String getModuleName() {
    return "com.google.gwt.cell.CellTest";
  }

  public void testDependsOnSelection() {
    assertEquals(dependsOnSelection(), createCell().dependsOnSelection());
  }

  public void testGetConsumedEvents() {
    Set<String> consumedEvents = createCell().getConsumedEvents();
    String[] expected = getConsumedEvents();
    if (consumedEvents == null && expected == null) {
      return;
    }
    assertEquals(expected.length, consumedEvents.size());
    for (String typeName : expected) {
      assertTrue(consumedEvents.contains(typeName));
    }
  }

  public void testHandlesSelection() {
    // None of the provided cells handle selection.
    assertFalse(createCell().handlesSelection());
  }

  /**
   * Test rendering the cell with a valid value and no view data.
   */
  public void testRender() {
    Cell<T> cell = createCell();
    T value = createCellValue();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, null);
    cell.render(context, value, sb);
    assertEquals(getExpectedInnerHtml(), sb.toSafeHtml().asString());
  }

  /**
   * Test rendering the cell with a negative index is handled.
   */
  public void testRenderNegativeIndex() {
    Cell<T> cell = createCell();
    T value = createCellValue();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(-1, -1, null);
    cell.render(context, value, sb);
    assertEquals(getExpectedInnerHtml(), sb.toSafeHtml().asString());
  }

  /**
   * Test rendering the cell with a null value and no view data.
   */
  public void testRenderNull() {
    Cell<T> cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, null);
    cell.render(context, null, sb);
    assertEquals(getExpectedInnerHtmlNull(), sb.toSafeHtml().asString());
  }

  /**
   * Create a new cell to test.
   *
   * @return the new cell
   */
  protected abstract Cell<T> createCell();

  /**
   * Create a value to test.
   *
   * @return the cell value
   */
  protected abstract T createCellValue();

  /**
   * Does the cell type depend on selection? Default to false.
   *
   * @return true expected value of dependsOnSelection
   */
  protected abstract boolean dependsOnSelection();

  /**
   * Get the expected events that the cell should consume.
   *
   * @return the consumed events.
   */
  protected abstract String[] getConsumedEvents();

  /**
   * Get the expected inner HTML value of the rendered cell.
   *
   * @return the expected string
   */
  protected abstract String getExpectedInnerHtml();

  /**
   * Get the expected inner HTML value of the rendered cell when null is passed
   * as the cell value.
   *
   * @return the expected string
   */
  protected abstract String getExpectedInnerHtmlNull();

  /**
   * Test
   * {@link Cell#onBrowserEvent(Element, Object, Object, NativeEvent, ValueUpdater)}
   * with the specified conditions.
   * 
   * @param startHtml the innerHTML of the cell before the test starts
   * @param event the event to fire
   * @param value the cell value
   * @param expectedValue the expected value passed to the value updater, or
   *          null if none expected
   * @return the parent element
   */
  protected Element testOnBrowserEvent(String startHtml, NativeEvent event, final T value,
      T expectedValue) {
    return testOnBrowserEvent(createCell(), startHtml, event, value, expectedValue, true);
  }

  /**
   * Test
   * {@link Cell#onBrowserEvent(Element, Object, Object, NativeEvent, ValueUpdater)}
   * with the specified conditions.
   * 
   * @param cell the cell to use
   * @param startHtml the innerHTML of the cell before the test starts
   * @param event the event to fire
   * @param value the cell value
   * @param expectedValue the expected value passed to the value updater, or
   *          null if none expected
   * @param dispatchToFirstChild true to dispatch to the first child of the
   *          rendered parent element, if one is available
   * @return the parent element
   */
  protected Element testOnBrowserEvent(final Cell<T> cell, String startHtml, NativeEvent event,
      final T value, T expectedValue, boolean dispatchToFirstChild) {
    // Setup the parent element.
    final com.google.gwt.user.client.Element parent = Document.get().createDivElement().cast();
    parent.setInnerHTML(startHtml);
    Document.get().getBody().appendChild(parent);

    // If the element has a child, use it as the event target.
    Element target = parent;
    if (dispatchToFirstChild) {
      Element child = parent.getFirstChildElement();
      target = (child == null) ? parent : child;
    }

    // Pass the event to the cell.
    final MockValueUpdater valueUpdater = new MockValueUpdater();
    Event.setEventListener(parent, new EventListener() {
      public void onBrowserEvent(Event event) {
        try {
          DOM.setEventListener(parent, null);
          Context context = new Context(0, 0, DEFAULT_KEY);
          cell.onBrowserEvent(context, parent, value, event, valueUpdater);
          parent.removeFromParent();
        } catch (Exception e) {
          // We are in an event loop, so events may not propagate out to JUnit.
          fail("An exception occured while handling the event: " + e.getMessage());
        }
      }
    });
    Event.sinkEvents(target, Event.getTypeInt(event.getType()));
    target.dispatchEvent(event);
    assertNull(DOM.getEventListener(parent));

    // Check the expected value and view data.
    valueUpdater.assertLastValue(expectedValue);
    return parent;
  }
}
