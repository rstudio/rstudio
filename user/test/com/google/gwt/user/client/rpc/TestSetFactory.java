/*
 * Copyright 2007 Google Inc.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * TODO: document me.
 */
public class TestSetFactory {

  /**
   * TODO: document me.
   */
  public static class SerializableClass implements IsSerializable {
    public IsSerializable getElementRef() {
      return elementRef;
    }

    public IsSerializable[] getElements() {
      return elements;
    }

    public void setElementRef(IsSerializable elementRef) {
      this.elementRef = elementRef;
    }

    public void setElements(IsSerializable[] elements) {
      this.elements = elements;
    }

    IsSerializable elementRef;

    IsSerializable[] elements;
  }

  /**
   * TODO: document me.
   */
  public static class SerializableDoublyLinkedNode implements IsSerializable {
    public String getData() {
      return data;
    }

    public SerializableDoublyLinkedNode getLeftChild() {
      return leftChild;
    }

    public SerializableDoublyLinkedNode getRightChild() {
      return rightChild;
    }

    public void setData(String data) {
      this.data = data;
    }

    public void setLeftChild(SerializableDoublyLinkedNode leftChild) {
      this.leftChild = leftChild;
    }

    public void setRightChild(SerializableDoublyLinkedNode rightChild) {
      this.rightChild = rightChild;
    }

    protected String data;
    protected SerializableDoublyLinkedNode leftChild;
    protected SerializableDoublyLinkedNode rightChild;
  }

  /**
   * TODO: document me.
   */
  public static class SerializableList extends ArrayList implements
      IsSerializable {
  }

  /**
   * TODO: document me.
   */
  public static class SerializableMap extends HashMap implements IsSerializable {
  }

  /**
   * TODO: document me.
   */
  public static class SerializableNode extends UnserializableNode implements
      IsSerializable {

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (!(obj instanceof SerializableNode)) {
        return false;
      }

      SerializableNode other = (SerializableNode) obj;
      if (data != other.data) {
        if (data == null || !data.equals(other.data)) {
          return false;
        }
      }

      if (next != other.next) {
        if (next == null || !next.equals(other.next)) {
          return false;
        }
      }

      return true;
    }

    public String getData() {
      return data;
    }

    public SerializableNode getNext() {
      return next;
    }

    public int hashCode() {
      int hashValue = 0;
      if (data != null) {
        hashValue += data.hashCode();
      }

      if (next != null && next != this) {
        hashValue += next.hashCode();
      }

      return hashValue;
    }

    public void setData(String data) {
      this.data = data;
    }

    public void setNext(SerializableNode next) {
      this.next = next;
    }

    protected String data;

