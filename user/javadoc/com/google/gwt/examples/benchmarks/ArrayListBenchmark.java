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
import com.google.gwt.benchmarks.client.RangeEnum;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.benchmarks.client.Setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Benchmarks common operations on {@link List Lists}. This test covers
 * appends, inserts, and removes for various sizes and positions.
 */
public class ArrayListBenchmark extends Benchmark {

  private static final int PRIME = 3001;

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

  protected final List<Position> explicitPositions = Arrays.asList(
      Position.BEGIN, Position.EXPLICIT_END, Position.VARIED);

  protected final IntRange insertRemoveRange = new IntRange(64,
      Integer.MAX_VALUE, Operator.MULTIPLY, 2);

  protected final IntRange baseRange = new IntRange(512, Integer.MAX_VALUE,
      Operator.MULTIPLY, 2);

  List<String> list;

  int index = 0;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Appends <code>size</code> items to an empty {@code List}.
   * 
   * @param size the size of the {@code List}
   */
  @Setup("beginListAdds")
  public void testListAdds(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      list.add("hello");
    }
  }

  // Required for JUnit
  public void testListAdds() {
  }

  /**
   * Performs <code>size</code> gets on a {@code List} of size,
   * <code>size</code>.
   * 
   * @param size the size of the {@code List}
   */
  @Setup("beginListGets")
  public void testListGets(@RangeField("baseRange")
  Integer size) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      list.get(i);
    }
  }

  // Required for JUnit
  public void testListGets() {
  }

  /**
   * Performs <code>size</code> inserts at position, <code>where</code>, on
   * an empty <code>List</code>.
   * 
   * @param where Where the inserts happen
   * @param size The size of the <code>List</code>
   * 
   */
  @Setup("beginListInserts")
  public void testListInserts(@RangeEnum(Position.class)
  Position where, @RangeField("insertRemoveRange")
  Integer size) {
    insertIntoCollection(size, where, list);
  }

  // Required for JUnit
  public void testListInserts() {
  }

  /**
   * Performs <code>size</code> removes at position, <code>where</code>, on
   * an ArrayList of size, <code>size</code>.
   * 
   * @param where Where the inserts happen
   * @param size The size of the <code>List</code>
   */
  @Setup("beginListRemoves")
  public void testListRemoves(@RangeField("explicitPositions")
  Position where, @RangeField("insertRemoveRange")
  Integer size) {
    removeFromCollection(size, where, list);
  }

  // Required for JUnit
  public void testListRemoves() {
  }

  /**
   * Creates a new empty List.
   * 
   * @return a not <code>null</code>, empty List
   */
  protected List<String> newList() {
    return new ArrayList<String>();
  }

  void beginListAdds(Integer size) {
    list = newList();
  }

  void beginListGets(Integer size) {
    createList(size);
  }

  void beginListInserts(Position where, Integer size) {
    list = newList();
    index = 0;
  }

  void beginListRemoves(Position where, Integer size) {
    beginListInserts(where, size);
    testListInserts(where, size);
  }

  private void createList(Integer size) {
    beginListAdds(size);
    testListAdds(size);
  }

  private void insertIntoCollection(Integer size, Position where, List<String> l) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      if (where == Position.IMPLICIT_END) {
        l.add("hello");
      } else if (where == Position.BEGIN) {
        l.add(0, "hello");
      } else if (where == Position.EXPLICIT_END) {
        l.add(l.size(), "hello");
      } else if (where == Position.VARIED) {
        l.add(index, "hello");
        index += PRIME;
        index %= l.size();
      }
    }
  }

  private int removeFromCollection(Integer size, Position where, List<String> l) {
    int num = size.intValue();
    for (int i = 0; i < num; i++) {
      if (where == Position.IMPLICIT_END) {
        throw new RuntimeException("cannot remove from the end implicitly");
      } else if (where == Position.BEGIN) {
        l.remove(0);
      } else if (where == Position.EXPLICIT_END) {
        l.remove(l.size() - 1);
      } else if (where == Position.VARIED) {
        l.remove(index);
        index += PRIME;
        int currentSize = l.size();
        if (currentSize > 0) {
          index %= l.size();
        }
      }
    }
    return index;
  }
}
