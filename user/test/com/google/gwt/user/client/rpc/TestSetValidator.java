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
package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmpty;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableGraphWithCFS;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializablePrivateNoArg;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableWithTwoArrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

/**
 * Misnamed set of static validation methods used by various collection class
 * tests.
 * <p>
 * TODO: could add generics to require args to be of the same type
 */
public class TestSetValidator {

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

  public static boolean isValid(List<MarkerTypeEmpty> list) {
    return list != null && list.size() == 0;
  }

  public static boolean isValid(Map<MarkerTypeEmpty, MarkerTypeEmpty> map) {
    return map != null && map.size() == 0;
  }

  public static boolean isValid(Set<MarkerTypeEmpty> set) {
    return set != null && set.size() == 0;
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
