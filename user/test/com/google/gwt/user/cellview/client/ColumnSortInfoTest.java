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
 * Tests for {@link ColumnSortInfo}.
 */
public class ColumnSortInfoTest extends TestCase {

  public void testAccessors() {
    Column<String, String> column = new IdentityColumn<String>(new TextCell());
    ColumnSortInfo info = new ColumnSortInfo(column, true);
    assertEquals(column, info.getColumn());
    assertTrue(info.isAscending());
  }

  public void testEquals() {
    // Test equals.
    Column<String, String> column0 = new IdentityColumn<String>(new TextCell());
    ColumnSortInfo info0a = new ColumnSortInfo(column0, true);
    ColumnSortInfo info0b = new ColumnSortInfo(column0, true);
    assertTrue(info0a.equals(info0b));
    assertTrue(info0b.equals(info0a));
    assertEquals(info0a.hashCode(), info0b.hashCode());

    // Test null.
    assertFalse(info0a.equals(null));

    // Test different object.
    assertFalse(info0a.equals("not a ColumnSortInfo"));

    // Test different sort order.
    ColumnSortInfo info0desc = new ColumnSortInfo(column0, false);
    assertFalse(info0a.equals(info0desc));
    assertFalse(info0desc.equals(info0a));
    assertTrue(info0a.hashCode() != info0desc.hashCode());

    // Test different column.
    Column<String, String> column1 = new IdentityColumn<String>(new TextCell());
    ColumnSortInfo info1 = new ColumnSortInfo(column1, true);
    assertFalse(info0a.equals(info1));
    assertFalse(info1.equals(info0a));
    assertTrue(info0a.hashCode() != info1.hashCode());
  }
}