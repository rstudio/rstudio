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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.HasDataPresenter.View;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MockHasData;
import com.google.gwt.view.client.MockHasData.MockRangeChangeHandler;
import com.google.gwt.view.client.MockHasData.MockRowCountChangeHandler;
import com.google.gwt.view.client.MockSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests for {@link HasDataPresenter}.
 */
public class HasDataPresenterTest extends GWTTestCase {

  /**
   * A mock {@link SelectionChangeEvent.Handler} used for testing.
   */
  private static class MockSelectionChangeHandler implements
      SelectionChangeEvent.Handler {

    private boolean eventFired;

    /**
     * Assert that a {@link SelectionChangeEvent} was fired and clear the
     * boolean.
     * 
     * @param expected the expected value
     */
    public void assertEventFired(boolean expected) {
      assertEquals(expected, eventFired);
      eventFired = false;
    }

    public void onSelectionChange(SelectionChangeEvent event) {
      assertFalse(eventFired);
      eventFired = true;
    }
  }

  /**
   * A mock {@link SelectionModel} used for testing without used any GWT client
   * code.
   * 
   * @param <T> the selection type
   */
  private class MockSingleSelectionModel<T> extends SingleSelectionModel<T> {

    public MockSingleSelectionModel(ProvidesKey<T> keyProvider) {
      super(keyProvider);
    }

    @Override
    public void fireSelectionChangeEvent() {
      super.fireSelectionChangeEvent();
    }
  }

  /**
   * A mock view used for testing.
   * 
   * @param <T> the data type
   */
  private static class MockView<T> implements View<T> {

    private int childCount;
    private List<Integer> keyboardSelectedRow = new ArrayList<Integer>();
    private List<Boolean> keyboardSelectedRowState = new ArrayList<Boolean>();
    private final List<SafeHtml> lastHtml = new ArrayList<SafeHtml>();
    private LoadingState loadingState;
    private boolean replaceAllChildrenCalled;
    private boolean replaceChildrenCalled;

    public <H extends EventHandler> HandlerRegistration addHandler(H handler,
        Type<H> type) {
      throw new UnsupportedOperationException();
    }

    /**
     * Assert the value of the oldest keyboard selected row and pop it.
     * 
     * @param row the row index
     * @param selected true if selected, false if not
     */
    public void assertKeyboardSelectedRow(int row, boolean selected) {
      int actualRow = keyboardSelectedRow.remove(0);
      boolean actualSelected = keyboardSelectedRowState.remove(0);
      assertEquals(row, actualRow);
      assertEquals(selected, actualSelected);
    }

    /**
     * Assert that the keyboard selected row queue is empty.
     */
    public void assertKeyboardSelectedRowEmpty() {
      assertEquals(0, keyboardSelectedRow.size());
    }

    public void assertLastHtml(String html) {
      if (html == null) {
        assertTrue(lastHtml.isEmpty());
      } else {
        assertEquals(html, lastHtml.remove(0).asString());
      }
    }

    public void assertLoadingState(LoadingState expected) {
      assertEquals(expected, loadingState);
    }

    public void assertReplaceAllChildrenCalled(boolean expected) {
      assertEquals(expected, replaceAllChildrenCalled);
      replaceAllChildrenCalled = false;
    }

    public void assertReplaceChildrenCalled(boolean expected) {
      assertEquals(expected, replaceChildrenCalled);
      replaceChildrenCalled = false;
    }

    public int getChildCount() {
      return childCount;
    }

    public void render(SafeHtmlBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      sb.appendHtmlConstant("start=").append(start);
      sb.appendHtmlConstant(",size=").append(values.size());
    }

    public void replaceAllChildren(List<T> values, SafeHtml html,
        boolean stealFocus) {
      childCount = values.size();
      replaceAllChildrenCalled = true;
      lastHtml.add(html);
    }

    public void replaceChildren(List<T> values, int start, SafeHtml html,
        boolean stealFocus) {
      childCount = Math.max(childCount, start + values.size());
      replaceChildrenCalled = true;
      lastHtml.add(html);
    }

    public void resetFocus() {
    }

    public void setKeyboardSelected(int index, boolean selected,
        boolean stealFocus) {
      keyboardSelectedRow.add(index);
      keyboardSelectedRowState.add(selected);
    }

