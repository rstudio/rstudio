/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.emultest.java.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Tests <code>TreeSet</code> with a <code>Comparator</code>.
 */
public class TreeSetIntegerTest extends TreeSetTest<Integer> {

  /**
   * Used to test updating a set to make sure it doesn't replace
   * an equal value.
   */
  private static class Record {
    public int key;
    public int extra;
    
    public Record(int key, int extra) {
      this.key = key;
      this.extra = extra;
    }
  }
  
  private static class RecordCompare implements Comparator<Record> {
    // Handle nulls as less than any other key
    public int compare(Record r1, Record r2) {
      if (r1 == null) {
        return r2 == null ? 0 : -1;
      }
      if (r2 == null) {
        return 1;
      }
      return r1.key - r2.key;
    }
  }

  /**
   * Verify nulls are handled properly.
   */
  public void testAdd_null() {
    TreeSet<Record> set = new TreeSet<Record>(new RecordCompare());
    set.add(new Record(10, 1));
    set.add(new Record(2, 2));
    set.add(null);
    set.add(new Record(7, 7));
    Iterator<Record> it = set.iterator();
    assertTrue(it.hasNext());
    assertNull(it.next());
    assertTrue(it.hasNext());
    assertEquals(2, it.next().key);
    assertTrue(it.hasNext());
    assertEquals(7, it.next().key);
    assertTrue(it.hasNext());
    assertEquals(10, it.next().key);
    assertFalse(it.hasNext());
  }
  
  /**
   * Verify that Set.add doesn't replace an existing entry that compares equal.
   */
  public void testAdd_overwrite() {
    TreeSet<Record> set = new TreeSet<Record>(new RecordCompare());
    assertTrue(set.add(new Record(1, 1)));
    assertTrue(set.add(new Record(2, 2)));
    assertFalse(set.add(new Record(1, -1)));
    Record first = set.first();
    assertEquals(1, first.extra);
  }

  @Override
  Integer getGreaterThanMaximumKey() {
    return Integer.MAX_VALUE;
  }

  @Override
  Integer[] getKeys() {
    return new Integer[] {1, 2, 3, 4};
  }

  @Override
  Integer[] getKeys2() {
    return new Integer[] {5, 6, 7, 8};
  }

  @Override
  Integer getLessThanMinimumKey() {
    return Integer.MIN_VALUE;
  }

  @Override
  protected Object getConflictingKey() {
    return "key";
  }

  @Override
  protected Object getConflictingValue() {
    return "value";
  }
}
