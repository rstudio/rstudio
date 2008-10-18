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

/**
 * Tests <code>TreeMap</code> with a <code>Comparator</code>.
 */
public class TreeMapIntegerDoubleTest extends TreeMapTest<Integer, Double> {

  @Override
  Integer getGreaterThanMaximumKey() {
    return Integer.MAX_VALUE;
  }

  @Override
  Integer[] getKeys() {
    return new Integer[] {1, 2, 3, 4};
  }

  @Override
  Integer[] getKeys2() {
    return new Integer[] {5, 6, 7, 8};
  }

  @Override
  Integer getLessThanMinimumKey() {
    return Integer.MIN_VALUE;
  }

  @Override
  Double[] getValues() {
    return new Double[] {0.1, 0.2, 0.3, 0.4};
  }

  @Override
  Double[] getValues2() {
    return new Double[] {1.1, 1.2, 1.3, 1.4};
  }

  @Override
  protected Object getConflictingKey() {
    return "key";
  }

  @Override
  protected Object getConflictingValue() {
    return "value";
  }

  @Override
  protected Object[] getOtherKeys() {
    return getKeys2();
  }

  @Override
  protected Object[] getOtherValues() {
    return getValues2();
  }

  @Override
  protected Object[] getSampleKeys() {
    return getKeys();
  }

  @Override
  protected Object[] getSampleValues() {
    return getValues();
  }

  @Override
  protected Object[] getNewSampleValues() {
    return getValues2();
  }
}
