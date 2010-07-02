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
import com.google.gwt.user.cellview.client.PagingListViewPresenter.ElementIterator;
import com.google.gwt.user.cellview.client.PagingListViewPresenter.LoadingState;
import com.google.gwt.user.cellview.client.PagingListViewPresenter.View;
import com.google.gwt.view.client.MockPagingListView;
import com.google.gwt.view.client.MockSelectionModel;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.MockPagingListView.MockDelegate;
import com.google.gwt.view.client.MockPagingListView.MockPager;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Tests for {@link PagingListViewPresenter}.
 */
public class PagingListViewPresenterTest extends TestCase {

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
    private String lastHtml;
    private LoadingState loadingState;
    private boolean onUpdateSelectionFired;
    private boolean replaceAllChildrenCalled;
    private boolean replaceChildrenCalled;
    private Set<Integer> selectedRows = new HashSet<Integer>();

    public void assertLastHtml(String html) {
      assertEquals(html, lastHtml);
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
        assertTrue("Row " + row + "is not selected", selectedRows.contains(row));
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

    public void render(StringBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      sb.append("start=").append(start);
      sb.append(",size=").append(values.size());
    }

    public void replaceAllChildren(List<T> values, String html) {
      childCount = values.size();
      replaceAllChildrenCalled = true;
      lastHtml = html;
    }

    public void replaceChildren(List<T> values, int start, String html) {
      childCount = Math.max(childCount, start + values.size());
      replaceChildrenCalled = true;
      lastHtml = html;
    }

    public void setDependsOnSelection(boolean dependsOnSelection) {
      this.dependsOnSelection = dependsOnSelection;
    }

    public void setLoadingState(LoadingState state) {
      this.loadingState = state;
    }

    protected void setSelected(int index, boolean selected) {
      if (selected) {
        selectedRows.add(index);
      } else {
        selectedRows.remove(index);
      }
    }
  }

  public void testClearSelectionModel() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    view.setDependsOnSelection(true);
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setData(0, 10, createData(0, 10));
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
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    presenter.setDataSize(35, true);

    // First page.
    assertEquals(10, presenter.getCurrentPageSize());

