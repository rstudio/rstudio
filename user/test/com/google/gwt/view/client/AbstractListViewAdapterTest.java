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
package com.google.gwt.view.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test cases for {@link AbstractListViewAdapter}.
 */
public class AbstractListViewAdapterTest extends GWTTestCase {

  /**
   * A mock {@link AbstractListViewAdapter} used for testing.
   *
   * @param <T> the data type
   */
  static class MockListViewAdapter<T> extends AbstractListViewAdapter<T> {

    private HasData<T> lastChanged;

    public void assertLastRangeChanged(HasData<T> expected) {
      assertEquals(expected, lastChanged);
    }

    public void clearLastRangeChanged() {
      lastChanged = null;
    }

    @Override
    protected void onRangeChanged(HasData<T> view) {
      lastChanged = view;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.view.View";
  }

  public void testAddRemoveView() {
    MockListViewAdapter<String> adapter = new MockListViewAdapter<String>();

    // Test with no views.
    adapter.updateDataSize(10, true);
    adapter.assertLastRangeChanged(null);

    // Add the first view.
    MockHasData<String> view0 = new MockHasData<String>();
    adapter.addView(view0);
    adapter.assertLastRangeChanged(view0);

    // Add a second view.
    MockHasData<String> view1 = new MockHasData<String>();
    adapter.addView(view1);
    adapter.assertLastRangeChanged(view1);

    // Try to remove an invalid view.
    MockHasData<String> invalidView = new MockHasData<String>();
    try {
      adapter.removeView(invalidView);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

    // Remove a valid view.
    adapter.removeView(view1);

    // Make sure the remaining view triggers the delegate.
    adapter.assertLastRangeChanged(view1);
    view0.setVisibleRange(100, 20);
    view1.setVisibleRange(100, 20); // Shouldn't affect the adapter.
    adapter.assertLastRangeChanged(view0);
  }

  public void testGetRanges() {
    AbstractListViewAdapter<String> adapter = createListViewAdapter();

    // No ranges.
    {
      Range[] ranges = adapter.getRanges();
      assertEquals(0, ranges.length);
    }

    // One range.
    {
      MockHasData<String> view0 = new MockHasData<String>();
      view0.setVisibleRange(0, 10);
      adapter.addView(view0);
      Range[] ranges = adapter.getRanges();
      assertEquals(1, ranges.length);
      assertEquals(new Range(0, 10), ranges[0]);
    }

    // Multiple ranges.
    {
      MockHasData<String> view1 = new MockHasData<String>();
      view1.setVisibleRange(3, 10);
      adapter.addView(view1);
      MockHasData<String> view2 = new MockHasData<String>();
      view2.setVisibleRange(30, 35);
      adapter.addView(view2);
      Set<Range> ranges = new HashSet<Range>();
      for (Range range : adapter.getRanges()) {
        ranges.add(range);
      }
      assertEquals(3, ranges.size());
      assertTrue(ranges.contains(new Range(0, 10)));
      assertTrue(ranges.contains(new Range(3, 10)));
      assertTrue(ranges.contains(new Range(30, 35)));
    }
  }

  public void testGetViews() {
    AbstractListViewAdapter<String> adapter = createListViewAdapter();
    MockHasData<String> view0 = new MockHasData<String>();
    MockHasData<String> view1 = new MockHasData<String>();
    assertEquals(0, adapter.getViews().size());

    // Add two views.
    adapter.addView(view0);
    adapter.addView(view1);
    Set<HasData<String>> views = adapter.getViews();
    assertEquals(2, views.size());
    assertTrue(views.contains(view0));
    assertTrue(views.contains(view1));

    // Remove one view.
    adapter.removeView(view0);
    views = adapter.getViews();
    assertEquals(1, views.size());
    assertTrue(views.contains(view1));
  }

  public void testSetKeyProvider() {
    AbstractListViewAdapter<String> adapter = createListViewAdapter();

    // By default, use the object as a key.
    assertNull(adapter.getKeyProvider());
    assertEquals("test", adapter.getKey("test"));
    assertEquals(null, adapter.getKey(null));

    // Defer to the key provider if one is set.
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
      public Object getKey(String item) {
        return item == null ? item : item.toUpperCase();
      }
    };
    adapter.setKeyProvider(keyProvider);
    assertEquals(keyProvider, adapter.getKeyProvider());
    assertEquals("TEST", adapter.getKey("test"));
    assertEquals(null, adapter.getKey(null));
  }

