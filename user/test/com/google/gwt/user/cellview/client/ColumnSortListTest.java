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
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;

import junit.framework.TestCase;

/**
 * Tests for {@link ColumnSortList}.
 */
public class ColumnSortListTest extends TestCase {

  public void testClear() {
    ColumnSortList list = new ColumnSortList();
    assertEquals(0, list.size());

    list.push(createColumnSortInfo());
    list.push(createColumnSortInfo());
    assertEquals(2, list.size());
    list.clear();
    assertEquals(0, list.size());
  }

  public void testEquals() {
    ColumnSortList list0 = new ColumnSortList();
    ColumnSortList list1 = new ColumnSortList();

    // Compare empty lists.
    assertTrue(list0.equals(list1));
    assertTrue(list1.equals(list0));
    assertEquals(list0.hashCode(), list1.hashCode());

    // Compare with one item.
    ColumnSortInfo info0 = createColumnSortInfo();
    list0.push(info0);
    list1.push(info0);
    assertTrue(list0.equals(list1));
    assertTrue(list1.equals(list0));
    assertEquals(list0.hashCode(), list1.hashCode());

    // Compare different sizes.
    ColumnSortInfo info1 = createColumnSortInfo();
    list0.push(info1);
    assertFalse(list0.equals(list1));
    assertFalse(list1.equals(list0));
    assertFalse(list0.hashCode() == list1.hashCode());
    list1.push(info1); // Make the lists equal again.

    // Compare with different items that equals each other.
    ColumnSortInfo info2a = createColumnSortInfo();
    ColumnSortInfo info2b = new ColumnSortInfo(info2a.getColumn(), info2a.isAscending());
    list0.push(info2a);
    list1.push(info2b);
    assertTrue(list0.equals(list1));
    assertTrue(list1.equals(list0));
    assertEquals(list0.hashCode(), list1.hashCode());

    // Compare same items, but out of order.
    list0.push(info0);
    assertFalse(list0.equals(list1));
    assertFalse(list1.equals(list0));
    assertFalse(list0.hashCode() == list1.hashCode());

    // Compare to null.
    assertFalse(list0.equals(null));
    assertFalse(list1.equals(null));
  }

