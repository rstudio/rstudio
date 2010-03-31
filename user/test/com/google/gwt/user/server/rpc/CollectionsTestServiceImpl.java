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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.CollectionsTestService;
import com.google.gwt.user.client.rpc.TestSetFactory;
import com.google.gwt.user.client.rpc.TestSetValidator;
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
import java.util.Arrays;
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
public class CollectionsTestServiceImpl extends HybridServiceServlet implements
    CollectionsTestService {

  private static String toString(Object[] values) {
    return Arrays.asList(values).toString();
  }

  public ArrayList<MarkerTypeArrayList> echo(ArrayList<MarkerTypeArrayList> list)
      throws CollectionsTestServiceException {
    if (!TestSetValidator.isValid(list)) {
      throw new CollectionsTestServiceException();
    }

    return list;
  }

  public boolean[] echo(boolean[] actual)
      throws CollectionsTestServiceException {
    boolean[] expected = TestSetFactory.createPrimitiveBooleanArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Boolean[] echo(Boolean[] actual)
      throws CollectionsTestServiceException {
    Boolean[] expected = TestSetFactory.createBooleanArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public byte[] echo(byte[] actual) throws CollectionsTestServiceException {
    byte[] expected = TestSetFactory.createPrimitiveByteArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Byte[] echo(Byte[] actual) throws CollectionsTestServiceException {
    Byte[] expected = TestSetFactory.createByteArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public char[] echo(char[] actual) throws CollectionsTestServiceException {
    char[] expected = TestSetFactory.createPrimitiveCharArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Character[] echo(Character[] actual)
      throws CollectionsTestServiceException {
    Character[] expected = TestSetFactory.createCharArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Date[] echo(Date[] actual) throws CollectionsTestServiceException {
    Date[] expected = TestSetFactory.createDateArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + toString(expected) + " actual: " + toString(actual));
    }

    return actual;
  }

  public double[] echo(double[] actual) throws CollectionsTestServiceException {
    double[] expected = TestSetFactory.createPrimitiveDoubleArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Double[] echo(Double[] actual) throws CollectionsTestServiceException {
    Double[] expected = TestSetFactory.createDoubleArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Enum<?>[] echo(Enum<?>[] actual)
      throws CollectionsTestServiceException {
    Enum<?>[] expected = TestSetFactory.createEnumArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public float[] echo(float[] actual) throws CollectionsTestServiceException {
    float[] expected = TestSetFactory.createPrimitiveFloatArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Float[] echo(Float[] actual) throws CollectionsTestServiceException {
    Float[] expected = TestSetFactory.createFloatArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public HashMap<String, MarkerTypeHashMap> echo(
      HashMap<String, MarkerTypeHashMap> actual)
      throws CollectionsTestServiceException {
    HashMap<String, MarkerTypeHashMap> expected = TestSetFactory.createHashMap();
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }

    return actual;
  }

  public HashSet<MarkerTypeHashSet> echo(HashSet<MarkerTypeHashSet> actual)
      throws CollectionsTestServiceException {
    HashSet<MarkerTypeHashSet> expected = TestSetFactory.createHashSet();
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }

    return actual;
  }

  public int[] echo(int[] actual) throws CollectionsTestServiceException {
    int[] expected = TestSetFactory.createPrimitiveIntegerArray();
    if (!TestSetValidator.equals(expected, actual)) {

      // It could be the very large array
      expected = TestSetFactory.createVeryLargeArray();
      if (!TestSetValidator.equals(expected, actual)) {
        throw new CollectionsTestServiceException("expected: "
            + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
      }
    }

    return actual;
  }

  public Integer[] echo(Integer[] actual)
      throws CollectionsTestServiceException {
    Integer[] expected = TestSetFactory.createIntegerArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public java.sql.Date[] echo(java.sql.Date[] actual)
      throws CollectionsTestServiceException {
    java.sql.Date[] expected = TestSetFactory.createSqlDateArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + toString(expected) + " actual: " + toString(actual));
    }

    return actual;
  }

  public LinkedHashMap<String, MarkerTypeLinkedHashMap> echo(
      LinkedHashMap<String, MarkerTypeLinkedHashMap> actual)
      throws CollectionsTestServiceException {
    LinkedHashMap<String, MarkerTypeLinkedHashMap> expected = TestSetFactory.createLinkedHashMap();
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }
    return actual;
  }

  public LinkedHashSet<MarkerTypeLinkedHashSet> echo(
      LinkedHashSet<MarkerTypeLinkedHashSet> actual)
      throws CollectionsTestServiceException {
    LinkedHashSet<MarkerTypeLinkedHashSet> expected = TestSetFactory.createLinkedHashSet();
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }
    return actual;
  }

  public List<MarkerTypeEmpty> echo(List<MarkerTypeEmpty> list)
      throws CollectionsTestServiceException {
    if (!TestSetValidator.isValid(list)) {
      throw new CollectionsTestServiceException();
    }

    return list;
  }

  public long[] echo(long[] actual) throws CollectionsTestServiceException {
    long[] expected = TestSetFactory.createPrimitiveLongArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Long[] echo(Long[] actual) throws CollectionsTestServiceException {
    Long[] expected = TestSetFactory.createLongArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + toString(expected) + " actual: " + toString(actual));
    }

    return actual;
  }

  public Map<MarkerTypeEmpty, MarkerTypeEmpty> echo(
      Map<MarkerTypeEmpty, MarkerTypeEmpty> map)
      throws CollectionsTestServiceException {
    if (!TestSetValidator.isValid(map)) {
      throw new CollectionsTestServiceException();
    }

    return map;
  }

  public Set<MarkerTypeEmpty> echo(Set<MarkerTypeEmpty> set)
      throws CollectionsTestServiceException {
    if (!TestSetValidator.isValid(set)) {
      throw new CollectionsTestServiceException();
    }

    return set;
  }

  public short[] echo(short[] actual) throws CollectionsTestServiceException {
    short[] expected = TestSetFactory.createPrimitiveShortArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public Short[] echo(Short[] actual) throws CollectionsTestServiceException {
    Short[] expected = TestSetFactory.createShortArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public String[] echo(String[] actual) throws CollectionsTestServiceException {
    String[] expected = TestSetFactory.createStringArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + Arrays.toString(expected) + " actual: " + Arrays.toString(actual));
    }

    return actual;
  }

  public String[][] echo(String[][] value)
      throws CollectionsTestServiceException {
    return value;
  }

  public Time[] echo(Time[] actual) throws CollectionsTestServiceException {
    Time[] expected = TestSetFactory.createSqlTimeArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + toString(expected) + " actual: " + toString(actual));
    }

    return actual;
  }

  public Timestamp[] echo(Timestamp[] actual)
      throws CollectionsTestServiceException {
    Timestamp[] expected = TestSetFactory.createSqlTimestampArray();
    if (!TestSetValidator.equals(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + toString(expected) + " actual: " + toString(actual));
    }

    return actual;
  }

  public TreeMap<String, MarkerTypeTreeMap> echo(
      TreeMap<String, MarkerTypeTreeMap> actual, boolean option)
      throws CollectionsTestServiceException {
    TreeMap<String, MarkerTypeTreeMap> expected = TestSetFactory.createTreeMap(option);
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }

    return actual;
  }

  public TreeSet<MarkerTypeTreeSet> echo(TreeSet<MarkerTypeTreeSet> actual,
      boolean option) throws CollectionsTestServiceException {
    TreeSet<MarkerTypeTreeSet> expected = TestSetFactory.createTreeSet(option);
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }

    return actual;
  }

  public Vector<MarkerTypeVector> echo(Vector<MarkerTypeVector> actual)
      throws CollectionsTestServiceException {
    Vector<MarkerTypeVector> expected = TestSetFactory.createVector();
    if (!TestSetValidator.isValid(expected, actual)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + actual.toString());
    }

    return actual;
  }

  public ArrayList<Void> echoArrayListVoid(ArrayList<Void> value)
      throws CollectionsTestServiceException {
    ArrayList<Void> expected = TestSetFactory.createArrayListVoid();
    if (!TestSetValidator.isValidArrayListVoid(value)) {
      throw new CollectionsTestServiceException("expected: "
          + expected.toString() + " actual: " + value.toString());
    }

    return value;
  }

  public List<MarkerTypeArraysAsList> echoArraysAsList(
      List<MarkerTypeArraysAsList> value)
      throws CollectionsTestServiceException {
    if (!TestSetValidator.isValidAsList(value)) {
      throw new CollectionsTestServiceException();
    }

    return value;
  }

}
