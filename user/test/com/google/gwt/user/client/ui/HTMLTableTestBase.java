/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Base test for HTMLTable derived classes.
 */
public abstract class HTMLTableTestBase extends GWTTestCase {
  static class Adder implements HasWidgetsTester.WidgetAdder {
    private int row = -1;

    public void addChild(HasWidgets container, Widget child) {
      ((HTMLTable) container).setWidget(++row, 0, child);
    }
  }

  private static final String html = "<b>hello</b><i>world</i>";

  public static void assertEquals(Object[] x, Object[] y) {
    assertEquals(x.length, y.length);
    for (int i = 0; i < y.length; i++) {
      assertEquals(x[i], y[i]);
    }
  }

  /**
   * Easy way to test what should be in a list.
   */
  protected static void assertEquals(Object[] array, List<?> target) {
    if (target.size() != array.length) {
      fail(target + " should be the same length as" + Arrays.toString(array));
    }
    for (int i = 0; i < array.length; i++) {
      assertEquals(target.get(i), array[i]);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public abstract HTMLTable getTable(int row, int column);

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(getTable(25, 1), new Adder(), true);
  }

  public void testBoundsOnEmptyTable() {
    HTMLTable t = getTable(0, 0);
    try {
      t.getCellFormatter().getElement(4, 5);
    } catch (IndexOutOfBoundsException e) {
      return;
    }
    fail("should have throw an index out of bounds");
  }

  /**
   * Tests for {@link HTMLTable.Cell}.
   */
  public void testCell() {
    HTMLTable table = getTable(1, 4);
    table.setText(0, 3, "test");
    Cell cell = table.new Cell(0, 3);

    assertEquals(0, cell.getRowIndex());
    assertEquals(3, cell.getCellIndex());

    TableCellElement elem = cell.getElement().cast();
    assertEquals(3, elem.getCellIndex());
    assertEquals("test", elem.getInnerText());
  }

  public void testClearWidgetsAndHtml() {
    HTMLTable table = getTable(4, 4);
    for (int row = 0; row < 4; row++) {
      table.setHTML(row, 0, row + ":0");
      table.setHTML(row, 1, row + ":1");
      table.setWidget(row, 2, new Button(row + ":2"));
      table.setWidget(row, 3, new Button(row + ":3"));
    }

    table.clear(true);
    for (int row = 0; row < 4; row++) {
      assertEquals("", table.getHTML(row, 0).trim());
      assertEquals("", table.getHTML(row, 1).trim());
      assertEquals("", table.getHTML(row, 2).trim());
      assertNull(table.getWidget(row, 2));
      assertEquals("", table.getHTML(row, 3).trim());
      assertNull(table.getWidget(row, 3));
    }
  }

  public void testClearWidgetsOnly() {
    HTMLTable table = getTable(4, 4);
    for (int row = 0; row < 4; row++) {
      table.setHTML(row, 0, row + ":0");
      table.setHTML(row, 1, row + ":1");
      table.setWidget(row, 2, new Button(row + ":2"));
      table.setWidget(row, 3, new Button(row + ":3"));
    }

    table.clear();
    for (int row = 0; row < 4; row++) {
      assertEquals(row + ":0", table.getHTML(row, 0));
      assertEquals(row + ":1", table.getHTML(row, 1));
      assertEquals("", table.getHTML(row, 2).trim());
      assertNull(table.getWidget(row, 2));
      assertEquals("", table.getHTML(row, 3).trim());
      assertNull(table.getWidget(row, 3));
    }
  }

  public void testColumnFormatter() {
    HTMLTable table = getTable(4, 4);
    ColumnFormatter formatter = table.getColumnFormatter();
    Element colGroup = formatter.columnGroup;

    // getElement.
    Element col0 = formatter.getElement(0);
    assertEquals(colGroup.getChild(0), col0);
    Element col3 = formatter.getElement(3);
    assertEquals(colGroup.getChild(3), col3);
    try {
      formatter.getElement(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testDebugId() {
    HTMLTable table = getTable(4, 3);
    for (int row = 0; row < 4; row++) {
      for (int cell = 0; cell < 3; cell++) {
        table.setHTML(row, cell, row + ":" + cell);
      }
    }

    // Check the Debug IDs
    table.ensureDebugId("myTable");
    UIObjectTest.assertDebugId("myTable", table.getElement());
    CellFormatter formatter = table.getCellFormatter();
    for (int row = 0; row < 4; row++) {
      for (int cell = 0; cell < 3; cell++) {
        Element cellElem = formatter.getElement(row, cell);
        UIObjectTest.assertDebugId("myTable-" + row + "-" + cell, cellElem);
      }
    }
  }

  public void testDoubleSet() {
    HTMLTable t = getTable(4, 4);
    t.setWidget(0, 0, new Label());
    Widget s = new Label();
    t.setWidget(0, 0, s);
    assertEquals(s, t.getWidget(0, 0));
  }

  public void testIterator() {
    // Check remove.
    HTMLTable t = getTable(5, 5);
    Label l = new Label("hello");
    t.setWidget(0, 0, l);
    Iterator<Widget> iter = t.iterator();
    assertEquals(l, iter.next());
    iter.remove();
    Iterator<Widget> iter2 = t.iterator();
    assertFalse(iter2.hasNext());

    // Check put after remove.
    Widget w = new Label("bo");
    t.setWidget(0, 0, w);
    Iterator<Widget> iter3 = t.iterator();
    assertEquals(w, iter3.next());
    assertFalse(iter3.hasNext());

    // Check swapping widgets.
    Widget w2 = new Label("ba");
    t.setWidget(0, 0, w2);
    Iterator<Widget> iter4 = t.iterator();
    assertEquals(w2, iter4.next());
    assertFalse(iter4.hasNext());

    // Check put after put.
    Widget w3 = new Label("be");
    t.setWidget(1, 1, w3);
    Iterator<Widget> iter5 = t.iterator();
    assertEquals(w2, iter5.next());
    assertEquals(w3, iter5.next());
    assertFalse(iter5.hasNext());
  }

  public void testSetColumnFormatter() {
    HTMLTable t = getTable(1, 1);
    Element columnGroup = t.getColumnFormatter().columnGroup;
    assertNotNull(columnGroup);

    ColumnFormatter formatter = t.new ColumnFormatter();
    assertNull(formatter.columnGroup);
    t.setColumnFormatter(formatter);
    assertEquals(columnGroup, formatter.columnGroup);
  }

  /**
   * Tests {@link HTMLTable#setWidget(int, int, Widget)}.
   */
  public void testSetWidget() {
    HTMLTable t = getTable(2, 2);
    Widget widget = new Label("foo");

    t.setWidget(1, 1, widget);

    assertLogicalPaternity(t, widget);
    assertPhysicalPaternityInPosition(t, widget, 1, 1);
  }

  /**
   * Tests {@link HTMLTable#setWidget(int, int, IsWidget)}.
   */
  public void testSetWidgetAsIsWidget() {
    HTMLTable t = getTable(2, 2);
    Widget widget = new Label("foo");

    // IsWidget cast to call the overloaded version
    t.setWidget(1, 1, (IsWidget) widget);

    assertLogicalPaternity(t, widget);
    assertPhysicalPaternityInPosition(t, widget, 1, 1);
  }

  /**
   * Ensures that {@link HTMLTable#setWidget(int, int, IsWidget)} does
   * <b>NOT</b> throws a {@link NullPointerException} when the Widget argument
   * is <code>null</code>, for compatibility with setWidget(Widget) foolishness.
   */
  public void testSetNullWidgetAsIsWidget() {
    HTMLTable t = getTable(2, 2);
    // IsWidget reference to call the overloaded version
    IsWidget widget = null;

    t.setWidget(1, 1, widget);
    // ta da...
  }

  public void testSettingCellAttributes() {
    // These tests simple test for errors while setting these fields. The
    // Patient sample under the survey project has the visual part of the test.
    HTMLTable t = getTable(4, 4);

    CellFormatter formatter = t.getCellFormatter();
    formatter.setHeight(0, 0, "100%");
    formatter.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_BOTTOM);
    formatter.setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_RIGHT);
    formatter.setWidth(0, 2, "100%");
  }

  public void testSetWidgetNull() {
    HTMLTable t = getTable(1, 2);

    // Set some text and a widget.
    Label content = new Label("widget");
    t.setText(0, 0, "hello world");
    t.setWidget(0, 1, content);
    assertEquals("hello world", t.getText(0, 0));
    assertEquals(content, t.getWidget(0, 1));

    // Set the text cell to a null widget.
    t.setWidget(0, 0, null);
    assertEquals("text should be cleared when the widget is set to null", "",
        t.getText(0, 0));
    assertEquals("widget should be cleared when set to null", content,
        t.getWidget(0, 1));

    // Set the widget cell to a null widget.
    t.setWidget(0, 1, null);
    assertEquals("", t.getText(0, 0));
    assertNull(t.getWidget(0, 1));
  }

  public void testSafeHtml() {
    HTMLTable table = getTable(1, 1);
    table.setHTML(0, 0, SafeHtmlUtils.fromSafeConstant(html));
    assertEquals(html, table.getHTML(0, 0).toLowerCase());
  }

  public void testStyles() {
    HTMLTable t = getTable(4, 4);
    t.getCellFormatter().setStyleName(2, 2, "hello");
    assertEquals("hello", t.getCellFormatter().getStyleName(2, 2));
    t.getCellFormatter().setStyleName(2, 2, "goodbye");
    t.getCellFormatter().addStyleName(2, 2, "hello");

    // Visable Styles.
    t.getCellFormatter().setVisible(0, 0, false);
    assertTrue(t.getCellFormatter().isVisible(2, 2));
    assertFalse(t.getCellFormatter().isVisible(0, 0));
    RowFormatter formatter = t.getRowFormatter();
    formatter.setVisible(3, false);
    assertFalse(formatter.isVisible(3));
    assertTrue(formatter.isVisible(2));
    assertTrue(t.getCellFormatter().isVisible(2, 0));

    // Style name.
    assertEquals("goodbye hello", t.getCellFormatter().getStyleName(2, 2));
    t.getRowFormatter().setStyleName(3, "newStyle");
    assertEquals("newStyle", t.getRowFormatter().getStyleName(3));
  }

  private void assertPhysicalPaternityInPosition(HTMLTable parent,
      Widget child, int row, int column) {
    assertSame("The child should be in te given position", child,
        parent.getWidget(row, column));
  }

  private void assertLogicalPaternity(HTMLTable parent, Widget child) {
    Iterator<Widget> iterator = parent.iterator();
    assertTrue(iterator.hasNext());
    assertSame(child, iterator.next());
  }
}
