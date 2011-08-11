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

package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializedTypeViolationException;
import com.google.gwt.user.client.rpc.TestSetFactory.ReverseSorter;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Collections test cases for the type checking code found in server-side RPC
 * deserialization.
 */
@SuppressWarnings("rawtypes")
public class RPCTypeCheckCollectionsTest extends TestCase {

  /*
   * Test top-level generic type mismatches for all custom serialized container
   * classes. These are all tested using a class that involves a pathological
   * hash-code object. Hence checking that improper objects are not deserialized
   * at all (the test will time out if there is a problem).
   *   - ArrayList<Integer>: testListHashSetSpoofingList
   *   - ArraysAsList<Integer>: testListHashSetSpoofingList
   *   - HashMap<String, Integer>: testMapHashSetSpoofingMap
   *   - HashSet<Integer>: testSetHashSetSpoofingSet
   *   - IdentityHashMap<AClass, Integer>: testMapHashSetSpoofingMap
   *   - LinkedHashMap<String, Integer>: testMapHashSetSpoofingMap
   *   - LinkedHashSet<Integer>: testSetSpoofingSet
   *   - LinkedList<Integer>: testListHashSetSpoofingList
   *   - SingletonList<Integer>: testListHashSetSpoofingList
   *   - TreeMap<String, Integer>: testMapHashSetSpoofingMap
   *   - TreeSet<Integer>: testSetHashSetSpoofingSet
   *   - Vector<Integer>: testVectorHashSetSpoofingVector
   *   
   * Also tests raw version of all of these types, to verify no false positives
   * for type violations.
   *
   * Test nested generics, at least 2 levels deep:
   *   - HashMap<String, HashSet<Integer>>: testNestedGenerics
   *   - HashMap<String, HashMap<Integer, AClass>>: testNestedGenerics
   *   - HashMap<String, HashMap<Integer, List<Long>>>: testNestedGenerics
   *   - HashMap<String, HashMap>: testNestedGenerics
   */  
  
  /**
   * A class containing a method used to check type spoofing attacks on RPC
   * messages containing collection types.
   */
  public static class ContainerParamTestClass implements RemoteService {
    @SuppressWarnings("unused")
    public static void testList(List<Integer> arg1, List<Integer> arg2, List<Integer> arg3,
        List<Integer> arg4) {
    }

    @SuppressWarnings("unused")
    public static void testListRaw(List arg1, List arg2, List arg3, List arg4) {
    }

    @SuppressWarnings("unused")
    public static void testMap(Map<String, Integer> arg1, Map<String, Integer> arg2,
        Map<String, Integer> arg3, Map<String, Integer> arg4) { }

    @SuppressWarnings("unused")
    public static void testMapRaw(Map arg1, Map arg2, Map arg3, Map arg4) { }

    @SuppressWarnings("unused")
    public static void testSet(Set<Integer> arg1, Set<Integer> arg2, Set<Integer> arg3) { }

    @SuppressWarnings("unused")
    public static void testSetRaw(Set arg1, Set arg2, Set arg3) { }

    @SuppressWarnings("unused")
    public static void testVector(Vector<Integer> arg1) { }

    @SuppressWarnings("unused")
    public static void testVectorRaw(Vector arg1) { }

    @SuppressWarnings("unused")
    public static void testNestedGenerics(HashMap<String, HashSet<Integer>> arg1,
        HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>> arg2,
        HashMap<String, HashMap<Integer, List<Long>>> arg3) { }

    @SuppressWarnings("unused")
    public static void testNestedGenericsRaw(HashMap<String, HashSet> arg1,
        HashMap<String, HashMap> arg2,
        HashMap<String, HashMap<Integer, List>> arg3) { }
  }

  /**
   * Used to test a generic class that extends another generic class.
   * 
   * Also necessary because LinkedHashSet is not serializable alone.
   */
  public static class TestHashSet<T> extends LinkedHashSet<T> implements IsSerializable {
    // Does nothing different.
  }

  private static String generateArrayListHashSetSpoofingList() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testList");
      
