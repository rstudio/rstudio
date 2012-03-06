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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextButtonCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;
import com.google.gwt.view.client.ProvidesKey;

/**
 * Tests for {@link CellWidget}.
 */
public class CellWidgetTest extends GWTTestCase {

  /**
   * A custom cell used for testing.
   */
  private static class CustomCell extends AbstractCell<String> {

    private String lastEventValue;
    private Object lastEventKey;
    private String lastRenderedValue = "never_rendered";

    public CustomCell() {
      super("change");
    }

    public void assertLastEventKey(String expected) {
      assertEquals(expected, lastEventKey);
      lastEventKey = null;
    }

    public void assertLastEventValue(String expected) {
      assertEquals(expected, lastEventValue);
      lastEventValue = null;
    }

    public void assertLastRenderedValue(String expected) {
      assertEquals(expected, lastRenderedValue);
      lastRenderedValue = null;
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
        ValueUpdater<String> valueUpdater) {
      lastEventValue = value;
      lastEventKey = context.getKey();
      if (valueUpdater != null) {
        valueUpdater.update("newValue");
      }
    }

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
      lastRenderedValue = value;
      if (value != null) {
        sb.appendEscaped(value);
      }
    }
  }

  /**
   * A mock value change handler used for testing.
   * 
   * @param <C> the data type
   */
  private static class MockValueChangeHandler<C> implements ValueChangeHandler<C> {

    private boolean onValueChangeCalled = false;
    private C lastValue;

    public void assertOnValueChangeNotCalled() {
      assertFalse(onValueChangeCalled);
    }

    public void assertLastValue(C expected) {
      assertTrue(onValueChangeCalled);
      assertEquals(expected, lastValue);
      lastValue = null;
      onValueChangeCalled = false;
    }

    @Override
    public void onValueChange(ValueChangeEvent<C> event) {
      assertFalse("ValueChangeEvent fired twice", onValueChangeCalled);
      onValueChangeCalled = true;
      lastValue = event.getValue();
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  /**
   * Tests that the cell widget will render correctly with an initial value of null.
   */
  public void testInitialValueNull() {
    CustomCell cell = new CustomCell();
    CellWidget<String> cw = new CellWidget<String>(cell);
    assertNull(cw.getValue());
    cell.assertLastRenderedValue(null);
  }

  public void testOnBrowserEvent() {
    CustomCell cell = new CustomCell();
    CellWidget<String> cw = new CellWidget<String>(cell, "test");
    assertEquals("test", cw.getValue());

    Event event = Document.get().createChangeEvent().cast();
    cw.onBrowserEvent(event);
    cell.assertLastEventKey("test");
    cell.assertLastEventValue("test");
    assertEquals("newValue", cw.getValue());
  }

  public void testOnBrowserEventWithKeyProvider() {
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
      @Override
      public Object getKey(String item) {
        // Return the first character as the key.
        return (item == null) ? null : item.substring(0, 1);
      }
    };
    CustomCell cell = new CustomCell();
    final CellWidget<String> cw = new CellWidget<String>(cell, "test", keyProvider);
    assertEquals("test", cw.getValue());
    assertEquals(keyProvider, cw.getKeyProvider());

    Event event = Document.get().createChangeEvent().cast();
    cw.onBrowserEvent(event);
    cell.assertLastEventKey("t");
    cell.assertLastEventValue("test");
    assertEquals("newValue", cw.getValue());
  }

  public void testOnBrowserEventWithValueChangeHandler() {
    CustomCell cell = new CustomCell();
    final CellWidget<String> cw = new CellWidget<String>(cell, "test");
    assertEquals("test", cw.getValue());

    // Add a ValueChangeHandler.
    MockValueChangeHandler<String> handler = new MockValueChangeHandler<String>();
    cw.addValueChangeHandler(handler);

    // Fire an native event that will trigger a value change event.
    Event event = Document.get().createChangeEvent().cast();
    cw.onBrowserEvent(event);
    cell.assertLastEventKey("test");
    cell.assertLastEventValue("test");
    handler.assertLastValue("newValue");
    assertEquals("newValue", cw.getValue());
  }

  /**
   * Test that a cell that defines an HTML elment can be rendered.
   */
  public void testRedrawWithMultipleInnerChildren() {
    Cell<String> cell = new AbstractCell<String>() {
      @Override
      public void render(com.google.gwt.cell.client.Cell.Context context, String value,
          SafeHtmlBuilder sb) {
        if (value != null) {
          sb.appendHtmlConstant("<div>").appendEscaped(value).appendHtmlConstant("</div>");
          sb.appendHtmlConstant("<div>child2</div>");
        }
      }
    };
    CellWidget<String> cw = new CellWidget<String>(cell);

    // Set value without redrawing.
    cw.setValue("test123", false, false);
    assertEquals("", cw.getElement().getInnerText());

    // Redraw.
    cw.redraw();
    assertTrue(cw.getElement().getInnerText().contains("test123"));
    Style firstChildStyle = cw.getElement().getFirstChildElement().getStyle();
    assertFalse(firstChildStyle.getHeight().matches("100(.0)?%"));
    assertFalse(firstChildStyle.getWidth().matches("100(.0)?%"));
  }

  /**
   * Test that a cell that defines an HTML elment can be rendered.
   */
  public void testRedrawWithOneInnerChild() {
    CellWidget<String> cw = new CellWidget<String>(new TextButtonCell());

    // Set value without redrawing.
    cw.setValue("test123", false, false);
    assertEquals("", cw.getElement().getInnerText());

    // Redraw.
    cw.redraw();
    assertTrue(cw.getElement().getInnerText().contains("test123"));
    Style firstChildStyle = cw.getElement().getFirstChildElement().getStyle();
    assertTrue(firstChildStyle.getHeight().matches("100(.0)?%"));
    assertTrue(firstChildStyle.getWidth().matches("100(.0)?%"));
  }

  /**
   * Test that a cell that defines no HTML elments can be rendered.
   */
  public void testRedrawWithoutInnerChild() {
    CellWidget<String> cw = new CellWidget<String>(new CustomCell());

    // Set value without redrawing.
    cw.setValue("test", false, false);
    assertEquals("", cw.getElement().getInnerText());

    // Redraw.
    cw.redraw();
    assertEquals("test", cw.getElement().getInnerText());
  }

  public void testSetValue() {
    CustomCell cell = new CustomCell();
    CellWidget<String> cw = new CellWidget<String>(cell, "initial");
    MockValueChangeHandler<String> handler = new MockValueChangeHandler<String>();
    cw.addValueChangeHandler(handler);

    // Check the intial value.
    assertEquals("initial", cw.getValue());
    assertEquals("initial", cw.getElement().getInnerText());
    cell.assertLastRenderedValue("initial");

    // Set value without firing events.
    cw.setValue("test0");
    assertEquals("test0", cw.getValue());
    assertEquals("test0", cw.getElement().getInnerText());
    cell.assertLastRenderedValue("test0");
    handler.assertOnValueChangeNotCalled();

    // Set value to the existing value, shouldn't fire events.
    cw.setValue("test0", true);
    handler.assertOnValueChangeNotCalled();

    // Set value and fire events.
    cw.setValue("test1", true);
    assertEquals("test1", cw.getValue());
    assertEquals("test1", cw.getElement().getInnerText());
    cell.assertLastRenderedValue("test1");
    handler.assertLastValue("test1");

    // Set value, fire events, but do not redraw.
    cw.setValue("test no redraw", true, false);
    assertEquals("test no redraw", cw.getValue());
    assertEquals("test1", cw.getElement().getInnerText());
    handler.assertLastValue("test no redraw");
  }
}