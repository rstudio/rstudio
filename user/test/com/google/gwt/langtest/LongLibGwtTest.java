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
package com.google.gwt.langtest;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.lang.LongLibTestBase;

/**
 * Test the LongLib class as a GWTTestCase.
 */
public class LongLibGwtTest extends GWTTestCase {

  private LongLibTestBase impl = new LongLibTestBase();

  @Override
  public String getModuleName() {
    return "com.google.gwt.langtest.LongLibGwtTest";
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

  public void testMod() {
    impl.testMod();
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
