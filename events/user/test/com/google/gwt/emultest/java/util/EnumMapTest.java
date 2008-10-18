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
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;

/**
 * Tests EnumMap.
 * TODO(tobyr) Consider using Apache collections tests.
 */
public class EnumMapTest extends GWTTestCase {

  enum Number {

    Zero,
    One,
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine,
    Ten,
    Eleven,
    Twelve,
    Thirteen,
    Fourteen,
    Fifteen,
    Sixteen,
    Seventeen,
    Eighteen,
    Nineteen,
    Twenty,
    TwentyOne,
    TwentyTwo,
    TwentyThree,
    TwentyFour,
    TwentyFive,
    TwentySix,
    TwentySeven,
    TwentyEight,
    TwentyNine,
    Thirty,
  }

  private static <E extends Enum<E>> void enumTests(Class<E> e) {
    E[] enums = e.getEnumConstants();

    EnumMap<E, Integer> numbers = new EnumMap<E, Integer>(e);
    HashMap<E, Integer> numberMap = new HashMap<E, Integer>();
    assertEquals(numberMap, numbers);

    numbers.put(enums[1], 1);
    numberMap.put(enums[1], 1);
    numbers.put(enums[2], 2);
    numberMap.put(enums[2], 2);
    assertEquals(numberMap, numbers);

    numbers.put(enums[23], 23);
    numberMap.put(enums[23], 23);
    assertEquals(numberMap, numbers);

    numbers.remove(enums[1]);
    numberMap.remove(enums[1]);
    assertEquals(numberMap, numbers);

    // Attempt an add at the beginning
    numbers.put(enums[0], 0);
    numberMap.put(enums[0], 0);
    assertEquals(numberMap, numbers);

    // Attempt an add at the end
    numbers.put(enums[enums.length - 1], enums.length - 1);
    numberMap.put(enums[enums.length - 1], enums.length - 1);
    assertEquals(numberMap, numbers);

    // Attempt to remove something bogus
    numbers.remove(enums[15]);
    numberMap.remove(enums[15]);
    assertEquals(numberMap, numbers);

    // Attempt to add a duplicate value
    numbers.put(enums[23], 23);
    numberMap.put(enums[23], 23);
    assertEquals(numberMap, numbers);

    numbers.clear();
    numberMap.clear();
    for (E val : enums) {
      numbers.put(val, val.ordinal());
      numberMap.put(val, val.ordinal());
    }
    assertEquals(numberMap, numbers);

    assertEquals(numberMap, numbers.clone());
    assertEquals(numberMap, new EnumMap<E, Integer>(numberMap));

    // Test entrySet, keySet, and values
    // Make sure that modifications through these views works correctly
    Set<Map.Entry<E, Integer>> numbersEntrySet = numbers.entrySet();
    Set<Map.Entry<E, Integer>> mapEntrySet = numberMap.entrySet();
    assertEquals(mapEntrySet, numbersEntrySet);

    final Map.Entry<E, Integer> entry = numbers.entrySet().iterator().next();
    /*
     * Copy entry because it is no longer valid after
     * numbersEntrySet.remove(entry).
     */
    Map.Entry<E, Integer> entryCopy = new Map.Entry<E, Integer>() {
      E key = entry.getKey();
      Integer value = entry.getValue();
      
      public E getKey() {
        return key;
      }

      public Integer getValue() {
        return value;
      }

      public Integer setValue(Integer value) {
        Integer oldValue = this.value;
        this.value = value;
        return oldValue;
      }
    };
    numbersEntrySet.remove(entry);
    mapEntrySet.remove(entryCopy);
    assertEquals(mapEntrySet, numbersEntrySet);
    assertEquals(numberMap, numbers);

    Set<E> numbersKeySet = numbers.keySet();
    Set<E> mapKeySet = numberMap.keySet();
    assertEquals(mapKeySet, numbersKeySet);
    numbersKeySet.remove(enums[2]);
    mapKeySet.remove(enums[2]);
    assertEquals(mapKeySet, numbersKeySet);
    assertEquals(numberMap, numbers);

    Collection<Integer> numbersValues = numbers.values();
    Collection<Integer> mapValues = numberMap.values();
    assertEquals(sort(mapValues), sort(numbersValues));
    numbersValues.remove(23);
    mapValues.remove(23);
    assertEquals(sort(mapValues), sort(numbersValues));
    assertEquals(numberMap, numbers);
  }

  private static <T extends Comparable<T>> Collection<T> sort(
      Collection<T> col) {
    ArrayList<T> list = new ArrayList<T>(col);
    Collections.sort(list);
    return list;
  }

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBasics() {
    enumTests(Number.class);
  }

  public void testNulls() {
    EnumMap<Number, Integer> numbers = new EnumMap<Number, Integer>(Number.class);

    assertFalse("Should not contain null value", numbers.containsValue(null));
    assertFalse("Should not contain null key", numbers.containsKey(null));

    numbers.put(Number.Two, null);
    assertTrue("Should contain a null value",numbers.containsValue(null));

    try {
      numbers.put(null, 3);
      fail("Should not be able to insert a null key.");
    } catch (NullPointerException ex) {
    }
  }

  public void testOrdering() {
    EnumMap<Number, Integer> numbers = new EnumMap<Number, Integer>(Number.class);
    Number[] enums = Number.values();

    for (int i = enums.length - 1; i >= 0; --i) {
      numbers.put(enums[i], i);
    }

    int lastOrdinal = -1;
    for (Map.Entry<Number, Integer> val : numbers.entrySet()) {
      int newOrdinal = val.getKey().ordinal();
      assertTrue("EnumMap must maintain Enums in order", lastOrdinal < newOrdinal);
      lastOrdinal = newOrdinal;
    }
  }
}
