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
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapValue;
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
import java.util.HashSet;
import java.util.HashMap;
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
 * The Remote Service for testing GWT RPC for Collections.
 */
@RemoteServiceRelativePath("collections")
public interface CollectionsTestService extends RemoteService {

  /**
   * A custom exception for the Collection test.
   */
  final class CollectionsTestServiceException extends Exception {
    public CollectionsTestServiceException() {
    }

    public CollectionsTestServiceException(String msg) {
      super(msg);
    }
  }

  ArrayList<MarkerTypeArrayList> echo(ArrayList<MarkerTypeArrayList> value)
      throws CollectionsTestServiceException;

  // For Collections.emptyList()
  List<MarkerTypeEmptyList> echo(List<MarkerTypeEmptyList> value)
      throws CollectionsTestServiceException;

  // For Collections.emptyMap()
  Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue> echo(
      Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue> value) throws CollectionsTestServiceException;

  // For Collections.emptySet()
  Set<MarkerTypeEmptySet> echo(Set<MarkerTypeEmptySet> value)
      throws CollectionsTestServiceException;

  boolean[] echo(boolean[] value) throws CollectionsTestServiceException;

  Boolean[] echo(Boolean[] value) throws CollectionsTestServiceException;

  byte[] echo(byte[] value) throws CollectionsTestServiceException;

  Byte[] echo(Byte[] value) throws CollectionsTestServiceException;

  char[] echo(char[] value) throws CollectionsTestServiceException;

  Character[] echo(Character[] value) throws CollectionsTestServiceException;

  Date[] echo(Date[] date) throws CollectionsTestServiceException;

  double[] echo(double[] value) throws CollectionsTestServiceException;

  Double[] echo(Double[] value) throws CollectionsTestServiceException;

  Enum<?>[] echo(Enum<?>[] value) throws CollectionsTestServiceException;

  float[] echo(float[] value) throws CollectionsTestServiceException;

  Float[] echo(Float[] value) throws CollectionsTestServiceException;

  EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> echo(
      EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> value)
      throws CollectionsTestServiceException;
 
  HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue> echo(
      HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue> value)
      throws CollectionsTestServiceException;

  HashSet<MarkerTypeHashSet> echo(HashSet<MarkerTypeHashSet> value)
      throws CollectionsTestServiceException;

  IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> echo(
      IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> value)
      throws CollectionsTestServiceException;

  int[] echo(int[] value) throws CollectionsTestServiceException;

  Integer[] echo(Integer[] value) throws CollectionsTestServiceException;

  java.sql.Date[] echo(java.sql.Date[] value)
      throws CollectionsTestServiceException;

  LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> echo(
      LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> value)
      throws CollectionsTestServiceException;

  LinkedHashSet<MarkerTypeLinkedHashSet> echo(
      LinkedHashSet<MarkerTypeLinkedHashSet> value)
      throws CollectionsTestServiceException;

  LinkedList<MarkerTypeLinkedList> echo(LinkedList<MarkerTypeLinkedList> value)
      throws CollectionsTestServiceException;

  long[] echo(long[] value) throws CollectionsTestServiceException;

  Long[] echo(Long[] value) throws CollectionsTestServiceException;

  short[] echo(short[] value) throws CollectionsTestServiceException;

  Short[] echo(Short[] value) throws CollectionsTestServiceException;

  String[] echo(String[] value) throws CollectionsTestServiceException;

  String[][] echo(String[][] value) throws CollectionsTestServiceException;

  Time[] echo(Time[] value) throws CollectionsTestServiceException;

  Timestamp[] echo(Timestamp[] value) throws CollectionsTestServiceException;

  TreeMap<String, MarkerTypeTreeMap> echo(
      TreeMap<String, MarkerTypeTreeMap> value, boolean option)
      throws CollectionsTestServiceException;

  TreeSet<MarkerTypeTreeSet> echo(TreeSet<MarkerTypeTreeSet> value,
      boolean option) throws CollectionsTestServiceException;

  Vector<MarkerTypeVector> echo(Vector<MarkerTypeVector> value)
      throws CollectionsTestServiceException;

  ArrayList<Void> echoArrayListVoid(ArrayList<Void> value)
      throws CollectionsTestServiceException;

  List<MarkerTypeArraysAsList> echoArraysAsList(
      List<MarkerTypeArraysAsList> value)
      throws CollectionsTestServiceException;
  
  EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> echoEmptyEnumMap(
      EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> value)
      throws CollectionsTestServiceException;

  EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> echoEnumKey(
      EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> value)
      throws CollectionsTestServiceException;
  
  IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> echoEnumKey(
      IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> value)
      throws CollectionsTestServiceException;

  // For Collections.singletonList()
  List<MarkerTypeSingleton> echoSingletonList(List<MarkerTypeSingleton> value)
      throws CollectionsTestServiceException;
}
