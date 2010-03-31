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

import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArrayList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArraysAsList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmpty;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeVector;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
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
 * TODO: document me.
 */
public interface CollectionsTestService extends RemoteService {

  /**
   * TODO: document me.
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
  List<MarkerTypeEmpty> echo(List<MarkerTypeEmpty> value)
  throws CollectionsTestServiceException;

  // For Collections.emptyMap()
  Map<MarkerTypeEmpty, MarkerTypeEmpty> echo(Map<MarkerTypeEmpty,
      MarkerTypeEmpty> value) throws CollectionsTestServiceException;

  // For Collections.emptySet()
  Set<MarkerTypeEmpty> echo(Set<MarkerTypeEmpty> value)
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

  HashMap<String, MarkerTypeHashMap> echo(
      HashMap<String, MarkerTypeHashMap> value)
      throws CollectionsTestServiceException;

  HashSet<MarkerTypeHashSet> echo(HashSet<MarkerTypeHashSet> value)
      throws CollectionsTestServiceException;

  int[] echo(int[] value) throws CollectionsTestServiceException;

  Integer[] echo(Integer[] value) throws CollectionsTestServiceException;

  java.sql.Date[] echo(java.sql.Date[] value)
      throws CollectionsTestServiceException;

  LinkedHashMap<String, MarkerTypeLinkedHashMap> echo(
      LinkedHashMap<String, MarkerTypeLinkedHashMap> value)
      throws CollectionsTestServiceException;

  LinkedHashSet<MarkerTypeLinkedHashSet> echo(
      LinkedHashSet<MarkerTypeLinkedHashSet> value)
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
}
