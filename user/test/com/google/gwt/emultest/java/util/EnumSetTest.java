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

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests EnumSet. TODO(tobyr) Consider using Apache collections tests.
 */
public class EnumSetTest extends GWTTestCase {

  enum FalseEnum {
    Zero, One,
  }

  enum Numbers {
    Zero, One, Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten, Eleven, Twelve, Thirteen, Fourteen, Fifteen, Sixteen, Seventeen, Eighteen, Nineteen, Twenty, TwentyOne, TwentyTwo, TwentyThree, TwentyFour, TwentyFive, TwentySix, TwentySeven, TwentyEight, TwentyNine, Thirty, ThirtyOne, ThirtyTwo, ThirtyThree, Thirtyfour,
  }

  enum ClinitRace {
    Zero, One, Two, Three;

    public static final Set<ClinitRace> set = EnumSet.allOf(ClinitRace.class);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Tests that an EnumSet can be statically initialized in an enum.
   */
  public void testClinitRace() {
    assertEquals(4, ClinitRace.set.size());
    assertTrue(ClinitRace.set.contains(ClinitRace.Zero));
    assertTrue(ClinitRace.set.contains(ClinitRace.One));
    assertTrue(ClinitRace.set.contains(ClinitRace.Two));
    assertTrue(ClinitRace.set.contains(ClinitRace.Three));
  }

  /**
   * Test failure mode from issue 3605.  Previously resulted in an incorrect size.
   */
  public void testDuplicates() {
    EnumSet<Numbers> set = EnumSet.of(Numbers.Two, Numbers.One, Numbers.Two, Numbers.One);
    assertEquals(2, set.size());
    assertTrue(set.contains(Numbers.One));
    assertTrue(set.contains(Numbers.Two));
  }

  /**
   * Test failure mode from issue 3605.  Previously resulted in a NoSuchElementException.
   */
  public void testDuplicatesToArray() {
    EnumSet<Numbers> set = EnumSet.of(Numbers.Two, Numbers.One, Numbers.Two, Numbers.One);
    Numbers[] array = set.toArray(new Numbers[set.size()]);
    assertNotNull(array);
    assertEquals(2, array.length);
    if (array[0] != Numbers.One && array[1] != Numbers.One) {
      fail("Numbers.One not found");
    }
    if (array[0] != Numbers.Two && array[1] != Numbers.Two) {
      fail("Numbers.Two not found");
    }
  }

  public void testNumbers() {
    enumTest(Numbers.class);
  }

  private <E extends Enum<E>> void enumTest(Class<E> e) {
    E[] enums = e.getEnumConstants();

    EnumSet<E> numbers = EnumSet.of(enums[1], enums[2]);
    HashSet<E> numberSet = new HashSet<E>();
    numberSet.add(enums[1]);
    numberSet.add(enums[2]);
    assertEquals(numberSet, numbers);

    numbers.add(enums[23]);
    numberSet.add(enums[23]);
    assertEquals(numberSet, numbers);

    numbers.remove(enums[1]);
    numberSet.remove(enums[1]);
    assertEquals(numberSet, numbers);

    // Attempt an add at the beginning
    numbers.add(enums[0]);
    numberSet.add(enums[0]);
    assertEquals(numberSet, numbers);

    // Attempt an add at the end
    numbers.add(enums[enums.length - 1]);
    numberSet.add(enums[enums.length - 1]);
    assertEquals(numberSet, numbers);

    // Attempt to remove something bogus
    numbers.remove(enums[15]);
    numberSet.remove(enums[15]);
    assertEquals(numberSet, numbers);

    // Attempt to add a duplicate value
    int numbersSize = numbers.size();
    int numberSetSize = numberSet.size();
    numbers.add(enums[23]);
    numberSet.add(enums[23]);
    assertEquals(numberSet, numbers);
    // Check sizes haven't changed
    assertEquals(numbersSize, numbers.size());
    assertEquals(numberSetSize, numberSet.size());

    numbers = EnumSet.allOf(e);
    numberSet.clear();
    numberSet.addAll(Arrays.asList(enums));
    assertEquals(numberSet, numbers);

    numbers = EnumSet.noneOf(e);
    numberSet.clear();
    assertEquals(numberSet, numbers);

    numbers = EnumSet.complementOf(EnumSet.allOf(e));
    numberSet.clear();
    assertEquals(numberSet, numbers);

    numbers = EnumSet.complementOf(EnumSet.noneOf(e));
    numberSet.clear();
    numberSet.addAll(Arrays.asList(enums));
    assertEquals(numberSet, numbers);

    numbers = EnumSet.allOf(e);
    numbers.remove(enums[20]);
    numbers.remove(enums[17]);
    numbers = EnumSet.complementOf(numbers);
    numberSet.clear();
    numberSet.add(enums[20]);
    numberSet.add(enums[17]);
    assertEquals(numberSet, numbers);

    numbers = EnumSet.range(enums[0], enums[enums.length - 1]);
    numberSet.clear();
    numberSet.addAll(Arrays.asList(enums));
    assertEquals(numberSet, numbers);

    numbers = EnumSet.range(enums[5], enums[5]);
    numberSet.clear();
    numberSet.add(enums[5]);
    assertEquals(numberSet, numbers);

    numbers = EnumSet.range(enums[5], enums[10]);
    numberSet.clear();
    for (int i = 5, end = 10; i <= end; ++i) {
      numberSet.add(enums[i]);
    }
    assertEquals(numberSet, numbers);

    // Test duplicates
    List<E> numberList = new ArrayList<E>();
    for (int i = 5, end = 10; i <= end; ++i) {
      numberList.add(enums[i]);
    }
    for (int i = 5, end = 10; i <= end; ++i) {
      numberList.add(enums[i]);
    }
    numbers = EnumSet.copyOf(numberList);
    numberSet = new HashSet<E>(numberList);
    assertEquals(numberSet, numbers);

    try {
      numbers.add(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }

    // Try testing for null
    assertFalse("EnumSet should allow testing for null", numbers.contains(null));

    // Try testing for an Enum that looks the same, but has different type
    numbers.clear();
    numbers.add(enums[1]);
    assertFalse("EnumSet should test for correct type.",
        numbers.contains(FalseEnum.One));

    // Same type testing, but on remove.
    numbers.remove(FalseEnum.One);
    assertTrue("EnumSet should test for correct type on remove",
        numbers.contains(enums[1]));
  }
}
