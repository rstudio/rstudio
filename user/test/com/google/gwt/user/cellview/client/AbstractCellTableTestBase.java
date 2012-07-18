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
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractHasData.DefaultKeyboardSelectionHandler;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.Range;

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

  /**
   * Test that an error is thrown if the builder ends the table body element
   * instead of the table row element.
   */
  public void testBuildTooManyEnds() {
    final List<Integer> builtRows = new ArrayList<Integer>();
    T table = createAbstractHasData(new TextCell());
    CellTableBuilder<String> builder = new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        builtRows.add(absRowIndex);
        TableRowBuilder tr = startRow();
        tr.endTR(); // End the tr.
        tr.end(); // Accidentally end the table section.

        // Try to start another row.
        try {
          startRow();
          fail("Expected IllegalStateException: tbody is already ended");
        } catch (IllegalStateException e) {
          // Expected.
        }
      }
    };
    table.setTableBuilder(builder);
    table.setVisibleRange(0, 1);
    populateData(table);
    table.getPresenter().flush();

    assertEquals(1, builtRows.size());
    assertEquals(0, builtRows.get(0).intValue());
  }

  /**
   * Test that the table works if a row value is rendered into multiple rows.
   */
  public void testBuildMultipleRows() {
    T table = createAbstractHasData(new TextCell());
    CellTableBuilder<String> builder = new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        super.buildRowImpl(rowValue, absRowIndex);

        // Add child rows to row five.
        if (absRowIndex == 5) {
          for (int i = 0; i < 4; i++) {
            TableRowBuilder tr = startRow();
            tr.startTD().colSpan(2).text("child " + i).endTD();
            tr.endTR();
          }
        }
      }
    };
    table.setTableBuilder(builder);
    table.setVisibleRange(0, 10);
    populateData(table);
    table.getPresenter().flush();

    // Verify the structure.
    TableSectionElement tbody = table.getTableBodyElement();
    assertEquals(14, tbody.getChildCount());
    assertEquals("child 0", getBodyElement(table, 6, 0).getInnerText());
    assertEquals("child 1", getBodyElement(table, 7, 0).getInnerText());
    assertEquals("child 2", getBodyElement(table, 8, 0).getInnerText());
    assertEquals("child 3", getBodyElement(table, 9, 0).getInnerText());

    // Verify the row values are accessible.
    assertEquals("test 5", table.getVisibleItem(5));
    assertEquals("test 9", table.getVisibleItem(9));

    // Verify the child elements map correctly.
    assertEquals(4, table.getChildElement(4).getSectionRowIndex());
    assertEquals(5, table.getChildElement(5).getSectionRowIndex());
    assertEquals(10, table.getChildElement(6).getSectionRowIndex());
  }

  /**
   * Test that the table works if a row value is rendered into zero rows.
   */
  public void testBuildZeroRows() {
    T table = createAbstractHasData(new TextCell());
    CellTableBuilder<String> builder = new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        // Skip row index 5.
        if (absRowIndex != 5) {
          super.buildRowImpl(rowValue, absRowIndex);
        }
      }
    };
    table.setTableBuilder(builder);
    table.setVisibleRange(0, 10);
    populateData(table);
    table.getPresenter().flush();

    // Verify the structure.
    TableSectionElement tbody = table.getTableBodyElement();
    assertEquals(9, tbody.getChildCount());

    // Verify the row values are accessible.
    assertEquals("test 5", table.getVisibleItem(5));
    assertEquals("test 9", table.getVisibleItem(9));

    // Verify the child elements map correctly.
    assertEquals(4, table.getChildElement(4).getSectionRowIndex());
    assertNull(table.getChildElement(5));
    assertEquals(5, table.getChildElement(6).getSectionRowIndex());
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
    T table = createAbstractHasData(cell);
    RootPanel.get().add(table);
    table.setRowData(createData(0, 10));
    table.getPresenter().flush();

    // Trigger an event at index 5.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    getBodyElement(table, 5, 0).getFirstChildElement().dispatchEvent(event);
    cell.assertLastBrowserEventIndex(5);
    cell.assertLastEditingIndex(5);

    RootPanel.get().remove(table);
  }

  public void testCellStyles() {
    T table = createAbstractHasData();

    // A column that return a static cell style.
    TextColumn<String> col0 = new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return object;
      }
    };
    col0.setCellStyleNames("col0");
    table.addColumn(col0);

    // A column that returns dynamic cell styles.
    TextColumn<String> col1 = new TextColumn<String>() {
      @Override
      public String getCellStyleNames(Context context, String object) {
        return object.replace(" ", "_");
      }

      @Override
      public String getValue(String object) {
        return object;
      }
    };
    table.addColumn(col1);

    // Populate the table.
    table.setRowData(createData(0, 10));
    table.flush();

    assertTrue(getBodyElement(table, 1, 0).getClassName().contains(" col0"));
    assertFalse(getBodyElement(table, 1, 0).getClassName().contains(" test_1"));
    assertFalse(getBodyElement(table, 1, 1).getClassName().contains(" col0"));
    assertTrue(getBodyElement(table, 1, 1).getClassName().contains(" test_1"));
  }

  public void testClearColumnWidth() {
    T table = createAbstractHasData();
    assertEquals(0, table.getRealColumnCount());

    table.setColumnWidth(0, "100px");
    assertEquals(1, table.getRealColumnCount());

    table.setColumnWidth(2, "300px");
    assertEquals(3, table.getRealColumnCount());

    table.clearColumnWidth(2);
    assertEquals(1, table.getRealColumnCount());
  }

  public void testDefaultKeyboardSelectionHandlerChangePage() {
    T table = createAbstractHasData();
    DefaultKeyboardSelectionHandler<String> keyHandler =
        new DefaultKeyboardSelectionHandler<String>(table);
    table.setKeyboardSelectionHandler(keyHandler);
    HasDataPresenter<String> presenter = table.getPresenter();

    table.setRowCount(100, true);
    table.setVisibleRange(new Range(50, 10));
    populateData(table);
    presenter.flush();
    table.setKeyboardPagingPolicy(KeyboardPagingPolicy.CHANGE_PAGE);

    // keyboardPrev in middle.
    table.setKeyboardSelectedRow(1);
    presenter.flush();
    assertEquals(1, table.getKeyboardSelectedRow());
    keyHandler.prevRow();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());

    // keyboardPrev at beginning goes to previous page.
    keyHandler.prevRow();
    populateData(table);
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());
    assertEquals(new Range(40, 10), table.getVisibleRange());

    // keyboardNext in middle.
    table.setKeyboardSelectedRow(8);
    presenter.flush();
    assertEquals(8, table.getKeyboardSelectedRow());
    keyHandler.nextRow();
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());

    // keyboardNext at end.
    keyHandler.nextRow();
    populateData(table);
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardPrevPage.
    table.setKeyboardSelectedRow(5);
    presenter.flush();
    assertEquals(5, table.getKeyboardSelectedRow());
    keyHandler.prevPage();
    populateData(table);
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(40, 10), table.getVisibleRange());

    // keyboardNextPage.
    table.setKeyboardSelectedRow(5);
    presenter.flush();
    assertEquals(5, table.getKeyboardSelectedRow());
    keyHandler.nextPage();
    populateData(table);
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardHome.
    keyHandler.home();
    populateData(table);
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(0, 10), table.getVisibleRange());

    // keyboardPrev at first row.
    keyHandler.prevRow();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());

    // keyboardEnd.
    keyHandler.end();
    populateData(table);
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());
    assertEquals(new Range(90, 10), table.getVisibleRange());

    // keyboardNext at last row.
    keyHandler.nextRow();
    presenter.flush();
  }

  public void testDefaultKeyboardSelectionHandlerCurrentPage() {
    T table = createAbstractHasData();
    DefaultKeyboardSelectionHandler<String> keyHandler =
        new DefaultKeyboardSelectionHandler<String>(table);
    table.setKeyboardSelectionHandler(keyHandler);
    HasDataPresenter<String> presenter = table.getPresenter();

    table.setRowCount(100, true);
    table.setVisibleRange(new Range(50, 10));
    populateData(table);
    presenter.flush();
    table.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);

    // keyboardPrev in middle.
    table.setKeyboardSelectedRow(1);
    presenter.flush();
    assertEquals(1, table.getKeyboardSelectedRow());
    keyHandler.prevRow();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());

    // keyboardPrev at beginning.
    keyHandler.prevRow();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardNext in middle.
    table.setKeyboardSelectedRow(8);
    presenter.flush();
    assertEquals(8, table.getKeyboardSelectedRow());
    keyHandler.nextRow();
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());

    // keyboardNext at end.
    keyHandler.nextRow();
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardPrevPage.
    keyHandler.prevPage(); // ignored.
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardNextPage.
    keyHandler.nextPage(); // ignored.
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardHome.
    keyHandler.home();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());

    // keyboardEnd.
    keyHandler.end();
    presenter.flush();
    assertEquals(9, table.getKeyboardSelectedRow());
    assertEquals(new Range(50, 10), table.getVisibleRange());
  }

  public void testDefaultKeyboardSelectionHandlerIncreaseRange() {
    int pageStart = 150;
    int pageSize = 10;
    int increment = HasDataPresenter.PAGE_INCREMENT;

    T table = createAbstractHasData();
    DefaultKeyboardSelectionHandler<String> keyHandler =
        new DefaultKeyboardSelectionHandler<String>(table);
    table.setKeyboardSelectionHandler(keyHandler);
    HasDataPresenter<String> presenter = table.getPresenter();

    table.setRowCount(300, true);
    table.setVisibleRange(new Range(pageStart, pageSize));
    populateData(table);
    presenter.flush();
    table.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);

    // keyboardPrev in middle.
    table.setKeyboardSelectedRow(1);
    presenter.flush();
    assertEquals(1, table.getKeyboardSelectedRow());
    keyHandler.prevRow();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());

    // keyboardPrev at beginning.
    keyHandler.prevRow();
    populateData(table);
    presenter.flush();
    pageStart -= increment;
    pageSize += increment;
    assertEquals(increment - 1, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardNext in middle.
    table.setKeyboardSelectedRow(pageSize - 2);
    presenter.flush();
    assertEquals(pageSize - 2, table.getKeyboardSelectedRow());
    keyHandler.nextRow();
    presenter.flush();
    assertEquals(pageSize - 1, table.getKeyboardSelectedRow());

    // keyboardNext at end.
    keyHandler.nextRow();
    populateData(table);
    presenter.flush();
    pageSize += increment;
    assertEquals(pageSize - increment, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardPrevPage within range.
    table.setKeyboardSelectedRow(increment);
    presenter.flush();
    assertEquals(increment, table.getKeyboardSelectedRow());
    keyHandler.prevPage();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardPrevPage outside range.
    keyHandler.prevPage();
    populateData(table);
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    pageStart -= increment;
    pageSize += increment;
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardNextPage inside range.
    keyHandler.nextPage();
    presenter.flush();
    assertEquals(increment, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardNextPage outside range.
    table.setKeyboardSelectedRow(pageSize - 1);
    presenter.flush();
    assertEquals(pageSize - 1, table.getKeyboardSelectedRow());
    keyHandler.nextPage();
    populateData(table);
    presenter.flush();
    pageSize += increment;
    assertEquals(pageSize - 1, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardHome.
    keyHandler.home();
    populateData(table);
    presenter.flush();
    pageSize += pageStart;
    pageStart = 0;
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardPrev at first row.
    keyHandler.prevRow();
    presenter.flush();
    assertEquals(0, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());

    // keyboardEnd.
    keyHandler.end();
    pageSize = 300;
    populateData(table);
    presenter.flush();
    assertEquals(pageSize - 1, table.getKeyboardSelectedRow());
    assertEquals(new Range(0, pageSize), table.getVisibleRange());

    // keyboardNext at last row.
    keyHandler.nextRow();
    presenter.flush();
    assertEquals(pageSize - 1, table.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), table.getVisibleRange());
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

  public void testGetSubRowElement() {
    T table = createAbstractHasData(new TextCell());
    CellTableBuilder<String> builder = new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        super.buildRowImpl(rowValue, absRowIndex);

        // Add some children.
        for (int i = 0; i < 4; i++) {
          TableRowBuilder tr = startRow();
          tr.startTD().colSpan(2).text("child " + absRowIndex + ":" + i).endTD();
          tr.endTR();
        }
      }
    };
    table.setTableBuilder(builder);
    table.setVisibleRange(0, 5);
    populateData(table);
    table.getPresenter().flush();

    // Verify the structure.
    TableSectionElement tbody = table.getTableBodyElement();
    assertEquals(25, tbody.getChildCount());

    // Test sub rows within range.
    assertEquals(0, table.getSubRowElement(0, 0).getSectionRowIndex());
    assertEquals(1, table.getSubRowElement(0, 1).getSectionRowIndex());
    assertEquals(4, table.getSubRowElement(0, 4).getSectionRowIndex());
    assertEquals(5, table.getSubRowElement(1, 0).getSectionRowIndex());
    assertEquals(8, table.getSubRowElement(1, 3).getSectionRowIndex());
    assertEquals(20, table.getSubRowElement(4, 0).getSectionRowIndex());
    assertEquals(24, table.getSubRowElement(4, 4).getSectionRowIndex());

    // Sub row does not exist within the row.
    assertNull(table.getSubRowElement(0, 5));
    assertNull(table.getSubRowElement(4, 5));

    // Row index out of bounds.
    try {
      assertNull(table.getSubRowElement(5, 0));
      fail("Expected IndexOutOfBoundsException: row index is out of bounds");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testHeaderEvent() {
    T table = createAbstractHasData();
    IndexCell<String> cell = new IndexCell<String>("click");
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return object;
      }
    }, new Header<String>(cell) {
      @Override
      public String getValue() {
        return "header0";
      }
    });
    RootPanel.get().add(table);
    table.setRowData(createData(0, 10));
    table.getPresenter().flush();

    // Trigger an event on the header.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    getHeaderElement(table, 0).dispatchEvent(event);
    cell.assertLastBrowserEventIndex(0);

    RootPanel.get().remove(table);
  }

  public void testSetHeaderBuilder() {
    T table = createAbstractHasData();
    HeaderBuilder<String> headerBuilder = new AbstractHeaderOrFooterBuilder<String>(table, false) {
      @Override
      protected boolean buildHeaderOrFooterImpl() {
        TableRowBuilder tr = startRow();
        tr.startTH().text("Col 0").endTH();
        tr.startTH().text("Col 1").endTH();
        tr.startTH().text("Col 2").endTH();
        tr.endTR();
        return true;
      }
    };

    // Change the header builder.
    table.setHeaderBuilder(headerBuilder);
    assertEquals(headerBuilder, table.getHeaderBuilder());
    table.getPresenter().flush();

    // Verify the new header.
    NodeList<TableRowElement> rows = table.getTableHeadElement().getRows();
    assertEquals(1, rows.getLength());
    NodeList<TableCellElement> cells = rows.getItem(0).getCells();
    assertEquals(3, cells.getLength());
    assertEquals("Col 0", cells.getItem(0).getInnerText());
    assertEquals("Col 1", cells.getItem(1).getInnerText());
    assertEquals("Col 2", cells.getItem(2).getInnerText());
  }

  public void testSetFooterBuilder() {
    T table = createAbstractHasData();
    FooterBuilder<String> footerBuilder = new AbstractHeaderOrFooterBuilder<String>(table, true) {
      @Override
      protected boolean buildHeaderOrFooterImpl() {
        TableRowBuilder tr = startRow();
        tr.startTH().text("Col 0").endTH();
        tr.startTH().text("Col 1").endTH();
        tr.startTH().text("Col 2").endTH();
        tr.endTR();
        return true;
      }
    };

    // Change the header builder.
    table.setFooterBuilder(footerBuilder);
    assertEquals(footerBuilder, table.getFooterBuilder());
    table.getPresenter().flush();

    // Verify the new header.
    NodeList<TableRowElement> rows = table.getTableFootElement().getRows();
    assertEquals(1, rows.getLength());
    NodeList<TableCellElement> cells = rows.getItem(0).getCells();
    assertEquals(3, cells.getLength());
    assertEquals("Col 0", cells.getItem(0).getInnerText());
    assertEquals("Col 1", cells.getItem(1).getInnerText());
    assertEquals("Col 2", cells.getItem(2).getInnerText());
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

  public void testSetAutoFooterRefreshDisabled() {
    AbstractCellTable<String> table = createAbstractHasData();
    assertFalse(table.isAutoHeaderRefreshDisabled());
    assertFalse(table.isAutoFooterRefreshDisabled());

    table.setAutoFooterRefreshDisabled(true);
    assertFalse(table.isAutoHeaderRefreshDisabled());
    assertTrue(table.isAutoFooterRefreshDisabled());

    /*
     * Inserting a column should render the headers and footers, even if auto
     * refresh is disabled.
     */
    final List<String> log = new ArrayList<String>();
    Column<String, ?> col0 = new MockColumn<String, String>();
    TextHeader header0 = new TextHeader("header0") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("header0 rendered");
      }
    };
    TextHeader footer0 = new TextHeader("footer0") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("footer0 rendered");
      }
    };
    table.addColumn(col0, header0, footer0);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header0 rendered", log.remove(0));
    assertEquals("footer0 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Inserting another column should render the headers and footers, even if
     * auto refresh is disabled.
     */
    Column<String, ?> col1 = new MockColumn<String, String>();
    TextHeader header1 = new TextHeader("header1") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("header1 rendered");
      }
    };
    TextHeader footer1 = new TextHeader("footer1") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("footer1 rendered");
      }
    };
    table.addColumn(col1, header1, footer1);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header0 rendered", log.remove(0));
    assertEquals("header1 rendered", log.remove(0));
    assertEquals("footer0 rendered", log.remove(0));
    assertEquals("footer1 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Removing a column should render the headers and footers, even if auto
     * refresh is disabled.
     */
    table.removeColumn(col0);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header1 rendered", log.remove(0));
    assertEquals("footer1 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Setting data only causes footers to render if auto refresh is enabled,
     * which it is not. Header refresh is still enabled.
     */
    populateData(table);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header1 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Sorting a column forces the headers only to refresh. The footers are not
     * refreshed.
     */
    table.getColumnSortList().push(col1);
    assertEquals("header1 rendered", log.remove(0));
    assertEquals(0, log.size());
  }

  public void testSetAutoHeaderRefreshDisabled() {
    AbstractCellTable<String> table = createAbstractHasData();
    assertFalse(table.isAutoHeaderRefreshDisabled());
    assertFalse(table.isAutoFooterRefreshDisabled());

    table.setAutoHeaderRefreshDisabled(true);
    assertTrue(table.isAutoHeaderRefreshDisabled());
    assertFalse(table.isAutoFooterRefreshDisabled());

    /*
     * Inserting a column should render the headers and footers, even if auto
     * refresh is disabled.
     */
    final List<String> log = new ArrayList<String>();
    Column<String, ?> col0 = new MockColumn<String, String>();
    TextHeader header0 = new TextHeader("header0") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("header0 rendered");
      }
    };
    TextHeader footer0 = new TextHeader("footer0") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("footer0 rendered");
      }
    };
    table.addColumn(col0, header0, footer0);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header0 rendered", log.remove(0));
    assertEquals("footer0 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Inserting another column should render the headers and footers, even if
     * auto refresh is disabled.
     */
    Column<String, ?> col1 = new MockColumn<String, String>();
    TextHeader header1 = new TextHeader("header1") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("header1 rendered");
      }
    };
    TextHeader footer1 = new TextHeader("footer1") {
      @Override
      public void render(Context context, SafeHtmlBuilder sb) {
        super.render(context, sb);
        log.add("footer1 rendered");
      }
    };
    table.addColumn(col1, header1, footer1);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header0 rendered", log.remove(0));
    assertEquals("header1 rendered", log.remove(0));
    assertEquals("footer0 rendered", log.remove(0));
    assertEquals("footer1 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Removing a column should render the headers and footers, even if auto
     * refresh is disabled.
     */
    table.removeColumn(col0);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("header1 rendered", log.remove(0));
    assertEquals("footer1 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Setting data only causes headers to render if auto refresh is enabled,
     * which it is not. Footer refresh is still enabled.
     */
    populateData(table);
    assertEquals(0, log.size()); // Headers are rendered asynchronously.
    table.getPresenter().flush(); // Force headers to render.
    assertEquals("footer1 rendered", log.remove(0));
    assertEquals(0, log.size());

    /*
     * Sorting a column forces the headers only to refresh. The footers are not
     * refreshed.
     */
    table.getColumnSortList().push(col1);
    assertEquals("header1 rendered", log.remove(0));
    assertEquals(0, log.size());
  }

  public void testSetColumnWidth() {
    AbstractCellTable<String> table = createAbstractHasData(new TextCell());
    Column<String, ?> col0 = new MockColumn<String, String>();
    Column<String, ?> col1 = new MockColumn<String, String>();
    Column<String, ?> col2 = new MockColumn<String, String>();

    // Set a column width.
    table.setColumnWidth(col0, "100px");
    table.setColumnWidth(col1, 200.0, Unit.EM);
    assertEquals("100px", table.getColumnWidth(col0));

    // Some browsers return 200.0, others 200.
    assertTrue(table.getColumnWidth(col1).contains("200"));

    // Check a column that has not been set.
    assertNull(table.getColumnWidth(col2));

    // Check a column that has been cleared.
    table.clearColumnWidth(col0);
    assertNull(table.getColumnWidth(col0));
  }

  /**
   * Test that setting column widths using columns and column indexes works
   * correctly.
   */
  public void testSetColumnWidthMixed() {
    AbstractCellTable<String> table = createAbstractHasData(new TextCell());
    Column<String, ?> col0 = new MockColumn<String, String>();
    Column<String, ?> col1 = new MockColumn<String, String>();
    Column<String, ?> col2 = new MockColumn<String, String>();

    // Column 0 set by Column.
    table.setColumnWidth(col0, "100px");

    // Column 1 set by Column and column index.
    table.setColumnWidth(col1, 200.0, Unit.EM);
    table.setColumnWidth(1, "210em");

    // Column 2 set by column index.
    table.setColumnWidth(2, "300px");

    assertEquals("100px", table.getColumnWidth(col0));
    assertEquals("300px", table.getColumnWidth(2));

    /*
     * Some browsers return 200.0, others 200. Column takes precendence over
     * column index.
     */
    assertTrue(table.getColumnWidth(col1).contains("200"));

    // Check a column that has not been set.
    assertNull(table.getColumnWidth(col2));

    // Check a column that has been cleared.
    table.clearColumnWidth(col0);
    assertNull(table.getColumnWidth(col0));
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

  public void testSetKeyboardSelectedRow() {
    AbstractCellTable<String> table = createAbstractHasData(new TextCell());
    table.setVisibleRange(0, 10);

    // Without a subrow.
    table.setKeyboardSelectedRow(5);
    assertEquals(5, table.getKeyboardSelectedRow());
    assertEquals(0, table.getKeyboardSelectedSubRow());

    // Specify a subrow.
    table.setKeyboardSelectedRow(6, 2, false);
    assertEquals(6, table.getKeyboardSelectedRow());
    assertEquals(2, table.getKeyboardSelectedSubRow());

    // Change the subrow.
    table.setKeyboardSelectedRow(6, 5, false);
    assertEquals(6, table.getKeyboardSelectedRow());
    assertEquals(5, table.getKeyboardSelectedSubRow());

    // Change the row.
    table.setKeyboardSelectedRow(7);
    assertEquals(7, table.getKeyboardSelectedRow());
    assertEquals(0, table.getKeyboardSelectedSubRow());
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
    
    // Sort the same column again, this time using the Enter key.
    NativeEvent enter = Document.get().createKeyDownEvent(false, false, false, false,
        KeyCodes.KEY_ENTER);
    getHeaderElement(table, 0).dispatchEvent(enter);
    assertEquals(1, sortList.size());
    assertEquals(table.getColumn(0), sortList.get(0).getColumn());
    assertTrue(sortList.get(0).isAscending());
    assertEquals(1, lastSorted.size());
    lastSorted.clear();

    // Sort the same column using the Enter key again.
    getHeaderElement(table, 0).dispatchEvent(enter);
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
