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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;

import org.apache.commons.collections.TestSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests <code>TreeSet</code>.
 * 
 * @param <E> The key type for the underlying TreeSet
 * 
 * TODO(jat): this whole structure needs work. Ideally we would port a new
 * Apache collections test to GWT, but that is not an insignificant amount of
 * work.
 */
public abstract class TreeSetTest<E extends Comparable<E>> extends TestSet {

  /**
   * Verify a Set is explicitly and implicitly empty.
   * 
   * @param set
   */
  private static <E> void _assertEmpty(Set<E> set) {
    assertNotNull(set);
    assertTrue(set.isEmpty());
    assertEquals(0, set.size());
  }

  /**
   * Verify that two Collections are deeply equivalent. Some of the Sets that
   * need to be verified do not implement a sensible equals method
   * (TreeSet.values for example).
   * 
   * @param expected
   * @param actual
   */
  private static <T> void _assertEquals(Collection<T> expected,
      Collection<T> actual) {
    // verify equivalence using collection interface
    assertEquals(expected.isEmpty(), actual.isEmpty());
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
    assertTrue(actual.containsAll(expected));
    for (T expectedValue : expected) {
      assertTrue(actual.contains(expectedValue));
    }
    for (T actualValue : actual) {
      assertTrue(expected.contains(actualValue));
    }
  }

  /**
   * Verify that two SortedMaps are deeply equivalent.
   * 
   * @param expected
   * @param actual
   */
  private static <E> void _assertEquals(SortedSet<E> expected,
      SortedSet<E> actual) {
    _assertEquals((Set<E>) expected, (Set<E>) actual);

    // verify the order of the associated collections
    assertEquals(expected.toArray(), actual.toArray());
  }

  /**
   * comparator used when creating the SortedSet.
   */
  private Comparator<E> comparator = null;

  private boolean isAddSupported = true;
  private boolean isClearSupported = true;
  private boolean isNullKeySupported = true;
  private boolean isPutAllSupported = true;
  private boolean isRemoveSupported = true;

