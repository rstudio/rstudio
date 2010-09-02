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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

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
    cell.render(value, DEFAULT_KEY, sb);
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
   * Test {@link Cell#onBrowserEvent(Element, Object, Object, NativeEvent,
   * ValueUpdater)} with the specified conditions.
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
      T value, V viewData, T expectedValue, V expectedViewData) {
    // Setup the parent element.
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(startHtml);

    // Pass the event to the cell.
    MockValueUpdater valueUpdater = new MockValueUpdater();
    AbstractEditableCell<T, V> cell = createCell();
    cell.setViewData(DEFAULT_KEY, viewData);
    cell.onBrowserEvent(parent, value, DEFAULT_KEY, event, valueUpdater);
    assertEquals(expectedViewData, cell.getViewData(DEFAULT_KEY));
    valueUpdater.assertLastValue(expectedValue);

    return parent;
  }
}