    public void setLoadingState(LoadingState state) {
      this.loadingState = state;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  public void testAddRowCountChangeHandler() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
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
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
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

  /**
   * Test that the presenter can gracefully handle a view that throws exceptions
   * when rendering the content.
   */
  public void testBadViewSelectionModel() {
    SelectionModel<String> badModel = new SelectionModel<String>() {
      public void fireEvent(GwtEvent<?> event) {
      }

      public Object getKey(String item) {
        return null;
      }

      public HandlerRegistration addSelectionChangeHandler(Handler handler) {
        return null;
      }

      public boolean isSelected(String object) {
        throw new NullPointerException();
      }

      public void setSelected(String object, boolean selected) {
        throw new NullPointerException();
      }
    };

    // Use the bad view in a presenter.
    MockView<String> view = new MockView<String>();
    HasData<String> listView = new MockHasData<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setSelectionModel(badModel);
    presenter.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
    testPresenterWithBadUserCode(presenter);
  }

  /**
   * Test that the presenter can gracefully handle a view that throws exceptions
   * when rendering the content.
   */
  public void testBadViewRender() {
    MockView<String> badView = new MockView<String>() {
      @Override
      public void render(SafeHtmlBuilder sb, List<String> values, int start,
          SelectionModel<? super String> selectionModel) {
        throw new NullPointerException();
      }
    };

    // Use the bad view in a presenter.
    HasData<String> listView = new MockHasData<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        badView, 10, null);
    testPresenterWithBadUserCode(presenter);
  }

  /**
   * Test that the presenter can gracefully handle a view that throws exceptions
   * when rendering the children.
   */
  public void testBadViewReplaceChildren() {
    MockView<String> badView = new MockView<String>() {
      @Override
      public void replaceAllChildren(List<String> values, SafeHtml html,
          boolean stealFocus) {
        throw new NullPointerException();
      }

      @Override
      public void replaceChildren(List<String> values, int start,
          SafeHtml html, boolean stealFocus) {
        throw new NullPointerException();
      }
    };

    // Use the bad view in a presenter.
    HasData<String> listView = new MockHasData<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        badView, 10, null);
    testPresenterWithBadUserCode(presenter);
  }

  public void testCalculateModifiedRanges() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    TreeSet<Integer> rows = new TreeSet<Integer>();

    // Empty set of rows.
    assertListContains(presenter.calculateModifiedRanges(rows, 0, 10));

    // One row in range.
    rows.add(5);
    assertListContains(presenter.calculateModifiedRanges(rows, 0, 10),
        new Range(5, 1));

    // One row not in range.
    assertListContains(presenter.calculateModifiedRanges(rows, 6, 10));

    // Consecutive rows (should return only one range).
    rows.add(6);
    rows.add(7);
    rows.add(8);
    assertListContains(presenter.calculateModifiedRanges(rows, 0, 10),
        new Range(5, 4));

    // Disjoint rows. Should return two ranges.
    rows.add(10);
    rows.add(11);
    assertListContains(presenter.calculateModifiedRanges(rows, 0, 20),
        new Range(5, 4), new Range(10, 2));