  public TreeSetTest() {
    super("TreeSetTest");
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Test method for 'java.util.Set.add(Object)'.
   * 
   * @see java.util.Set#put(Object)
   */
  public void testAdd() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isAddSupported) {
      Set<E> set = createSet();
      assertTrue(set.add(getKeys()[0]));
      assertFalse(set.isEmpty());
      assertEquals(1, set.size());
    }
  }

  /**
   * Test method for 'java.util.Set.add(Object)'.
   * 
   * @see java.util.Set#add(Object)
   */
  public void testAdd_entries3() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isAddSupported) {
      // populate the set
      Set<E> set = createSet();
      set.add(getKeys()[0]);
      set.add(getKeys()[1]);
      set.add(getKeys()[2]);

      // test contents
      assertFalse(set.isEmpty());
      assertEquals(3, set.size());
      Collection<E> keys = set;
      // test contains all keys
      assertTrue(keys.contains(getKeys()[0]));
      assertTrue(keys.contains(getKeys()[1]));
      assertTrue(keys.contains(getKeys()[2]));
    }
  }

  /**
   * Test method for 'java.util.Set.add(Object)'.
   * 
   * @see java.util.Set#add(Object)
   */
  public void testAdd_replace() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isAddSupported) {
      Set<E> set = createSet();
      assertTrue(set.add(getKeys()[0]));
      assertFalse(set.isEmpty());
      assertEquals(1, set.size());

      assertFalse(set.add(getKeys()[0]));
      assertEquals(1, set.size());
    }
  }

  /**
   * Test method for 'java.util.Set.add(Object)'.
   * 
   * @see java.util.Set#add(Object)
   */
  @SuppressWarnings("unchecked")
  public void testAdd_throwsClassCastException_key() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isAddSupported) {
      Set<E> set = createSet();
      set.add(getKeys()[0]);
      try {
        Set untypedSet = set;
        untypedSet.add(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Set.add(Object)'.
   * 
   * @see java.util.Set#add(Object)
   */
  @SuppressWarnings("unchecked")
  public void testAdd_throwsClassCastException_value() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isAddSupported) {
      Set<E> set = createSet();
      set.add(getKeys()[0]);

      Set untypedSet = set;
      untypedSet.add(getKeys()[1]);
      // You might think this should throw an exception here but, no. Makes
      // sense since the class cast is attributed to comparability of the
      // keys... generics really have nothing to do with it .
    }
  }

  /**
   * Test method for 'java.util.Set.add(Object)'.
   * 
   * @see java.util.Set#add(Object)
   */
  public void testAdd_throwsUnsupportedOperationException() {
    if (!isAddSupported) {
      Set<E> set = createSet();
      try {
        set.add(getKeys()[0]);
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set<E> sourceSet = createSet();
      sourceSet.add(getKeys()[0]);
      sourceSet.add(getKeys()[1]);
      sourceSet.add(getKeys()[2]);

      Set<E> destSet = createSet();
      destSet.addAll(sourceSet);
      // Make sure that the data is copied correctly
      _assertEquals(sourceSet, destSet);
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll_addEntries() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set<E> sourceMap = createSet();
      sourceMap.add(getKeys()[0]);

      Set<E> destSet = createSet();
      destSet.addAll(sourceMap);
      // Verify that entries get added.
      sourceMap.add(getKeys()[1]);
      destSet.addAll(sourceMap);
      _assertEquals(sourceMap, destSet);
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll_emptyMap() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set<E> sourceSet = createSet();
      sourceSet.add(getKeys()[0]);

      Set<E> destSet = createSet();
      destSet.addAll(sourceSet);
      // Verify that putting an empty set does not clear.
      destSet.addAll(createSet());
      _assertEquals(sourceSet, destSet);
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll_overwrite() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set<E> sourceSet = createSet();
      sourceSet.add(getKeys()[0]);

      Set<E> destSet = createSet();
      destSet.addAll(sourceSet);
      // Verify that entries get replaced.
      sourceSet.add(getKeys()[0]);
      destSet.addAll(sourceSet);
      _assertEquals(sourceSet, destSet);
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll_self() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set<E> sourceSet = createSet();
      sourceSet.add(getKeys()[0]);
      sourceSet.addAll(sourceSet);
      // verify putAll with self succeeds and has no effect.
      assertEquals(1, sourceSet.size());
      assertEquals(getKeys()[0], sourceSet.iterator().next());
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  @SuppressWarnings("unchecked")
  public void testAddAll_throwsClassCastException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set sourceSet = new HashSet();
      sourceSet.add(getConflictingKey());

      Set<E> destSet = createSet();
      destSet.add(getKeys()[0]);
      try {
        // This throws in dev mode because we're putting a second entry in
        // the set and TreeSet calls the compare method to order them.
        destSet.addAll(sourceSet);
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll_throwsNullPointerException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Set<E> set = createSet();
      try {
        set.addAll((Set<E>) null);
        fail("expected exception");
      } catch (NullPointerException e) {
        // expected outcome
      } catch (JavaScriptException e) {
        // in Production Mode we don't actually do null checks, so we get a JS
        // exception
      }
    }
  }

  /**
   * Test method for 'java.util.Set.addAll(Map)'.
   * 
   * @see java.util.Set#addAll(Map)
   */
  public void testAddAll_throwsUnsupportedOperationException() {
    Set<E> set = createSet();
    if (!isPutAllSupported) {
      try {
        set.addAll(createSet());
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Set.clear()'.
   * 
   * @see java.util.Set#clear()
   */
  public void testClear() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isClearSupported) {
      // Execute this test only if supported.
      Set<E> set = createSet();
      set.add(getKeys()[0]);
      assertFalse(set.isEmpty());
      set.clear();
      _assertEmpty(set);
    }
  }

  /**
   * Test method for 'java.util.Set.clear()'.
   * 
   * @see java.util.Set#clear()
   */
  public void testClear_throwsUnsupportedOperationException() {
    Set<E> set = createSet();
    if (!isClearSupported) {
      try {
        set.clear();
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.SortedSet.comparator()'.
   * 
   * @see java.util.SortedSet#comparator()
   */
  public void testComparator() {
    SortedSet<E> sortedSet = createSortedSet();
    if (isNaturalOrder()) {
      assertEquals(null, sortedSet.comparator());
    } else {
      assertEquals(getComparator(), sortedSet.comparator());
    }
  }

  /**
   * Test method for default constructor.
   * 
   * @see java.util.TreeSet#TreeSet()
   */
  public void testConstructor() {
    TreeSet<E> treeSet = new TreeSet<E>();
    _assertEmpty(treeSet);
  }

  /**
   * Test method for 'java.util.TreeSet.TreeSet(Comparator)'.
   * 
   * @see java.util.TreeSet#TreeSet(Comparator)
   */
  public void testConstructor_comparator() {
    TreeSet<E> TreeSet = new TreeSet<E>(getComparator());
    _assertEmpty(TreeSet);
    if (isNaturalOrder()) {
      assertNull(TreeSet.comparator());
    } else {
      assertSame(getComparator(), TreeSet.comparator());
    }
  }

  /**
   * Test method for 'java.util.TreeSet.TreeSet(Set)'.
   * 
   * @see java.util.TreeSet#TreeSet(Set)
   */
  public void testConstructor_Set() {
    // The source set should be just a Map. Not a sorted set.
    Set<E> sourceMap = new HashSet<E>();

    // populate the source set
    sourceMap.add(getKeys()[0]);
    sourceMap.add(getKeys()[1]);
    sourceMap.add(getKeys()[2]);

    TreeSet<E> copyConstructed = new TreeSet<E>(sourceMap);
    _assertEquals(sourceMap, copyConstructed);
  }

  /**
   * Test method for 'java.util.TreeSet.TreeSet(Set)'.
   * 
   * @see java.util.TreeSet#TreeSet(Set)
   */
  @SuppressWarnings("unchecked")
  public void testConstructor_Set_rawType() {
    Set sourceSet = new HashSet();
    sourceSet.add(getConflictingKey());
    // Raw types can be used to defeat the type system, and this will work
    // so long as the key is Comparable and there's only one entry (so compare()
    // won't be called.)
    new TreeSet<E>(sourceSet);
  }

  /**
   * Test method for 'java.util.TreeSet.TreeSet(Set)'.
   * 
   * @see java.util.TreeSet#TreeSet(Set)
   */
  public void testConstructor_Set_throwsNullPointerException() {
    try {
      new TreeSet<E>((Set<E>) null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // expected outcome
    } catch (JavaScriptException e) {
      // in Production Mode we don't actually do null checks, so we get a JS
      // exception
    }
  }

  /**
   * Test method for 'java.util.TreeSet.TreeSet(SortedSet).
   * 
   * @see java.util.TreeSet#TreeSet(SortedSet)
   */
  public void testConstructor_SortedMap_throwsNullPointerException() {
    try {
      new TreeSet<E>((SortedSet<E>) null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // expected outcome
    } catch (JavaScriptException e) {
      // in Production Mode we don't actually do null checks, so we get a JS
      // exception
    }
  }

  /**
   * Test method for 'java.util.TreeSet.TreeSet(SortedSet)'.
   * 
   * @see java.util.TreeSet#TreeSet(SortedSet)
   */
  public void testConstructor_SortedSet() {
    SortedSet<E> sourceMap = new TreeSet<E>();
    _assertEmpty(sourceMap);

    // populate the source set
    sourceMap.add(getKeys()[0]);
    sourceMap.add(getKeys()[1]);
    sourceMap.add(getKeys()[2]);

    TreeSet<E> copyConstructed = new TreeSet<E>(sourceMap);
    _assertEquals(sourceMap, copyConstructed);
  }

  /**
   * Test method for 'java.util.Set.contains(Object)'. *
   * 
   * @see java.util.Set#contains(Object)
   */
  public void testContains() {
    Set<E> set = createSet();
    assertFalse(set.contains(getKeys()[0]));
    assertTrue(set.add(getKeys()[0]));
    assertEquals(1, set.size());
    assertTrue(set.contains(getKeys()[0]));
    assertFalse(set.contains(getKeys()[1]));
  }

  /**
   * Test method for 'java.util.Set.contains(Object)'.
   * 
   * @see java.util.Set#contains(Object)
   */
  public void testContains_throwsClassCastException() {
    Set<E> set = createSet();
    set.add(getKeys()[0]);
    try {
      set.contains(getConflictingKey());
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Set.contains(Object)'.
   * 
   * @see java.util.Set#contains(Object)
   */
  public void testContains_throwsNullPointerException() {
    Set<E> set = createSet();
    if (isNaturalOrder() && !isNullKeySupported) {
      try {
        set.contains(null);
        fail("expected exception");
      } catch (NullPointerException e) {
        // expected outcome
      } catch (JavaScriptException e) {
        // in Production Mode we don't actually do null checks, so we get a JS
        // exception
      }
    }
  }

  /**
   * Test method for 'java.util.Object.equals(Object)'.
   * 
   * @see java.util.Set#equals(Object)
   */
  public void testEquals() {
    Set<E> set0 = createSet();
    Set<E> set1 = createSet();
    assertTrue(set0.equals(set1));
    set0.add(getKeys()[0]);
    set1.add(getKeys()[0]);
    assertTrue(set0.equals(set0));
    assertTrue(set0.equals(set1));
    set0.add(getKeys()[1]);
    assertFalse(set0.equals(set1));
  }

  /**
   * Test method for 'java.util.SortedSet.first()'.
   * 
   * @see java.util.SortedSet#first()
   */
  public void testFirst() {
    SortedSet<E> sortedSet = createSortedSet();
    // test with a single entry set
    sortedSet.add(getKeys()[0]);
    assertEquals(getKeys()[0], sortedSet.first());
    // is it consistent with other methods
    assertEquals(sortedSet.toArray()[0], sortedSet.first());
    assertEquals(getKeys()[0], sortedSet.last());
    assertEquals(sortedSet.last(), sortedSet.first());

    // test with two entry set
    sortedSet.add(getKeys()[1]);
    assertEquals(getKeys()[0], sortedSet.first());
    assertFalse(getKeys()[1].equals(sortedSet.first()));
    // is it consistent with other methods
    assertEquals(sortedSet.toArray()[0], sortedSet.first());
    assertFalse(getKeys()[0].equals(sortedSet.last()));
    assertFalse(sortedSet.last().equals(sortedSet.first()));
  }

  /**
   * Test method for 'java.util.SortedSet.first()'.
   * 
   * @see java.util.SortedSet#first()
   */
  public void testFirstKey_throwsNoSuchElementException() {
    SortedSet<E> SortedSet = createSortedSet();
    // test with no entries
    try {
      SortedSet.first();
      fail("expected exception");
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.lang.Object.hashCode()'.
   * 
   * @see java.util.Set#hashCode()
   */
  public void testHashCode() {
    Set<E> set0 = createSet();
    Set<E> set1 = createSet();

    int hashCode0 = set0.hashCode();
    int hashCode1 = set1.hashCode();
    assertTrue("empty maps have different hash codes", hashCode0 == hashCode1);

    // Check that hashCode changes
    set0.add(getKeys()[0]);
    hashCode0 = set0.hashCode();
    assertTrue("hash code didn't change", hashCode0 != hashCode1);

    Set<String> set2 = new TreeSet<String>();
    set2.add("");
    Set<Integer> set3 = new TreeSet<Integer>();
    set3.add(0);
    int hashCode2 = set2.hashCode();
    int hashCode3 = set3.hashCode();
    assertEquals("empty string/0 hash codes not the same", hashCode2, hashCode3);
  }

  /**
   * Test method for 'java.util.SortedSet.headSet(Object, Object)'.
   * 
   * @see java.util.SortedSet#headSet(Object)
   */
  @SuppressWarnings("unchecked")
  public void testHeadMap_throwsClassCastException() {
    SortedSet SortedSet = createSortedSet();
    SortedSet.add(getKeys()[0]);
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subSet doesn't?
      SortedSet.headSet(getConflictingKey());
    } else {
      try {
        SortedSet.headSet(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.SortedSet.headSet(Object)'.
   * 
   * @see java.util.SortedSet#headSet(Object)
   */
  public void testHeadSet() {
    // test with no entries
    assertNotNull(createSortedSet().headSet(getKeys()[0]));
  }

  /**
   * Test method for 'java.util.SortedSet.headSet(Object)'.
   * 
   * @see java.util.SortedSet#headSet(Object)
   */
  public void testHeadSet_entries0_size() {
    // test with no entries
    assertEquals(0, createSortedSet().headSet(getKeys()[0]).size());
  }

  /**
   * Test method for 'java.util.SortedSet.headSet(Object)'.
   * 
   * @see java.util.SortedSet#headSet(Object)
   */
  public void testHeadSet_entries1() {
    SortedSet<E> SortedSet = createSortedSet();
    // test with a single entry set
    SortedSet.add(getKeys()[0]);
    assertEquals(0, SortedSet.headSet(getKeys()[0]).size());
  }

  /**
   * Test method for 'java.util.SortedSet.headSet(Object)'.
   * 
   * @see java.util.SortedSet#headSet(Object)
   */
  public void testHeadSet_entries2() {
    SortedSet<E> SortedSet = createSortedSet();
    // test with two entry set
    SortedSet.add(getKeys()[0]);
    SortedSet.add(getKeys()[1]);
    assertEquals(0, SortedSet.headSet(getKeys()[0]).size());
    assertEquals(1, SortedSet.headSet(getKeys()[1]).size());
    assertEquals(getKeys()[0], SortedSet.tailSet(getKeys()[0]).toArray()[0]);
  }

  /**
   * Test method for 'java.util.Set.isEmpty()'. *
   * 
   * @see java.util.Set#isEmpty()
   * 
   */
  public void testIsEmpty() {
    Set<E> sourceSet = createSet();
    Set<E> destSet = createSet();

    destSet.addAll(sourceSet);
    assertTrue(destSet.isEmpty());

    destSet.add(getKeys()[0]);
    assertFalse(destSet.isEmpty());

    destSet.remove(getKeys()[0]);
    assertTrue(destSet.isEmpty());
    assertEquals(destSet.size(), 0);
  }

  /**
   * Test method for 'java.util.SortedSet.last()'.
   * 
   * @see java.util.SortedSet#last()
   */
  public void testLastKey() {
    SortedSet<E> sortedSet = createSortedSet();

    // test with a single entry set
    sortedSet.add(getKeys()[0]);
    assertEquals(getKeys()[0], sortedSet.last());
    // is it consistent with other methods
    assertEquals(sortedSet.toArray()[0], sortedSet.last());
    assertEquals(getKeys()[0], sortedSet.first());
    assertEquals(sortedSet.first(), sortedSet.last());

    // test with two entry set
    sortedSet.add(getKeys()[1]);
    assertEquals(getKeys()[1], sortedSet.last());
    assertFalse(getKeys()[0].equals(sortedSet.last()));
    // is it consistent with other methods
    assertEquals(sortedSet.toArray()[1], sortedSet.last());
    assertEquals(getKeys()[0], sortedSet.first());
    assertFalse(sortedSet.first().equals(sortedSet.last()));
  }

  /**
   * Test method for 'java.util.SortedSet.last()'.
   * 
   * @see java.util.SortedSet#last()
   */
  public void testLastKey_throwsNoSuchElementException() {
    SortedSet<E> SortedSet = createSortedSet();
    // test with no entries
    try {
      SortedSet.last();
      fail("expected exception");
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Set.remove(Object)'.
   * 
   * @see java.util.Set#remove(Object)
   */
  public void testRemove() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isRemoveSupported) {
      Set<E> set = createSet();

      assertFalse(set.remove(getKeys()[0]));
      assertTrue(set.add(getKeys()[0]));
      assertTrue(set.remove(getKeys()[0]));
      assertFalse(set.remove(getKeys()[0]));
    }
  }

  /**
   * Test method for 'java.util.Set.remove(Object)'.
   * 
   * @see java.util.Set#remove(Object)
   */
  public void testRemove_throwsClassCastException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isRemoveSupported) {
      Set<E> set = createSet();
      set.add(getKeys()[0]);
      try {
        set.remove(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Set.remove(Object)'.
   * 
   * @see java.util.Set#remove(Object)
   */
  public void testRemove_throwsUnsupportedOperationException() {
    Set<E> set = createSet();
    if (!isRemoveSupported) {
      try {
        set.remove(getKeys()[0]);
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Set.size()'.
   * 
   * @see java.util.Set#size()
   */
  public void testSize() {
    Set<E> set = createSet();

    // Test size behavior on add
    set.add(getKeys()[0]);
    assertEquals(1, set.size());
    set.add(getKeys()[1]);
    assertEquals(2, set.size());
    set.add(getKeys()[2]);
    assertEquals(3, set.size());

    // Test size behavior on remove
    set.remove(getKeys()[0]);
    assertEquals(2, set.size());
    set.remove(getKeys()[1]);
    assertEquals(1, set.size());
    set.remove(getKeys()[2]);
    assertEquals(0, set.size());

    // Test size behavior on putAll
    set.add(getKeys()[0]);
    set.add(getKeys()[1]);
    set.add(getKeys()[2]);
    assertEquals(3, set.size());

    // Test size behavior on clear
    set.clear();
    _assertEmpty(set);
  }

  /**
   * Test method for 'java.util.SortedSet.subSet(Object, Object)'.
   * 
   * @see java.util.SortedSet#subSet(Object, Object)
   */
  @SuppressWarnings("unchecked")
  public void testSubMap_throwsClassCastException() {
    SortedSet SortedSet = createSortedSet();
    SortedSet.add(getKeys()[0]);
    try {
      SortedSet.subSet(getConflictingKey(), getKeys()[0]);
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (IllegalArgumentException e) {
      // since we can't ensure CCEs in Production Mode, we may get IAE
      assertTrue("IllegalArgumentException in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
    try {
      SortedSet.subSet(getKeys()[0], getConflictingKey());
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (IllegalArgumentException e) {
      // since we can't ensure CCEs in Production Mode, we may get IAE
      assertTrue("IllegalArgumentException in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.SortedSet.subSet(Object, Object)'.
   * 
   * @see java.util.SortedSet#subSet(Object, Object)
   */
  public void testSubMap_throwsIllegalArgumentException() {
    SortedSet<E> SortedSet = createSortedSet();
    try {
      SortedSet.subSet(getGreaterThanMaximumKey(), getLessThanMinimumKey());
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // from key is greater than the to key
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.SortedSet.subSet(Object, Object)'.
   * 
   * @see java.util.SortedSet#subSet(Object, Object)
   */
  public void testSubSet() {
    SortedSet<E> sortedSet = createSortedSet();
    // test with no entries
    assertEquals(0, sortedSet.subSet(getKeys()[0], getKeys()[0]).size());

    // test with a single entry set
    sortedSet.add(getKeys()[0]);
    assertEquals(0, sortedSet.subSet(getKeys()[0], getKeys()[0]).size());
    // bounded by a "wide" range
    assertEquals(1, sortedSet.subSet(getLessThanMinimumKey(),
        getGreaterThanMaximumKey()).size());

    // test with two entry set
    sortedSet.add(getKeys()[1]);
    assertEquals(1, sortedSet.subSet(getKeys()[0], getKeys()[1]).size());
    assertEquals(getKeys()[0],
        sortedSet.subSet(getKeys()[0], getKeys()[1]).toArray()[0]);
    // bounded by a "wide" range
    SortedSet<E> subSet = sortedSet.subSet(getLessThanMinimumKey(),
        getGreaterThanMaximumKey());
    assertEquals(2, subSet.size());
  }

  /**
   * Test method for 'java.util.SortedSet.tailSet(Object)'.
   * 
   * @see java.util.SortedSet#tailSet(Object)
   */
  public void testTailSet_entries0() {
    // test with no entries
    Set<E> tailSet = createSortedSet().tailSet(getKeys()[0]);
    assertNotNull(tailSet);
  }

  /**
   * Test method for 'java.util.SortedSet.tailSet(Object)'.
   * 
   * @see java.util.SortedSet#tailSet(Object)
   */
  public void testTailSet_entries0_size() {
    // test with no entries
    Set<E> tailSet = createSortedSet().tailSet(getKeys()[0]);
    assertNotNull(tailSet);
    assertEquals(0, tailSet.size());
  }

  /**
   * Test method for 'java.util.SortedSet.tailSet(Object)'.
   * 
   * @see java.util.SortedSet#tailSet(Object)
   */
  public void testTailSet_entries1_size_keyValue() {
    SortedSet<E> sortedSet = createSortedSet();
    // test with a single entry set
    sortedSet.add(getKeys()[0]);
    Set<E> tailSet = sortedSet.tailSet(getKeys()[0]);
    assertEquals(1, tailSet.size());
    assertEquals(getKeys()[0], tailSet.toArray()[0]);
  }

  /**
   * Test method for 'java.util.SortedSet.tailSet(Object)'.
   * 
   * @see java.util.SortedSet#tailSet(Object)
   */
  public void testTailSet_entries2_size_keyValue() {
    SortedSet<E> sortedSet = createSortedSet();
    // test with two entry set
    sortedSet.add(getKeys()[0]);
    Set<E> tailSet = sortedSet.tailSet(getKeys()[0]);
    assertEquals(1, tailSet.size());
    sortedSet.add(getKeys()[1]);
    tailSet = sortedSet.tailSet(getKeys()[1]);
    assertEquals(1, tailSet.size());
    tailSet = sortedSet.tailSet(getKeys()[0]);
    assertEquals(2, tailSet.size());
    assertEquals(getKeys()[0], tailSet.toArray()[0]);
    assertEquals(getKeys()[1], tailSet.toArray()[1]);
  }

  /**
   * Test method for 'java.util.SortedSet.tailSet(Object, Object)'.
   * 
   * @see java.util.SortedSet#tailSet(Object)
   */
  @SuppressWarnings("unchecked")
  public void testTailSet_throwsClassCastException() {
    SortedSet SortedSet = createSortedSet();
    SortedSet.add(getKeys()[0]);
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subSet doesn't?
      SortedSet.tailSet(getConflictingKey());
    } else {
      try {
        SortedSet.tailSet(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.SortedSet.tailSet(Object, Object)'.
   * 
   * @see java.util.SortedSet#tailSet(Object)
   */
  public void testTailSet_throwsIllegalArgumentException() {
    // TODO I don't know of any case where this could happen.
  }

  protected Comparator<E> getComparator() {
    return comparator;
  }

  protected abstract Object getConflictingKey();

  protected abstract Object getConflictingValue();

  @Override
  protected Object[] getFullElements() {
    return getKeys();
  }

  @Override
  protected Object[] getOtherElements() {
    return getKeys2();
  }

  @Override
  protected void gwtSetUp() throws Exception {
    setComparator(null);
  }

  protected boolean isNaturalOrder() {
    return comparator == null;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Set makeEmptySet() {
    return createTreeSet();
  }

  protected void setComparator(Comparator<E> comparator) {
    this.comparator = comparator;
  }

  Set<E> createSet() {
    return createSortedSet();
  }

  SortedSet<E> createSortedSet() {
    SortedSet<E> set = createTreeSet();
    return set;
  }

  TreeSet<E> createTreeSet() {
    if (isNaturalOrder()) {
      return new TreeSet<E>();
    } else {
      return new TreeSet<E>(getComparator());
    }
  }

  abstract E getGreaterThanMaximumKey();

  abstract E[] getKeys();

  abstract E[] getKeys2();

  abstract E getLessThanMinimumKey();
}
