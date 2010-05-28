/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.collections;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;
import com.google.gwt.benchmarks.client.RangeField;

import java.util.ArrayList;

/**
 * Benchmarks the performance of various MutableArray methods.
 */
public class MutableArrayBenchmarkTest extends Benchmark {

  final IntRange elemRange = new IntRange(5, 30000, Operator.ADD, 500);

  @Override
  public String getModuleName() {
    return "com.google.gwt.collections.Collections";
  }
  
  public void testGwtCollectionsArrayAddGrowth() {    
  }
  
  public void testGwtCollectionsArrayAddGrowth(@RangeField("elemRange") Integer numElements) {
    MutableArray<Integer> ma = CollectionFactory.createMutableArray();
    
    for (int i = 0; i < numElements; i++) {
      ma.add(i);
    }
  }
  
  public void testGwtCollectionsArraySetSizeGrowth() {  
  }
  
  public void testGwtCollectionsArraySetSizeGrowth(@RangeField("elemRange") Integer numElements) {
    MutableArray<Integer> ma = CollectionFactory.createMutableArray();
    
    ma.setSize(numElements, null);
    for (int i = 0; i < numElements; i++) {
      ma.set(i, i);
    }    
  }
  
  public void testGwtCollectionsArraySetSizeInitGrowth() {  
  }
  
  public void testGwtCollectionsArraySetSizeInitGrowth(
      @RangeField("elemRange") Integer numElements) {
    MutableArray<Integer> ma = CollectionFactory.createMutableArray();
    
    ma.setSize(numElements, new Integer(0));
    for (int i = 0; i < numElements; i++) {
      ma.set(i, i);
    }    
  }
  
  public void testJavaArraySetGrowth() {
  }
  
  public void testJavaArraySetGrowth(@RangeField("elemRange") Integer numElements) {
    Integer[] ia = new Integer[numElements];
    
    for (int i = 0; i < numElements; i++) {
      ia[i] = i;
    }
  }
  
  public void testJreArrayListAddGrowth() {
  }
  
  public void testJreArrayListAddGrowth(@RangeField("elemRange") Integer numElements) {
    ArrayList<Integer> al = new ArrayList<Integer>();
    
    for (int i = 0; i < numElements; i++) {
      al.add(i);
    }
  }

}
