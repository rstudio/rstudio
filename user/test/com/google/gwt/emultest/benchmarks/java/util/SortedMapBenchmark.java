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
import com.google.gwt.benchmarks.client.RangeEnum;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.benchmarks.client.Setup;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;

/**
 * Benchmarks common operations on {@link SortedMap SortedMaps}. This test covers
 * appends, inserts, and removes for various sizes and positions.
 * 
 */
public abstract class SortedMapBenchmark extends Benchmark {

  /*
   * TODO(rovrevik) Add more tests such as iteration, non-sequential random
   * access, and sublists.
   */

  /**
   * The various positions that data can be inserted into a list.
   */
  protected enum Position {

    BEGIN("at the beginning"), EXPLICIT_END("explicitly at the end"), IMPLICIT_END(
        "implicitly at the end"), VARIED("in varied locations");

    private String label;

    /**
     * Constructor for <code>Position</code>.
     * 
     * @param label a not <code>null</code> label describing this
     *          <code>Position</code>.
     */
    Position(String label) {
      this.label = label;
    }

    /**
     * Returns the textual description for the position.
     * 
     * @return a not <code>null</code> description.
     */
    @Override
    public String toString() {
      return label;
    }
  }

  private static final int PRIME = 3001;

  protected final IntRange baseRange = new IntRange(512, Integer.MAX_VALUE,
      Operator.MULTIPLY, 2);

  protected final List<Position> explicitPositions = Arrays.asList(
      Position.BEGIN, Position.EXPLICIT_END, Position.VARIED);

  protected final IntRange insertRemoveRange = new IntRange(64,
      Integer.MAX_VALUE, Operator.MULTIPLY, 2);

  int index = 0;

  SortedMap<Integer, String> sortedMap;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuiteBenchmarks";
  }

  // Required for JUnit
  public void testSortedMapGets() {
  }

  /**
   * Performs <code>size</code> gets on a {@code SortedMap} of size,
   * <code>size</code>.
   * 
   * @param size the size of the {@code SortedMap}
   */
  @Setup("beginSortedMapGets")
  public void testSortedMapGets(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      sortedMap.get(i);
    }
  }

  // Required for JUnit
  public void testSortedMapInserts() {
  }

  /**
   * Performs <code>size</code> inserts at position, <code>where</code>, on
   * an empty <code>SortedMap</code>.
   * 
   * @param where Where the inserts happen
   * @param size The size of the <code>SortedMap</code>
   * 
   */
  @Setup("beginSortedMapInserts")
  public void testSortedMapInserts(@RangeEnum(Position.class)
  Position where, @RangeField("insertRemoveRange")
  Integer size) {
    insertIntoCollection(size, where, sortedMap);
  }

  // Required for JUnit
  public void testSortedMapPuts() {
  }

  /**
   * Appends <code>size</code> items to an empty {@code SortedMap}.
   * 
   * @param size the size of the {@code SortedMap}
   */
  @Setup("beginSortedMapPuts")
  public void testSortedMapPuts(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      sortedMap.put(i, "hello");
    }
  }

  // Required for JUnit
  public void testSortedMapRemoves() {
  }

  /**
   * Performs <code>size</code> removes at position, <code>where</code>, on
   * an TreeMap of size, <code>size</code>.
   * 
   * @param where Where the inserts happen
   * @param size The size of the <code>SortedMap</code>
   */
  @Setup("beginSortedMapRemoves")
  public void testSortedMapRemoves(@RangeField("explicitPositions")
  Position where, @RangeField("insertRemoveRange")
  Integer size) {
    removeFromCollection(size, where, sortedMap);
  }

  /**
   * Creates a new empty SortedMap.
   * 
   * @return a not <code>null</code>, empty SortedMap
   */
  protected abstract SortedMap<Integer, String> newSortedMap();

  void beginSortedMapGets(Integer size) {
    createSortedMap(size);
  }

  void beginSortedMapInserts(Position where, Integer size) {
    sortedMap = newSortedMap();
    index = 0;
  }

  void beginSortedMapPuts(Integer size) {
    sortedMap = newSortedMap();
  }

  void beginSortedMapRemoves(Position where, Integer size) {
    beginSortedMapInserts(where, size);
    testSortedMapInserts(where, size);
  }

  private void createSortedMap(Integer size) {
    beginSortedMapPuts(size);
    testSortedMapPuts(size);
  }

  private void insertIntoCollection(Integer size, Position where,
      SortedMap<Integer, String> m) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      if (where == Position.IMPLICIT_END) {
        Integer key;
        try {
          key = m.lastKey();
        } catch (NoSuchElementException e) {
          key = Integer.valueOf(0);
        }
        m.put((key.intValue() + 1), "hello");
      } else if (where == Position.BEGIN) {
        m.put(0, "hello");
      } else if (where == Position.EXPLICIT_END) {
        m.put(m.size(), "hello");
      } else if (where == Position.VARIED) {
        m.put(index, "hello");
        index += PRIME;
        index %= m.size();
      }
    }
  }

  private int removeFromCollection(Integer size, Position where,
      SortedMap<Integer, String> m) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      if (where == Position.IMPLICIT_END) {
        throw new RuntimeException("cannot remove from the end implicitly");
      } else if (where == Position.BEGIN) {
        m.remove(0);
      } else if (where == Position.EXPLICIT_END) {
        m.remove(m.size() - 1);
      } else if (where == Position.VARIED) {
        m.remove(index);
        index += PRIME;
        int currentSize = m.size();
        if (currentSize > 0) {
          index %= m.size();
        }
      }
    }
    return index;
  }
}
