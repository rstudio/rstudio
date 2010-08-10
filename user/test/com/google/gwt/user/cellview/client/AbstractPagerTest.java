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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.MockHasData;
import com.google.gwt.view.client.Range;

/**
 * Tests for {@link AbstractPager}.
 */
public class AbstractPagerTest extends GWTTestCase {

  /**
   * Mock {@link AbstractPager} used for testing.
   */
  private class MockPager extends AbstractPager {
    @Override
    protected void onRangeOrRowCountChanged() {
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  public void testFirstPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.firstPage();

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(14, 20);
    pager.firstPage();
    assertEquals(new Range(0, 20), view.getVisibleRange());
  }

  public void testGetPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    assertEquals(-1, pager.getPage());

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    // Exact page.
    view.setVisibleRange(0, 20);
    assertEquals(0, pager.getPage());
    view.setVisibleRange(200, 20);
    assertEquals(10, pager.getPage());

    // Inexact page.
    view.setVisibleRange(1, 20);
    assertEquals(1, pager.getPage());
    view.setVisibleRange(205, 20);
    assertEquals(11, pager.getPage());
  }

  public void testGetPageCount() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    assertEquals(-1, pager.getPageCount());

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    // Perfect count.
    view.setVisibleRange(0, 20);
    view.setRowCount(100, true);
    assertEquals(5, pager.getPageCount());

    // Imperfect page.
    view.setRowCount(105, true);
    assertEquals(6, pager.getPageCount());
  }

  public void testHasNextPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    assertFalse(pager.hasNextPage());

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(0, 20);
    view.setRowCount(20, true);
    assertFalse(pager.hasNextPage());
    assertFalse(pager.hasNextPages(1));

    view.setRowCount(105, true);
    assertTrue(pager.hasNextPage());
    assertTrue(pager.hasNextPages(5));
    assertFalse(pager.hasNextPages(6));
  }

  public void testHasPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    assertFalse(pager.hasPage(0));

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(0, 20);
    view.setRowCount(105, true);
    assertTrue(pager.hasPage(0));
    assertTrue(pager.hasPage(5));
    assertFalse(pager.hasPage(6));
  }

  public void testHasPreviousPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    assertFalse(pager.hasPreviousPage());

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setRowCount(105, true);
    view.setVisibleRange(0, 20);
    assertFalse(pager.hasPreviousPage());
    assertFalse(pager.hasPreviousPages(1));

    view.setVisibleRange(40, 20);
    assertTrue(pager.hasPreviousPage());
    assertTrue(pager.hasPreviousPages(2));
    assertFalse(pager.hasPreviousPages(3));

    view.setVisibleRange(41, 20);
    assertTrue(pager.hasPreviousPage());
    assertTrue(pager.hasPreviousPages(3));
    assertFalse(pager.hasPreviousPages(4));
  }

  public void testLastPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.lastPage();

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(14, 20);
    view.setRowCount(105, true);
    pager.lastPage();
    assertEquals(new Range(100, 20), view.getVisibleRange());
  }

  public void testLastPageStart() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.lastPageStart();

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    pager.setRangeLimited(false);
    view.setVisibleRange(14, 20);
    view.setRowCount(105, true);
    pager.lastPageStart();
    assertEquals(new Range(85, 20), view.getVisibleRange());
  }

  public void testNextPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.nextPage();

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(10, 20);
    view.setRowCount(105, true);
    pager.nextPage();
    assertEquals(new Range(30, 20), view.getVisibleRange());
  }

  public void testPreviousPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.previousPage();

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(45, 20);
    view.setRowCount(105, true);
    pager.previousPage();
    assertEquals(new Range(25, 20), view.getVisibleRange());
  }

  public void testSetPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.setPage(0);

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(10, 20);
    view.setRowCount(105, true);

    pager.setPage(0);
    assertEquals(new Range(0, 20), view.getVisibleRange());

    pager.setPage(3);
    assertEquals(new Range(60, 20), view.getVisibleRange());

    pager.setPage(5);
    assertEquals(new Range(100, 20), view.getVisibleRange());
  }

  public void testSetPageStart() {
    AbstractPager pager = createPager();

    // Should not throw an error when the view is null.
    assertNull(pager.getView());
    pager.setPageStart(0);

    // Set the view.
    HasRows view = new MockHasData<String>();
    pager.setView(view);

    view.setVisibleRange(10, 20);
    view.setRowCount(105, true);

    pager.setPageStart(0);
    assertEquals(new Range(0, 20), view.getVisibleRange());

    pager.setPageStart(45);
    assertEquals(new Range(45, 20), view.getVisibleRange());

    pager.setPageStart(100);
    assertEquals(new Range(85, 20), view.getVisibleRange());
  }

  public void testSetRangeLimited() {
    AbstractPager pager = createPager();
    HasRows view = new MockHasData<String>();
    pager.setView(view);
    view.setRowCount(110, true);
    view.setVisibleRange(70, 20);

    // Invalid ranges should be constrained by default.
    assertTrue(pager.isRangeLimited());
    view.setRowCount(84, true);
    assertEquals(new Range(64, 20), view.getVisibleRange());

    // Allow invalid ranges.
    pager.setRangeLimited(false);
    assertFalse(pager.isRangeLimited());
    view.setVisibleRange(50, 20);
    view.setRowCount(10, true);
    assertEquals(new Range(50, 20), view.getVisibleRange());
  }

  /**
   * Create the pager to test. The list view should not be set in the pager.
   *
   * @return the pager
   */
  protected AbstractPager createPager() {
    return new MockPager();
  }
}