    protected SerializableNode next;
  }

  /**
   * TODO: document me.
   */
  public static class SerializableSet extends HashSet implements IsSerializable {
  }

  /**
   * TODO: document me.
   */
  public static class SerializableVector extends Vector implements
      IsSerializable {
  }

  /**
   * TODO: document me.
   */
  public static class UnserializableNode {
  }

  public static Boolean[] createBooleanArray() {
    return new Boolean[] {
        Boolean.valueOf(true), Boolean.valueOf(false), Boolean.valueOf(true),
        Boolean.valueOf(false)};
  }

  public static Byte[] createByteArray() {
    return new Byte[] {
        new Byte(Byte.MAX_VALUE), new Byte(Byte.MIN_VALUE),
        new Byte(Byte.MAX_VALUE), new Byte(Byte.MIN_VALUE)};
  }

  public static Character[] createCharArray() {
    return new Character[] {
        new Character(Character.MAX_VALUE), new Character(Character.MIN_VALUE),
        new Character(Character.MAX_VALUE), new Character(Character.MIN_VALUE)};
  }

  public static Date[] createDateArray() {
    return new Date[] {
        new Date(1992 - 1900, 9, 18), new Date(1997 - 1900, 6, 6)};
  }

  public static Double[] createDoubleArray() {
    return new Double[] {
        new Double(Double.MAX_VALUE), new Double(Double.MIN_VALUE),
        new Double(Double.MAX_VALUE), new Double(Double.MIN_VALUE)};
  }

  public static Float[] createFloatArray() {
    return new Float[] {
        new Float(Float.MAX_VALUE), new Float(Float.MIN_VALUE),
        new Float(Float.MAX_VALUE), new Float(Float.MIN_VALUE)};
  }

  public static HashMap createHashMap() {
    HashMap map = new HashMap();
    map.put("SerializableNode", new SerializableNode());
    map.put("SerializableList", new SerializableList());
    map.put("SerializableMap", new SerializableMap());
    map.put("SerializableSet", new SerializableSet());
    map.put("SerializableVector", new SerializableVector());
    return map;
  }

  public static HashSet createHashSet() {
    HashSet set = new HashSet();
    set.add(new SerializableNode());
    set.add(new SerializableList());
    set.add(new SerializableMap());
    set.add(new SerializableSet());
    set.add(new SerializableVector());

    return set;
  }

  public static Integer[] createIntegerArray() {
    return new Integer[] {
        new Integer(Integer.MAX_VALUE), new Integer(Integer.MIN_VALUE),
        new Integer(Integer.MAX_VALUE), new Integer(Integer.MIN_VALUE)};
  }

  public static Long[] createLongArray() {
    return new Long[] {
        new Long(Long.MAX_VALUE), new Long(Long.MIN_VALUE),
        new Long(Long.MAX_VALUE), new Long(Long.MIN_VALUE)};
  }

  public static boolean[] createPrimitiveBooleanArray() {
    return new boolean[] {true, true, false, false, true, false};
  }

  public static byte[] createPrimitiveByteArray() {
    return new byte[] {
        Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE};
  }

  public static char[] createPrimitiveCharArray() {
    return new char[] {
        Character.MAX_VALUE, Character.MIN_VALUE, Character.MAX_VALUE,
        Character.MIN_VALUE};
  }

  public static double[] createPrimitiveDoubleArray() {
    return new double[] {
        Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE};
  }

  public static float[] createPrimitiveFloatArray() {
    return new float[] {
        Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_VALUE};
  }

  public static int[] createPrimitiveIntegerArray() {
    return new int[] {
        Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
        Integer.MIN_VALUE};
  }

  public static long[] createPrimitiveLongArray() {
    return new long[] {
        Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
  }

  public static short[] createPrimitiveShortArray() {
    return new short[] {
        Short.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, Short.MIN_VALUE};
  }

  public static Short[] createShortArray() {
    return new Short[] {
        new Short(Short.MAX_VALUE), new Short(Short.MIN_VALUE),
        new Short(Short.MAX_VALUE), new Short(Short.MIN_VALUE)};
  }

  /*
   * Check names that collide with JS properties inherited from Object.prototype
   * to make sure they are handled properly.
   */
  public static String[] createStringArray() {
    return new String[] {null, "", "one", "two", "toString", "watch",
        "prototype", "eval", "valueOf", "constructor", "__proto__" };
  }

  public static Vector createVector() {
    Vector vector = new Vector();
    vector.add(new SerializableNode());
    vector.add(new SerializableList());
    vector.add(new SerializableMap());
    vector.add(new SerializableSet());
    vector.add(new SerializableVector());
    return vector;
  }

  static SerializableDoublyLinkedNode createAcyclicGraph() {
    SerializableDoublyLinkedNode head = new SerializableDoublyLinkedNode();
    head.setData("head");

    SerializableDoublyLinkedNode leftChild = new SerializableDoublyLinkedNode();
    leftChild.setData("lchild");

    SerializableDoublyLinkedNode rightChild = new SerializableDoublyLinkedNode();
    rightChild.setData("rchild");

    head.setLeftChild(leftChild);
    head.setRightChild(rightChild);

    return head;
  }

  static ArrayList createArrayList() {
    ArrayList list = new ArrayList();
    list.add(new SerializableNode());
    list.add(new SerializableList());
    list.add(new SerializableMap());
    list.add(new SerializableSet());
    list.add(new SerializableVector());
    return list;
  }

  static SerializableDoublyLinkedNode createComplexCyclicGraph() {
    SerializableDoublyLinkedNode n1 = new SerializableDoublyLinkedNode();
    n1.setData("n0");

    SerializableDoublyLinkedNode n2 = new SerializableDoublyLinkedNode();
    n2.setData("n1");

    SerializableDoublyLinkedNode n3 = new SerializableDoublyLinkedNode();
    n3.setData("n2");

    SerializableDoublyLinkedNode n4 = new SerializableDoublyLinkedNode();
    n4.setData("n3");

    n1.setLeftChild(n4);
    n1.setRightChild(n2);
    n2.setLeftChild(n1);
    n2.setRightChild(n3);
    n3.setLeftChild(n2);
    n3.setRightChild(n4);
    n4.setLeftChild(n3);
    n4.setRightChild(n1);

    return n1;
  }

  static SerializableClass createSerializableClass() {
    SerializableClass cls = new SerializableClass();
    IsSerializable[] elements = new IsSerializable[] {
        new SerializableClass(), new SerializableClass(),
        new SerializableClass(), new SerializableClass(),};

    cls.setElements(elements);
    cls.setElementRef(elements[3]);

    return cls;
  }

  static SerializableDoublyLinkedNode createTrivialCyclicGraph() {
    SerializableDoublyLinkedNode root = new SerializableDoublyLinkedNode();
    root.setData("head");
    root.setLeftChild(root);
    root.setRightChild(root);

    return root;
  }
}
