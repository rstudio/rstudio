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
import com.google.gwt.view.client.MockPagingListView;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;

/**
 * Tests for {@link AbstractPager}.
 */
public class AbstractPagerTest extends GWTTestCase {

  /**
   * Mock {@link PagingListView.Pager} used for testing.
   */
  private class MockPager<T> extends AbstractPager<T> {
    public MockPager(PagingListView<T> view) {
      super(view);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  public void testFirstPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(14, 20);

    pager.firstPage();
    assertEquals(new Range(0, 20), view.getRange());
  }

  public void testGetPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();

    // Exact page.
    view.setRange(0, 20);
    assertEquals(0, pager.getPage());
    view.setRange(200, 20);
    assertEquals(10, pager.getPage());

    // Inexact page.
    view.setRange(1, 20);
    assertEquals(1, pager.getPage());
    view.setRange(205, 20);
    assertEquals(11, pager.getPage());
  }

  public void testGetPageCount() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(0, 20);

    // Perfect count.
    view.setDataSize(100, true);
    assertEquals(5, pager.getPageCount());

    // Imperfect page.
    view.setDataSize(105, true);
    assertEquals(6, pager.getPageCount());
  }

  public void testHasNextPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(0, 20);

    view.setDataSize(20, true);
    assertFalse(pager.hasNextPage());
    assertFalse(pager.hasNextPages(1));

    view.setDataSize(105, true);
    assertTrue(pager.hasNextPage());
    assertTrue(pager.hasNextPages(5));
    assertFalse(pager.hasNextPages(6));
  }

  public void testHasPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(0, 20);
    view.setDataSize(105, true);

    assertTrue(pager.hasPage(0));
    assertTrue(pager.hasPage(5));
    assertFalse(pager.hasPage(6));
  }

  public void testHasPreviousPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setDataSize(105, true);

    view.setRange(0, 20);
    assertFalse(pager.hasPreviousPage());
    assertFalse(pager.hasPreviousPages(1));

    view.setRange(40, 20);
    assertTrue(pager.hasPreviousPage());
    assertTrue(pager.hasPreviousPages(2));
    assertFalse(pager.hasPreviousPages(3));

    view.setRange(41, 20);
    assertTrue(pager.hasPreviousPage());
    assertTrue(pager.hasPreviousPages(3));
    assertFalse(pager.hasPreviousPages(4));
  }

  public void testLastPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(14, 20);
    view.setDataSize(105, true);

    pager.lastPage();
    assertEquals(new Range(100, 20), view.getRange());
  }

  public void testLastPageStart() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    pager.setRangeLimited(false);
    view.setRange(14, 20);
    view.setDataSize(105, true);

    pager.lastPageStart();
    assertEquals(new Range(85, 20), view.getRange());
  }

  public void testNextPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(10, 20);
    view.setDataSize(105, true);

    pager.nextPage();
    assertEquals(new Range(30, 20), view.getRange());
  }

  public void testPreviousPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(45, 20);
    view.setDataSize(105, true);

    pager.previousPage();
    assertEquals(new Range(25, 20), view.getRange());
  }

  public void testSetPage() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(10, 20);
    view.setDataSize(105, true);

    pager.setPage(0);
    assertEquals(new Range(0, 20), view.getRange());

    pager.setPage(3);
    assertEquals(new Range(60, 20), view.getRange());

    pager.setPage(5);
    assertEquals(new Range(100, 20), view.getRange());
  }

  public void testSetPageStart() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setRange(10, 20);
    view.setDataSize(105, true);

    pager.setPageStart(0);
    assertEquals(new Range(0, 20), view.getRange());

    pager.setPageStart(45);
    assertEquals(new Range(45, 20), view.getRange());

    pager.setPageStart(100);
    assertEquals(new Range(85, 20), view.getRange());
  }

  public void testSetRangeLimited() {
    AbstractPager<Void> pager = createPager();
    PagingListView<Void> view = pager.getPagingListView();
    view.setDataSize(110, true);
    view.setRange(70, 20);

    // Invalid ranges should be constrained by default.
    assertTrue(pager.isRangeLimited());
    view.setDataSize(84, true);
    assertEquals(new Range(64, 20), view.getRange());

    // Allow invalid ranges.
    pager.setRangeLimited(false);
    assertFalse(pager.isRangeLimited());
    view.setRange(50, 20);
    view.setDataSize(10, true);
    assertEquals(new Range(50, 20), view.getRange());
  }

  protected <R> AbstractPager<R> createPager() {
    return new MockPager<R>(new MockPagingListView<R>());
  }
}
