// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.TestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

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

  public static boolean isValid(ArrayList list) {
    if (list == null) {
      return false;
    }

    ArrayList reference = TestSetFactory.createArrayList();
    if (reference.size() != list.size()) {
      return false;
    }

    return reference.equals(list);
  }

  public static boolean isValid(HashSet expected, HashSet actual) {
    if (actual == null) {
      return false;
    }

    if (expected.size() != actual.size()) {
      return false;
    }

    Iterator entryIter = expected.iterator();
    while (entryIter.hasNext()) {
      Object entry = entryIter.next();

      if (!actual.contains(entry)) {
        return false;
      }
    }

    return true;
  }

  public static boolean isValid(Map expected, HashMap map) {
    if (map == null) {
      return false;
    }

    if (expected.size() != map.size()) {
      return false;
    }

    Set entries = expected.entrySet();
    Iterator entryIter = entries.iterator();
    while (entryIter.hasNext()) {
      Entry entry = (Entry) entryIter.next();

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

  public static boolean isValid(SerializableClass actual) {
    if (actual == null) {
      return false;
    }

    IsSerializable[] elements = actual.getElements();
    IsSerializable elementRef = actual.getElementRef();

    if (elements == null || elementRef == null) {
      return false;
    }

    if (elements.length != 4) {
      return false;
    }

    if (elements[3] != elementRef) {
      return false;
    }

    return true;
  }

  public static boolean isValid(Vector expected, Vector actual) {
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

  public static boolean isValidComplexCyclicGraph(
      SerializableDoublyLinkedNode actual) {

    if (actual == null) {
      return false;
    }

    int i = 0;
    SerializableDoublyLinkedNode currNode = actual;
    for (; i < 5; ++i) {
      if (!currNode.getData().equals("n" + Integer.toString(i))) {
        return false;
      }

      SerializableDoublyLinkedNode nextNode = currNode.getRightChild();
      SerializableDoublyLinkedNode prevNode = currNode.getLeftChild();

      if (nextNode == null || prevNode == null) {
        return false;
      }

      if (nextNode.getLeftChild() != currNode) {
        return false;
      }

      if (prevNode.getRightChild() != currNode) {
        return false;
      }

      currNode = currNode.getRightChild();
      if (currNode == actual) {
        break;
      }
    }

    if (i >= 4) {
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
}
