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

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.view.client.MockHasData;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tests for {@link ColumnSortEvent}.
 */
public class ColumnSortEventTest extends TestCase {

  public void testAccessors() {
    ColumnSortList sortList = new ColumnSortList();
    IdentityColumn<String> col0 = new IdentityColumn<String>(new TextCell());
    IdentityColumn<String> col1 = new IdentityColumn<String>(new TextCell());
    sortList.push(new ColumnSortInfo(col0, true));
    sortList.push(new ColumnSortInfo(col1, false));

    ColumnSortEvent event = new ColumnSortEvent(sortList);
    assertEquals(sortList, event.getColumnSortList());
    assertEquals(col1, event.getColumn());
    assertFalse(event.isSortAscending());
  }

  public void testAsyncHandler() {
    MockHasData<String> hasData = new MockHasData<String>();
    final List<Range> events = new ArrayList<Range>();
    hasData.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      public void onRangeChange(RangeChangeEvent event) {
        events.add(event.getNewRange());
      }
    });
    AsyncHandler handler = new AsyncHandler(hasData);
    assertEquals(0, events.size());

    // Fire an event to the handler.
    ColumnSortList sortList = new ColumnSortList();
    handler.onColumnSort(new ColumnSortEvent(sortList));
    assertEquals(1, events.size());
  }

  public void testListHandler() {
    // Create some unsorted values.
    List<String> values = new ArrayList<String>();
    values.add("b");
    values.add("a");
    values.add("c");

    // Create a handler for the list of values.
    ListHandler<String> handler = new ListHandler<String>(values);
    IdentityColumn<String> col0 = new IdentityColumn<String>(new TextCell());
    Comparator<String> col0Comparator = new Comparator<String>() {
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    };
    handler.setComparator(col0, col0Comparator);
    IdentityColumn<String> col1 = new IdentityColumn<String>(new TextCell());
    handler.setComparator(col1, null);

    // Sort ascending.
    ColumnSortList sortList = new ColumnSortList();
    sortList.push(col0);
    handler.onColumnSort(new ColumnSortEvent(sortList));
    assertEquals("a", values.get(0));
    assertEquals("b", values.get(1));
    assertEquals("c", values.get(2));

    // Sort descending.
    sortList.push(col0); // Switches sort to descending.
    handler.onColumnSort(new ColumnSortEvent(sortList));
    assertEquals("c", values.get(0));
    assertEquals("b", values.get(1));
    assertEquals("a", values.get(2));

    // Null comparator.
    sortList.push(col1);
    assertEquals("c", values.get(0));
    assertEquals("b", values.get(1));
    assertEquals("a", values.get(2));
    
    // Retrieve the comparators.
    assertEquals(col0Comparator, handler.getComparator(col0));
    assertNull(handler.getComparator(col1));
    assertNull(handler.getComparator(new IdentityColumn<String>(
        new TextCell())));
    
    // Create some new unsorted values.
    List<String> newValues = new ArrayList<String>();
    newValues.add("e");
    newValues.add("d");
    newValues.add("f");
    
    // Update the handler to be for the new list of values.
    handler.setList(newValues);
    
    // Sort the new list in ascending order.
    sortList.push(col0);
    handler.onColumnSort(new ColumnSortEvent(sortList));

    // The new values, sorted in ascending order.
    assertEquals("d", newValues.get(0));
    assertEquals("e", newValues.get(1));
    assertEquals("f", newValues.get(2));

    // The old values, still sorted in descending order.
    assertEquals("c", values.get(0));
    assertEquals("b", values.get(1));
    assertEquals("a", values.get(2));
  }
}
