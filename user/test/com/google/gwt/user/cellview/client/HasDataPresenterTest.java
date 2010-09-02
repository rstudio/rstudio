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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.HasDataPresenter.ElementIterator;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.cellview.client.HasDataPresenter.View;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MockHasData;
import com.google.gwt.view.client.MockHasData.MockRangeChangeHandler;
import com.google.gwt.view.client.MockHasData.MockRowCountChangeHandler;
import com.google.gwt.view.client.MockSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Tests for {@link HasDataPresenter}.
 */
public class HasDataPresenterTest extends TestCase {

  /**
   * Mock iterator over DOM elements.
   */
  private static class MockElementIterator implements ElementIterator {

    private final int count;
    private int next = 0;
    private final MockView<?> view;

    public MockElementIterator(MockView<?> view, int count) {
      this.view = view;
      this.count = count;
    }

    public boolean hasNext() {
      return next < count;
    }

    public Element next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      next++;
      return null;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    /**
     * Set the selection state of the current element.
     *
     * @param selected the selection state
     * @throws IllegalStateException if {@link #next()} has not been called
     */
    public void setSelected(boolean selected) throws IllegalStateException {
      if (next == 0) {
        throw new IllegalStateException();
      }
      view.setSelected(next - 1, selected);
    }
  }

  /**
   * A mock view used for testing.
   *
   * @param <T> the data type
   */
  private static class MockView<T> implements View<T> {

    private int childCount;
    private boolean dependsOnSelection;
    private SafeHtml lastHtml;
    private LoadingState loadingState;
    private boolean onUpdateSelectionFired;
    private boolean replaceAllChildrenCalled;
    private boolean replaceChildrenCalled;
    private Set<Integer> selectedRows = new HashSet<Integer>();

    public <H extends EventHandler> HandlerRegistration addHandler(
        H handler, Type<H> type) {
      throw new UnsupportedOperationException();
    }

    public void assertLastHtml(String html) {
      if (html == null) {
        assertNull(lastHtml);
      } else {
        assertEquals(html, lastHtml.asString());
      }
      lastHtml = null;
    }

    public void assertLoadingState(LoadingState expected) {
      assertEquals(expected, loadingState);
    }

    public void assertOnUpdateSelectionFired(boolean expected) {
      assertEquals(expected, onUpdateSelectionFired);
      onUpdateSelectionFired = false;
    }

    public void assertReplaceAllChildrenCalled(boolean expected) {
      assertEquals(expected, replaceAllChildrenCalled);
      replaceAllChildrenCalled = false;
    }

    public void assertReplaceChildrenCalled(boolean expected) {
      assertEquals(expected, replaceChildrenCalled);
      replaceChildrenCalled = false;
    }

    /**
     * Assert that {@link #setSelected(int, boolean)} was called for the
     * specified rows.
     *
     * @param rows the rows
     */
    public void assertSelectedRows(Integer... rows) {
      assertEquals(rows.length, selectedRows.size());
      for (Integer row : rows) {
        assertTrue(
            "Row " + row + "is not selected", selectedRows.contains(row));
      }
    }

    public boolean dependsOnSelection() {
      return dependsOnSelection;
    }

    public int getChildCount() {
      return childCount;
    }

    public MockElementIterator getChildIterator() {
      return new MockElementIterator(this, 10);
    }

    public void onUpdateSelection() {
      onUpdateSelectionFired = true;
    }

    public void render(SafeHtmlBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      sb.appendHtmlConstant("start=").append(start);
      sb.appendHtmlConstant(",size=").append(values.size());
    }

    public void replaceAllChildren(List<T> values, SafeHtml html) {
      childCount = values.size();
      replaceAllChildrenCalled = true;
      lastHtml = html;
    }

    public void replaceChildren(List<T> values, int start, SafeHtml html) {
      childCount = Math.max(childCount, start + values.size());
      replaceChildrenCalled = true;
      lastHtml = html;
    }

    public void resetFocus() {
    }

    public void setDependsOnSelection(boolean dependsOnSelection) {
      this.dependsOnSelection = dependsOnSelection;
    }

    public void setLoadingState(LoadingState state) {
      this.loadingState = state;
    }

    public void setSelected(Element elem, boolean selected) {
      // Not used in this mock.
      throw new UnsupportedOperationException();
    }

