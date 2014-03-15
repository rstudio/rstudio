/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.user.client.rpc;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptySet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnum;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnumMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeSingleton;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableGraphWithCFS;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializablePrivateNoArg;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableWithTwoArrays;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Misnamed set of static validation methods used by various collection class
 * tests.
 * <p>
 * TODO: could add generics to require args to be of the same type
 */
public class TestSetValidator {

  private static class UnassignableObject {
  }

  /**
   * Check that an Object array has it's meta-data preserved (e.g. Array.runtimeTypeId),
   * to ensure proper type checking on element assignment.
   */
  public static boolean checkObjectArrayElementAssignment(Object[] array,
      int index, Object value) {

    // first check that the requested assignment succeeds
    try {
      array[index] = value;
    } catch (ArrayStoreException e) {
      return false;
    }

    // next check that assignment with a bogus type throws ArrayStoreException
    try {
      array[index] = new UnassignableObject();
      return false;
    } catch (ArrayStoreException e) {
      return true;
    }
  }

  public static boolean equals(boolean[] expected, boolean[] actual) {
    if (actual == null) {
      return false;
    }

    if (expected.length != actual.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(byte[] expected, byte[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(char[] expected, char[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(double[] expected, double[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(float[] expected, float[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(int[] expected, int[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(Iterator<?> expected, Iterator<?> actual) {
    while (expected.hasNext() && actual.hasNext()) {
      if (!expected.next().equals(actual.next())) {
        return false;
      }
    }
    return expected.hasNext() == actual.hasNext();
  }

  public static boolean equals(long[] expected, long[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(Object[] o1, Object[] o2) {
    if (o1 == o2) {
      return true;
    }

    if (o1 == null || o2 == null) {
      return false;
    }

    if (o1.length != o2.length) {
      return false;
    }

    for (int i = 0; i < o1.length; ++i) {
      Object e1 = o1[i];
      Object e2 = o2[i];

      if (e1 == e2) {
        continue;
      }

      if (e1 == null || e2 == null) {
        return false;
      }

      if (!e1.equals(e2)) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(short[] expected, short[] actual) {
    if (actual == null) {
      return false;
    }

    if (actual.length != expected.length) {
      return false;
    }

    for (int i = 0; i < expected.length; ++i) {
      if (expected[i] != actual[i]) {
        return false;
      }
    }

    return true;
  }

  private static boolean equals(Throwable expected, Throwable actual) {
    // If one is null, both must be null (or vice-versa)
    if ((expected == null) != (actual == null)) {
      return false;
    }
    // Only check expected for null, as we know that actual is the same.
    if (actual == null) {
      return false;
    }

    String expectedMessage = expected.getMessage();
    String actualMessage = actual.getMessage();
    if (!equalsWithNullCheck(expectedMessage, actualMessage)) {
        return false;
    }

    /*
     * The cause field is not serialized, so we cannot verify it.
     */

    /*
     * Stack traces are not comparable because they are automatically filled in when
     * the exception is instantiated, with the instantiation site's stack trace.
     */

    return true;
  }

  public static boolean isValid(ArrayList<?> list) {
    if (list == null) {
      return false;
    }

    ArrayList<?> reference = TestSetFactory.createArrayList();
    if (reference.size() != list.size()) {
      return false;
    }

    return reference.equals(list);
  }

  public static boolean isValid(List<MarkerTypeEmptyList> list) {
    return list != null && list.size() == 0;
  }

  public static boolean isValid(Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue> map) {
    return map != null && map.size() == 0;
  }

  public static boolean isValid(Set<MarkerTypeEmptySet> set) {
    return set != null && set.size() == 0;
  }

  public static boolean isValidEnumKey(
    EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected,
    EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> map) {
    if (map == null) {
      return false;
    }

    Set<?> entries = expected.entrySet();
    Iterator<?> entryIter = entries.iterator();
    while (entryIter.hasNext()) {
      Entry<?, ?> entry = (Entry<?, ?>) entryIter.next();

      Object value = map.get(entry.getKey());

      if (value != entry.getValue()) {
        if (value == null || entry.getValue() == null) {
          return false;
        }

        if (!map.get(entry.getKey()).equals(entry.getValue())) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean isValid(EnumMap<?, ?> expected, EnumMap<?, ?> map) {
    if (map == null) {
      return false;
    }

    Set<?> entries = expected.entrySet();
    Iterator<?> entryIter = entries.iterator();
    while (entryIter.hasNext()) {
      Entry<?, ?> entry = (Entry<?, ?>) entryIter.next();

      Object value = map.get(entry.getKey());

      if (value != entry.getValue()) {
        if (value == null || entry.getValue() == null) {
          return false;
        }

        if (!map.get(entry.getKey()).equals(entry.getValue())) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean isValid(HashMap<?, ?> expected, HashMap<?, ?> map) {
    if (map == null) {
      return false;
    }

    if (expected.size() != map.size()) {
      return false;
    }

    Set<?> entries = expected.entrySet();
    Iterator<?> entryIter = entries.iterator();
    while (entryIter.hasNext()) {
      Entry<?, ?> entry = (Entry<?, ?>) entryIter.next();

      Object value = map.get(entry.getKey());

      if (value != entry.getValue()) {
        if (value == null || entry.getValue() == null) {
          return false;
        }

        if (!map.get(entry.getKey()).equals(entry.getValue())) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean isValid(HashSet<?> expected, HashSet<?> actual) {
    if (actual == null) {
      return false;
    }

    if (expected.size() != actual.size()) {
      return false;
    }

    Iterator<?> entryIter = expected.iterator();
    while (entryIter.hasNext()) {
      Object entry = entryIter.next();

      if (!actual.contains(entry)) {
        return false;
      }
    }

    return true;
  }

  public static boolean isValidEnumKey(
      IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> expected,
      IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> map) {
    if (map == null) {
      return false;
    }

    if (expected.size() != map.size()) {
      return false;
    }

    Set<?> entries = expected.entrySet();
    Iterator<?> entryIter = entries.iterator();
    while (entryIter.hasNext()) {
      Entry<?, ?> entry = (Entry<?, ?>) entryIter.next();

      Object value = map.get(entry.getKey());

      if (value != entry.getValue()) {
        if (value == null || entry.getValue() == null) {
          return false;
        }

        if (!map.get(entry.getKey()).equals(entry.getValue())) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean isValid(
      IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> expected,
      IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> map) {
    if (map == null) {
      return false;
    }

    if (expected.size() != map.size()) {
      return false;
    }

    Set<Entry<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue>> mapEntries =
        map.entrySet();
    Set<Entry<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue>> expectedEntries =
        expected.entrySet();

    /*
     * An IdentityHashMap uses reference equality for keys. The maps we are
     * comparing have keys from different sources, so we cannot simply use get
     * to find one key in the other map. Instead we need to iterate looking for
     * equality. We do not check for null keys/value; rather, catch them with
     * NPEs.
     */
    Iterator<Entry<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue>> expectedIter =
        expectedEntries.iterator();
    while (expectedIter.hasNext()) {
      Entry<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> expectedEntry =
          expectedIter.next();

      Iterator<Entry<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue>> mapIter =
          mapEntries.iterator();
      boolean found = false;
      while (!found && mapIter.hasNext()) {
        Entry<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> mapEntry =
            mapIter.next();

        if (mapEntry.getKey().equals(expectedEntry.getKey())
            && mapEntry.getValue().equals(expectedEntry.getValue())) {
          found = true;
        }
      }
      if (!found) {
        return false;
      }
    }

    return true;
  }

  public static boolean isValid(LinkedHashMap<?, ?> expected,
      LinkedHashMap<?, ?> map) {
    if (isValid((HashMap<?, ?>) expected, (HashMap<?, ?>) map)) {
      Iterator<?> expectedEntries = expected.entrySet().iterator();
      Iterator<?> actualEntries = map.entrySet().iterator();
      return equals(expectedEntries, actualEntries);
    }
    return false;
  }

  public static boolean isValid(LinkedHashSet<?> expected, LinkedHashSet<?> map) {
    if (isValid((HashSet<?>) expected, (HashSet<?>) map)) {
      Iterator<?> expectedEntries = expected.iterator();
      Iterator<?> actualEntries = map.iterator();
      return equals(expectedEntries, actualEntries);
    }
    return false;
  }

  public static boolean isValid(LinkedList<?> expected, LinkedList<?> actual) {
    if (actual == null) {
      return false;
    }

    Iterator<?> expectedEntries = expected.iterator();
    Iterator<?> actualEntries = actual.iterator();
    return equals(expectedEntries, actualEntries);
  }

  public static boolean isValid(SerializablePrivateNoArg actual) {
    if (actual == null) {
      return false;
    }

    return actual.getValue() == 1;
  }

  /**
   * We want to assert that the two fields have object identity.
   */
  public static boolean isValid(SerializableWithTwoArrays node) {
    return node.one == node.two;
  }

  // also checks whether the sorting of entries is maintained or not.
  public static boolean isValid(TreeMap<String, MarkerTypeTreeMap> expected,
      TreeMap<String, MarkerTypeTreeMap> map) {
    if (map == null) {
      return false;
    }
    if (!equalsWithNullCheck(map.comparator(), expected.comparator())) {
      return false;
    }
    int size = 0;
    if ((size = expected.size()) != map.size()) {
      return false;
    }
    // entrySet returns entries in the sorted order
    List<Map.Entry<String, MarkerTypeTreeMap>> actualList = new ArrayList<Map.Entry<String, MarkerTypeTreeMap>>(
        map.entrySet());
    List<Map.Entry<String, MarkerTypeTreeMap>> expectedList = new ArrayList<Map.Entry<String, MarkerTypeTreeMap>>(
        expected.entrySet());
    for (int index = 0; index < size; index++) {
      Entry<String, MarkerTypeTreeMap> expectedEntry = expectedList.get(index);
      Entry<String, MarkerTypeTreeMap> actualEntry = actualList.get(index);
      if (!equalsWithNullCheck(expectedEntry.getKey(), actualEntry.getKey())
          || !equalsWithNullCheck(expectedEntry.getValue(),
              actualEntry.getValue())) {
        return false;
      }
    }
    return true;
  }

  // also checks whether the sorting of entries is maintained or not.
  public static boolean isValid(TreeSet<MarkerTypeTreeSet> expected,
      TreeSet<MarkerTypeTreeSet> set) {
    if (set == null) {
      return false;
    }
    if (!equalsWithNullCheck(set.comparator(), expected.comparator())) {
      return false;
    }
    int size = 0;
    if ((size = expected.size()) != set.size()) {
      return false;
    }
    // entrySet returns entries in the sorted order
    List<MarkerTypeTreeSet> actualList = new ArrayList<MarkerTypeTreeSet>(set);
    List<MarkerTypeTreeSet> expectedList = new ArrayList<MarkerTypeTreeSet>(
        expected);
    for (int index = 0; index < size; index++) {
      if (!equalsWithNullCheck(expectedList.get(index), actualList.get(index))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isValid(UmbrellaException expected, UmbrellaException actual) {
    if (actual == null) {
      return false;
    }

    /*
     * Throwable doesn't declare equals.
     */
    if (!equals(expected, actual)) {
      return false;
    }

    /* Check causes. */
    Set<Throwable> expectedCauses = expected.getCauses();
    Set<Throwable> actualCauses = actual.getCauses();

    /* Size. */
    if (actualCauses.size() != expectedCauses.size()) {
      return false;
    }

    /* Null elements, and make a copy of the actualCauses set */
    Iterator<Throwable> expectedIter = expectedCauses.iterator();
    while (expectedIter.hasNext()) {
      Throwable expectedCause = expectedIter.next();
      if (expectedCause == null) {
        return false;
      }
    }
    Iterator<Throwable> actualIter = actualCauses.iterator();
    while (actualIter.hasNext()) {
      Throwable actualCause = actualIter.next();
      if (actualCause == null) {
        return false;
      }
    }

    /*
     * The elements themselves. We rely on the fact that the test sets do not
     * contain duplicates of causes.
     */
    expectedIter = expectedCauses.iterator();
    while (expectedIter.hasNext()) {
      Throwable expectedCause = expectedIter.next();
      actualIter = actualCauses.iterator();
      boolean found = false;
      while (!found && actualIter.hasNext()) {
        Throwable actualCause = actualIter.next();
        if (equals(expectedCause, actualCause)) {
          found = true;
        }
      }
      if (!found) {
        return false;
      }
    }

    return true;
  }

  public static boolean isValid(Vector<?> expected, Vector<?> actual) {
    if (actual == null) {
      return false;
    }

    return expected.equals(actual);
  }

  public static boolean isValidAcyclicGraph(SerializableDoublyLinkedNode actual) {
    if (actual == null) {
      return false;
    }

    if (!actual.getData().equals("head")) {
      return false;
    }

    SerializableDoublyLinkedNode leftChild = actual.getLeftChild();
    if (leftChild == null) {
      return false;
    }

    if (!leftChild.getData().equals("lchild")) {
      return false;
    }

    if (leftChild.getLeftChild() != null || leftChild.getRightChild() != null) {
      return false;
    }

    SerializableDoublyLinkedNode rightChild = actual.getRightChild();
    if (rightChild == null) {
      return false;
    }

    if (!rightChild.getData().equals("rchild")) {
      return false;
    }

    if (rightChild.getLeftChild() != null || rightChild.getRightChild() != null) {
      return false;
    }

    return true;
  }

  public static boolean isValidArrayListVoid(ArrayList<Void> list) {
    if (list.size() != 2) {
      return false;
    }

    if (list.get(0) != null || list.get(1) != null) {
      return false;
    }

    return true;
  }

  public static boolean isValidAsList(List<?> list) {
    if (list == null) {
      return false;
    }

    List<?> reference = TestSetFactory.createArraysAsList();
    if (reference.size() != list.size()) {
      return false;
    }

    return reference.equals(list);
  }

  public static boolean isValidComplexCyclicGraph(
      SerializableDoublyLinkedNode actual) {

    assertNotNull(actual);
    if (actual == null) {
      return false;
    }

    int i = 0;
    SerializableDoublyLinkedNode currNode = actual;
    for (; i < 5; ++i) {
      assertEquals("n" + Integer.toString(i), currNode.getData());
      if (!currNode.getData().equals("n" + Integer.toString(i))) {
        return false;
      }

      SerializableDoublyLinkedNode nextNode = currNode.getRightChild();
      SerializableDoublyLinkedNode prevNode = currNode.getLeftChild();

      assertNotNull("next node", nextNode);
      assertNotNull("prev node", prevNode);
      if (nextNode == null || prevNode == null) {
        return false;
      }

      assertSame("A", currNode, nextNode.getLeftChild());
      if (nextNode.getLeftChild() != currNode) {
        return false;
      }

      assertSame("B", currNode, prevNode.getRightChild());
      if (prevNode.getRightChild() != currNode) {
        return false;
      }

      currNode = currNode.getRightChild();
      if (currNode == actual) {
        break;
      }
    }

    assertFalse("i = " + i, i >= 4);
    if (i >= 4) {
      return false;
    }

    return true;
  }

  public static boolean isValidComplexCyclicGraphWithCFS(
      SerializableGraphWithCFS result) {
    assertNotNull(result);
    List<SerializableGraphWithCFS> array = result.getArray();
    assertNotNull(array);
    assertEquals(1, array.size());

    SerializableGraphWithCFS child = array.get(0);
    assertFalse(result == child);
    assertSame(result, child.getParent());
    return true;
  }

  public static boolean isValidSingletonList(List<MarkerTypeSingleton> list) {
    if (list == null || list.size() != 1) {
      return false;
    }
    Object value = list.get(0);
    // Perform instanceof check in case RPC did the wrong thing
    if (!(value instanceof MarkerTypeSingleton)) {
      return false;
    }
    MarkerTypeSingleton singleton = (MarkerTypeSingleton) value;
    if (!"singleton".equals(singleton.getValue())) {
      return false;
    }
    return true;
  }

  public static boolean isValidTrivialCyclicGraph(
      SerializableDoublyLinkedNode actual) {
    if (actual == null) {
      return false;
    }

    if (!actual.getData().equals("head")) {
      return false;
    }

    SerializableDoublyLinkedNode lchild = actual.getLeftChild();
    if (lchild == null) {
      return false;
    }

    SerializableDoublyLinkedNode rchild = actual.getRightChild();
    if (rchild == null) {
      return false;
    }

    if (actual != lchild && actual != rchild) {
      return false;
    }

    return true;
  }

  /**
   * Wrap an exception in RuntimeException if necessary so it doesn't have to be
   * listed in throws clauses.
   *
   * @param caught exception to wrap
   */
  public static void rethrowException(Throwable caught) {
    if (caught instanceof RuntimeException) {
      throw (RuntimeException) caught;
    } else {
      throw new RuntimeException(caught);
    }
  }

  private static boolean equalsWithNullCheck(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }
}
