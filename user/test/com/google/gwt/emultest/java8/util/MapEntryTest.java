/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;

/**
 * Tests for java.util.Map.Entry Java 8 API emulation.
 */
public class MapEntryTest extends EmulTestBase {

  private Entry<String, String> entry1;
  private Entry<String, String> entry2;

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    entry1 = createTestEntry1();
    entry2 = createTestEntry2();
  }

  public void testEntryComparingByKey() {
    final Comparator<Entry<String, String>> entryComparator = Entry.comparingByKey();

    assertEquals(-1, entryComparator.compare(entry1, entry2));
    assertEquals(1, entryComparator.compare(entry2, entry1));
    assertEquals(0, entryComparator.compare(entry1, entry1));
    assertEquals(0, entryComparator.compare(entry2, entry2));
    assertEquals(0, entryComparator.compare(entry1, createTestEntry1()));
    assertEquals(0, entryComparator.compare(entry2, createTestEntry2()));
  }

  public void testEntryComparingByKeyWithComparator() {
    final Comparator<Entry<String, String>> entryComparator =
        Entry.comparingByKey(Collections.reverseOrder());

    assertEquals(1, entryComparator.compare(entry1, entry2));
    assertEquals(-1, entryComparator.compare(entry2, entry1));
    assertEquals(0, entryComparator.compare(entry1, entry1));
    assertEquals(0, entryComparator.compare(entry2, entry2));
    assertEquals(0, entryComparator.compare(entry1, createTestEntry1()));
    assertEquals(0, entryComparator.compare(entry2, createTestEntry2()));
  }

  public void testEntryComparingByValue() {
    final Comparator<Entry<String, String>> valueComparator = Entry.comparingByValue();

    assertEquals(-1, valueComparator.compare(entry1, entry2));
    assertEquals(1, valueComparator.compare(entry2, entry1));
    assertEquals(0, valueComparator.compare(entry1, entry1));
    assertEquals(0, valueComparator.compare(entry2, entry2));
    assertEquals(0, valueComparator.compare(entry1, createTestEntry1()));
    assertEquals(0, valueComparator.compare(entry2, createTestEntry2()));
  }

  public void testEntryComparingByValueWithComparator() {
    final Comparator<Entry<String, String>> valueComparator =
        Entry.comparingByValue(Collections.reverseOrder());

    assertEquals(1, valueComparator.compare(entry1, entry2));
    assertEquals(-1, valueComparator.compare(entry2, entry1));
    assertEquals(0, valueComparator.compare(entry1, entry1));
    assertEquals(0, valueComparator.compare(entry2, entry2));
    assertEquals(0, valueComparator.compare(entry1, createTestEntry1()));
    assertEquals(0, valueComparator.compare(entry2, createTestEntry2()));
  }

  private static Entry<String, String> createTestEntry1() {
    return new SimpleImmutableEntry<>("a", "A");
  }

  private static Entry<String, String> createTestEntry2() {
    return new SimpleImmutableEntry<>("b", "B");
  }
}