    // Multiple gaps. The largest gap should be between the two ranges.
    rows.add(15);
    rows.add(17);
    assertListContains(presenter.calculateModifiedRanges(rows, 0, 20),
        new Range(5, 7), new Range(15, 3));
  }

  public void testClearSelectionModel() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");

    // Set the selection model.
    SelectionModel<String> model = new MockSelectionModel<String>(null);
    model.setSelected("test 0", true);
    presenter.setSelectionModel(model);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=0,size=1");

    // Clear the selection model without updating the view.
    presenter.clearSelectionModel();
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
  }

  public void testDefaults() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    assertEquals(0, presenter.getRowCount());
    assertFalse(presenter.isRowCountExact());
    assertEquals(0, presenter.getCurrentPageSize());
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
  }

  /**
   * Test that keyboard selection moves if its value moves.
   */
  public void testFindIndexOfBestMatch() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    populatePresenter(presenter);

    // Select the second element.
    presenter.setKeyboardSelectedRow(2, false, false);
    presenter.flush();
    assertEquals(2, presenter.getKeyboardSelectedRow());
    assertEquals("test 2", presenter.getKeyboardSelectedRowValue());

    // Shift the values by one.
    presenter.setRowData(1, createData(0, 9));
    presenter.flush();
    assertEquals(3, presenter.getKeyboardSelectedRow());
    assertEquals("test 2", presenter.getKeyboardSelectedRowValue());

    // Replace the keyboard selected value.
    presenter.setRowData(0, createData(100, 10));
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    assertEquals(null, presenter.getKeyboardSelectedRowValue());
  }

  public void testFlush() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Data should not be pushed to the view until flushed.
    populatePresenter(presenter);
    view.assertReplaceAllChildrenCalled(false);

    // Now the data is pushed.
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
  }

  public void testGetCurrentPageSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setRowCount(35, true);

    // First page.
    assertEquals(10, presenter.getCurrentPageSize());

    // Last page.
    presenter.setVisibleRange(new Range(30, 10));
    assertEquals(5, presenter.getCurrentPageSize());
  }

  public void testIsEmpty() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Non-zero row count.
    presenter.setRowCount(1, true);
    populatePresenter(presenter);
    presenter.flush();
    assertFalse(presenter.isEmpty());

    // Zero row count with unknown size.
    presenter.setRowCount(0, false);
    populatePresenter(presenter);
    presenter.flush();
    assertFalse(presenter.isEmpty());

    // Zero row count with known size.
    presenter.setRowCount(0, true);
    populatePresenter(presenter);
    presenter.flush();
    assertFalse(presenter.isEmpty());
  }
  
  public void testKeyboardNavigationChangePage() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setRowCount(100, true);
    presenter.setVisibleRange(new Range(50, 10));
    populatePresenter(presenter);
    presenter.flush();
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.CHANGE_PAGE);

    // keyboardPrev in middle.
    presenter.setKeyboardSelectedRow(1, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(1, true);
    assertTrue(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(0, true);

    // keyboardPrev at beginning goes to previous page.
    assertTrue(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(new Range(40, 10), presenter.getVisibleRange());

    // keyboardNext in middle.
    presenter.setKeyboardSelectedRow(8, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(9, false);
    view.assertKeyboardSelectedRow(8, true);
    assertTrue(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(8, false);
    view.assertKeyboardSelectedRow(9, true);

    // keyboardNext at end.
    assertTrue(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(new Range(50, 10), presenter.getVisibleRange());

    // keyboardPrevPage.
    presenter.setKeyboardSelectedRow(5, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(5, true);
    presenter.keyboardPrevPage();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(new Range(40, 10), presenter.getVisibleRange());

    // keyboardNextPage.
    presenter.setKeyboardSelectedRow(5, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(5, true);
    presenter.keyboardNextPage();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(new Range(50, 10), presenter.getVisibleRange());

    // keyboardHome.
    presenter.keyboardHome();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(new Range(0, 10), presenter.getVisibleRange());

    // keyboardPrev at first row.
    assertFalse(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertKeyboardSelectedRowEmpty();

    // keyboardEnd.
    presenter.keyboardEnd();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(new Range(90, 10), presenter.getVisibleRange());

    // keyboardNext at last row.
    assertFalse(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertKeyboardSelectedRowEmpty();
  }

  public void testKeyboardNavigationCurrentPage() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(50, 10));
    populatePresenter(presenter);
    presenter.flush();
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);

    // keyboardPrev in middle.
    presenter.setKeyboardSelectedRow(1, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(1, true);
    assertTrue(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(0, true);

    // keyboardPrev at beginning.
    assertFalse(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRowEmpty();

    // keyboardNext in middle.
    presenter.setKeyboardSelectedRow(8, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(8, true);
    assertTrue(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(8, false);
    view.assertKeyboardSelectedRow(9, true);

    // keyboardNext at end.
    assertFalse(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRowEmpty();

    // keyboardPrevPage.
    presenter.keyboardPrevPage();
    presenter.flush();
    view.assertKeyboardSelectedRowEmpty();

    // keyboardNextPage.
    presenter.keyboardNextPage();
    presenter.flush();
    view.assertKeyboardSelectedRowEmpty();

    // keyboardHome.
    presenter.keyboardHome();
    presenter.flush();
    view.assertKeyboardSelectedRowEmpty();

    // keyboardEnd.
    presenter.keyboardEnd();
    presenter.flush();
    view.assertKeyboardSelectedRowEmpty();
  }

  public void testKeyboardNavigationIncreaseRange() {
    int pageStart = 150;
    int pageSize = 10;
    int increment = HasDataPresenter.PAGE_INCREMENT;
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setRowCount(300, true);
    presenter.setVisibleRange(new Range(pageStart, pageSize));
    populatePresenter(presenter);
    presenter.flush();
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);

    // keyboardPrev in middle.
    presenter.setKeyboardSelectedRow(1, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(1, true);
    assertTrue(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(0, true);

    // keyboardPrev at beginning.
    assertTrue(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    pageStart -= increment;
    pageSize += increment;
    assertEquals(increment - 1, presenter.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardNext in middle.
    presenter.setKeyboardSelectedRow(pageSize - 2, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(increment - 1, false);
    view.assertKeyboardSelectedRow(pageSize - 2, true);
    assertTrue(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    presenter.flush();
    assertEquals(pageSize - 1, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(pageSize - 2, false);
    view.assertKeyboardSelectedRow(pageSize - 1, true);

    // keyboardNext at end.
    assertTrue(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    pageSize += increment;
    assertEquals(pageSize - increment, presenter.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardPrevPage within range.
    presenter.setKeyboardSelectedRow(increment, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(pageSize - increment, false);
    view.assertKeyboardSelectedRow(increment, true);
    presenter.keyboardPrevPage();
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(increment, false);
    view.assertKeyboardSelectedRow(0, true);
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardPrevPage outside range.
    presenter.keyboardPrevPage();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    pageStart -= increment;
    pageSize += increment;
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardNextPage inside range.
    presenter.keyboardNextPage();
    presenter.flush();
    assertEquals(increment, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(increment, true);
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardNextPage outside range.
    presenter.setKeyboardSelectedRow(pageSize - 1, false, false);
    presenter.flush();
    view.assertKeyboardSelectedRow(increment, false);
    view.assertKeyboardSelectedRow(pageSize - 1, true);
    presenter.keyboardNextPage();
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    pageSize += increment;
    assertEquals(pageSize - 1, presenter.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardHome.
    presenter.keyboardHome();
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    pageSize += pageStart;
    pageStart = 0;
    assertEquals(0, presenter.getKeyboardSelectedRow());
    assertEquals(new Range(pageStart, pageSize), presenter.getVisibleRange());

    // keyboardPrev at first row.
    assertFalse(presenter.hasKeyboardPrev());
    presenter.keyboardPrev();
    presenter.flush();
    view.assertKeyboardSelectedRowEmpty();

    // keyboardEnd.
    presenter.keyboardEnd();
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(299, presenter.getKeyboardSelectedRow());
    assertEquals(new Range(0, 300), presenter.getVisibleRange());

    // keyboardNext at last row.
    assertFalse(presenter.hasKeyboardNext());
    presenter.keyboardNext();
    presenter.flush();
    view.assertKeyboardSelectedRowEmpty();
  }

  /**
   * Test that we can detect an infinite loop caused by user code updating the
   * presenter every time we try to resolve state.
   */
  public void testLoopDetection() {
    HasData<String> listView = new MockHasData<String>();
    final MockView<String> view = new MockView<String>();
    final HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10, null);
    presenter.setSelectionModel(new SingleSelectionModel<String>() {
      @Override
      public boolean isSelected(String object) {
        // This selection model triggers a selection change event every time it
        // is accessed, which puts the presenter in a pending state.
        SelectionChangeEvent.fire(this);
        return super.isSelected(object);
      }
    });

    populatePresenter(presenter);
    try {
      presenter.flush();
      fail("Expected IllegalStateException because of infinite loop.");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  /**
   * Test that pending command execute in a finally loop.
   */
  public void testPendingCommand() {
    HasData<String> listView = new MockHasData<String>();
    final MockView<String> view = new MockView<String>();
    final HasDataPresenter<String> presenter = new HasDataPresenter<String>(
        listView, view, 10, null);

    // Data should not be pushed to the view until the pending command executes.
    populatePresenter(presenter);
    assertTrue(presenter.hasPendingState());
    view.assertReplaceAllChildrenCalled(false);

    // The pending command is scheduled. Wait for it to execute.
    delayTestFinish(5000);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        assertFalse(presenter.hasPendingState());
        view.assertReplaceAllChildrenCalled(true);
        finishTest();
      }
    });
  }

  public void testRedraw() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Initialize some data.
    presenter.setRowCount(10, true);
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Redraw.
    presenter.redraw();
    view.assertReplaceAllChildrenCalled(false);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetKeyboardSelectedRowBound() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(0, 10));
    populatePresenter(presenter);
    presenter.flush();

    // The default is ENABLED.
    assertEquals(KeyboardSelectionPolicy.ENABLED,
        presenter.getKeyboardSelectionPolicy());

    // Change to bound with paging.
    presenter.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.CHANGE_PAGE);

    // Add a selection model.
    MockSelectionModel<String> model = new MockSelectionModel<String>(null);
    presenter.setSelectionModel(model);
    presenter.flush();
    assertEquals(0, model.getSelectedSet().size());

    // Clear the data and push new data. This should not select an item because
    // there has not been any user interaction.
    presenter.setRowCount(0, true);
    presenter.flush();
    presenter.setRowCount(100, false);
    presenter.flush();
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(0, model.getSelectedSet().size());

    // Select an element.
    presenter.setKeyboardSelectedRow(5, false, false);
    presenter.flush();
    assertEquals(1, model.getSelectedSet().size());
    assertTrue(model.isSelected("test 5"));

    // Select another element.
    presenter.setKeyboardSelectedRow(9, false, false);
    presenter.flush();
    assertEquals(1, model.getSelectedSet().size());
    assertTrue(model.isSelected("test 9"));

    // Select an element on another page.
    presenter.setKeyboardSelectedRow(11, false, false);
    presenter.flush();
    // The previous value is still selected because we don't have new data.
    assertEquals(1, model.getSelectedSet().size());
    assertTrue(model.isSelected("test 9"));
    populatePresenter(presenter);
    presenter.flush();
    // Once data is pushed, the selection model should be populated.
    assertEquals(1, model.getSelectedSet().size());
    assertTrue(model.isSelected("test 10"));
  }

  /**
   * Test that we only get one selection event when keyboard selection changes.
   */
  public void testSetKeyboardSelectedRowFiresOneSelectionEvent() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(0, 10));
    populatePresenter(presenter);
    presenter.flush();

    // Bind keyboard selection to the selection model.
    presenter.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.CHANGE_PAGE);

    // Add a selection model.
    MockSingleSelectionModel<String> model = new MockSingleSelectionModel<String>(
        null);
    presenter.setSelectionModel(model);
    presenter.flush();
    assertNull(model.getSelectedObject());

    // Add an selection event handler.
    MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    // Select an element.
    presenter.setKeyboardSelectedRow(5, false, false);
    presenter.flush();
    model.fireSelectionChangeEvent();
    handler.assertEventFired(true);
    assertEquals("test 5", model.getSelectedObject());

    // Select another element.
    presenter.setKeyboardSelectedRow(9, false, false);
    presenter.flush();
    model.fireSelectionChangeEvent();
    handler.assertEventFired(true);
    assertEquals("test 9", model.getSelectedObject());

    // Select an element on another page.
    presenter.setKeyboardSelectedRow(11, false, false);
    presenter.flush();
    model.fireSelectionChangeEvent();
    // The previous value is still selected because we don't have new data.
    handler.assertEventFired(false);
    assertEquals("test 9", model.getSelectedObject());
    populatePresenter(presenter);
    presenter.flush();
    model.fireSelectionChangeEvent();
    // Once data is pushed, the selection model should be populated.
    assertEquals("test 10", model.getSelectedObject());
    handler.assertEventFired(true);
  }

  public void testSetKeyboardSelectedRowChangePage() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(10, 10));
    populatePresenter(presenter);
    presenter.flush();

    // Default policy is CHANGE_PAGE.
    assertEquals(KeyboardPagingPolicy.CHANGE_PAGE,
        presenter.getKeyboardPagingPolicy());

    // Default to row 0.
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRowEmpty();

    // Move to middle.
    presenter.setKeyboardSelectedRow(1, false, false);
    assertEquals("test 11", presenter.getKeyboardSelectedRowValue());
    presenter.flush();
    assertEquals(1, presenter.getKeyboardSelectedRow());
    assertEquals("test 11", presenter.getKeyboardSelectedRowValue());
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(1, true);

    // Move to same row (should not early out).
    presenter.setKeyboardSelectedRow(1, false, true);
    assertEquals("test 11", presenter.getKeyboardSelectedRowValue());
    presenter.flush();
    assertEquals(1, presenter.getKeyboardSelectedRow());
    assertEquals("test 11", presenter.getKeyboardSelectedRowValue());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(1, true);

    // Move to last row.
    presenter.setKeyboardSelectedRow(9, false, false);
    assertEquals("test 19", presenter.getKeyboardSelectedRowValue());
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    assertEquals("test 19", presenter.getKeyboardSelectedRowValue());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(9, true);
    assertEquals(10, presenter.getVisibleRange().getStart());
    assertEquals(10, presenter.getVisibleRange().getLength());

    // Move to next page.
    presenter.setKeyboardSelectedRow(10, false, false);
    populatePresenter(presenter);
    assertNull(presenter.getKeyboardSelectedRowValue());
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    assertEquals("test 20", presenter.getKeyboardSelectedRowValue());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(20, presenter.getVisibleRange().getStart());
    assertEquals(10, presenter.getVisibleRange().getLength());

    // Negative index.
    presenter.setKeyboardSelectedRow(-1, false, false);
    populatePresenter(presenter);
    assertNull(presenter.getKeyboardSelectedRowValue());
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    assertEquals("test 19", presenter.getKeyboardSelectedRowValue());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(10, presenter.getVisibleRange().getStart());
    assertEquals(10, presenter.getVisibleRange().getLength());
  }

  public void testSetKeyboardSelectedRowCurrentPage() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(10, 10));
    populatePresenter(presenter);
    presenter.flush();
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);

    // Default to row 0.
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRowEmpty();

    // Negative index (should remain at index 0).
    presenter.setKeyboardSelectedRow(-1, false, false);
    presenter.flush();
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(0, true);

    // Move to middle.
    presenter.setKeyboardSelectedRow(1, false, false);
    presenter.flush();
    assertEquals(1, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(1, true);

    // Move to same row (should not early out).
    presenter.setKeyboardSelectedRow(1, false, true);
    presenter.flush();
    assertEquals(1, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(1, true);

    // Move to last row.
    presenter.setKeyboardSelectedRow(9, false, false);
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(9, true);

    // Move to next page (confined to page).
    presenter.setKeyboardSelectedRow(10, false, false);
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(9, false);
    view.assertKeyboardSelectedRow(9, true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(10, presenter.getVisibleRange().getStart());
    assertEquals(10, presenter.getVisibleRange().getLength());
  }

  public void testSetKeyboardSelectedRowDisabled() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(10, 10));
    populatePresenter(presenter);
    presenter.flush();
    presenter.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    assertEquals(-1, presenter.getKeyboardSelectedRow());
    assertNull(presenter.getKeyboardSelectedRowValue());
    view.assertKeyboardSelectedRowEmpty();

    presenter.setKeyboardSelectedRow(1, false, false);
    presenter.flush();
    assertEquals(-1, presenter.getKeyboardSelectedRow());
    assertNull(presenter.getKeyboardSelectedRowValue());
    view.assertKeyboardSelectedRowEmpty();
  }

  public void testSetKeyboardSelectedRowIncreaseRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(10, 10));
    populatePresenter(presenter);
    presenter.flush();
    presenter.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);
    int pageSize = presenter.getVisibleRange().getLength();

    // Default to row 0.
    assertEquals(0, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRowEmpty();

    // Move to middle.
    presenter.setKeyboardSelectedRow(1, false, false);
    presenter.flush();
    assertEquals(1, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(0, false);
    view.assertKeyboardSelectedRow(1, true);

    // Move to same row (should not early out).
    presenter.setKeyboardSelectedRow(1, false, true);
    presenter.flush();
    assertEquals(1, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(1, true);

    // Move to last row.
    presenter.setKeyboardSelectedRow(9, false, false);
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertKeyboardSelectedRow(1, false);
    view.assertKeyboardSelectedRow(9, true);
    assertEquals(10, presenter.getVisibleRange().getStart());
    assertEquals(pageSize, presenter.getVisibleRange().getLength());

    // Move to next page.
    presenter.setKeyboardSelectedRow(10, false, false);
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(10, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(10, presenter.getVisibleRange().getStart());
    pageSize += HasDataPresenter.PAGE_INCREMENT;
    assertEquals(pageSize, presenter.getVisibleRange().getLength());

    // Negative index near index 0.
    presenter.setKeyboardSelectedRow(-1, false, false);
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(9, presenter.getKeyboardSelectedRow());
    view.assertReplaceAllChildrenCalled(true);
    view.assertKeyboardSelectedRowEmpty();
    assertEquals(0, presenter.getVisibleRange().getStart());
    pageSize += 10;
    assertEquals(pageSize, presenter.getVisibleRange().getLength());
  }

  public void testSetRowCount() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    view.assertLoadingState(null);

    // Set size to 100.
    presenter.setRowCount(100, true);
    assertEquals(100, presenter.getRowCount());
    assertTrue(presenter.isRowCountExact());
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADING);

    // Set size to 0, but not exact. The state is loading until we know there is
    // no data.
    presenter.setRowCount(0, false);
    assertEquals(0, presenter.getRowCount());
    assertFalse(presenter.isRowCountExact());
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADING);

    // Set size to 0 and exact. Now we know the list is empty.
    presenter.setRowCount(0, true);
    assertEquals(0, presenter.getRowCount());
    assertTrue(presenter.isRowCountExact());
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetRowCountNoBoolean() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

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
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    view.assertLoadingState(null);

    // Initialize some data.
    presenter.setRowCount(10, true);
    presenter.setVisibleRange(new Range(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    populatePresenter(presenter);
    presenter.flush();
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Trim the size.
    presenter.setRowCount(8, true);
    assertEquals(8, presenter.getRowCount());
    assertTrue(presenter.isRowCountExact());
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(8, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetRowData() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(5, 10));
    presenter.flush();
    view.assertLastHtml("start=5,size=0");
    view.assertLoadingState(LoadingState.LOADING);

    // Page range same as data range.
    List<String> expectedData = createData(5, 10);
    presenter.setRowData(5, createData(5, 10));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Page range contains data range.
    expectedData.set(2, "test 100");
    expectedData.set(3, "test 101");
    presenter.setRowData(7, createData(100, 2));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=7,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range overlaps page start.
    expectedData.set(0, "test 202");
    expectedData.set(1, "test 203");
    presenter.setRowData(3, createData(200, 4));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=5,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range overlaps page end.
    expectedData.set(8, "test 300");
    expectedData.set(9, "test 301");
    presenter.setRowData(13, createData(300, 4));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=13,size=2");
    assertEquals(10, view.getChildCount());
    view.assertLoadingState(LoadingState.LOADED);

    // Data range contains page range.
    expectedData = createData(400, 20).subList(2, 12);
    presenter.setRowData(3, createData(400, 20));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
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
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Set the initial data size.
    presenter.setRowCount(10, true);
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADING);

    // Set the data within the range.
    presenter.setRowData(0, createData(0, 10));
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADED);

    // Set the data past the range.
    presenter.setRowData(5, createData(5, 10));
    assertEquals(15, presenter.getRowCount());
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Setting an empty list that starts on the page start should pass through to
   * the view.
   */
  public void testSetRowValuesEmptySet() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Set the initial data size.
    presenter.setRowCount(10, true);
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADING);

    // Set an empty list of row values.
    presenter.setRowData(0, createData(0, 0));
    presenter.flush();
    view.assertLoadingState(LoadingState.LOADING);
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
  }

  public void testSetRowValuesOutsideRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    presenter.setVisibleRange(new Range(5, 10));
    presenter.flush();
    view.assertLastHtml("start=5,size=0");
    view.assertLoadingState(LoadingState.LOADING);

    // Page range same as data range.
    List<String> expectedData = createData(5, 10);
    presenter.setRowData(5, createData(5, 10));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Data range past page end.
    presenter.setRowData(15, createData(15, 5));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);

    // Data range before page start.
    presenter.setRowData(0, createData(0, 5));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADED);
  }

  /**
   * Test that modifying more than 30% of the rows forces a full redraw.
   */
  public void testSetRowValuesRequiresRedraw() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 100, null);

    // Initialize 100% of the rows.
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);

    // Modify 30% of the rows.
    presenter.setRowData(0, createData(0, 30));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);

    // Modify 31% of the rows.
    presenter.setRowData(0, createData(0, 31));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);

    /*
     * Modify 4 rows in a 5 row table. This should NOT require a redraw because
     * it is less than the minimum threshold.
     */
    presenter.setRowCount(5, true);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    presenter.setRowData(0, createData(0, 4));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
  }

  /**
   * As an optimization, the presenter does not replace the rendered string if
   * the rendered string is identical to the previously rendered string. This is
   * useful for tables that refresh on an interval.
   */
  public void testSetRowValuesSameContents() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    view.assertLoadingState(null);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    presenter.setRowData(0, createData(0, 10));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Set the same data over the entire range.
    presenter.setRowData(0, createData(0, 10));
    presenter.flush();
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
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    view.assertLoadingState(null);

    List<String> expectedData = createData(5, 3);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    expectedData.add(0, null);
    presenter.setVisibleRange(new Range(0, 10));
    presenter.setRowData(5, createData(5, 3));
    assertPresenterRowData(expectedData, presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetSelectionModel() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);
    assertNull(presenter.getSelectionModel());

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    populatePresenter(presenter);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");

    // Set the selection model.
    SelectionModel<String> model = new MockSelectionModel<String>(null);
    model.setSelected("test 0", true);
    presenter.setSelectionModel(model);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=0,size=1");

    // Select something.
    model.setSelected("test 2", true);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=2,size=1");

    // Set selection model to null.
    presenter.setSelectionModel(null);
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
    view.assertLastHtml("start=0,size=1");
    view.assertLastHtml("start=2,size=1");
    view.assertLastHtml(null);
  }

  public void testSetVisibleRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Set the range the first time.
    presenter.setVisibleRange(new Range(0, 100));
    assertEquals(new Range(0, 100), presenter.getVisibleRange());
    assertEquals(0, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADING);

    // Set the range to the same value.
    presenter.setVisibleRange(new Range(0, 100));
    assertEquals(new Range(0, 100), presenter.getVisibleRange());
    assertEquals(0, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.LOADING);

    // Set the start to a negative value.
    try {
      presenter.setVisibleRange(new Range(-1, 100));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // Set the length to a negative value.
    try {
      presenter.setVisibleRange(new Range(0, -100));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  public void testSetVisibleRangeAndClearDataDifferentRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

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
    assertEquals(10, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=5,size=10");
    view.assertLoadingState(LoadingState.LOADED);
    assertEquals(1, events.size());

    // Set a different range.
    presenter.setVisibleRangeAndClearData(new Range(0, 10), false);
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(0, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=0");
    view.assertLoadingState(LoadingState.LOADING);
    assertEquals(2, events.size());
  }

  public void testSetVisibleRangeAndClearDataSameRange() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

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
    assertEquals(10, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);
    assertEquals(0, events.size());

    // Set the same range.
    presenter.setVisibleRangeAndClearData(new Range(0, 10), false);
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(0, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=0");
    view.assertLoadingState(LoadingState.LOADING);
    assertEquals(0, events.size());
  }

  public void testSetVisibleRangeAndClearDataSameRangeForceEvent() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

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
    assertEquals(10, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);
    assertEquals(0, events.size());

    // Set the same range.
    presenter.setVisibleRangeAndClearData(new Range(0, 10), true);
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    assertEquals(0, presenter.getVisibleItemCount());
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=0");
    view.assertLoadingState(LoadingState.LOADING);
    assertEquals(1, events.size());
  }

  public void testSetVisibleRangeDecreasePageSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Decrease the page size.
    presenter.setVisibleRange(new Range(0, 8));
    assertEquals(new Range(0, 8), presenter.getVisibleRange());
    assertEquals(8, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=8");
    view.assertLoadingState(LoadingState.LOADED);
  }

  public void testSetVisibleRangeDecreasePageStart() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Initialize some data.
    presenter.setVisibleRange(new Range(10, 30));
    assertEquals(new Range(10, 30), presenter.getVisibleRange());
    presenter.setRowData(10, createData(0, 10));
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=10,size=10");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);

    // Decrease the start index.
    presenter.setVisibleRange(new Range(8, 30));
    assertEquals(new Range(8, 30), presenter.getVisibleRange());
    assertEquals(12, presenter.getVisibleItemCount());
    assertEquals(null, presenter.getVisibleItem(0));
    assertEquals(null, presenter.getVisibleItem(1));
    assertEquals("test 0", presenter.getVisibleItem(2));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=8,size=12");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRangeIncreasePageSize() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 10));
    assertEquals(new Range(0, 10), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.LOADED);

    // Increase the page size.
    presenter.setVisibleRange(new Range(0, 20));
    assertEquals(new Range(0, 20), presenter.getVisibleRange());
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml(null);
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRangeIncreasePageStart() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Initialize some data.
    presenter.setVisibleRange(new Range(0, 20));
    assertEquals(new Range(0, 20), presenter.getVisibleRange());
    presenter.setRowData(0, createData(0, 10));
    assertEquals(10, presenter.getVisibleItemCount());
    assertEquals("test 0", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=0,size=10");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);

    // Increase the start index.
    presenter.setVisibleRange(new Range(2, 20));
    assertEquals(new Range(2, 20), presenter.getVisibleRange());
    assertEquals(8, presenter.getVisibleItemCount());
    assertEquals("test 2", presenter.getVisibleItem(0));
    presenter.flush();
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);
    view.assertLastHtml("start=2,size=8");
    view.assertLoadingState(LoadingState.PARTIALLY_LOADED);
  }

  public void testSetVisibleRangeInts() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    try {
      presenter.setVisibleRange(0, 100);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected.
    }
  }

  /**
   * Test that the view is correctly updated if we move the page start back and
   * forth in the same render loop.
   */
  public void testSetVisibleRangeResetPageStart() {
    HasData<String> listView = new MockHasData<String>();
    MockView<String> view = new MockView<String>();
    HasDataPresenter<String> presenter = new HasDataPresenter<String>(listView,
        view, 10, null);

    // Initialize the view.
    populatePresenter(presenter);
    presenter.flush();
    view.assertLastHtml("start=0,size=10");
    view.assertReplaceAllChildrenCalled(true);
    view.assertReplaceChildrenCalled(false);

    // Move pageStart to 2, then back to 0.
    presenter.setVisibleRange(new Range(2, 8));
    presenter.setVisibleRange(new Range(0, 10));
    presenter.flush();
    view.assertLastHtml("start=0,size=2");
    view.assertReplaceAllChildrenCalled(false);
    view.assertReplaceChildrenCalled(true);
  }

  /**
   * Assert that the expected List of values matches the row data in the
   * specified {@link HasDataPresenter}.
   * 
   * @param <T> the data type
   * @param expected the expected values
   * @param presenter the presenter
   */
  private <T> void assertPresenterRowData(List<T> expected,
      HasDataPresenter<T> presenter) {
    assertEquals(expected.size(), presenter.getVisibleItemCount());
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), presenter.getVisibleItem(i));
    }
  }

  /**
   * Assert that the specified set contains specified values in order.
   * 
   * @param <T> the data type
   * @param list the list to check
   * @param values the expected values
   */
  private <T> void assertListContains(List<T> list, T... values) {
    assertEquals(values.length, list.size());
    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], list.get(i));
    }
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

  /**
   * Populate the entire range of a presenter.
   * 
   * @param presenter the presenter
   */
  private void populatePresenter(HasDataPresenter<String> presenter) {
    Range range = presenter.getVisibleRange();
    int start = range.getStart();
    int length = range.getLength();
    presenter.setRowData(start, createData(start, length));
  }

  /**
   * Test that the presenter can gracefully handle a view or
   * {@link SelectionModel} that throws an exception.
   * 
   * @param presenter the presenter to test
   */
  private void testPresenterWithBadUserCode(HasDataPresenter<String> presenter) {
    // Render some data with an exception.
    try {
      populatePresenter(presenter);
      presenter.setKeyboardSelectedRow(0, false, false);
      presenter.flush();
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // Expected.
    }

    // Render additional data with an exception.
    try {
      presenter.setVisibleRange(new Range(10, 10));
      populatePresenter(presenter);
      presenter.setKeyboardSelectedRow(1, false, false);
      presenter.flush();
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      /*
       * Expected. If we do not get a NullPointerException, then we are stuck in
       * the rendering loop. We should not get an IllegalStateException from the
       * rendering loop if the presenter fails gracefully.
       */
    }
  }
}
