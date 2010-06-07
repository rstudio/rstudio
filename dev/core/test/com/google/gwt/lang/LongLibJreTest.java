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
package com.google.gwt.lang;

import junit.framework.TestCase;

/**
 * Test the LongLib class as a non-GWT TestCase.
 */
public class LongLibJreTest extends TestCase {
  
  static {
    LongLibBase.RUN_IN_JVM = true;
  }
  
  private LongLibTestBase impl = new LongLibTestBase();

  public void testAAAA() {
    assertEquals(1.4e-45f, 1.401298464324817E-45, 0.0);
  }
  
  public void testAdditive() {
    impl.testAdditive();
  }

  public void testBitOps() {
    impl.testBitOps();
  }

  public void testComparisons() {
    impl.testComparisons();
  }

  public void testConversions() {
    impl.testConversions();
  }

  public void testDiv() {
    impl.testDiv();
  }

  public void testFactorial() {
    impl.testFactorial();
  }

  public void testFromDouble() {
    impl.testFromDouble();
  }

  public void testMinMax() {
    impl.testMinMax();
  }

  public void testMultiplicative() {
    impl.testMultiplicative();
  }

  public void testNegate() {
    impl.testNegate();
  }

  public void testShift() {
    impl.testShift();
  }

  public void testToHexString() {
    impl.testToHexString();
  }

  public void testToString() {
    impl.testToString();
  }
}
