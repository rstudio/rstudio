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

import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArrayList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArraysAsList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptySet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnum;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnumMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeSingleton;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeVector;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
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
 * The Remote Service Async class for GWT RPC Collections testing.
 */
public interface CollectionsTestServiceAsync {
  void echo(ArrayList<MarkerTypeArrayList> value,
      AsyncCallback<ArrayList<MarkerTypeArrayList>> callback);

  // For Collections.emptyList()
  void echo(List<MarkerTypeEmptyList> value,
      AsyncCallback<List<MarkerTypeEmptyList>> callback);

  // For Collections.emptyMap()
  void echo(Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue> value,
      AsyncCallback<Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue>> callback);

  // For Collections.emptySet()
  void echo(Set<MarkerTypeEmptySet> value,
      AsyncCallback<Set<MarkerTypeEmptySet>> callback);

  void echo(boolean[] value, AsyncCallback<boolean[]> callback);

  void echo(Boolean[] value, AsyncCallback<Boolean[]> callback);

  void echo(byte[] value, AsyncCallback<byte[]> callback);

  void echo(Byte[] value, AsyncCallback<Byte[]> callback);

  void echo(char[] value, AsyncCallback<char[]> callback);

  void echo(Character[] value, AsyncCallback<Character[]> callback);

  void echo(Date[] date, AsyncCallback<Date[]> callback);

  void echo(double[] value, AsyncCallback<double[]> callback);

  void echo(Double[] value, AsyncCallback<Double[]> callback);

  void echo(Enum<?>[] value, AsyncCallback<Enum<?>[]> callback);

  void echo(float[] value, AsyncCallback<float[]> callback);

  void echo(Float[] value, AsyncCallback<Float[]> callback);

  void echo(EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> value,
      AsyncCallback<EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue>> callback);

  void echo(HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue> value,
      AsyncCallback<HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue>> callback);

  void echo(HashSet<MarkerTypeHashSet> value,
      AsyncCallback<HashSet<MarkerTypeHashSet>> callback);

  void echo(IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> value,
      AsyncCallback<IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue>> callback);

  void echo(int[] value, AsyncCallback<int[]> callback);

  void echo(Integer[] value, AsyncCallback<Integer[]> callback);

  void echo(java.sql.Date[] value, AsyncCallback<java.sql.Date[]> callback);

  void echo(LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> value,
      AsyncCallback<LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue>> callback);

  void echo(LinkedHashSet<MarkerTypeLinkedHashSet> value,
      AsyncCallback<LinkedHashSet<MarkerTypeLinkedHashSet>> callback);

  void echo(LinkedList<MarkerTypeLinkedList> value,
      AsyncCallback<LinkedList<MarkerTypeLinkedList>> callback);

  void echo(long[] value, AsyncCallback<long[]> callback);

  void echo(Long[] value, AsyncCallback<Long[]> callback);

  void echo(short[] value, AsyncCallback<short[]> callback);

  void echo(Short[] value, AsyncCallback<Short[]> callback);

  void echo(String[] value, AsyncCallback<String[]> callback);

  void echo(String[][] value, AsyncCallback<String[][]> callback);

  void echo(Time[] value, AsyncCallback<Time[]> callback);

  void echo(TreeMap<String, MarkerTypeTreeMap> value, boolean option,
      AsyncCallback<TreeMap<String, MarkerTypeTreeMap>> callback);

  void echo(TreeSet<MarkerTypeTreeSet> value, boolean option,
      AsyncCallback<TreeSet<MarkerTypeTreeSet>> callback);

  void echo(Timestamp[] value, AsyncCallback<Timestamp[]> callback);

  void echo(Vector<MarkerTypeVector> value,
      AsyncCallback<Vector<MarkerTypeVector>> callback);

  void echoArrayListVoid(ArrayList<Void> value,
      AsyncCallback<ArrayList<Void>> callback);

  void echoArraysAsList(List<MarkerTypeArraysAsList> value,
      AsyncCallback<List<MarkerTypeArraysAsList>> callback);

  void echoEmptyEnumMap(EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected,
      AsyncCallback<EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue>> asyncCallback);

  void echoEnumKey(EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> value,
      AsyncCallback<EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue>> callback);

  void echoEnumKey(IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> value,
      AsyncCallback<IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue>> callback);

  // For Collections.singletonList()
  void echoSingletonList(List<MarkerTypeSingleton> value,
      AsyncCallback<List<MarkerTypeSingleton>> callback);
}