    // Last page.
    presenter.setRange(30, 10);
    assertEquals(5, presenter.getCurrentPageSize());
  }

  public void testRedraw() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setDataSize(10, true);
    presenter.setData(0, 10, createData(0, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
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

  public void testSetData() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    presenter.setRange(5, 10);
    view.assertLoadingState(LoadingState.LOADING);

    // Page range same as data range.
    List<String> expectedData = createData(5, 10);
    presenter.setData(5, 10, createData(5, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Page range contains data range.
    expectedData.set(2, "test 100");
    expectedData.set(3, "test 101");
    presenter.setData(7, 2, createData(100, 2));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=7,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range overlaps page start.
    expectedData.set(0, "test 202");
    expectedData.set(1, "test 203");
    presenter.setData(3, 4, createData(200, 4));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=5,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range overlaps page end.
    expectedData.set(8, "test 300");
    expectedData.set(9, "test 301");
    presenter.setData(13, 4, createData(300, 4));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=13,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range contains page range.
    expectedData = createData(400, 20).subList(2, 12);
    presenter.setData(3, 20, createData(400, 20));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Setting data outside of the data size should update the data size.
   */
  public void testSetDataChangesDataSize() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Set the initial data size.
    presenter.setDataSize(10, true);
    view.assertLoadingState(LoadingState.LOADING);

    // Set the data within the range.
    presenter.setData(0, 10, createData(0, 10));
    view.assertLoadingState(LoadingState.LOADED);

    // Set the data past the range.
    presenter.setData(5, 10, createData(5, 10));
    assertEquals(15, presenter.getDataSize());
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetDataOutsideRange() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    presenter.setRange(5, 10);
    view.assertLoadingState(LoadingState.LOADING);

    // Page range same as data range.
    List<String> expectedData = createData(5, 10);
    presenter.setData(5, 10, createData(5, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Data range past page end.
    presenter.setData(15, 5, createData(15, 5));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);

    // Data range before page start.
    presenter.setData(0, 5, createData(0, 5));
    assertEquals(10, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
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
  public void testSetDataSameContents() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(LoadingState.LOADING);

    // Initialize some data.
    presenter.setRange(0, 10);
    presenter.setData(0, 10, createData(0, 10));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Set the same data over the entire range.
    presenter.setData(0, 10, createData(0, 10));
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Set data at the end of the page only.
   */
  public void testSetDataSparse() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(LoadingState.LOADING);

    List<String> expectedData = createData(5, 3);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    presenter.setRange(0, 10);
    presenter.setData(5, 3, createData(5, 3));
    assertEquals(8, presenter.getData().size());
    assertEquals(expectedData, presenter.getData());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetDataSize() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(LoadingState.LOADING);

    // Set size to 100.
    presenter.setDataSize(100, true);
    assertEquals(100, presenter.getDataSize());
    assertTrue(presenter.isDataSizeExact());
    view.assertLoadingState(LoadingState.LOADING);

    // Set size to 0.
    presenter.setDataSize(0, false);
    assertEquals(0, presenter.getDataSize());
    assertFalse(presenter.isDataSizeExact());
    view.assertLoadingState(LoadingState.EMPTY);
  }

  public void testSetDataSizeTrimsCurrentPage() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    view.assertLoadingState(LoadingState.LOADING);

    // Initialize some data.
    presenter.setDataSize(10, true);
    presenter.setRange(0, 10);
    assertEquals(new Range(0, 10), presenter.getRange());
    presenter.setData(0, 10, createData(0, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Trim the size.
    presenter.setDataSize(8, true);
    assertEquals(8, presenter.getDataSize());
    assertTrue(presenter.isDataSizeExact());
    assertEquals(new Range(0, 10), presenter.getRange());
    assertEquals(8, presenter.getData().size());
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetDelegate() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    MockDelegate<String> delegate = new MockDelegate<String>();
    presenter.setDelegate(delegate);

    // Change the pageStart.
    presenter.setRange(10, 10);
    assertEquals(listView, delegate.getLastListView());
    delegate.clearListView();

    // Change the pageSize.
    presenter.setRange(10, 20);
    assertEquals(listView, delegate.getLastListView());
    delegate.clearListView();

    // Reuse the same range.
    presenter.setRange(10, 20);
    assertNull(delegate.getLastListView());

    // Change the data size, which does not affect the delegate.
    presenter.setDataSize(100, true);
    assertNull(delegate.getLastListView());

    // Unset the delegate.
    presenter.setDelegate(null);
    presenter.setRange(20, 100);
    assertNull(delegate.getLastListView());
  }

  public void testSetPager() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    MockPager<String> pager = new MockPager<String>();
    presenter.setPager(pager);

    // Change the pageStart.
    presenter.setRange(10, 10);
    assertEquals(listView, pager.getLastListView());
    pager.clearListView();

    // Change the pageSize.
    presenter.setRange(10, 20);
    assertEquals(listView, pager.getLastListView());
    pager.clearListView();

    // Reuse the same range.
    presenter.setRange(10, 20);
    assertNull(pager.getLastListView());

    // Change the data size.
    presenter.setDataSize(100, true);
    assertEquals(listView, pager.getLastListView());
    pager.clearListView();

    // Unset the delegate.
    presenter.setPager(null);
    presenter.setRange(20, 100);
    assertNull(pager.getLastListView());
  }

  public void testSetRange() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Set the range the first time.
    presenter.setRange(0, 100);
    assertEquals(new Range(0, 100), presenter.getRange());
    assertEquals(0, presenter.getData().size());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADING);

    // Set the range to the same value.
    presenter.setRange(0, 100);
    assertEquals(new Range(0, 100), presenter.getRange());
    assertEquals(0, presenter.getData().size());
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADING);
  }

  public void testSetRangeDecreasePageSize() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setRange(0, 10);
    assertEquals(new Range(0, 10), presenter.getRange());
    presenter.setData(0, 10, createData(0, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Decrease the page size.
    presenter.setRange(0, 8);
    assertEquals(new Range(0, 8), presenter.getRange());
    assertEquals(8, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetRangeDecreasePageStart() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setRange(10, 30);
    assertEquals(new Range(10, 30), presenter.getRange());
    presenter.setData(10, 10, createData(0, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=10,size=10");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);

    // Decrease the start index.
    presenter.setRange(8, 30);
    assertEquals(new Range(8, 30), presenter.getRange());
    assertEquals(12, presenter.getData().size());
    assertEquals(null, presenter.getData().get(0));
    assertEquals(null, presenter.getData().get(1));
    assertEquals("test 0", presenter.getData().get(2));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=8,size=12");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetRangeIncreasePageSize() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setRange(0, 10);
    assertEquals(new Range(0, 10), presenter.getRange());
    presenter.setData(0, 10, createData(0, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Increase the page size.
    presenter.setRange(0, 20);
    assertEquals(new Range(0, 20), presenter.getRange());
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetRangeIncreasePageStart() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);

    // Initialize some data.
    presenter.setRange(0, 20);
    assertEquals(new Range(0, 20), presenter.getRange());
    presenter.setData(0, 10, createData(0, 10));
    assertEquals(10, presenter.getData().size());
    assertEquals("test 0", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);

    // Increase the start index.
    presenter.setRange(2, 20);
    assertEquals(new Range(2, 20), presenter.getRange());
    assertEquals(8, presenter.getData().size());
    assertEquals("test 2", presenter.getData().get(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=2,size=8");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  /**
   * If the cells depend on selection, the cells should be replaced.
   */
  public void testSetSelectionModelDependOnSelection() {
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    view.setDependsOnSelection(true);
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setRange(0, 10);
    presenter.setData(0, 10, createData(0, 10));
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
    PagingListView<String> listView = new MockPagingListView<String>();
    MockView<String> view = new MockView<String>();
    PagingListViewPresenter<String> presenter = new PagingListViewPresenter<String>(
        listView, view, 10);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setRange(0, 10);
    presenter.setData(0, 10, createData(0, 10));
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
