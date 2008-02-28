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
package com.google.gwt.emultest.java.util;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;

import java.util.Arrays;

/**
 * Benchmarks sorts on arrays.
 */
public class ArraySortBenchmark extends Benchmark {

  private static class TestObject implements Comparable<TestObject> {

    private int value;
    @SuppressWarnings("unused")
    private int index;

    public TestObject(int value, int index) {
      this.value = value;
      this.index = index;
    }

    public int compareTo(TestObject o) {
      return value - o.value;
    }
  }

  public final static int SUBARRAY_SKIP = 2;

  public final static int MAX_ARRAY_SIZE = 8192;

  // protected since the generated code is a subclass
  protected byte[] initByteArray;
  protected int[] initIntArray;
  protected TestObject[] initObjectArray;

  protected byte[] byteArray;
  protected int[] intArray;
  protected TestObject[] objectArray;

  final IntRange sizeRange = new IntRange(128, MAX_ARRAY_SIZE, Operator.ADD,
      256);

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void beginByteArray(Integer size) {
    byteArray = new byte[size.intValue()];
    System.arraycopy(initByteArray, 0, byteArray, 0, size.intValue());
  }

  public void beginIntArray(Integer size) {
    intArray = new int[size.intValue()];
    System.arraycopy(initIntArray, 0, intArray, 0, size.intValue());
  }

  public void beginObjectArray(Integer size) {
    objectArray = new TestObject[size.intValue()];
    System.arraycopy(initObjectArray, 0, objectArray, 0, size.intValue());
  }

  public void beginSubarray(Integer size) {
    byteArray = new byte[size.intValue()];
    System.arraycopy(initByteArray, 0, byteArray, 0, size.intValue());
  }

  // Required for JUnit
  public void testByteArray() {
  }

  /**
   * Sorts <code>size</code> byte entries.
   * 
   * @gwt.benchmark.param size -limit = sizeRange
   */
  public void testByteArray(Integer size) {
    Arrays.sort(byteArray);
  }

  // Required for JUnit
  public void testIntArray() {
  }

  /**
   * Sorts <code>size</code> int entries.
   * 
   * @gwt.benchmark.param size -limit = sizeRange
   */
  public void testIntArray(Integer size) {
    Arrays.sort(intArray);
  }

  // Required for JUnit
  public void testObjectArray() {
  }

  /**
   * Sorts <code>size</code> object entries.
   * 
   * @gwt.benchmark.param size -limit = sizeRange
   */
  public void testObjectArray(Integer size) {
    Arrays.sort(objectArray);
  }

  // Required for JUnit
  public void testSubarray() {
  }

  /**
   * Sorts <code>size</code> byte entries as a subarray.
   * 
   * @gwt.benchmark.param size -limit = sizeRange
   */
  public void testSubarray(Integer size) {
    Arrays.sort(byteArray, SUBARRAY_SKIP, size);
  }

  @Override
  protected void setUp() throws Exception {
    /*
     * Since the RNG available in web mode cannot accept a seed for reproducible
     * reports we use a simple pseudorandom sequence here. Its only purpose is
     * to reasonably shuffle the data.
     */
    super.setUp();
    initByteArray = new byte[MAX_ARRAY_SIZE + SUBARRAY_SKIP];
    for (int i = 0; i < MAX_ARRAY_SIZE + SUBARRAY_SKIP; i++) {
      initByteArray[i] = (byte) (i * 31 + 17);
    }
    initIntArray = new int[MAX_ARRAY_SIZE];
    for (int i = 0; i < MAX_ARRAY_SIZE; i++) {
      initIntArray[i] = i * 3151017 + 17;
    }
    initObjectArray = new TestObject[MAX_ARRAY_SIZE + SUBARRAY_SKIP];
    for (int i = 0; i < MAX_ARRAY_SIZE + SUBARRAY_SKIP; i++) {
      initObjectArray[i] = new TestObject((i * 31 + 17) % 500, i);
    }
  }

}
