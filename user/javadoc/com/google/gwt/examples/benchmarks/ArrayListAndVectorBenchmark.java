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
package com.google.gwt.examples.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Benchmarks common operations on both ArrayLists and Vectors. This test covers
 * appends, inserts, and removes for various sizes and positions on both
 * ArrayLists and Vectors.
 * 
 */
public class ArrayListAndVectorBenchmark extends Benchmark {

  /**
   * Many profiled widgets have position dependent insert/remove code.
   * <code>Position</code> is a helper class meant to capture the positional
   * information for these sorts of operations.
   */
  protected static class Position {

    public static final Position BEGIN = new Position("at the beginning");
    public static final Position END = new Position("at the end");
    public static final Position NONE = new Position("no location specified");
    public static final Position VARIED = new Position("in varied locations");

    public static final Iterable positions = new Iterable() {
      public Iterator iterator() {
        return Arrays.asList(new Position[] {BEGIN, END, NONE, VARIED}).iterator();
      }
    };

    public static final Iterable positions2 = new Iterable() {
      public Iterator iterator() {
        return Arrays.asList(new Position[] {BEGIN, END, VARIED}).iterator();
      }
    };

    private String label;

    /**
     * Constructor for <code>Position</code>.
     */
    public Position(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return " " + label;
    }
  }

  private static final int PRIME = 3001;

  final IntRange insertRemoveRange = new IntRange(64, Integer.MAX_VALUE,
      Operator.MULTIPLY, 2);

  final IntRange baseRange = new IntRange(512, Integer.MAX_VALUE,
      Operator.MULTIPLY, 2);

  ArrayList list;
  Vector vector;
  int index = 0;

  @Override
  public String getModuleName() {
    return "com.google.gwt.examples.Benchmarks";
  }

  /**
   * Appends <code>size</code> items to an empty ArrayList.
   * 
   * @gwt.benchmark.param size -limit = baseRange
   */
  public void testArrayListAdds(Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      list.add("hello");
    }
  }

  // Required for JUnit
  public void testArrayListAdds() {
  }

  /**
   * Performs <code>size</code> gets on an ArrayList of size,
   * <code>size</code>.
   * 
   * @gwt.benchmark.param size -limit = baseRange
   */
  public void testArrayListGets(Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      list.get(i);
    }
  }

  // Required for JUnit
  public void testArrayListGets() {
  }

  /**
   * Performs <code>size</code> inserts at position, <code>where</code>, on
   * an empty ArrayList.
   * 
   * @gwt.benchmark.param where = Position.positions
   * @gwt.benchmark.param size -limit = insertRemoveRange
   */
  public void testArrayListInserts(Position where, Integer size) {
    insertIntoCollection(size, where, list);
  }

  // Required for JUnit
  public void testArrayListInserts() {
  }

  /**
   * Performs <code>size</code> removes at position, <code>where</code>, on
   * an ArrayList of size, <code>size</code>.
   * 
   * @gwt.benchmark.param where = Position.positions2
   * @gwt.benchmark.param size -limit = insertRemoveRange
   */
  public void testArrayListRemoves(Position where, Integer size) {
    removeFromCollection(size, where, list);
  }

  // Required for JUnit
  public void testArrayListRemoves() {
  }

  /**
   * Appends <code>size</code> items to an empty Vector.
   * 
   * @gwt.benchmark.param size -limit = baseRange
   */
  public void testVectorAdds(Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      vector.add("hello");
    }
  }

  // Required for JUnit
  public void testVectorAdds() {
  }

  /**
   * Performs <code>size</code> gets on a Vector of size, <code>size</code>.
   * 
   * @gwt.benchmark.param size -limit = baseRange
   */
  public void testVectorGets(Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      vector.get(i);
    }
  }

  // Required for JUnit
  public void testVectorGets() {
  }

  /**
   * Performs <code>size</code> inserts at position, <code>where</code>, on
   * an empty Vector.
   * 
   * @gwt.benchmark.param where = Position.positions
   * @gwt.benchmark.param size -limit = insertRemoveRange
   */
  public void testVectorInserts(Position where, Integer size) {
    insertIntoCollection(size, where, vector);
  }

  // Required for JUnit
  public void testVectorInserts() {
  }

  /**
   * Performs <code>size</code> removes at position, <code>where</code>, on
   * a Vector of size, <code>size</code>.
   * 
   * @gwt.benchmark.param where = Position.positions2
   * @gwt.benchmark.param size -limit = insertRemoveRange
   */
  public void testVectorRemoves(Position where, Integer size) {
    removeFromCollection(size, where, vector);
  }

  // Required for JUnit
  public void testVectorRemoves() {
  }

  void beginArrayListAdds(Integer size) {
    list = new ArrayList();
  }

  void beginArrayListGets(Integer size) {
    createArrayList(size);
  }

  void beginArrayListInserts(Position where, Integer size) {
    list = new ArrayList();
    index = 0;
  }

  void beginArrayListRemoves(Position where, Integer size) {
    beginArrayListInserts(where, size);
    testArrayListInserts(where, size);
  }

  void beginVectorAdds(Integer size) {
    vector = new Vector();
  }

  void beginVectorGets(Integer size) {
    createVector(size);
  }

  void beginVectorInserts(Position where, Integer size) {
    vector = new Vector();
    index = 0;
  }

  void beginVectorRemoves(Position where, Integer size) {
    beginVectorInserts(where, size);
    testVectorInserts(where, size);
  }

  private void createArrayList(Integer size) {
    beginArrayListAdds(size);
    testArrayListAdds(size);
  }

  private void createVector(Integer size) {
    beginVectorAdds(size);
    testVectorAdds(size);
  }

  private void insertIntoCollection(Integer size, Position where, List v) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      if (where == Position.NONE) {
        v.add("hello");
      } else if (where == Position.BEGIN) {
        v.add(0, "hello");
      } else if (where == Position.END) {
        v.add(v.size(), "hello");
      } else if (where == Position.VARIED) {
        v.add(index, "hello");
        index += PRIME;
        index %= v.size();
      }
    }
  }

  private int removeFromCollection(Integer size, Position where, List v) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      if (where == Position.NONE) {
        throw new RuntimeException("cannot remove with no position");
      } else if (where == Position.BEGIN) {
        v.remove(0);
      } else if (where == Position.END) {
        v.remove(v.size() - 1);
      } else if (where == Position.VARIED) {
        v.remove(index);
        index += PRIME;
        int currentSize = v.size();
        if (currentSize > 0) {
          index %= v.size();
        }
      }
    }
    return index;
  }
}
