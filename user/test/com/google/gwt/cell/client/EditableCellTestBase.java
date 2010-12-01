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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * Base class for testing {@link AbstractEditableCell}s that can be modified.
 *
 * @param <T> the cell type
 * @param <V> the view data type
 */
public abstract class EditableCellTestBase<T, V> extends CellTestBase<T> {

  /**
   * Test rendering the cell with a valid value and view data.
   */
  public void testRenderViewData() {
    AbstractEditableCell<T, V> cell = createCell();
    T value = createCellValue();
    cell.setViewData(DEFAULT_KEY, createCellViewData());
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, DEFAULT_KEY);
    cell.render(context, value, sb);
    String expectedInnerHtmlViewData = getExpectedInnerHtmlViewData();
    String asString = sb.toSafeHtml().asString();
    assertEquals(expectedInnerHtmlViewData, asString);
  }

  @Override
  protected abstract AbstractEditableCell<T, V> createCell();

  /**
   * Create a view data to test.
   *
   * @return the cell view data
   */
  protected abstract V createCellViewData();

  /**
   * Get the expected inner HTML value of the rendered cell when view data is
   * present.
   *
   * @return the expected string
   */
  protected abstract String getExpectedInnerHtmlViewData();

  /**
   * Test
   * {@link Cell#onBrowserEvent(Element, Object, Object, NativeEvent, ValueUpdater)}
   * with the specified conditions.
   *
   * @param startHtml the innerHTML of the cell before the test starts
   * @param event the event to fire
   * @param value the cell value
   * @param viewData the initial view data
   * @param expectedValue the expected value passed to the value updater, or
   *          null if none expected
   * @param expectedViewData the expected value of the view data after the event
   * @return the parent element
   */
  protected Element testOnBrowserEvent(String startHtml, NativeEvent event,
      final T value, V viewData, T expectedValue, V expectedViewData) {
    // Setup the parent element.
    final com.google.gwt.user.client.Element parent = Document.get().createDivElement().cast();
    parent.setInnerHTML(startHtml);
    Document.get().getBody().appendChild(parent);

    // If the element has a child, use it as the event target.
    Element child = parent.getFirstChildElement();
    Element target = (child == null) ? parent : child;

    // Pass the event to the cell.
    final MockValueUpdater valueUpdater = new MockValueUpdater();
    final AbstractEditableCell<T, V> cell = createCell();
    cell.setViewData(DEFAULT_KEY, viewData);
    Event.setEventListener(parent, new EventListener() {
      public void onBrowserEvent(Event event) {
        try {
          DOM.setEventListener(parent, null);
          Context context = new Context(0, 0, DEFAULT_KEY);
          cell.onBrowserEvent(context, parent, value, event, valueUpdater);
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
    assertEquals(expectedViewData, cell.getViewData(DEFAULT_KEY));
    valueUpdater.assertLastValue(expectedValue);

    return parent;
  }
}
