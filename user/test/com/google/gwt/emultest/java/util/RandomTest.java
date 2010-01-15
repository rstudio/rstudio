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

import java.util.Random;

/**
 * Tests for GWT's emulation of the JRE Random class.  The JRE specifies the
 * exact algorithm used to generate the pseudorandom output.
 */
public class RandomTest extends GWTTestCase {

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }
  
  public void testNextBytes() {
    Random r = new Random(1);
    byte[] b = new byte[5];
    r.nextBytes(b);
    assertEquals((byte) 115, b[0]);
    assertEquals((byte) -43, b[1]);
    assertEquals((byte) 26, b[2]);
    assertEquals((byte) -69, b[3]);
    assertEquals((byte) -40, b[4]);
    
    try {
      r.nextBytes(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
    }
  }
  
  public void testNextDouble() {
    Random r = new Random(1);
    assertEquals(0.7308781907032909, r.nextDouble());
    assertEquals(0.41008081149220166, r.nextDouble());
    assertEquals(0.20771484130971707, r.nextDouble());
    assertEquals(0.3327170559595112, r.nextDouble());
    assertEquals(0.9677559094241207, r.nextDouble());
  }
  
  public void testNextFloat() {
    Random r = new Random(1);
    assertEquals(0.7308782f, r.nextFloat());
    assertEquals(0.100473166f, r.nextFloat());
    assertEquals(0.4100808f, r.nextFloat());
    assertEquals(0.40743977f, r.nextFloat());
    assertEquals(0.2077148f, r.nextFloat());
  }
  
  public void testNextGaussian() {
    Random r = new Random(1);
    assertEquals(1.561581040188955, r.nextGaussian());
    assertEquals(-0.6081826070068602, r.nextGaussian());
    assertEquals(-1.0912278829447088, r.nextGaussian());
    assertEquals(-0.6245401364066232, r.nextGaussian());
    assertEquals(-1.1182832102556484, r.nextGaussian());
  }
  
  public void testNextInt() {
    Random r = new Random(1);
    assertEquals(-1155869325, r.nextInt());
    assertEquals(431529176, r.nextInt());
    assertEquals(1761283695, r.nextInt());
    assertEquals(1749940626, r.nextInt());
    assertEquals(892128508, r.nextInt());
    
    try {
      r.nextInt(0);
      fail("Expected IlledgalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    
    try {
      r.nextInt(-1);
      fail("Expected IlledgalArgumentException");
    } catch (IllegalArgumentException e) {
    }
  }
  
  public void testNextInt100() {
    Random r = new Random(1);
    assertEquals(85, r.nextInt(100));
    assertEquals(88, r.nextInt(100));
    assertEquals(47, r.nextInt(100));
    assertEquals(13, r.nextInt(100));
    assertEquals(54, r.nextInt(100));
  }  
  
  public void testNextInt128() {
    Random r = new Random(1);
    assertEquals(93, r.nextInt(128));
    assertEquals(12, r.nextInt(128));
    assertEquals(52, r.nextInt(128));
    assertEquals(52, r.nextInt(128));
    assertEquals(26, r.nextInt(128));
  }
  
  public void testNextLong() {
    Random r = new Random(1);
    assertEquals(-4964420948893066024L, r.nextLong());
    assertEquals(7564655870752979346L, r.nextLong());
    assertEquals(3831662765844904176L, r.nextLong());
    assertEquals(6137546356583794141L, r.nextLong());
    assertEquals(-594798593157429144L, r.nextLong());
  }
  
  public void testSetSeed() {
    Random r = new Random();
    
    r.setSeed(1);
    byte[] b = new byte[1];
    r.nextBytes(b);
    assertEquals((byte) 115, b[0]);
    
    r.setSeed(1);
    assertEquals(0.7308781907032909, r.nextDouble());
    
    r.setSeed(1);
    assertEquals(0.7308782f, r.nextFloat());
    
    r.setSeed(1);
    assertEquals(1.561581040188955, r.nextGaussian());
    
    r.setSeed(1);
    assertEquals(-1155869325, r.nextInt());
    
    r.setSeed(1);
    assertEquals(85, r.nextInt(100));
    
    r.setSeed(1);
    assertEquals(93, r.nextInt(128));
    
    r.setSeed(1);
    assertEquals(-4964420948893066024L, r.nextLong());
  }
}
