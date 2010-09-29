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
    private T lastEventValue;
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

    @Override
    public boolean dependsOnSelection() {
      return isSelectable;
    }

    @Override
    public boolean handlesSelection() {
      return isSelectable;
    }

    @Override
    public void onBrowserEvent(Element parent, T value, Object key,
        NativeEvent event, ValueUpdater<T> valueUpdater) {
      lastEventValue = value;
      if (valueUpdater != null) {
        valueUpdater.update(updateValue);
      }
    }

    @Override
    public void render(T value, Object key, SafeHtmlBuilder sb) {
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
    cell.render(value, null, sb);
    assertEquals(getExpectedInnerHtml(), sb.toSafeHtml().asString());
  }

  /**
   * Test rendering the cell with a null value and no view data.
   */
  public void testRenderNull() {
    Cell<T> cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
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
  protected Element testOnBrowserEvent(String startHtml, NativeEvent event,
      final T value, T expectedValue) {
    // Setup the parent element.
    final com.google.gwt.user.client.Element parent = Document.get().createDivElement().cast();
    parent.setInnerHTML(startHtml);
    Document.get().getBody().appendChild(parent);

    // If the element has a child, use it as the event target.
    Element child = parent.getFirstChildElement();
    Element target = (child == null) ? parent : child;

    // Pass the event to the cell.
    final MockValueUpdater valueUpdater = new MockValueUpdater();
    Event.setEventListener(parent, new EventListener() {
      public void onBrowserEvent(Event event) {
        try {
          DOM.setEventListener(parent, null);
          createCell().onBrowserEvent(parent, value, DEFAULT_KEY, event,
              valueUpdater);
          parent.removeFromParent();
        } catch (Exception e) {
          // We are in an event loop, so events may not propagate out to JUnit.
          fail("An exception occured while handling the event: "
              + e.getMessage());
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
