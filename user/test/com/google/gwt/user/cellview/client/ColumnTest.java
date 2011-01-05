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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Timer;

/**
 * Tests for {@link Column}.
 */
public class ColumnTest extends GWTTestCase {

  /**
   * A mock cell used for testing.
   */
  private static class MockEditableCell extends
      AbstractEditableCell<String, String> {

    @Override
    public boolean isEditing(Context context, Element parent, String value) {
      return false;
    }

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
      sb.appendEscaped(value);
    }
  }

  /**
   * A mock {@link FieldUpdater} used for testing.
   *
   * @param <T> the field type
   * @param <C> the value type
   */
  private static class MockFieldUpdater<T, C> implements FieldUpdater<T, C> {

    private int index;
    private T object;
    private boolean updateCalled;
    private C value;

    public void assertIndex(int expected) {
      assertEquals(expected, index);
    }

    public void assertObject(T expected) {
      assertEquals(expected, object);
    }

    public void assertUpdateCalled(boolean expected) {
      assertEquals(expected, updateCalled);
    }

    public void assertValue(C expected) {
      assertEquals(expected, value);
    }

    public void update(int index, T object, C value) {
      assertFalse("Update called twice", updateCalled);
      this.updateCalled = true;
      this.index = index;
      this.object = object;
      this.value = value;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  /**
   * Test that a cell can hold onto the {@link ValueUpdater} and update it
   * later.
   */
  public void testDelayedValueUpdaer() {
    final Element theElem = Document.get().createDivElement();
    final NativeEvent theEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    final MockEditableCell cell = new MockEditableCell() {
      @Override
      public void onBrowserEvent(Context context, Element parent, String value,
          NativeEvent event, final ValueUpdater<String> valueUpdater) {
        setViewData("test", "newViewData");
        new Timer() {
          @Override
          public void run() {
            valueUpdater.update("newValue");
          }
        }.schedule(200);
      }
    };
    final Column<String, String> column = new IdentityColumn<String>(cell);
    final MockFieldUpdater<String, String> fieldUpdater = new MockFieldUpdater<String, String>() {
      @Override
      public void update(int index, String object, String value) {
        assertEquals("newViewData", cell.getViewData("test"));
        super.update(index, object, value);
        finishTest();
      }
    };
    column.setFieldUpdater(fieldUpdater);

    // Fire the event to the cell.
    delayTestFinish(5000);
    cell.setViewData("test", "oldViewData");
    Context context = new Context(3, 0, null);
    column.onBrowserEvent(context, theElem, "test", theEvent);
  }

  public void testGetCell() {
    TextCell cell = new TextCell();
    Column<String, String> column = new IdentityColumn<String>(cell);
    assertEquals(cell, column.getCell());
  }

  public void testOnBrowserEventWithFieldUpdater() {
    final Element theElem = Document.get().createDivElement();
    final NativeEvent theEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    final MockEditableCell cell = new MockEditableCell() {
      @Override
      public void onBrowserEvent(Context context, Element parent, String value,
          NativeEvent event, ValueUpdater<String> valueUpdater) {
        assertEquals(theElem, parent);
        assertEquals("test", value);
        assertEquals("oldViewData", getViewData("test"));
        assertEquals(theEvent, event);
        assertNotNull(valueUpdater);
        setViewData("test", "newViewData");
        valueUpdater.update("newValue");
      }
    };
    final Column<String, String> column = new IdentityColumn<String>(cell);
    final MockFieldUpdater<String, String> fieldUpdater = new MockFieldUpdater<String, String>() {
      @Override
      public void update(int index, String object, String value) {
        // The new view data should already be set.
        assertEquals("newViewData", cell.getViewData("test"));
        super.update(index, object, value);
      }
    };
    column.setFieldUpdater(fieldUpdater);

    cell.setViewData("test", "oldViewData");
    Context context = new Context(3, 0, null);
    column.onBrowserEvent(context, theElem, "test", theEvent);

    fieldUpdater.assertUpdateCalled(true);
    fieldUpdater.assertIndex(3);
    fieldUpdater.assertObject("test");
    fieldUpdater.assertValue("newValue");
  }

  public void testOnBrowserEventWithoutFieldUpdater() {
    final Element theElem = Document.get().createDivElement();
    final NativeEvent theEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    final MockEditableCell cell = new MockEditableCell() {
      @Override
      public void onBrowserEvent(Context context, Element parent, String value,
          NativeEvent event, ValueUpdater<String> valueUpdater) {
        assertEquals(theElem, parent);
        assertEquals("test", value);
        assertEquals("oldViewData", getViewData("test"));
        assertEquals(theEvent, event);
        assertNull(valueUpdater);
        setViewData("test", "newViewData");
      }
    };
    Column<String, String> column = new IdentityColumn<String>(cell);

    cell.setViewData("test", "oldViewData");
    Context context = new Context(3, 0, null);
    column.onBrowserEvent(context, theElem, "test", theEvent);
  }

  public void testRender() {
    TextCell cell = new TextCell();
    Column<String, String> column = new IdentityColumn<String>(cell);

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, null);
    column.render(context, "test", sb);
    assertEquals("test", sb.toSafeHtml().asString());
  }

  public void testSetSortable() {
    TextCell cell = new TextCell();
    Column<String, String> column = new IdentityColumn<String>(cell);
    assertFalse(column.isSortable());

    column.setSortable(true);
    assertTrue(column.isSortable());

    column.setSortable(false);
    assertFalse(column.isSortable());
  }
}
