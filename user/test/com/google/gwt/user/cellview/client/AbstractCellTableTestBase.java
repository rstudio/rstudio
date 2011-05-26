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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Bases tests for subclasses of {@link AbstractCellTable}.
 * 
 * @param <T> the subclass type
 */
public abstract class AbstractCellTableTestBase<T extends AbstractCellTable<String>> extends
    AbstractHasDataTestBase {

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
    final List<LoadingState> loadingStates = new ArrayList<LoadingState>();
    T table = createAbstractHasData();
    table.setPageSize(10);
    table.addLoadingStateChangeHandler(new LoadingStateChangeEvent.Handler() {
      @Override
      public void onLoadingStateChanged(LoadingStateChangeEvent event) {
        if (LoadingState.LOADED == event.getLoadingState()) {
          loadingStates.add(event.getLoadingState());
        }
      }
    });
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
    assertEquals(1, loadingStates.size());
  }

  public void testCellAlignment() {
    T table = createAbstractHasData(new TextCell());
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
    AbstractCellTable<String> table = createAbstractHasData(cell);
    RootPanel.get().add(table);
    table.setRowData(createData(0, 10));
    table.getPresenter().flush();

    // Trigger an event at index 5.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    table.getRowElement(5).getCells().getItem(0).dispatchEvent(event);
    cell.assertLastBrowserEventIndex(5);
    cell.assertLastEditingIndex(5);

    RootPanel.get().remove(table);
  }

  public void testGetColumnIndex() {
    T table = createAbstractHasData();
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
    T table = createAbstractHasData();

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
    AbstractCellTable<String> table = createAbstractHasData(new TextCell());
    table.setRowData(0, createData(0, 10));

    // Ensure that calling getRowElement() flushes all pending changes.
    assertNotNull(table.getRowElement(9));
  }

  public void testInsertColumn() {
    T table = createAbstractHasData();
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

  public void testSetEmptyTableWidget() {
    AbstractCellTable<String> table = createAbstractHasData(new TextCell());

    // Set a widget.
    Label l = new Label("Empty");
    table.setEmptyTableWidget(l);
    assertEquals(l, table.getEmptyTableWidget());

    // Null widget.
    table.setEmptyTableWidget(null);
    assertNull(table.getEmptyTableWidget());
  }

  public void testSetLoadingIndicator() {
    AbstractCellTable<String> table = createAbstractHasData(new TextCell());

    // Set a widget.
    Label l = new Label("Loading");
    table.setLoadingIndicator(l);
    assertEquals(l, table.getLoadingIndicator());

    // Null widget.
    table.setLoadingIndicator(null);
    assertNull(table.getLoadingIndicator());
  }

  public void testSortableColumn() {
    T table = createAbstractHasData(new TextCell());
    table.getColumn(0).setSortable(true);
    table.getPresenter().flush();
    RootPanel.get().add(table);

    // Add a column sort handler.
    final List<Column<?, ?>> lastSorted = new ArrayList<Column<?, ?>>();
    table.addColumnSortHandler(new ColumnSortEvent.Handler() {
      @Override
      public void onColumnSort(ColumnSortEvent event) {
        lastSorted.clear();
        lastSorted.add(event.getColumn());
      }
    });

    // Default sort order is empty.
    ColumnSortList sortList = table.getColumnSortList();
    assertEquals(0, sortList.size());

    // Sort a column that is sortable.
    NativeEvent click = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
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

  /**
   * Create an empty cell table.
   */
  protected abstract T createAbstractHasData();

  @Override
  protected final T createAbstractHasData(Cell<String> cell) {
    T table = createAbstractHasData();
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
  protected abstract TableCellElement getBodyElement(T table, int row, int column);

  /**
   * Get the number of column headers in the table.
   * 
   * @param table the {@link CellTable}
   * @return the number of column headers
   */
  protected abstract int getHeaderCount(T table);

  /**
   * Get a column header from the table.
   * 
   * @param table the {@link CellTable}
   * @param column the column index
   * @return the column header
   */
  protected abstract TableCellElement getHeaderElement(T table, int column);
}
