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
package com.google.gwt.emultest.benchmarks.java.util;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.benchmarks.client.Setup;

import java.util.HashMap;

/**
 * Benchmarks the HashMap implementation.
 */
public class HashMapBenchmark extends Benchmark {

  protected IntRange baseRange = new IntRange(32, Integer.MAX_VALUE,
      Operator.MULTIPLY, 2);

  protected IntRange containsRange = new IntRange(10, 200, Operator.ADD, 20);

  private HashMap<Object, Object> map;

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuiteBenchmarks";
  }

  public void testHashMapContainsValueInt() {
  }

  /**
   * Checks for <code>size</code> values in a populated HashMap. All items are
   * Integers, and contain duplicate values.
   */
  @Setup("beginHashMapContainsValueInt")
  public void testHashMapContainsValueInt(@RangeField("containsRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      Integer intVal = new Integer(i);
      map.containsValue(intVal);
    }
  }

  public void testHashMapContainsValueString() {
  }

  /**
   * Checks for <code>size</code> values in a populated HashMap. All items are
   * Strings, and contain duplicate values.
   */
  @Setup("beginHashMapContainsValueString")
  public void testHashMapContainsValueString(@RangeField("containsRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      String strVal = Integer.toString(i);
      map.containsValue(strVal);
    }
  }

  public void testHashMapDuplicateIntAdds() {
  }

  /**
   * Appends <code>size</code> items to an empty HashMap. All items are
   * Integers, and contain duplicate values.
   */
  @Setup("initMap")
  public void testHashMapDuplicateIntAdds(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      Integer intVal = new Integer(i / 10);
      map.put(intVal, intVal);
    }
  }

  public void testHashMapDuplicateStringAdds() {
  }

  /**
   * Appends <code>size</code> items to an empty HashMap. All items are
   * Strings, and contain duplicate values.
   */
  @Setup("initMap")
  public void testHashMapDuplicateStringAdds(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      String strVal = Integer.toString(i / 10);
      map.put(strVal, strVal);
    }
  }

  public void testHashMapIntAdds() {
  }

  /**
   * Appends <code>size</code> items to an empty HashMap. All items are
   * Integers, and do not contain duplicate values.
   */
  @Setup("initMap")
  public void testHashMapIntAdds(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      Integer intVal = new Integer(i);
      map.put(intVal, intVal);
    }
  }

  public void testHashMapIntGets() {
  }

  /**
   * Checks for <code>size</code> values in a populated HashMap. All items are
   * Integers, and contain duplicate values.
   */
  @Setup("beginHashMapIntGets")
  public void testHashMapIntGets(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      Integer intVal = new Integer(i);
      map.get(intVal);
    }
  }

  public void testHashMapStringAdds() {
  }

  /**
   * Appends <code>size</code> items to an empty HashMap. All items are
   * Strings, and do not contain duplicate values.
   */
  @Setup("initMap")
  public void testHashMapStringAdds(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      String strVal = Integer.toString(i);
      map.put(strVal, strVal);
    }
  }

  public void testHashMapStringGets() {
  }

  /**
   * Checks for size values in a populated HashMap. All items are Strings, and
   * contain duplicate values.
   */
  @Setup("beginHashMapStringGets")
  public void testHashMapStringGets(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      String strVal = Integer.toString(i);
      map.get(strVal);
    }
  }

  protected void beginHashMapContainsValueInt(Integer size) {
    map = new HashMap<Object, Object>();
    testHashMapDuplicateIntAdds(size);
  }

  protected void beginHashMapContainsValueString(Integer size) {
    map = new HashMap<Object, Object>();
    testHashMapDuplicateStringAdds(size);
  }

  protected void beginHashMapIntGets(Integer size) {
    map = new HashMap<Object, Object>();
    testHashMapIntAdds(size);
  }

  protected void beginHashMapStringGets(Integer size) {
    map = new HashMap<Object, Object>();
    testHashMapStringAdds(size);
  }

  protected void initMap(Integer size) {
    map = new HashMap<Object, Object>(size);
  }
}
