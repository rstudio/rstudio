/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.emultest.java.math;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Tests for {@link MathContext}.
 */
public class MathContextTest extends EmulTestBase {
  
  public void testMathContextSingleArgConstructor() {
    MathContext mc1 = new MathContext("precision=16 roundingMode=CEILING"); 
    assertTrue(mc1.getPrecision() == 16);
    assertTrue(mc1.getRoundingMode().equals(RoundingMode.CEILING));
    
    MathContext mc2 = new MathContext("precision=17 roundingMode=DOWN"); 
    assertTrue(mc2.getPrecision() == 17);
    assertTrue(mc2.getRoundingMode().equals(RoundingMode.DOWN));
    
    MathContext mc3 = new MathContext("precision=18 roundingMode=FLOOR"); 
    assertTrue(mc3.getPrecision() == 18);
    assertTrue(mc3.getRoundingMode().equals(RoundingMode.FLOOR));
    
    MathContext mc4 = new MathContext("precision=19 roundingMode=HALF_DOWN"); 
    assertTrue(mc4.getPrecision() == 19);
    assertTrue(mc4.getRoundingMode().equals(RoundingMode.HALF_DOWN));
    
    MathContext mc5 = new MathContext("precision=20 roundingMode=HALF_EVEN"); 
    assertTrue(mc5.getPrecision() == 20);
    assertTrue(mc5.getRoundingMode().equals(RoundingMode.HALF_EVEN));
    
    MathContext mc6 = new MathContext("precision=21 roundingMode=HALF_UP"); 
    assertTrue(mc6.getPrecision() == 21);
    assertTrue(mc6.getRoundingMode().equals(RoundingMode.HALF_UP));
    
    MathContext mc7 = new MathContext("precision=22 roundingMode=UNNECESSARY"); 
    assertTrue(mc7.getPrecision() == 22);
    assertTrue(mc7.getRoundingMode().equals(RoundingMode.UNNECESSARY));
    
    MathContext mc8 = new MathContext("precision=23 roundingMode=UP"); 
    assertTrue(mc8.getPrecision() == 23);
    assertTrue(mc8.getRoundingMode().equals(RoundingMode.UP));
    
    // try some badly formatted args
    try {
      new MathContext("prcision=27 roundingMode=CEILING");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext("precision=26 roundingMoe=CEILING");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext("precision=25 roundingMode=CEILINGFAN");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext("precision=24 roundingMode=HALF");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext("precision=23 roundingMode=UPSIDEDOWN");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext("precision=22roundingMode=UP");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    
    try {
      new MathContext(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }
  
  public void testMathContextConstructorEquality() {
    MathContext mc1 = new MathContext(16,RoundingMode.CEILING); 
    MathContext mc1a = new MathContext("precision=16 roundingMode=CEILING"); 
    assertTrue(mc1.equals(mc1a));
    
    MathContext mc2 = new MathContext(17,RoundingMode.DOWN); 
    MathContext mc2a = new MathContext("precision=17 roundingMode=DOWN"); 
    assertTrue(mc2.equals(mc2a));
    
    MathContext mc3 = new MathContext(18,RoundingMode.FLOOR); 
    MathContext mc3a = new MathContext("precision=18 roundingMode=FLOOR"); 
    assertTrue(mc3.equals(mc3a));
    
    MathContext mc4 = new MathContext(19,RoundingMode.HALF_DOWN); 
    MathContext mc4a = new MathContext("precision=19 roundingMode=HALF_DOWN"); 
    assertTrue(mc4.equals(mc4a));
    
    MathContext mc5 = new MathContext(20,RoundingMode.HALF_EVEN); 
    MathContext mc5a = new MathContext("precision=20 roundingMode=HALF_EVEN"); 
    assertTrue(mc5.equals(mc5a));
    
    MathContext mc6 = new MathContext(21,RoundingMode.HALF_UP); 
    MathContext mc6a = new MathContext("precision=21 roundingMode=HALF_UP"); 
    assertTrue(mc6.equals(mc6a));
    
    MathContext mc7 = new MathContext(22,RoundingMode.UNNECESSARY); 
    MathContext mc7a = new MathContext("precision=22 roundingMode=UNNECESSARY"); 
    assertTrue(mc7.equals(mc7a));
    
    MathContext mc8 = new MathContext(23,RoundingMode.UP); 
    MathContext mc8a = new MathContext("precision=23 roundingMode=UP"); 
    assertTrue(mc8.equals(mc8a));
  }
}