      ArrayList<HashSet<Integer>> arrayList = new ArrayList<HashSet<Integer>>();
      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      arrayList.add(hashSet);
      strFactory.write(arrayList);
      
      Integer i = 54321;
      Integer j = 9876;
      List<Integer> arrayAsList = Arrays.asList(i, j);
      strFactory.write(arrayAsList);

      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.add(12345);
      linkedList.add(67890);
      strFactory.write(linkedList);
      
      Integer k = new Integer(45678);
      List<Integer> singletonList = Collections.singletonList(k);
      strFactory.write(singletonList);

      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArraysAsListHashSetSpoofingList() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testList");
      
      Integer i = 54321;
      Integer j = 9876;
      ArrayList<Integer> arrayList = new ArrayList<Integer>();
      arrayList.add(i);
      arrayList.add(j);
      strFactory.write(arrayList);
      
      List<HashSet> arrayAsList = Arrays.asList(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(arrayAsList);

      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.add(12345);
      linkedList.add(67890);
      strFactory.write(linkedList);
      
      Integer k = new Integer(45678);
      List<Integer> singletonList = Collections.singletonList(k);
      strFactory.write(singletonList);

      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArraysAsListHashSetSpoofingArraysAsList() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testList");
      
      List<HashSet> arrayAsList = Arrays.asList(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(arrayAsList);
      
      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.add(12345);
      linkedList.add(67890);
      strFactory.write(linkedList);
      
      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashMapKeyHashSetSpoofingHashMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<HashSet, Integer> hashMap = RPCTypeCheckFactory.generateTestHashMap();
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(12));
      identityHashMap.put("bar", new Integer(34));
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(56));
      linkedHashMap.put("bar", new Integer(78));
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashMapValueHashSetSpoofingHashMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, HashSet> hashMap = new HashMap<String, HashSet>();
      hashMap.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(12));
      identityHashMap.put("bar", new Integer(34));
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(56));
      linkedHashMap.put("bar", new Integer(78));
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashSetHashSetSpoofingSetInteger() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testSet");

      HashSet hashSet = RPCTypeCheckFactory.generateTestHashSet();
      strFactory.write(hashSet);

      ReverseSorter<Integer> sorter = new ReverseSorter<Integer>();
      TreeSet<Integer> treeSet = new TreeSet<Integer>(sorter);
      treeSet.add(12345);
      treeSet.add(67890);
      strFactory.write(treeSet);

      TestHashSet<Integer> treeHashSet = new TestHashSet<Integer>();
      treeHashSet.add(12345);
      treeHashSet.add(67890);
      strFactory.write(treeHashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIdentityHashMapKeyHashSetSpoofingIdentityHashMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", new Integer(12));
      hashMap.put("bar", new Integer(34));
      strFactory.write(hashMap);

      IdentityHashMap<HashSet, Integer> identityHashMap =
        RPCTypeCheckFactory.generateTestIdentityHashMap();
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(56));
      linkedHashMap.put("bar", new Integer(78));
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIdentityHashMapValueHashSetSpoofingIdentityHashMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", new Integer(12));
      hashMap.put("bar", new Integer(34));
      strFactory.write(hashMap);

      IdentityHashMap<String, HashSet> identityHashMap = new IdentityHashMap<String, HashSet>();
      identityHashMap.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(56));
      linkedHashMap.put("bar", new Integer(78));
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedHashMapKeyHashSetSpoofingLinkedHashMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", new Integer(12));
      hashMap.put("bar", new Integer(34));
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(56));
      identityHashMap.put("bar", new Integer(78));
      strFactory.write(identityHashMap);

      LinkedHashMap<HashSet, Integer> linkedHashMap =
        RPCTypeCheckFactory.generateTestLinkedHashMap();
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedHashMapValueHashSetSpoofingLinkedHashMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", new Integer(12));
      hashMap.put("bar", new Integer(34));
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(56));
      identityHashMap.put("bar", new Integer(78));
      strFactory.write(identityHashMap);

      LinkedHashMap<String, HashSet> linkedHashMap = new LinkedHashMap<String, HashSet>();
      linkedHashMap.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedHashSetHashSetSpoofingSetInteger() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testSet");

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      ReverseSorter<Integer> sorter = new ReverseSorter<Integer>();
      TreeSet<Integer> treeSet = new TreeSet<Integer>(sorter);
      treeSet.add(12345);
      treeSet.add(67890);
      strFactory.write(treeSet);

      LinkedHashSet linkedHashSet = RPCTypeCheckFactory.generateTestLinkedHashSet();
      strFactory.write(linkedHashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedListHashSetSpoofingList() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testList");

      ArrayList<Integer> arrayList = new ArrayList<Integer>();
      arrayList.add(12345);
      arrayList.add(67890);
      strFactory.write(arrayList);

      Integer i = 54321;
      Integer j = 9876;
      List<Integer> arrayAsList = Arrays.asList(i, j);
      strFactory.write(arrayAsList);

      LinkedList<HashSet> linkedList = new LinkedList<HashSet>();
      linkedList.add(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(linkedList);

      Integer k = new Integer(45678);
      List<Integer> singletonList = Collections.singletonList(k);
      strFactory.write(singletonList);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateListStringSpoofingListInteger() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testList");

      ArrayList<String> arrayList = new ArrayList<String>();
      arrayList.add("foo");
      arrayList.add("bar");
      strFactory.write(arrayList);

      LinkedList<String> linkedList = new LinkedList<String>();
      linkedList.add("foo");
      linkedList.add("bar");
      strFactory.write(linkedList);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateListValid(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, methodName);

      ArrayList<Integer> arrayList = new ArrayList<Integer>();
      arrayList.add(12345);
      arrayList.add(67890);
      strFactory.write(arrayList);

      Integer i = 54321;
      Integer j = 9876;
      List<Integer> arrayAsList = Arrays.asList(i, j);
      strFactory.write(arrayAsList);

      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.add(12345);
      linkedList.add(67890);
      strFactory.write(linkedList);

      Integer k = new Integer(45678);
      List<Integer> singletonList = Collections.singletonList(k);
      strFactory.write(singletonList);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateMapRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMapRaw");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", 1020);
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(12));
      identityHashMap.put("bar", new Integer(34));
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(56));
      linkedHashMap.put("bar", new Integer(78));
      strFactory.write(linkedHashMap);

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", new Integer(90));
      treeMap.put("bar", new Integer(12));
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateNestedGenericsA() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testNestedGenerics");

      HashMap<String, HashSet<HashSet>> arg1 = new HashMap<String, HashSet<HashSet>>();
      HashSet<HashSet> entry1 = RPCTypeCheckFactory.generateTestHashSetHashSet();
      arg1.put("foo", entry1);
      strFactory.write(arg1);

      HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>> arg2 =
          new HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>>();
      HashMap<Integer, RPCTypeCheckTest.AClass> entry2 =
          new HashMap<Integer, RPCTypeCheckTest.AClass>();
      entry2.put(12345, new RPCTypeCheckTest.AClass(1));
      arg2.put("bar", entry2);
      strFactory.write(arg2);

      HashMap<String, HashMap<Integer, List<Long>>> arg3 =
          new HashMap<String, HashMap<Integer, List<Long>>>();
      HashMap<Integer, List<Long>> entry3 = new HashMap<Integer, List<Long>>();
      List<Long> entry4 = new LinkedList<Long>();
      entry4.add(new Long(987654321));
      entry3.put(67890, entry4);
      arg3.put("baz", entry3);
      strFactory.write(arg3);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateNestedGenericsB() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testNestedGenerics");

      HashMap<String, HashSet<Integer>> arg1 = new HashMap<String, HashSet<Integer>>();
      HashSet<Integer> entry1 = new HashSet<Integer>();
      entry1.add(12345);
      arg1.put("foo", entry1);
      strFactory.write(arg1);

      HashMap<String, HashMap<Integer, HashSet>> arg2 =
          new HashMap<String, HashMap<Integer, HashSet>>();
      HashMap<Integer, HashSet> entry2 = new HashMap<Integer, HashSet>();
      entry2.put(12345, RPCTypeCheckFactory.generateTestHashSet());
      arg2.put("bar", entry2);
      strFactory.write(arg2);

      HashMap<String, HashMap<Integer, List<Long>>> arg3 =
          new HashMap<String, HashMap<Integer, List<Long>>>();
      HashMap<Integer, List<Long>> entry3 = new HashMap<Integer, List<Long>>();
      List<Long> entry4 = new LinkedList<Long>();
      entry4.add(new Long(987654321));
      entry3.put(67890, entry4);
      arg3.put("baz", entry3);
      strFactory.write(arg3);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateNestedGenericsC() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testNestedGenerics");

      HashMap<String, HashSet<Integer>> arg1 = new HashMap<String, HashSet<Integer>>();
      HashSet<Integer> entry1 = new HashSet<Integer>();
      entry1.add(12345);
      arg1.put("foo", entry1);
      strFactory.write(arg1);

      HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>> arg2 =
          new HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>>();
      HashMap<Integer, RPCTypeCheckTest.AClass> entry2 =
          new HashMap<Integer, RPCTypeCheckTest.AClass>();
      entry2.put(12345, new RPCTypeCheckTest.AClass(1));
      arg2.put("bar", entry2);
      strFactory.write(arg2);

      HashMap<String, HashMap<Integer, List<HashSet>>> arg3 =
          new HashMap<String, HashMap<Integer, List<HashSet>>>();
      HashMap<Integer, List<HashSet>> entry3 = new HashMap<Integer, List<HashSet>>();
      List<HashSet> entry4 = new LinkedList<HashSet>();
      entry4.add(RPCTypeCheckFactory.generateTestHashSet());
      entry3.put(67890, entry4);
      arg3.put("baz", entry3);
      strFactory.write(arg3);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateNestedGenericsRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testNestedGenericsRaw");

      HashMap<String, HashSet<Integer>> arg1 = new HashMap<String, HashSet<Integer>>();
      HashSet<Integer> entry1 = new HashSet<Integer>();
      entry1.add(12345);
      arg1.put("foo", entry1);
      strFactory.write(arg1);

      HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>> arg2 =
          new HashMap<String, HashMap<Integer, RPCTypeCheckTest.AClass>>();
      HashMap<Integer, RPCTypeCheckTest.AClass> entry2 =
          new HashMap<Integer, RPCTypeCheckTest.AClass>();
      entry2.put(12345, new RPCTypeCheckTest.AClass(1));
      arg2.put("bar", entry2);
      strFactory.write(arg2);

      HashMap<String, HashMap<Integer, List<Long>>> arg3 =
          new HashMap<String, HashMap<Integer, List<Long>>>();
      HashMap<Integer, List<Long>> entry3 = new HashMap<Integer, List<Long>>();
      List<Long> entry4 = new LinkedList<Long>();
      entry4.add(new Long(987654321));
      entry3.put(67890, entry4);
      arg3.put("baz", entry3);
      strFactory.write(arg3);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateSetStringSpoofingSetInteger() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testSet");

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      ReverseSorter<String> sorter = new ReverseSorter<String>();
      TreeSet<String> treeSet = new TreeSet<String>(sorter);
      treeSet.add("foo");
      treeSet.add("bar");
      strFactory.write(treeSet);

      TestHashSet<Integer> testHashSet = new TestHashSet<Integer>();
      testHashSet.add(12345);
      testHashSet.add(67890);
      strFactory.write(testHashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateSetValid(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, methodName);

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      ReverseSorter<Integer> sorter = new ReverseSorter<Integer>();
      TreeSet<Integer> treeSet = new TreeSet<Integer>(sorter);
      treeSet.add(12345);
      treeSet.add(67890);
      strFactory.write(treeSet);

      TestHashSet<Integer> treeHashSet = new TestHashSet<Integer>();
      treeHashSet.add(12345);
      treeHashSet.add(67890);
      strFactory.write(treeHashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateSingletonListHashSetSpoofingList() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testList");

      ArrayList<Integer> arrayList = new ArrayList<Integer>();
      arrayList.add(12345);
      arrayList.add(67890);
      strFactory.write(arrayList);

      Integer i = 54321;
      Integer j = 9876;
      List<Integer> arrayAsList = Arrays.asList(i, j);
      strFactory.write(arrayAsList);

      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.add(12345);
      linkedList.add(67890);
      strFactory.write(linkedList);

      List<HashSet> singletonList =
          Collections.singletonList(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(singletonList);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateTreeMapKeyHashSetSpoofingTreeMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", new Integer(12));
      hashMap.put("bar", new Integer(34));
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(56));
      identityHashMap.put("bar", new Integer(78));
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(90));
      linkedHashMap.put("bar", new Integer(12));
      strFactory.write(linkedHashMap);

      TreeMap<HashSet, Integer> treeMap = new TreeMap<HashSet, Integer>();
      treeMap.put(RPCTypeCheckFactory.generateTestHashSet(), 12345);
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateTreeMapValueHashSetSpoofingTreeMap() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testMap");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", new Integer(12));
      hashMap.put("bar", new Integer(34));
      strFactory.write(hashMap);

      IdentityHashMap<String, Integer> identityHashMap = new IdentityHashMap<String, Integer>();
      identityHashMap.put("foo", new Integer(56));
      identityHashMap.put("bar", new Integer(78));
      strFactory.write(identityHashMap);

      LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
      linkedHashMap.put("foo", new Integer(90));
      linkedHashMap.put("bar", new Integer(12));
      strFactory.write(linkedHashMap);

      TreeMap<String, HashSet> treeMap = new TreeMap<String, HashSet>();
      treeMap.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(treeMap);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateTreeSetHashSetSpoofingSetInteger() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testSet");

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      ReverseSorter sorter = new ReverseSorter();
      TreeSet treeSet = new TreeSet(sorter);
      treeSet.add(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(treeSet);

      TestHashSet<Integer> treeHashSet = new TestHashSet<Integer>();
      treeHashSet.add(12345);
      treeHashSet.add(67890);
      strFactory.write(treeHashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateVectorHashSetSpoofingVector() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testVector");

      Vector<HashSet> vector = new Vector<HashSet>();
      vector.add(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(vector);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateVectorRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ContainerParamTestClass.class, "testVectorRaw");

      Vector<Integer> vector = new Vector<Integer>();
      vector.add(12345);
      vector.add(67890);
      strFactory.write(vector);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  /**
   * This checks that a List generated by Arrays.asList correctly reports that
   * it is an incorrect type.
   */
  public void testArraysAsListHashSetSpoofingArraysAsList() {
    try {
      RPC.decodeRequest(generateArraysAsListHashSetSpoofingArraysAsList());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testArraysAsListHashSetSpoofingArraysAsList");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that List correctly handles correct types, and reports when it
   * gets and incorrectly parameterized generic type.
   */
  public void testListSpoofingList() {
    try {
      RPC.decodeRequest(generateListValid("testList"));
    } catch (Exception e) {
      fail("Received unexpected Exception from testListSpoofingList (a)" + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateListStringSpoofingListInteger());
      fail("Expected IncompatibleRemoteServiceException from testListSpoofingList (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*String.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateArrayListHashSetSpoofingList());
      fail("Expected IncompatibleRemoteServiceException from testListSpoofingList (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateArraysAsListHashSetSpoofingList());
      fail("Expected IncompatibleRemoteServiceException from testListSpoofingList (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateLinkedListHashSetSpoofingList());
      fail("Expected IncompatibleRemoteServiceException from testListSpoofingList (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateSingletonListHashSetSpoofingList());
      fail("Expected IncompatibleRemoteServiceException from testListSpoofingList (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that List correctly handles correct types, and reports when it
   * gets and incorrectly parameterized generic type.
   */
  public void testListSpoofingListRaw() {
    try {
      RPC.decodeRequest(generateListValid("testListRaw"));
    } catch (Exception e) {
      fail("Received unexpected Exception from testListSpoofingListRaw" + e.getMessage());
    }
  }

  /**
   * This checks that Maps correctly reports when it's generic parameter types
   * do not match the actual contents.
   */
  public void testMapHashSetSpoofingMap() {
    try {
      RPC.decodeRequest(generateHashMapKeyHashSetSpoofingHashMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateHashMapValueHashSetSpoofingHashMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateIdentityHashMapKeyHashSetSpoofingIdentityHashMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateIdentityHashMapValueHashSetSpoofingIdentityHashMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateLinkedHashMapKeyHashSetSpoofingLinkedHashMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateLinkedHashMapValueHashSetSpoofingLinkedHashMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (6)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateTreeMapKeyHashSetSpoofingTreeMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (7)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateTreeMapValueHashSetSpoofingTreeMap());
      fail("Expected IncompatibleRemoteServiceException from " +
          "testHashMapHashSetSpoofingHashMap (8)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that Maps correctly processes raw maps without type information.
   */
  public void testMapRaw() {
    try {
      RPC.decodeRequest(generateMapRaw());
    } catch (Exception e) {
      fail("Unexpected Exception from testMapRaw: " + e.getMessage());
    }
  }

  /**
   * This checks that nested generics types are checked correctly.
   */
  public void testNestedGenerics() {
    try {
      RPC.decodeRequest(generateNestedGenericsA());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenerics (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateNestedGenericsB());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenerics (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*AClass.*"));
    }
    try {
      RPC.decodeRequest(generateNestedGenericsC());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenerics (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Long.*"));
    }
  }

  /**
   * This checks that raw nested generics types are checked correctly.
   */
  public void testNestedGenericsRaw() {
    try {
      RPC.decodeRequest(generateNestedGenericsRaw());
    } catch (Exception e) {
      fail("Unexpected Exception from testNestedGenericsRaw: " + e.getMessage());
    }
  }

  /**
   * This checks that Set correctly handles correct types, and reports when it
   * gets and incorrectly parameterized generic type.
   */
  public void testSetHashSetSpoofingSet() {
    try {
      RPC.decodeRequest(generateSetValid("testSet"));
    } catch (Exception e) {
      fail("Received unexpected Exception from testSetSpoofingSet (1)" + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateSetStringSpoofingSetInteger());
      fail("Expected IncompatibleRemoteServiceException from testSetSpoofingSet (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*String.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateHashSetHashSetSpoofingSetInteger());
      fail("Expected IncompatibleRemoteServiceException from testSetSpoofingSet (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateLinkedHashSetHashSetSpoofingSetInteger());
      fail("Expected IncompatibleRemoteServiceException from testSetSpoofingSet (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateTreeSetHashSetSpoofingSetInteger());
      fail("Expected IncompatibleRemoteServiceException from testSetSpoofingSet (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that a raw Set does not generate errors.
   */
  public void testSetRaw() {
    try {
      RPC.decodeRequest(generateSetValid("testSetRaw"));
    } catch (Exception e) {
      fail("Received unexpected Exception from testSetRaw:" + e.getMessage());
    }
  }

  /**
   * This checks that Vector correctly reports an incorrect generic type.
   */
  public void testVectorHashSetSpoofingVector() {
    try {
      RPC.decodeRequest(generateVectorHashSetSpoofingVector());
      fail("Expected IncompatibleRemoteServiceException from testVectorHashSetSpoofingVector");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that a raw Vector allows any type. 
   */
  public void testVectorRaw() {
    try {
      RPC.decodeRequest(generateVectorRaw());
    } catch (Exception e) {
      fail("Received unexpected Exception from testVectorRaw:" + e.getMessage());
    }
  }
}
