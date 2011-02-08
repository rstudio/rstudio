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
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.CellTable.Style;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CellTable}.
 */
public class CellTableTest extends AbstractHasDataTestBase {

  /**
   * A concrete column that implements a getter that always returns null.
   * 
   * @param <T> the row type
   * @param <C> the column type
   */
  private static class MockColumn<T, C> extends Column<T, C> {

    public MockColumn() {
      super(new AbstractCell<C>() {
        @Override
        public void render(Context context, C value, SafeHtmlBuilder sb) {
        }
      });
    }

    @Override
    public C getValue(T object) {
      return null;
    }
  }

  /**
   * Test that calls to addColumn results in only one redraw.
   */
  public void testAddColumnSingleRedraw() {
    final List<SafeHtml> replaceValues = new ArrayList<SafeHtml>();
    CellTable<String> table = new CellTable<String>() {
      @Override
      protected void replaceAllChildren(List<String> values, SafeHtml html) {
        replaceValues.add(html);
      }
    };
    table.addColumn(new Column<String, String>(new TextCell()) {
      @Override
      public String getValue(String object) {
        return object + "-3";
      }
    });
    table.addColumn(new Column<String, String>(new TextCell()) {
      @Override
      public String getValue(String object) {
        return object + "-4";
      }
    });
    table.setRowData(0, createData(0, 10));
    table.getPresenter().flush();
    assertEquals(1, replaceValues.size());
  }

  public void testCellAlignment() {
    CellTable<String> table = createAbstractHasData(new TextCell());
    Column<String, String> column = new Column<String, String>(new TextCell()) {
      @Override
      public String getValue(String object) {
        return object;
      }
    };
    table.addColumn(column);

    /*
     * No alignment. Some browsers (FF) return a default value when alignment is
     * not specified, others (IE/HtmlUnit) return an empty string.
     */
    table.setRowData(0, createData(0, 1));
    table.getPresenter().flush();
    TableCellElement td = getBodyElement(table, 0, 2);
    String hAlign = td.getAlign();
    String vAlign = td.getVAlign();
    assertTrue("".equals(hAlign) || "left".equals(hAlign));
    assertTrue("".equals(vAlign) || "middle".equals(vAlign));

    // Horizontal alignment.
    column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    table.setRowData(0, createData(0, 1));
    table.getPresenter().flush();
    td = getBodyElement(table, 0, 2);
    hAlign = td.getAlign();
    vAlign = td.getVAlign();
    assertTrue("right".equals(hAlign));
    assertTrue("".equals(vAlign) || "middle".equals(vAlign));

    // Vertical alignment.
    column.setHorizontalAlignment(null);
    column.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
    table.setRowData(0, createData(0, 1));
    table.getPresenter().flush();
    td = getBodyElement(table, 0, 2);
    hAlign = td.getAlign();
    vAlign = td.getVAlign();
    assertTrue("".equals(hAlign) || "left".equals(hAlign));
    assertTrue("bottom".equals(vAlign));

    // Horizontal and vertical alignment.
    column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    table.setRowData(0, createData(0, 1));
    table.getPresenter().flush();
    td = getBodyElement(table, 0, 2);
    hAlign = td.getAlign();
    vAlign = td.getVAlign();
    assertTrue("right".equals(hAlign));
    assertTrue("bottom".equals(vAlign));
  }

  public void testCellEvent() {
    IndexCell<String> cell = new IndexCell<String>("click");
    CellTable<String> table = createAbstractHasData(cell);
    RootPanel.get().add(table);
    table.setRowData(createData(0, 10));
    table.getPresenter().flush();

    // Trigger an event at index 5.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    table.getRowElement(5).getCells().getItem(0).dispatchEvent(event);
    cell.assertLastBrowserEventIndex(5);
    cell.assertLastEditingIndex(5);

    RootPanel.get().remove(table);
  }

  public void testGetColumnIndex() {
    CellTable<String> table = new CellTable<String>();
    Column<String, String> col0 = new IdentityColumn<String>(new TextCell());
    table.addColumn(col0);
    Column<String, String> col1 = new IdentityColumn<String>(new TextCell());
    table.addColumn(col1);
    Column<String, String> col2 = new IdentityColumn<String>(new TextCell());
    table.addColumn(col2);
    assertEquals(0, table.getColumnIndex(col0));
    assertEquals(1, table.getColumnIndex(col1));
    assertEquals(2, table.getColumnIndex(col2));

    // Test a column that is not in the table.
    Column<String, String> other = new IdentityColumn<String>(new TextCell());
    assertEquals(-1, table.getColumnIndex(other));

    // Test null.
    assertEquals(-1, table.getColumnIndex(null));
  }

