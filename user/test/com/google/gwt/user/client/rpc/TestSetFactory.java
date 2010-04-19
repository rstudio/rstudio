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

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Creates test collections.
 */
public class TestSetFactory {

  /**
   * Base type for single-use marker types to independently check type parameter
   * exposure in various collections.
   */
  public static class MarkerBase implements IsSerializable {
    private String value;

    public MarkerBase(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof MarkerBase && obj.getClass() == this.getClass()) {
        MarkerBase other = (MarkerBase) obj;
        return value == other.value
            || (value != null && value.equals(other.value));
      }
      return false;
    }

    public String getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return value;
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeArrayList extends MarkerBase {
    public MarkerTypeArrayList(String value) {
      super(value);
    }

    MarkerTypeArrayList() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeArraysAsList extends MarkerBase {

    public MarkerTypeArraysAsList(String value) {
      super(value);
    }

    MarkerTypeArraysAsList() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various empty collections.
   */
  public static final class MarkerTypeEmpty extends MarkerBase {
    MarkerTypeEmpty() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static enum MarkerTypeEnum {
    A, B, C;
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeHashMap extends MarkerBase {

    public MarkerTypeHashMap(String value) {
      super(value);
    }

    MarkerTypeHashMap() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeHashSet extends MarkerBase {

    public MarkerTypeHashSet(String value) {
      super(value);
    }

    MarkerTypeHashSet() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeLinkedHashMap extends MarkerBase {

    public MarkerTypeLinkedHashMap(String value) {
      super(value);
    }

    MarkerTypeLinkedHashMap() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeLinkedHashSet extends MarkerBase {

    public MarkerTypeLinkedHashSet(String value) {
      super(value);
    }

    MarkerTypeLinkedHashSet() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * singleton collections.
   */
  public static final class MarkerTypeSingleton extends MarkerBase {
    MarkerTypeSingleton() {
      super("singleton");
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeTreeMap extends MarkerBase {

    public MarkerTypeTreeMap(String value) {
      super(value);
    }

    MarkerTypeTreeMap() {
      super(null);
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeTreeSet extends MarkerBase implements
      Comparable<MarkerTypeTreeSet> {

    public MarkerTypeTreeSet(String value) {
      super(value);
    }

    MarkerTypeTreeSet() {
      super(null);
    }

    // if getValue() returns null, a null-pointer expection will be thrown,
    // which is the intended effect
    public int compareTo(MarkerTypeTreeSet arg0) {
      return getValue().compareTo(arg0.getValue());
    }
  }

  /**
   * A single-use marker type to independently check type parameter exposure in
   * various collections.
   */
  public static final class MarkerTypeVector extends MarkerBase {

    public MarkerTypeVector(String value) {
      super(value);
    }

    MarkerTypeVector() {
      super(null);
    }
  }

  /**
   * TODO: document me.
   */
  public static class SerializableDoublyLinkedNode implements IsSerializable {
    protected String data;

    protected SerializableDoublyLinkedNode leftChild;

    protected SerializableDoublyLinkedNode rightChild;

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
  }

  /**
   * Test reference cycles where the reference graph passes through a
   * CustomFieldSerializer.
   */
  public static final class SerializableGraphWithCFS implements Serializable {
    private ArrayList<SerializableGraphWithCFS> array;
    private SerializableGraphWithCFS parent;

    public SerializableGraphWithCFS() {
      array = new ArrayList<SerializableGraphWithCFS>();
      array.add(new SerializableGraphWithCFS(this));
    }

    public SerializableGraphWithCFS(SerializableGraphWithCFS parent) {
      this.parent = parent;
    }

    public ArrayList<SerializableGraphWithCFS> getArray() {
      return array;
    }

    public SerializableGraphWithCFS getParent() {
      return parent;
    }
  }

  /**
   * Tests that classes with a private no-arg constructor can be serialized.
   */
  public static class SerializablePrivateNoArg implements IsSerializable {
    private int value;

    public SerializablePrivateNoArg(int value) {
      this();
      this.value = value;
    }

    private SerializablePrivateNoArg() {
    }

    public int getValue() {
      return value;
    }
  }

  /**
   * TODO: document me.
   */
  public static class SerializableWithTwoArrays implements IsSerializable {
    String[] one;
    String[] two;
  }

  /**
   * TODO: document me.
   */
  public static class UnserializableNode {
  }

  static class ReverseSorter<T extends Comparable<T>> implements Comparator<T>,
      Serializable {

    // for gwt-serialization
    ReverseSorter() {
    }

    public int compare(T a, T b) {
      // Explicit null check to match JRE specs
      if (a == null || b == null) {
        throw new NullPointerException();
      }
      return b.compareTo(a);
    }

    @Override
    public boolean equals(Object ob) {
      if (!(ob instanceof ReverseSorter)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  public static ArrayList<MarkerTypeArrayList> createArrayList() {
    ArrayList<MarkerTypeArrayList> list = new ArrayList<MarkerTypeArrayList>();
    list.add(new MarkerTypeArrayList("foo"));
    list.add(new MarkerTypeArrayList("bar"));
    list.add(new MarkerTypeArrayList("baz"));
    list.add(new MarkerTypeArrayList("bal"));
    list.add(new MarkerTypeArrayList("w00t"));
    return list;
  }

  public static ArrayList<Void> createArrayListVoid() {
    ArrayList<Void> list = new ArrayList<Void>();
    list.add(null);
    list.add(null);
    return list;
  }

  public static List<MarkerTypeArraysAsList> createArraysAsList() {
    return Arrays.asList(new MarkerTypeArraysAsList("foo"),
        new MarkerTypeArraysAsList("bar"), new MarkerTypeArraysAsList("baz"),
        new MarkerTypeArraysAsList("bal"), new MarkerTypeArraysAsList("w00t"));
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

  public static SerializableGraphWithCFS createComplexCyclicGraphWithCFS() {
    return new SerializableGraphWithCFS();
  }

  @SuppressWarnings("deprecation")
  public static Date[] createDateArray() {
    return new Date[] {
        new Date(1992 - 1900, 9, 18), new Date(1997 - 1900, 6, 6)};
  }

  public static Double[] createDoubleArray() {
    return new Double[] {
        new Double(Double.MAX_VALUE), new Double(Double.MIN_VALUE),
        new Double(Double.MAX_VALUE), new Double(Double.MIN_VALUE)};
  }

  public static List<MarkerTypeEmpty> createEmptyList() {
    return java.util.Collections.emptyList();
  }

  public static Map<MarkerTypeEmpty, MarkerTypeEmpty> createEmptyMap() {
    return java.util.Collections.emptyMap();
  }

  public static Set<MarkerTypeEmpty> createEmptySet() {
    return java.util.Collections.emptySet();
  }

  public static Enum<?>[] createEnumArray() {
    return new Enum<?>[] {
        MarkerTypeEnum.A, MarkerTypeEnum.B, MarkerTypeEnum.C, MarkerTypeEnum.A,};
  }

  public static Float[] createFloatArray() {
    return new Float[] {
        new Float(Float.MAX_VALUE), new Float(Float.MIN_VALUE),
        new Float(Float.MAX_VALUE), new Float(Float.MIN_VALUE)};
  }

  public static HashMap<String, MarkerTypeHashMap> createHashMap() {
    HashMap<String, MarkerTypeHashMap> map = new HashMap<String, MarkerTypeHashMap>();
    map.put("foo", new MarkerTypeHashMap("foo"));
    map.put("bar", new MarkerTypeHashMap("bar"));
    map.put("baz", new MarkerTypeHashMap("baz"));
    map.put("bal", new MarkerTypeHashMap("bal"));
    map.put("w00t", new MarkerTypeHashMap("w00t"));
    return map;
  }

  public static HashSet<MarkerTypeHashSet> createHashSet() {
    HashSet<MarkerTypeHashSet> set = new HashSet<MarkerTypeHashSet>();
    set.add(new MarkerTypeHashSet("foo"));
    set.add(new MarkerTypeHashSet("bar"));
    set.add(new MarkerTypeHashSet("baz"));
    set.add(new MarkerTypeHashSet("bal"));
    set.add(new MarkerTypeHashSet("w00t"));
    return set;
  }

  public static Integer[] createIntegerArray() {
    return new Integer[] {
        new Integer(Integer.MAX_VALUE), new Integer(Integer.MIN_VALUE),
        new Integer(Integer.MAX_VALUE), new Integer(Integer.MIN_VALUE)};
  }

  public static LinkedHashMap<String, MarkerTypeLinkedHashMap> createLinkedHashMap() {
    LinkedHashMap<String, MarkerTypeLinkedHashMap> map = new LinkedHashMap<String, MarkerTypeLinkedHashMap>();
    map.put("foo", new MarkerTypeLinkedHashMap("foo"));
    map.put("bar", new MarkerTypeLinkedHashMap("bar"));
    map.put("baz", new MarkerTypeLinkedHashMap("baz"));
    map.put("bal", new MarkerTypeLinkedHashMap("bal"));
    map.put("w00t", new MarkerTypeLinkedHashMap("w00t"));
    return map;
  }

  public static LinkedHashSet<MarkerTypeLinkedHashSet> createLinkedHashSet() {
    LinkedHashSet<MarkerTypeLinkedHashSet> set = new LinkedHashSet<MarkerTypeLinkedHashSet>();
    set.add(new MarkerTypeLinkedHashSet("foo"));
    set.add(new MarkerTypeLinkedHashSet("bar"));
    set.add(new MarkerTypeLinkedHashSet("baz"));
    set.add(new MarkerTypeLinkedHashSet("bal"));
    set.add(new MarkerTypeLinkedHashSet("w00t"));
    return set;
  }

  public static Long[] createLongArray() {
    return new Long[] {
        new Long(Long.MAX_VALUE), new Long(Long.MIN_VALUE),
        new Long(Long.MAX_VALUE), new Long(Long.MIN_VALUE)};
  }

  public static LinkedHashMap<String, MarkerTypeLinkedHashMap> createLRULinkedHashMap() {
    LinkedHashMap<String, MarkerTypeLinkedHashMap> map = new LinkedHashMap<String, MarkerTypeLinkedHashMap>(
        100, 1.0f, true);
    map.put("foo", new MarkerTypeLinkedHashMap("foo"));
    map.put("bar", new MarkerTypeLinkedHashMap("bar"));
    map.put("baz", new MarkerTypeLinkedHashMap("baz"));
    map.put("bal", new MarkerTypeLinkedHashMap("bal"));
    map.put("w00t", new MarkerTypeLinkedHashMap("w00t"));
    return map;
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

  public static SerializablePrivateNoArg createPrivateNoArg() {
    return new SerializablePrivateNoArg(1);
  }

  public static Short[] createShortArray() {
    return new Short[] {
        new Short(Short.MAX_VALUE), new Short(Short.MIN_VALUE),
        new Short(Short.MAX_VALUE), new Short(Short.MIN_VALUE)};
  }

  public static List<MarkerTypeSingleton> createSingletonList() {
    return java.util.Collections.singletonList(new MarkerTypeSingleton());
  }

  public static java.sql.Date[] createSqlDateArray() {
    return new java.sql.Date[] {
        new java.sql.Date(500L), new java.sql.Date(500000000L)};
  }

  public static Time[] createSqlTimeArray() {
    return new Time[] {new Time(500L), new Time(5000000L)};
  }

  public static Timestamp[] createSqlTimestampArray() {
    return new Timestamp[] {new Timestamp(500L), new Timestamp(5000000L)};
  }

  /*
   * Check names that collide with JS properties inherited from Object.prototype
   * to make sure they are handled properly.
   */
  public static String[] createStringArray() {
    return new String[] {
        null, "", "one", "two", "toString", "watch", "prototype", "eval",
        "valueOf", "constructor", "__proto__"};
  }

  public static TreeMap<String, MarkerTypeTreeMap> createTreeMap(
      boolean defaultComparator) {
    TreeMap<String, MarkerTypeTreeMap> map;
    if (defaultComparator) {
      map = new TreeMap<String, MarkerTypeTreeMap>();
    } else {
      map = new TreeMap<String, MarkerTypeTreeMap>(new ReverseSorter<String>());
    }
    map.put("foo", new MarkerTypeTreeMap("foo"));
    map.put("bar", new MarkerTypeTreeMap("bar"));
    map.put("baz", new MarkerTypeTreeMap("baz"));
    map.put("bal", new MarkerTypeTreeMap("bal"));
    map.put("w00t", new MarkerTypeTreeMap("w00t"));
    return map;
  }

  public static TreeSet<MarkerTypeTreeSet> createTreeSet(
      boolean defaultComparator) {
    TreeSet<MarkerTypeTreeSet> set;
    if (defaultComparator) {
      set = new TreeSet<MarkerTypeTreeSet>();
    } else {
      set = new TreeSet<MarkerTypeTreeSet>(
          new ReverseSorter<MarkerTypeTreeSet>());
    }
    set.add(new MarkerTypeTreeSet("foo"));
    set.add(new MarkerTypeTreeSet("bar"));
    set.add(new MarkerTypeTreeSet("baz"));
    set.add(new MarkerTypeTreeSet("bal"));
    set.add(new MarkerTypeTreeSet("w00t"));
    return set;
  }

  public static Vector<MarkerTypeVector> createVector() {
    Vector<MarkerTypeVector> vector = new Vector<MarkerTypeVector>();
    vector.add(new MarkerTypeVector("foo"));
    vector.add(new MarkerTypeVector("bar"));
    vector.add(new MarkerTypeVector("baz"));
    vector.add(new MarkerTypeVector("bal"));
    vector.add(new MarkerTypeVector("w00t"));
    return vector;
  }

  public static int[] createVeryLargeArray() {
    return new int[1 << 20];
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

  static SerializableWithTwoArrays createDoublyReferencedArray() {
    SerializableWithTwoArrays o = new SerializableWithTwoArrays();
    o.two = o.one = createStringArray();
    return o;
  }

  static SerializableDoublyLinkedNode createTrivialCyclicGraph() {
    SerializableDoublyLinkedNode root = new SerializableDoublyLinkedNode();
    root.setData("head");
    root.setLeftChild(root);
    root.setRightChild(root);

    return root;
  }
}
