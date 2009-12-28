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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * INCLUDES MODIFICATIONS BY GOOGLE.
 */
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Random;

/**
 * Test java.util.Random.
 */
public class RandomTest extends GWTTestCase {

  private Random r = new Random();

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Tests that two generators with the same seed produce the same sequence of
   * values.
   */
  public void test_ConstructorJ() {
    Random r = new Random(8409238L);
    Random r2 = new Random(8409238L);
    for (int i = 0; i < 100; i++) {
      assertEquals("Values from randoms with same seed don't match",
          r.nextInt(), r2.nextInt());
    }
  }

  /**
   * Tests {@link java.util.Random#nextBoolean()}.
   */
  public void test_nextBoolean() {
    boolean falseAppeared = false, trueAppeared = false;
    for (int counter = 0; counter < 100; counter++)
      if (r.nextBoolean()) {
        trueAppeared = true;
      } else {
        falseAppeared = true;
      }
    assertTrue("Calling nextBoolean() 100 times resulted in all trues",
        falseAppeared);
    assertTrue("Calling nextBoolean() 100 times resulted in all falses",
        trueAppeared);
  }

  /**
   * Tests {@link java.util.Random#nextBytes(byte[])}.
   */
  public void test_nextBytes$B() {
    boolean someDifferent = false;
    byte[] randomBytes = new byte[100];
    r.nextBytes(randomBytes);
    byte firstByte = randomBytes[0];
    for (int counter = 1; counter < randomBytes.length; counter++) {
      if (randomBytes[counter] != firstByte) {
        someDifferent = true;
      }
    }
    assertTrue("nextBytes() returned an array of length 100 of the same byte",
        someDifferent);
  }

  /**
   * Tests {@link java.util.Random#nextDouble()}.
   */
  public void test_nextDouble() {
    double lastNum = r.nextDouble();
    double nextNum;
    boolean someDifferent = false;
    boolean inRange = true;
    for (int counter = 0; counter < 100; counter++) {
      nextNum = r.nextDouble();
      if (nextNum != lastNum) {
        someDifferent = true;
      }
      if (!(0 <= nextNum && nextNum < 1.0)) {
        inRange = false;
      }
      lastNum = nextNum;
    }
    assertTrue("Calling nextDouble 100 times resulted in same number",
        someDifferent);
    assertTrue("Calling nextDouble resulted in a number out of range [0,1)",
        inRange);
  }

  /**
   * Tests {@link java.util.Random#nextFloat()}.
   */
  public void test_nextFloat() {
    float lastNum = r.nextFloat();
    float nextNum;
    boolean someDifferent = false;
    boolean inRange = true;
    for (int counter = 0; counter < 100; counter++) {
      nextNum = r.nextFloat();
      if (nextNum != lastNum) {
        someDifferent = true;
      }
      if (!(0 <= nextNum && nextNum < 1.0)) {
        inRange = false;
      }
      lastNum = nextNum;
    }
    assertTrue("Calling nextFloat 100 times resulted in same number",
        someDifferent);
    assertTrue("Calling nextFloat resulted in a number out of range [0,1)",
        inRange);
  }

  /**
   * Tests {@link java.util.Random#nextGaussian()}.
   */
  public void test_nextGaussian() {
    double lastNum = r.nextGaussian();
    double nextNum;
    boolean someDifferent = false;
    boolean someInsideStd = false;
    for (int counter = 0; counter < 100; counter++) {
      nextNum = r.nextGaussian();
      if (nextNum != lastNum) {
        someDifferent = true;
      }
      if (-1.0 <= nextNum && nextNum <= 1.0) {
        someInsideStd = true;
      }
      lastNum = nextNum;
    }
    assertTrue("Calling nextGaussian 100 times resulted in same number",
        someDifferent);
    assertTrue(
        "Calling nextGaussian 100 times resulted in no number within 1 std. deviation of mean",
        someInsideStd);
  }

  /**
   * Tests {@link java.util.Random#nextInt()}.
   */
  public void test_nextInt() {
    int lastNum = r.nextInt();
    int nextNum;
    boolean someDifferent = false;
    for (int counter = 0; counter < 100; counter++) {
      nextNum = r.nextInt();
      if (nextNum != lastNum) {
        someDifferent = true;
      }
      lastNum = nextNum;
    }
    assertTrue("Calling nextInt 100 times resulted in same number",
        someDifferent);
  }

  /**
   * Tests {@link java.util.Random#nextInt(int)}.
   */
  public void test_nextIntI() {
    final int range = 10;
    int lastNum = r.nextInt(range);
    int nextNum;
    boolean someDifferent = false;
    boolean inRange = true;
    for (int counter = 0; counter < 100; counter++) {
      nextNum = r.nextInt(range);
      if (nextNum != lastNum) {
        someDifferent = true;
      }
      if (!(0 <= nextNum && nextNum < range)) {
        inRange = false;
      }
      lastNum = nextNum;
    }
    assertTrue("Calling nextInt (range) 100 times resulted in same number",
        someDifferent);
    assertTrue("Calling nextInt (range) resulted in a number outside of [0, range)",
        inRange);
  }

  /**
   * Tests {@link java.util.Random#nextLong()}.
   */
  public void test_nextLong() {
    long lastNum = r.nextLong();
    long nextNum;
    boolean someDifferent = false;
    for (int counter = 0; counter < 100; counter++) {
      nextNum = r.nextLong();
      if (nextNum != lastNum) {
        someDifferent = true;
      }
      lastNum = nextNum;
    }
    assertTrue("Calling nextLong 100 times resulted in same number",
        someDifferent);
  }

  // two random create at a time should also generated different results
  // regression test for Harmony 4616
  public void test_random_generate() throws Exception {
    for (int i = 0; i < 100; i++) {
      Random random1 = new Random();
      Random random2 = new Random();
      assertFalse(random1.nextLong() == random2.nextLong());
    }
  }

  /**
   * Tests {@link java.util.Random#setSeed(long)}.
   */
  public void test_setSeedJ() {
    long[] randomArray = new long[100];
    boolean someDifferent = false;
    long firstSeed = 1000;
    long aLong, anotherLong, yetAnotherLong;
    Random aRandom = new Random();
    Random anotherRandom = new Random();
    Random yetAnotherRandom = new Random();
    aRandom.setSeed(firstSeed);
    anotherRandom.setSeed(firstSeed);
    for (int counter = 0; counter < randomArray.length; counter++) {
      aLong = aRandom.nextLong();
      anotherLong = anotherRandom.nextLong();
      assertEquals("Two randoms with same seeds gave differing nextLong values",
          aLong, anotherLong);
      yetAnotherLong = yetAnotherRandom.nextLong();
      randomArray[counter] = aLong;
      if (aLong != yetAnotherLong) {
        someDifferent = true;
      }
    }
    assertTrue("Two randoms with the different seeds gave the same chain of values",
        someDifferent);
    aRandom.setSeed(firstSeed);
    for (long element : randomArray) {
      assertEquals(
          "Reseting a random to its old seed did not result in the same chain of values as it gave before",
          element, aRandom.nextLong());
    }
  }
}