  public void testUpdateDataSize() {
    AbstractListViewAdapter<String> adapter = createListViewAdapter();

    // Test with no views.
    adapter.updateDataSize(10, true);

    // Add the first view.
    MockHasData<String> view0 = new MockHasData<String>();
    adapter.addView(view0);
    adapter.updateDataSize(20, true);
    assertEquals(20, view0.getRowCount());
    assertTrue(view0.isRowCountExact());

    // Add another view.
    MockHasData<String> view1 = new MockHasData<String>();
    adapter.addView(view1);
    adapter.updateDataSize(30, false);
    assertEquals(30, view0.getRowCount());
    assertFalse(view0.isRowCountExact());
    assertEquals(30, view1.getRowCount());
    assertFalse(view1.isRowCountExact());
  }

  public void testUpdateViewData() {
    AbstractListViewAdapter<String> adapter = createListViewAdapter();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(10, 5);
    adapter.addView(view);

    // Data equal to range.
    {
      List<String> values = createData(10, 5);
      adapter.updateViewData(10, 5, values);
      assertEquals(values, view.getLastRowValues());
      assertEquals(new Range(10, 5), view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }

    // Data contained within range.
    {
      List<String> values = createData(12, 2);
      adapter.updateViewData(12, 2, values);
      assertEquals(values, view.getLastRowValues());
      assertEquals(new Range(12, 2), view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }

    // Data before range.
    {
      List<String> values = createData(5, 5);
      adapter.updateViewData(5, 5, values);
      assertNull(view.getLastRowValues());
      assertNull(view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }

    // Data after range.
    {
      List<String> values = createData(15, 5);
      adapter.updateViewData(15, 5, values);
      assertNull(view.getLastRowValues());
      assertNull(view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }

    // Data overlaps entire range.
    {
      List<String> values = createData(5, 15);
      adapter.updateViewData(5, 15, values);
      assertEquals(values.subList(5, 10), view.getLastRowValues());
      assertEquals(new Range(10, 5), view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }

    // Data overlaps start of range.
    {
      List<String> values = createData(5, 7);
      adapter.updateViewData(5, 7, values);
      assertEquals(values.subList(5, 7), view.getLastRowValues());
      assertEquals(new Range(10, 2), view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }

    // Data overlaps end of range.
    {
      List<String> values = createData(13, 5);
      adapter.updateViewData(13, 5, values);
      assertEquals(values.subList(0, 2), view.getLastRowValues());
      assertEquals(new Range(13, 2), view.getLastRowValuesRange());
      view.clearLastRowValuesAndRange();
    }
  }

  public void testUpdateViewDataMultipleViews() {
    AbstractListViewAdapter<String> adapter = createListViewAdapter();
    MockHasData<String> view0 = new MockHasData<String>();
    view0.setVisibleRange(10, 5);
    adapter.addView(view0);
    MockHasData<String> view1 = new MockHasData<String>();
    view1.setVisibleRange(0, 5);
    adapter.addView(view1);
    MockHasData<String> view2 = new MockHasData<String>();
    view2.setVisibleRange(12, 10);
    adapter.addView(view2);

    List<String> values = createData(10, 5);
    adapter.updateViewData(10, 5, values);
    assertEquals(values, view0.getLastRowValues());
    assertEquals(new Range(10, 5), view0.getLastRowValuesRange());
    assertNull(view1.getLastRowValues());
    assertNull(view1.getLastRowValuesRange());
    assertEquals(values.subList(2, 5), view2.getLastRowValues());
    assertEquals(new Range(12, 3), view2.getLastRowValuesRange());
  }

  /**
   * Create an {@link AbstractListViewAdapter} for testing.
   *
   * @return the adapter
   */
  protected AbstractListViewAdapter<String> createListViewAdapter() {
    return new MockListViewAdapter<String>();
  }

  /**
   * Create a list of data for testing.
   *
   * @param start the start index
   * @param length the length
   * @return a list of data
   */
  protected List<String> createData(int start, int length) {
    List<String> toRet = new ArrayList<String>();
    for (int i = 0; i < length; i++) {
      toRet.add("test " + (i + start));
    }
    return toRet;
  }
}