    protected void setSelected(int index, boolean selected) {
      if (selected) {
        selectedRows.add(index);
      } else {
        selectedRows.remove(index);
      }
    }
  }

  public void testAddRowCountChangeHandler() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    MockRowCountChangeHandler handler = new MockRowCountChangeHandler();

    // Adding a handler should not invoke the handler.
    // Add the handler to the view because it is the source of events.
    HandlerRegistration reg = listView.addRowCountChangeHandler(handler);
    assertEquals(-1, handler.getLastRowCount());
    handler.reset();

    // Change the row count.
    presenter.setRowCount(100, true);
    assertEquals(100, handler.getLastRowCount());
    assertTrue(handler.isLastRowCountExact());
    handler.reset();

    // Set the same row count and verify no event is fired.
    presenter.setRowCount(100, true);
    assertEquals(-1, handler.getLastRowCount());
    handler.reset();

    // Change the row count again.
    presenter.setRowCount(110, false);
    assertEquals(110, handler.getLastRowCount());
    assertFalse(handler.isLastRowCountExact());
    handler.reset();

    // Change only the isExact param and verify an event is fired.
    presenter.setRowCount(110, true);
    assertEquals(110, handler.getLastRowCount());
    assertTrue(handler.isLastRowCountExact());
    handler.reset();