  public void testGetColumnOutOfBounds() {
    CellTable<String> table = new CellTable<String>();

    // Get column when there are no columns.
    try {
      table.getColumn(0);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }

    // Add some columns.
    table.addColumn(new MockColumn<String, String>());
    table.addColumn(new MockColumn<String, String>());

    // Negative index.
    try {
      table.getColumn(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }

    // Index too high.
    try {
      table.getColumn(2);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testGetRowElement() {
    CellTable<String> table = createAbstractHasData(new TextCell());
    table.setRowData(0, createData(0, 10));

    // Ensure that calling getRowElement() flushes all pending changes.
    assertNotNull(table.getRowElement(9));
  }

  public void testInsertColumn() {
    CellTable<String> table = new CellTable<String>();
    assertEquals(0, table.getColumnCount());

    // Insert first column.
    Column<String, ?> a = new MockColumn<String, String>();
    table.insertColumn(0, a);
    assertEquals(1, table.getColumnCount());
    assertEquals(a, table.getColumn(0));

    // Insert column at beginning.
    Column<String, ?> b = new MockColumn<String, String>();
    table.insertColumn(0, b);
    assertEquals(2, table.getColumnCount());
    assertEquals(b, table.getColumn(0));
    assertEquals(a, table.getColumn(1));

    // Insert column at end.
    Column<String, ?> c = new MockColumn<String, String>();
    table.insertColumn(2, c);
    assertEquals(3, table.getColumnCount());
    assertEquals(b, table.getColumn(0));
    assertEquals(a, table.getColumn(1));
    assertEquals(c, table.getColumn(2));

    // Insert column in middle.
    Column<String, ?> d = new MockColumn<String, String>();
    table.insertColumn(1, d);
    assertEquals(4, table.getColumnCount());
    assertEquals(b, table.getColumn(0));
    assertEquals(d, table.getColumn(1));
    assertEquals(a, table.getColumn(2));
    assertEquals(c, table.getColumn(3));

    // Insert column at invalid index.
    try {
      table.insertColumn(-1, d);
      fail("Expected IndexOutOfBoundsExecltion");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      table.insertColumn(6, d);
      fail("Expected IndexOutOfBoundsExecltion");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  /**
   * Test headers that span multiple columns.
   */
  public void testMultiColumnHeader() {
    Resources res = GWT.create(Resources.class);
    CellTable<String> table = new CellTable<String>(10, res);
    TextHeader header = new TextHeader("Hello");

    // Get the style information.
    Style style = res.cellTableStyle();
    String styleHeader = style.cellTableHeader();
    String styleFirstColumn = style.cellTableFirstColumnHeader();
    String styleLastColumn = style.cellTableLastColumnHeader();

    // No header.
    table.redraw();
    table.getPresenter().flush();
    assertEquals(0, getHeaderCount(table));

    // Single column.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.getPresenter().flush();
    assertEquals(1, getHeaderCount(table));
    assertEquals(1, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleLastColumn));

    // Header spans both columns.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.getPresenter().flush();
    assertEquals(1, getHeaderCount(table));
    assertEquals(2, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleLastColumn));

    // Header spans all three columns.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.getPresenter().flush();
    assertEquals(1, getHeaderCount(table));
    assertEquals(3, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleLastColumn));

    // New header at fourth column.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, "New Header");
    table.getPresenter().flush();
    assertEquals(2, getHeaderCount(table));
    assertEquals(3, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertEquals(1, getHeaderElement(table, 1).getColSpan());
    assertTrue(getHeaderElement(table, 1).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 1).getClassName().contains(
        styleLastColumn));

    // Two separate spans of same header: HHHXHH.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.getPresenter().flush();
    assertEquals(3, getHeaderCount(table));
    assertEquals(3, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertEquals(1, getHeaderElement(table, 1).getColSpan());
    assertTrue(getHeaderElement(table, 1).getClassName().contains(styleHeader));
    assertEquals(2, getHeaderElement(table, 2).getColSpan());
    assertTrue(getHeaderElement(table, 2).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 2).getClassName().contains(
        styleLastColumn));
  }

  /**
   * Test that removing a column sets its width to zero.
   */
  public void testRemoveColumnWithWidth() {
    CellTable<String> table = createAbstractHasData(new TextCell());
    Column<String, ?> column1 = table.getColumn(1);
    table.setColumnWidth(column1, "100px");
    Element col0 = table.colgroup.getFirstChildElement();
    Element col1 = col0.getNextSiblingElement();
    assertEquals("100px", col1.getStyle().getWidth().toLowerCase());

    // Remove column 1.
    table.removeColumn(column1);
    assertEquals("0px", col1.getStyle().getWidth());
  }

  public void testSetColumnWidth() {
    CellTable<String> table = createAbstractHasData(new TextCell());
    Column<String, ?> column0 = table.getColumn(0);
    Column<String, ?> column1 = table.getColumn(1);

    // Set the width.
    table.setColumnWidth(column1, "100px");
    Element col0 = table.colgroup.getFirstChildElement();
    Element col1 = col0.getNextSiblingElement();
    assertEquals("", col0.getStyle().getWidth());
    assertEquals("100px", col1.getStyle().getWidth().toLowerCase());

    // Clear the width.
    table.clearColumnWidth(column1);
    assertEquals("", col0.getStyle().getWidth());
    assertEquals("", col1.getStyle().getWidth());

    // Set the width again.
    table.setColumnWidth(column0, 30.1, Unit.PCT);
    assertEquals("30.1%", col0.getStyle().getWidth().toLowerCase());
    assertEquals("", col1.getStyle().getWidth());
  }

  public void testSetEmptyListWidget() {
    CellTable<String> table = createAbstractHasData(new TextCell());

    // Set a widget.
    Label l = new Label("Empty");
    table.setEmptyTableWidget(l);
    assertEquals(l, table.getEmptyTableWidget());

    // Null widget.
    table.setEmptyTableWidget(null);
    assertNull(table.getEmptyTableWidget());
  }

  public void testSetLoadingIndicator() {
    CellTable<String> table = createAbstractHasData(new TextCell());

    // Set a widget.
    Label l = new Label("Loading");
    table.setLoadingIndicator(l);
    assertEquals(l, table.getLoadingIndicator());

    // Null widget.
    table.setLoadingIndicator(null);
    assertNull(table.getLoadingIndicator());
  }

  public void testSetTableLayoutFixed() {
    CellTable<String> table = createAbstractHasData(new TextCell());
    assertNotSame("fixed",
        table.getElement().getStyle().getTableLayout());

    table.setTableLayoutFixed(true);
    assertEquals("fixed",
        table.getElement().getStyle().getTableLayout());

    table.setTableLayoutFixed(false);
    assertNotSame("fixed",
        table.getElement().getStyle().getTableLayout());
  }

  public void testSortableColumn() {
    CellTable<String> table = createAbstractHasData(new TextCell());
    table.getColumn(0).setSortable(true);
    table.getPresenter().flush();
    RootPanel.get().add(table);

    // Add a column sort handler.
    final List<Column<?, ?>> lastSorted = new ArrayList<Column<?, ?>>();
    table.addColumnSortHandler(new ColumnSortEvent.Handler() {
      public void onColumnSort(ColumnSortEvent event) {
        lastSorted.clear();
        lastSorted.add(event.getColumn());
      }
    });

    // Default sort order is empty.
    ColumnSortList sortList = table.getColumnSortList();
    assertEquals(0, sortList.size());

    // Sort a column that is sortable.
    NativeEvent click = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    getHeaderElement(table, 0).dispatchEvent(click);
    assertEquals(1, sortList.size());
    assertEquals(table.getColumn(0), sortList.get(0).getColumn());
    assertTrue(sortList.get(0).isAscending());
    assertEquals(1, lastSorted.size());
    lastSorted.clear();

    // Sort the same column again.
    getHeaderElement(table, 0).dispatchEvent(click);
    assertEquals(1, sortList.size());
    assertEquals(table.getColumn(0), sortList.get(0).getColumn());
    assertFalse(sortList.get(0).isAscending());
    assertEquals(1, lastSorted.size());
    lastSorted.clear();

    // Sort a column that is not sortable.
    getHeaderElement(table, 1).dispatchEvent(click);
    assertEquals(1, sortList.size());
    assertEquals(table.getColumn(0), sortList.get(0).getColumn());
    assertFalse(sortList.get(0).isAscending());
    assertEquals(0, lastSorted.size());

    // Cleanup.
    RootPanel.get().remove(table);
  }

  @Override
  protected CellTable<String> createAbstractHasData(Cell<String> cell) {
    CellTable<String> table = new CellTable<String>();
    table.addColumn(new Column<String, String>(cell) {
      @Override
      public String getValue(String object) {
        return object;
      }
    }, "Column 0");
    table.addColumn(new Column<String, String>(new TextCell()) {
      @Override
      public String getValue(String object) {
        return object + "-2";
      }
    }, "Column 1");
    return table;
  }

  /**
   * Get a td element from the table body.
   * 
   * @param table the {@link CellTable}
   * @param row the row index
   * @param column the column index
   * @return the column header
   */
  private TableCellElement getBodyElement(CellTable<?> table, int row,
      int column) {
    TableElement tableElem = table.getElement().cast();
    TableSectionElement tbody = tableElem.getTBodies().getItem(0);
    TableRowElement tr = tbody.getRows().getItem(row);
    return tr.getCells().getItem(column);
  }

  /**
   * Get the number of column headers in the table.
   * 
   * @param table the {@link CellTable}
   * @return the number of column headers
   */
  private int getHeaderCount(CellTable<?> table) {
    TableElement tableElem = table.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getLength();
  }

  /**
   * Get a column header from the table.
   * 
   * @param table the {@link CellTable}
   * @param column the column index
   * @return the column header
   */
  private TableCellElement getHeaderElement(CellTable<?> table, int column) {
    TableElement tableElem = table.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getItem(column);
  }
}