  public void testInsert() {
    ColumnSortList list = new ColumnSortList();
    assertEquals(0, list.size());

    // Insert into an empty list.
    ColumnSortInfo info0 = createColumnSortInfo();
    list.insert(0, info0);
    assertEquals(1, list.size());
    assertEquals(info0, list.get(0));

    // Insert null.
    try {
      list.insert(0, (ColumnSortInfo) null);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // Insert the same item.
    list.insert(0, info0);
    assertEquals(1, list.size());
    assertEquals(info0, list.get(0));

    // Insert a second item at index 0.
    ColumnSortInfo info1 = createColumnSortInfo();
    list.insert(0, info1);
    assertEquals(2, list.size());
    assertEquals(info1, list.get(0));
    assertEquals(info0, list.get(1));

    // Insert a third item at the last index.
    ColumnSortInfo info2 = createColumnSortInfo();
    list.insert(list.size(), info2);
    assertEquals(3, list.size());
    assertEquals(info1, list.get(0));
    assertEquals(info0, list.get(1));
    assertEquals(info2, list.get(2));

    // Insert item0 again. It should move to the new index.
    list.insert(list.size(), info0);
    assertEquals(3, list.size());
    assertEquals(info1, list.get(0));
    assertEquals(info2, list.get(1));
    assertEquals(info0, list.get(2));
  }

  public void testPushColumn() {
    ColumnSortList list = new ColumnSortList();
    assertEquals(0, list.size());

    // Push an item.
    Column<String, String> col0 = new IdentityColumn<String>(new TextCell());
    ColumnSortInfo item0 = list.push(col0);
    assertEquals(1, list.size());
    assertEquals(item0, list.get(0));
    assertEquals(col0, list.get(0).getColumn());
    assertTrue(list.get(0).isAscending());

    // Push the same item. Should change sort order.
    ColumnSortInfo item0desc = list.push(col0);
    assertEquals(1, list.size());
    assertEquals(item0desc, list.get(0));
    assertEquals(col0, list.get(0).getColumn());
    assertFalse(list.get(0).isAscending());

    // Push a second item.
    Column<String, String> col1 = new IdentityColumn<String>(new TextCell());
    list.push(col1);
    assertEquals(2, list.size());
    assertEquals(col1, list.get(0).getColumn());
    assertTrue(list.get(0).isAscending());
    assertEquals(col0, list.get(1).getColumn());
    assertFalse(list.get(1).isAscending());

    // Push a third item.
    Column<String, String> col2 = new IdentityColumn<String>(new TextCell());
    list.push(col2);
    assertEquals(3, list.size());
    assertEquals(col2, list.get(0).getColumn());
    assertTrue(list.get(0).isAscending());
    assertEquals(col1, list.get(1).getColumn());
    assertTrue(list.get(1).isAscending());
    assertEquals(col0, list.get(2).getColumn());
    assertFalse(list.get(2).isAscending());

    // Push col0 again. Should move back to the front in ascending order.
    list.push(col0);
    assertEquals(3, list.size());
    assertEquals(col0, list.get(0).getColumn());
    assertTrue(list.get(0).isAscending());
    assertEquals(col2, list.get(1).getColumn());
    assertTrue(list.get(1).isAscending());
    assertEquals(col1, list.get(2).getColumn());
    assertTrue(list.get(2).isAscending());
  }

  /**
   * Test pushing a column with a default sort order of descending.
   */
  public void testPushColumnDescending() {
    ColumnSortList list = new ColumnSortList();
    assertEquals(0, list.size());

    // Push a column.
    Column<String, String> col0 = new IdentityColumn<String>(new TextCell());
    col0.setDefaultSortAscending(false);
    ColumnSortInfo item0 = list.push(col0);
    assertEquals(1, list.size());
    assertEquals(item0, list.get(0));
    assertEquals(col0, list.get(0).getColumn());
    assertFalse(list.get(0).isAscending());

    // Push the same item. Should change sort order.
    ColumnSortInfo item0desc = list.push(col0);
    assertEquals(1, list.size());
    assertEquals(item0desc, list.get(0));
    assertEquals(col0, list.get(0).getColumn());
    assertTrue(list.get(0).isAscending());
  }

  public void testPushColumnSortInfo() {
    ColumnSortList list = new ColumnSortList();
    assertEquals(0, list.size());

    // Push null.
    try {
      list.push((ColumnSortInfo) null);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // Push an item.
    ColumnSortInfo info0 = createColumnSortInfo();
    list.push(info0);
    assertEquals(1, list.size());
    assertEquals(info0, list.get(0));

    // Push the same item.
    list.push(info0);
    assertEquals(1, list.size());
    assertEquals(info0, list.get(0));

    // Push a second item.
    ColumnSortInfo info1 = createColumnSortInfo();
    list.push(info1);
    assertEquals(2, list.size());
    assertEquals(info1, list.get(0));
    assertEquals(info0, list.get(1));

    // Push a third item.
    ColumnSortInfo info2 = createColumnSortInfo();
    list.push(info2);
    assertEquals(3, list.size());
    assertEquals(info2, list.get(0));
    assertEquals(info1, list.get(1));
    assertEquals(info0, list.get(2));

    // Push item0 again. Should move back to the front
    list.push(info0);
    assertEquals(3, list.size());
    assertEquals(info0, list.get(0));
    assertEquals(info2, list.get(1));
    assertEquals(info1, list.get(2));

    // Push a fourth item with the same column as item1. Should remove item1.
    ColumnSortInfo info1b = new ColumnSortInfo(info1.getColumn(), false);
    list.push(info1b);
    assertEquals(3, list.size());
    assertEquals(info1b, list.get(0));
    assertEquals(info0, list.get(1));
    assertEquals(info2, list.get(2));
  }

  /**
   * Verify that the Column can be null.
   */
  public void testPushNullColumn() {
    ColumnSortList list = new ColumnSortList();
    assertEquals(0, list.size());

    // Push an null column.
    ColumnSortInfo info0 = list.push((Column<?, ?>) null);
    assertEquals(1, list.size());
    assertNull(info0.getColumn());
    assertTrue(info0.isAscending());

    // Push null again.
    ColumnSortInfo info1 = list.push((Column<?, ?>) null);
    assertEquals(1, list.size());
    assertNull(info1.getColumn());
    assertFalse(info1.isAscending());

    // Push a non-null value.
    ColumnSortInfo info2 = createColumnSortInfo();
    list.push(info2);
    assertEquals(2, list.size());
    assertNull(list.get(1).getColumn());

    // Push null again.
    list.push((Column<?, ?>) null);
    assertEquals(2, list.size());
    assertNull(list.get(0).getColumn());
    assertEquals(info2, list.get(1));
  }

  public void testRemove() {
    ColumnSortList list = new ColumnSortList();

    // Remove the only item.
    ColumnSortInfo info = createColumnSortInfo();
    list.push(info);
    assertEquals(1, list.size());
    assertTrue(list.remove(info));
    assertEquals(0, list.size());

    // Remove a middle item.
    ColumnSortInfo info0 = createColumnSortInfo();
    ColumnSortInfo info1 = createColumnSortInfo();
    ColumnSortInfo info2 = createColumnSortInfo();
    list.push(info0);
    list.push(info1);
    list.push(info2);
    assertEquals(3, list.size());
    assertTrue(list.remove(info1));
    assertEquals(2, list.size());
    assertEquals(info2, list.get(0));
    assertEquals(info0, list.get(1));

    // Remove an item that doesn't exist.
    assertFalse(list.remove(createColumnSortInfo()));
  }

  /**
   * Create a {@link ColumnSortInfo} with a unique column and cell.
   * 
   * @return a new {@link ColumnSortInfo}
   */
  private ColumnSortInfo createColumnSortInfo() {
    return new ColumnSortInfo(new IdentityColumn<String>(new TextCell()), true);
  }

}