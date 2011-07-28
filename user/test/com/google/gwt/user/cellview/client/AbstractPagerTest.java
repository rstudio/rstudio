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
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;

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

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.firstPage();

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(14, 20);
    pager.firstPage();
    assertEquals(new Range(0, 20), display.getVisibleRange());
  }

  public void testGetPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    assertEquals(-1, pager.getPage());

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    // Exact page.
    display.setVisibleRange(0, 20);
    assertEquals(0, pager.getPage());
    display.setVisibleRange(200, 20);
    assertEquals(10, pager.getPage());

    // Inexact page.
    display.setVisibleRange(1, 20);
    assertEquals(1, pager.getPage());
    display.setVisibleRange(205, 20);
    assertEquals(11, pager.getPage());
  }

  public void testGetPageCount() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    assertEquals(-1, pager.getPageCount());

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    // Perfect count.
    display.setVisibleRange(0, 20);
    display.setRowCount(100, true);
    assertEquals(5, pager.getPageCount());

    // Imperfect page.
    display.setRowCount(105, true);
    assertEquals(6, pager.getPageCount());
  }

  public void testHasNextPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    assertFalse(pager.hasNextPage());

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(0, 20);
    display.setRowCount(20, true);
    assertFalse(pager.hasNextPage());
    assertFalse(pager.hasNextPages(1));

    display.setRowCount(105, true);
    assertTrue(pager.hasNextPage());
    assertTrue(pager.hasNextPages(5));
    assertFalse(pager.hasNextPages(6));
  }

  public void testHasPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    assertFalse(pager.hasPage(0));

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(0, 20);
    display.setRowCount(105, true);
    assertTrue(pager.hasPage(0));
    assertTrue(pager.hasPage(5));
    assertFalse(pager.hasPage(6));
  }

  public void testHasPreviousPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    assertFalse(pager.hasPreviousPage());

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setRowCount(105, true);
    display.setVisibleRange(0, 20);
    assertFalse(pager.hasPreviousPage());
    assertFalse(pager.hasPreviousPages(1));

    display.setVisibleRange(40, 20);
    assertTrue(pager.hasPreviousPage());
    assertTrue(pager.hasPreviousPages(2));
    assertFalse(pager.hasPreviousPages(3));

    display.setVisibleRange(41, 20);
    assertTrue(pager.hasPreviousPage());
    assertTrue(pager.hasPreviousPages(3));
    assertFalse(pager.hasPreviousPages(4));
  }

  public void testLastPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.lastPage();

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(14, 20);
    display.setRowCount(105, true);
    pager.lastPage();
    assertEquals(new Range(100, 20), display.getVisibleRange());
  }

  public void testLastPageStart() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.lastPageStart();

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    pager.setRangeLimited(false);
    display.setVisibleRange(14, 20);
    display.setRowCount(105, true);
    pager.lastPageStart();
    assertEquals(new Range(85, 20), display.getVisibleRange());
  }

  public void testNextPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.nextPage();

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(10, 20);
    display.setRowCount(105, true);
    pager.nextPage();
    assertEquals(new Range(30, 20), display.getVisibleRange());
  }

  public void testPreviousPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.previousPage();

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(45, 20);
    display.setRowCount(105, true);
    pager.previousPage();
    assertEquals(new Range(25, 20), display.getVisibleRange());
  }

  public void testSetDisplay() {
    AbstractPager pager = createPager();
    assertNull(pager.getDisplay());

    // Set display to a value.
    MockHasData<String> display0 = new MockHasData<String>();
    pager.setDisplay(display0);
    assertEquals(display0, pager.getDisplay());
    assertEquals(1, display0.getHandlerCount(RangeChangeEvent.getType()));
    assertEquals(1, display0.getHandlerCount(RowCountChangeEvent.getType()));
    assertNotNull(pager.rangeChangeHandler);
    assertNotNull(pager.rowCountChangeHandler);

    /*
     * Set display to null.
     * 
     * Verify that the handlers are removed.
     */
    pager.setDisplay(null);
    assertNull(pager.getDisplay());
    assertEquals(0, display0.getHandlerCount(RangeChangeEvent.getType()));
    assertEquals(0, display0.getHandlerCount(RowCountChangeEvent.getType()));
    assertNull(pager.rangeChangeHandler);
    assertNull(pager.rowCountChangeHandler);

    /*
     * Set display again.
     * 
     * Verify that the handlers are re-added.
     */
    MockHasData<String> display1 = new MockHasData<String>();
    pager.setDisplay(display1);
    assertEquals(display1, pager.getDisplay());
    assertEquals(1, display1.getHandlerCount(RangeChangeEvent.getType()));
    assertEquals(1, display1.getHandlerCount(RowCountChangeEvent.getType()));
    assertNotNull(pager.rangeChangeHandler);
    assertNotNull(pager.rowCountChangeHandler);
  }

  public void testSetPage() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.setPage(0);

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(10, 20);
    display.setRowCount(105, true);

    pager.setPage(0);
    assertEquals(new Range(0, 20), display.getVisibleRange());

    pager.setPage(3);
    assertEquals(new Range(60, 20), display.getVisibleRange());

    pager.setPage(5);
    assertEquals(new Range(100, 20), display.getVisibleRange());
  }

  public void testSetPageStart() {
    AbstractPager pager = createPager();

    // Should not throw an error when the display is null.
    assertNull(pager.getDisplay());
    pager.setPageStart(0);

    // Set the display.
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);

    display.setVisibleRange(10, 20);
    display.setRowCount(105, true);

    pager.setPageStart(0);
    assertEquals(new Range(0, 20), display.getVisibleRange());

    pager.setPageStart(45);
    assertEquals(new Range(45, 20), display.getVisibleRange());

    pager.setPageStart(100);
    assertEquals(new Range(85, 20), display.getVisibleRange());
  }

  public void testSetRangeLimited() {
    AbstractPager pager = createPager();
    HasRows display = new MockHasData<String>();
    pager.setDisplay(display);
    display.setRowCount(110, true);
    display.setVisibleRange(70, 20);

    // Invalid ranges should be constrained by default.
    assertTrue(pager.isRangeLimited());
    display.setRowCount(84, true);
    assertEquals(new Range(64, 20), display.getVisibleRange());

    // Allow invalid ranges.
    pager.setRangeLimited(false);
    assertFalse(pager.isRangeLimited());
    display.setVisibleRange(50, 20);
    display.setRowCount(10, true);
    assertEquals(new Range(50, 20), display.getVisibleRange());
  }

  /**
   * Create the pager to test. The list display should not be set in the pager.
   *
   * @return the pager
   */
  protected AbstractPager createPager() {
    return new MockPager();
  }
}
