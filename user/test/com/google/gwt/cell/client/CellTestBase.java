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

    private final boolean consumesEvents;
    private final boolean dependsOnSelection;
    private T lastEventValue;
    private final T updateValue;

    public MockCell(boolean consumesEvents, boolean dependsOnSelection,
        T updateValue) {
      this.consumesEvents = consumesEvents;
      this.dependsOnSelection = dependsOnSelection;
      this.updateValue = updateValue;
    }

    public void assertLastEventValue(T expected) {
      assertEquals(expected, lastEventValue);
    }

    @Override
    public boolean consumesEvents() {
      return consumesEvents;
    }

    @Override
    public boolean dependsOnSelection() {
      return dependsOnSelection;
    }

    @Override
    public Object onBrowserEvent(Element parent, T value, Object viewData,
        NativeEvent event, ValueUpdater<T> valueUpdater) {
      lastEventValue = value;
      if (valueUpdater != null) {
        valueUpdater.update(updateValue);
      }
      return null;
    }

    @Override
    public void render(T value, Object viewData, StringBuilder sb) {
      if (value != null) {
        sb.append(value);
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

  @Override
  public String getModuleName() {
    return "com.google.gwt.cell.Cell";
  }

  public void testConsumesEvents() {
    assertEquals(consumesEvents(), createCell().consumesEvents());
  }

  public void testDependsOnSelection() {
    assertEquals(dependsOnSelection(), createCell().dependsOnSelection());
  }

  public void testOnBrowserEventNullValueUpdater() {
    Cell<T> cell = createCell();
    T value = createCellValue();
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(getExpectedInnerHtml());

    cell.onBrowserEvent(parent, value, null, event, null);
    // Make sure that no exceptions occur.
  }

  public void testRender() {
    Cell<T> cell = createCell();
    T value = createCellValue();
    StringBuilder sb = new StringBuilder();
    cell.render(value, null, sb);
    assertEquals(getExpectedInnerHtml(), sb.toString());
  }

  public void testRenderNull() {
    Cell<T> cell = createCell();
    StringBuilder sb = new StringBuilder();
    cell.render(null, null, sb);
    assertEquals(getExpectedInnerHtmlNull(), sb.toString());
  }

  /**
   * Does the cell type consume events? Default to false.
   * 
   * @return true expected value of consumesEvents
   */
  protected abstract boolean consumesEvents();

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
   * @param viewData the view data to pass to the cell
   * @param value the cell value
   * @param expectedValue the expected value passed to the value updater, or
   *          null if none expected
   * @return the parent element
   */
  protected Element testOnBrowserEvent(String startHtml, NativeEvent event,
      Object viewData, T value, T expectedValue) {
    // Setup the parent element.
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(startHtml);

    // Pass the event to the cell.
    MockValueUpdater updater = new MockValueUpdater();
    createCell().onBrowserEvent(parent, value, viewData, event, updater);
    updater.assertLastValue(expectedValue);

    return parent;
  }
}