    // Remove the handler and verify it no longer receives events.
    reg.removeHandler();
    presenter.setRowCount(200, true);
    assertEquals(-1, handler.getLastRowCount());
  }

  public void testAddRangeChangeHandler() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    MockRangeChangeHandler handler = new MockRangeChangeHandler();

    // Adding a handler should not invoke the handler.
    // Add the handler to the view because it is the source of events.
    HandlerRegistration reg = listView.addRangeChangeHandler(handler);
    assertNull(handler.getLastRange());
    handler.reset();

    // Change the pageStart.
    presenter.setVisibleRange(new Range(10, 10));
    assertEquals(new Range(10, 10), handler.getLastRange());
    handler.reset();

    // Change the pageSize.
    presenter.setVisibleRange(new Range(10, 20));
    assertEquals(new Range(10, 20), handler.getLastRange());
    handler.reset();

    // Reuse the same range and verify an event is not fired.
    presenter.setVisibleRange(new Range(10, 20));
    assertNull(handler.getLastRange());
    handler.reset();

    // Remove the handler and verify it no longer receives events.
    reg.removeHandler();
    presenter.setVisibleRange(new Range(20, 100));
    assertNull(handler.getLastRange());
    handler.reset();
  }

  public void testClearSelectionModel() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    view.setDependsOnSelection(true);
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setRowData(0, createData(0, 10));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");

    // Set the selection model.
    SelectionModel<String> model = new MockSelectionModel<String>();
    model.setSelected("test 0", true);
    presenter.setSelectionModel(model);
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows();

    // Clear the selection model without updating the view.
    presenter.clearSelectionModel();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertOnUpdateSelectionFired(false);
    view.assertSelectedRows();
  }

  public void testGetCurrentPageSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    presenter.setRowCount(35, true);

    // First page.
    assertEquals(10, presenter.getCurrentPageSize());

    // Last page.
    presenter.setVisibleRange(new Range(30, 10));
    assertEquals(5, presenter.getCurrentPageSize());
  }

  public void testRedraw() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setRowCount(10, true);
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Redraw.
    presenter.redraw();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetRowCount() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(null);

    // Set size to 100.
    presenter.setRowCount(100, true);
    assertEquals(100, presenter.getRowCount());
    assertTrue(presenter.isRowCountExact());
    view.assertLoadingState(LoadingState.LOADING);

    // Set size to 0.
    presenter.setRowCount(0, false);
    assertEquals(0, presenter.getRowCount());
    assertFalse(presenter.isRowCountExact());
    view.assertLoadingState(LoadingState.EMPTY);
  }

  public void testSetRowCountNoBoolean() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    try {
      presenter.setRowCount(100);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected.
    }
  }

  public void testSetRowCountTrimsCurrentPage() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(null);

    // Initialize some data.
    presenter.setRowCount(10, true);
    presenter.setVisibleRange(new Range(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Trim the size.
    presenter.setRowCount(8, true);
    assertEquals(8, presenter.getRowCount());
    assertTrue(presenter.isRowCountExact());
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(8, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetRowData() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    presenter.setVisibleRange(new Range(5, 10));
    view.assertLoadingState(LoadingState.LOADING);

    // Page range same as data range.
    List<String> expectedData = createData(5, 10);
    presenter.setRowData(5, createData(5, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Page range contains data range.
    expectedData.set(2, "test 100");
    expectedData.set(3, "test 101");
    presenter.setRowData(7, createData(100, 2));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=7,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range overlaps page start.
    expectedData.set(0, "test 202");
    expectedData.set(1, "test 203");
    presenter.setRowData(3, createData(200, 4));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=5,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range overlaps page end.
    expectedData.set(8, "test 300");
    expectedData.set(9, "test 301");
    presenter.setRowData(13, createData(300, 4));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=13,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range contains page range.
    expectedData = createData(400, 20).subList(2, 12);
    presenter.setRowData(3, createData(400, 20));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Setting data outside of the data size should update the data size.
   */
  public void testSetRowValuesChangesDataSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Set the initial data size.
    presenter.setRowCount(10, true);
    view.assertLoadingState(LoadingState.LOADING);

    // Set the data within the range.
    presenter.setRowData(0, createData(0, 10));
    view.assertLoadingState(LoadingState.LOADED);

    // Set the data past the range.
    presenter.setRowData(5, createData(5, 10));
    assertEquals(15, presenter.getRowCount());
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Setting an empty list that starts on the page start should pass through to
   * the view.
   */
  public void testSetRowValuesEmptySet() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Set the initial data size.
    presenter.setRowCount(10, true);
    view.assertLoadingState(LoadingState.LOADING);

    // Set an empty list of row values.
    presenter.setRowData(0, createData(0, 0));
    view.assertLoadingState(LoadingState.LOADING);
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
  }

  public void testSetRowValuesOutsideRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    presenter.setVisibleRange(new Range(5, 10));
    view.assertLoadingState(LoadingState.LOADING);

    // Page range same as data range.
    List<String> expectedData = createData(5, 10);
    presenter.setRowData(5, createData(5, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Data range past page end.
    presenter.setRowData(15, createData(15, 5));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);

    // Data range before page start.
    presenter.setRowData(0, createData(0, 5));
    assertEquals(10, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * As an optimization, the presenter does not replace the rendered string if
   * the rendered string is identical to the previously rendered string. This is
   * useful for tables that refresh on an interval.
   */
  public void testSetRowValuesSameContents() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(null);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    presenter.setRowData(0, createData(0, 10));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Set the same data over the entire range.
    presenter.setRowData(0, createData(0, 10));
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Set data at the end of the page only.
   */
  public void testSetRowValuesSparse() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(null);

    List<String> expectedData = createData(5, 3);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    presenter.setVisibleRange(new Range(0, 10));
    presenter.setRowData(5, createData(5, 3));
    assertEquals(8, presenter.getRowData().size());
    assertEquals(expectedData, presenter.getRowData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Set the range the first time.
    presenter.setVisibleRange(new Range(0, 100));
    assertEquals(new Range(0, 100), presenter.getVisibleRange());
    assertEquals(0, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADING);

    // Set the range to the same value.
    presenter.setVisibleRange(new Range(0, 100));
    assertEquals(new Range(0, 100), presenter.getVisibleRange());
    assertEquals(0, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADING);
  }

  public void testSetVisibleRangeAndClearDataDifferentRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Add a range change handler.
    final List<Range> events = new ArrayList<Range>();
    listView.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      public void onRangeChange(RangeChangeEvent event) {
        events.add(event.getNewRange());
      }
    });

    // Set some initial data.
    presenter.setVisibleRange(new Range(5, 10));
    presenter.setRowData(5, createData(5, 10));
    assertEquals(new Range(5, 10), presenter.getVisibleRange());
    assertEquals(10, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    view.assertLoadingState(LoadingState.LOADED);
    assertEquals(1, events.size());

    // Set a different range.
    presenter.setVisibleRangeAndClearData(new Range(0, 10), false);
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(0, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=0");
    view.assertLoadingState(LoadingState.LOADING);
    assertEquals(2, events.size());
  }

  public void testSetVisibleRangeAndClearDataSameRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Add a range change handler.
    final List<Range> events = new ArrayList<Range>();
    listView.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      public void onRangeChange(RangeChangeEvent event) {
        events.add(event.getNewRange());
      }
    });

    // Set some initial data.
    presenter.setRowData(0, createData(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(10, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);
    assertEquals(0, events.size());

    // Set the same range.
    presenter.setVisibleRangeAndClearData(new Range(0, 10), false);
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(0, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=0");
    view.assertLoadingState(LoadingState.LOADING);
    assertEquals(0, events.size());
  }

  public void testSetVisibleRangeAndClearDataSameRangeForceEvent() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Add a range change handler.
    final List<Range> events = new ArrayList<Range>();
    listView.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      public void onRangeChange(RangeChangeEvent event) {
        events.add(event.getNewRange());
      }
    });

    // Set some initial data.
    presenter.setRowData(0, createData(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(10, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);
    assertEquals(0, events.size());

    // Set the same range.
    presenter.setVisibleRangeAndClearData(new Range(0, 10), true);
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(0, presenter.getRowData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=0");
    view.assertLoadingState(LoadingState.LOADING);
    assertEquals(1, events.size());
  }

  public void testSetVisibleRangeDecreasePageSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Decrease the page size.
    presenter.setVisibleRange(new Range(0, 8));
    assertEquals(new Range(0, 8), presenter.getVisibleRange());
    assertEquals(8, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetVisibleRangeDecreasePageStart() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setVisibleRange(new Range(10, 30));
    assertEquals(new Range(10, 30), presenter.getVisibleRange());
    presenter.setRowData(10, createData(0, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=10,size=10");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);

    // Decrease the start index.
    presenter.setVisibleRange(new Range(8, 30));
    assertEquals(new Range(8, 30), presenter.getVisibleRange());
    assertEquals(12, presenter.getRowData().size());
    assertEquals(null, presenter.getRowData().get(0));
    assertEquals(null, presenter.getRowData().get(1));
    assertEquals("test 0", presenter.getRowData().get(2));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=8,size=12");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRangeIncreasePageSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Increase the page size.
    presenter.setVisibleRange(new Range(0, 20));
    assertEquals(new Range(0, 20), presenter.getVisibleRange());
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRangeIncreasePageStart() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 20));
    assertEquals(new Range(0, 20), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getRowData().size());
    assertEquals("test 0", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);

    // Increase the start index.
    presenter.setVisibleRange(new Range(2, 20));
    assertEquals(new Range(2, 20), presenter.getVisibleRange());
    assertEquals(8, presenter.getRowData().size());
    assertEquals("test 2", presenter.getRowData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=2,size=8");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRangeInts() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);

    try {
      presenter.setVisibleRange(0, 100);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected.
    }
  }

  /**
   * If the cells depend on selection, the cells should be replaced.
   */
  public void testSetSelectionModelDependOnSelection() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    view.setDependsOnSelection(true);
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    presenter.setRowData(0, createData(0, 10));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");

    // Set the selection model.
    SelectionModel<String> model = new MockSelectionModel<String>();
    model.setSelected("test 0", true);
    presenter.setSelectionModel(model);
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows();

    // Select something.
    model.setSelected("test 2", true);
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows();

    // Set selection model to null.
    presenter.setSelectionModel(null);
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows();
  }

  /**
   * If the cells do not depend on selection, the view should be told to update
   * the cell container element.
   */
  public void testSetSelectionModelDoesNotDependOnSelection() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    presenter.setRowData(0, createData(0, 10));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");

    // Set the selection model.
    SelectionModel<String> model = new MockSelectionModel<String>();
    model.setSelected("test 0", true);
    presenter.setSelectionModel(model);
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows(0);

    // Select something.
    model.setSelected("test 2", true);
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows(0, 2);

    // Set selection model to null.
    presenter.setSelectionModel(null);
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertOnUpdateSelectionFired(true);
    view.assertSelectedRows();
  }

  /**
   * Create a list of data for testing.
   *
   * @param start the start index
   * @param length the length
   * @return a list of data
   */
  private List<String> createData(int start, int length) {
    List<String> toRet = new ArrayList<String>();
    for (int i = 0; i < length; i++) {
      toRet.add("test " + (i + start));
    }
    return toRet;
  }
}
