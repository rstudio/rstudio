/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.ext.linker;

import junit.framework.TestCase;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests for {@link TypeIndexedSet}.
 */
public class TypeIndexedSetTest extends TestCase {
  /*
   * Create a hierarchy of related Comparables for this test.  That hierarchy looks like:
   *
   * RootComparable
   *   ChildA
   *     ChildAChild
   *   ChildB
   */

  static class RootComparable implements Comparable<RootComparable> {
    private final Integer value;

    RootComparable(int value) {
      this.value = value;
    }

    @Override
    public int compareTo(RootComparable that) {
      return this.value - that.value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof RootComparable) && (((RootComparable) o).value == this.value);
    }
  }

  static class ChildA extends RootComparable {
    ChildA(int value) {
      super(value);
    }
  }

  static class ChildAChild extends ChildA {
    ChildAChild(int value) {
      super(value);
    }
  }

  static class ChildB extends RootComparable {
    ChildB(int value) {
      super(value);
    }
  }

  /**
   * An instance of the class under test.
   *
   * <p>At setUp, this is filled with a variety of even-valued RootComparable's (including the
   * subtypes of RootComparable).  During tests, new additions are made with odd-numbered
   * RootComparable's.
   */
  private final TypeIndexedSet<RootComparable> set =
      new TypeIndexedSet<RootComparable>(new TreeSet<RootComparable>());

  /**
   * All members of set that are ChildA's or ChildAChild's.
   */
  private final TreeSet<ChildA> childAs = new TreeSet<ChildA>();

  @Override
  protected void setUp() throws Exception {
    childAs.add(new ChildA(2));
    childAs.add(new ChildAChild(4));
    childAs.add(new ChildA(10));

    set.addAll(childAs);
    set.add(new ChildB(6));
    set.add(new RootComparable(8));
  }

  public void testFind_basic() throws Exception {
    assertEquals("When the root type is searched for, all members should be returned in the same"
        + " order as they appear in the original set.",
        set, set.getTypeIndex().findAssignableTo(RootComparable.class));

    // This'll exclude the RootComparable and the ChildB
    assertEquals(
        childAs, set.getTypeIndex().findAssignableTo(ChildA.class));
  }

  public void testFind_findAfterViewModifications() throws Exception {
    // Do a find to prime the index and cache
    set.getTypeIndex().findAssignableTo(ChildA.class);

    // Exclude the first ChildA [2] in a view and then append an item via the view
    SortedSet<RootComparable> subset = set.tailSet(new ChildAChild(4));
    ChildA newlyAdded = new ChildA(5);
    subset.add(new ChildA(5));

    // Check that the root can find the newly added item
    childAs.add(newlyAdded);
    assertEquals(
        childAs, set.getTypeIndex().findAssignableTo(ChildA.class));

    // Attempt a bad add (out of the view's range)
    try {
      subset.add(new ChildA(-1));
      fail("The backing sets should reject this add to the view");
    } catch (IllegalArgumentException ignored) {
    }
    assertEquals(
        childAs, set.getTypeIndex().findAssignableTo(ChildA.class));

    // Remove the recently added item, check find again
    subset.remove(newlyAdded);
    childAs.remove(newlyAdded);
    assertEquals(
        childAs, set.getTypeIndex().findAssignableTo(ChildA.class));

    // Clear the view, check that the parent has one findable item (the one not in the view)
    subset.clear();
    assertEquals(new ChildA(2), set.iterator().next());
    assertEquals(
        new ChildA(2), set.getTypeIndex().findAssignableTo(ChildA.class).iterator().next());

    // Clear via the iterator, check that there are no findable items
    Iterator<?> iterator = set.iterator();
    iterator.next();
    iterator.remove();
    assertTrue(set.isEmpty());
    assertTrue(set.getTypeIndex().findAssignableTo(RootComparable.class).isEmpty());
  }

  public void testViewsSeeMutatesToRoot() throws Exception {
    // Clear the sets
    set.clear();
    childAs.clear();

    // Create the subset
    ChildA partition = new ChildA(5);
    SortedSet<RootComparable> subset = set.headSet(partition);

    // Add some items to the root set
    ChildA added0 = new ChildA(4);
    ChildA added1 = new ChildA(6);
    childAs.add(added0);
    childAs.add(added1);
    set.addAll(childAs);

    // See that the in-range item appears in the view
    assertEquals(childAs.headSet(partition), subset);
  }

  public void testTypeIndexNotInvalidAfterBadAddAll() throws Exception {
    // Clear the sets
    set.clear();
    childAs.clear();

    // Create the subset
    ChildA partition = new ChildA(5);
    SortedSet<RootComparable> subset = set.tailSet(partition);

    // Do a find to prime the index and cache
    set.getTypeIndex().findAssignableTo(ChildA.class);

    // Add some items to the root set
    ChildA added0 = new ChildA(4);
    ChildA added1 = new ChildA(6);
    childAs.add(added0);
    childAs.add(added1);
    try {
      subset.addAll(childAs);
      fail("This call should fail since one item is out-of-range");
    } catch (IllegalArgumentException ignored) {
    }

    // There aren't guarantees about whether the in-range item will be added, but whatever the
    // outcome, the type index must not be corrupted.
    assertEquals(set, subset);
    assertEquals(set, set.getTypeIndex().findAssignableTo(ChildA.class));
  }
}
