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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that static evaluation, mainly in
 * {@link com.google.gwt.dev.jjs.impl.DeadCodeElimination DeadCodeElimination},
 * does not go wrong.
 * 
 * This test does not verify that static evaluation is happening, merely that it
 * does not make incorrect changes. To verify that static eval is happening, run
 * with <code>-Dgwt.jjs.traceMethods=JStaticEvalTest.*</code> . All calls to
 * assert should become trivial things like <code>assertEquals("", 5, 5)</code>.
 * 
 * To verify the test itself, which includes a lot of random constants, run in
 * Development Mode.
 */
public class JStaticEvalTest extends GWTTestCase {
  private static void assertEquals(double expected, double actual) {
    assertEquals(expected, actual, 0.0001);
  }

  private volatile double fieldDoubleFive = 5.0;
  private volatile boolean fieldFalse = false;
  private volatile float fieldFloatFive = 5.0F;
  private volatile int[] fieldIntArray = new int[10];
  private volatile int fieldIntFive = 5;
  private volatile long fieldLongFive = 5L;
  private volatile Object fieldObject = new Object();
  private volatile boolean fieldTrue = true;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  /**
   * Tests simplifications on ternary conditional expressions.
   */
  public void testConditionalExpressions() {
    assertEquals(1, returnTrue() ? 1 : 2);
    assertEquals(2, returnFalse() ? 1 : 2);
    assertEquals(true, fieldTrue ? true : fieldFalse);
    assertEquals(false, fieldTrue ? false : fieldTrue);
    assertEquals(true, fieldTrue ? fieldTrue : false);
    assertEquals(false, fieldTrue ? fieldFalse : true);
    assertEquals(2, !fieldTrue ? 1 : 2);
    assertEquals(2, !fieldTrue ? 1 : 2);

    /*
     * This example causes Simplifier to recurse into itself, which in previous
     * versions could cause a NullPointerException. The sequence of
     * simplifications is:
     */
    // (fieldFalse = false, !fieldFalse) ? 1 : 2 // inlining
    // (fieldFalse = false, !fieldfalse ? 1 : 2) // move multi outward
    // (fieldFalse = false, fieldFalse ? 2 : 1) // flip negative condition
    int res = doSomethingAndReturnTrue() ? 1 : 2;
    assertEquals(1, res);
  }

  /**
   * Test "true == booleanField" and permutations, as well as "true == false"
   * and permutations.
   */
  public void testEqualsBool() {
    assertTrue(fieldTrue == returnTrue());
    assertTrue(returnTrue() == fieldTrue);
    assertFalse(fieldTrue == returnFalse());
    assertFalse(returnFalse() == fieldTrue);
    assertTrue(fieldTrue != returnFalse());
    assertTrue(returnFalse() != fieldTrue);
    assertFalse(fieldTrue != returnTrue());
    assertFalse(returnTrue() != fieldTrue);
    assertTrue(returnTrue() & returnTrue());
    assertTrue(returnTrue() | returnTrue());
  }

  /**
   * Tests equality on literals.
   */
  public void testEqualsLit() {
    assertTrue(returnTrue() == returnTrue());
    assertFalse(returnTrue() == returnFalse());
    assertFalse(returnFalse() == returnTrue());
    assertTrue(returnFalse() == returnFalse());
    assertFalse(returnTrue() != returnTrue());
    assertTrue(returnTrue() != returnFalse());
    assertTrue(returnFalse() != returnTrue());
    assertFalse(returnFalse() != returnFalse());

    assertTrue(returnIntFive() == returnIntFive());
    assertFalse(returnIntFive() != returnIntFive());
    assertTrue(returnDoubleOneHalf() == returnDoubleOneHalf());
    assertFalse(returnDoubleOneHalf() != returnDoubleOneHalf());
  }

  /**
   * Test rewriting if (!x) a else b to if(x) b else a. Likewise for conditional
   * expressions.
   */
  public void testFlippedIf() {
    String branch;
    if (!fieldTrue) {
      branch = "A";
    } else {
      branch = "B";
    }
    assertEquals("B", branch);
    assertEquals("B", !fieldTrue ? "A" : "B");
  }

  /**
   * Tests constant folding.
   */
  public void testOpsOnLiterals() {
    assertEquals(10, returnIntFive() + returnIntFive());
    assertEquals(0, returnIntFive() - returnIntFive());
    assertEquals(25, returnIntFive() * returnIntFive());
    assertEquals(1, returnIntFive() / returnIntFive());
    assertEquals(3, returnIntThree() % returnIntFive());
    assertEquals(96, returnIntThree() << returnIntFive());
    assertEquals(0, returnIntThree() >> returnIntFive());
    assertEquals(134217727, (-returnIntThree()) >>> returnIntFive());
    assertEquals(7, returnIntFive() | returnIntThree());
    assertEquals(1, returnIntFive() & returnIntThree());
    assertEquals(0, returnIntFive() ^ returnIntFive());

    assertEquals(1.0, returnDoubleOneHalf() + returnDoubleOneHalf());
    assertEquals(0.0, returnDoubleOneHalf() - returnDoubleOneHalf());
    assertEquals(0.25, returnDoubleOneHalf() * returnDoubleOneHalf());
    assertEquals(1.0, returnDoubleOneHalf() / returnDoubleOneHalf());
    assertEquals(0.5, returnDoubleOneHalf() % returnDoubleFive());

    assertTrue(returnIntFive() == returnIntFive());
    assertTrue(returnLongFive() == returnLongFive());
    assertTrue(returnFloatFive() == returnFloatFive());
    assertTrue(returnDoubleFive() == returnDoubleFive());
    assertTrue(returnCharFive() == returnCharFive());
    assertTrue(returnIntFive() == returnFloatFive());

    assertFalse(returnIntFive() != returnIntFive());
    assertFalse(returnLongFive() != returnLongFive());
    assertFalse(returnFloatFive() != returnFloatFive());
    assertFalse(returnDoubleFive() != returnDoubleFive());
    assertFalse(returnCharFive() != returnCharFive());
    assertFalse(returnIntFive() != returnFloatFive());

    assertTrue(returnTrue() || returnFalse());
    assertFalse(returnTrue() && returnFalse());
    assertFalse(returnTrue() ^ returnTrue());

    assertFalse(returnIntFive() < returnIntThree());
    assertFalse(returnIntFive() <= returnIntThree());
    assertTrue(returnIntFive() > returnIntThree());
    assertTrue(returnIntFive() >= returnIntThree());

    assertTrue(returnDoubleOneHalf() < returnDoubleFive());
    assertTrue(returnDoubleOneHalf() <= returnDoubleFive());
    assertFalse(returnDoubleOneHalf() > returnDoubleFive());
    assertFalse(returnDoubleOneHalf() >= returnDoubleFive());

    assertEquals(10, returnIntFive() + returnCharFive());
    assertTrue(returnIntThree() < returnCharFive());

    assertEquals(-5, -returnIntFive());
    assertEquals(-5L, -returnLongFive());
    assertEquals(-5.0, -returnFloatFive());
    assertEquals(-5.0, -returnDoubleFive());
    assertEquals(-10000000000000000000.0, -returnBigDouble());

    assertFalse(!returnTrue());
    assertTrue(!returnFalse());

    assertEquals(-6, ~returnIntFive());
    assertEquals(-6L, ~returnLongFive());

    assertEquals(65536, ((char) returnIntNegOne()) + 1);
    assertEquals(10.0, ((double) returnIntFive()) + ((double) returnIntFive()));
    assertEquals(1.5, returnFloatOneHalf() + 1);
  }

  /**
   * Test various useless operations like x+0 and x*1.
   */
  public void testUselessOps() {
    assertEquals(5, fieldIntFive + returnIntZero());
    assertEquals(5L, fieldLongFive + returnIntZero());
    assertEquals(5.0, fieldFloatFive + returnIntZero());
    assertEquals(5.0, fieldDoubleFive + returnIntZero());
    assertEquals(5.0, fieldDoubleFive + returnCharZero());

    assertEquals(5, returnIntZero() + fieldIntFive);
    assertEquals(5L, returnIntZero() + fieldLongFive);
    assertEquals(5.0, returnIntZero() + fieldFloatFive);
    assertEquals(5.0, returnIntZero() + fieldDoubleFive);
    assertEquals(5.0, returnCharZero() + fieldDoubleFive);

    assertEquals(5, fieldIntFive - returnIntZero());
    assertEquals(5L, fieldLongFive - returnIntZero());
    assertEquals(5.0, fieldFloatFive - returnIntZero());
    assertEquals(5.0, fieldDoubleFive - returnIntZero());

    assertEquals(-5, returnIntZero() - fieldIntFive);
    assertEquals(-5L, returnIntZero() - fieldLongFive);
    assertEquals(-5.0, returnDoubleZero() - fieldLongFive);
    assertEquals(-5.0, returnIntZero() - fieldFloatFive);
    assertEquals(-5.0, returnIntZero() - fieldDoubleFive);

    assertEquals(5, fieldIntFive * returnIntOne());
    assertEquals(5L, fieldLongFive * returnLongOne());
    assertEquals(5.0, fieldFloatFive * returnFloatOne());
    assertEquals(5.0, fieldDoubleFive * returnDoubleOne());

    assertEquals(5, fieldIntFive / returnIntOne());
    assertEquals(5L, fieldLongFive / returnLongOne());
    assertEquals(5.0, fieldFloatFive / returnFloatOne());
    assertEquals(5.0, fieldDoubleFive / returnDoubleOne());

    assertEquals(-5, fieldIntFive * -returnIntOne());
    assertEquals(-5L, fieldLongFive * -returnLongOne());
    assertEquals(-5.0, fieldFloatFive * -returnFloatOne());
    assertEquals(-5.0, fieldDoubleFive * -returnDoubleOne());
    assertEquals(-327675, fieldIntFive * -returnCharNegOne());

    assertEquals(-5, fieldIntFive / -returnIntOne());
    assertEquals(-5L, fieldLongFive / -returnLongOne());
    assertEquals(-5.0, fieldFloatFive / -returnFloatOne());
    assertEquals(-5.0, fieldDoubleFive / -returnDoubleOne());

    assertEquals(5, -returnMinusFieldIntFive());
    assertEquals(5L, -returnMinusFieldLongFive());
    assertEquals(5.0, -returnMinusFieldFloatFive());
    assertEquals(5.0, -returnMinusFieldDoubleFive());

    assertEquals(5, fieldIntFive << returnIntZero());
    assertEquals(5L, fieldLongFive << returnIntZero());

    assertEquals(5, fieldIntFive >> returnIntZero());
    assertEquals(5L, fieldLongFive >> returnIntZero());

    assertEquals(5, fieldIntFive >>> returnIntZero());
    assertEquals(5L, fieldLongFive >>> returnIntZero());

    assertTrue(!returnNotFieldTrue());

    assertTrue(fieldTrue ^ returnFalse());
    assertFalse(fieldTrue ^ returnTrue());
    assertFalse(returnTrue() ^ fieldTrue);
    assertTrue(returnFalse() ^ fieldTrue);

    assertEquals(0.0, fieldIntFive * returnDoubleZero());

    // do not simplify x*0 if x has a side effect
    try {
      assertEquals(0.0, throwError() * returnDoubleZero());
      fail("Expected an exception");
    } catch (Error e) {
    }

    assertEquals(0.0, returnDoubleZero() * fieldIntFive);

    // do not simplify 0*x if x has a side effect
    try {
      assertEquals(0.0, returnDoubleZero() * throwError());
      fail("Expected an exception");
    } catch (Error e) {
    }

    assertTrue(fieldIntArray != null);
    assertFalse(fieldIntArray == null);
    if (fieldIntArray == null) {
      fail();
    }
    if (fieldIntArray != null) {
    } else {
      fail();
    }

    // do not simplify foo==null if foo can be a string
    assertTrue(returnEmptyString() != null);
    assertFalse(returnEmptyString() == null);
    if (returnEmptyString() == null) {
      fail();
    }
    assertFalse(fieldObject == null);
    if (fieldObject == null) {
      fail();
    }
  }

  /**
   * This method will inline as a multi.
   */
  private boolean doSomethingAndReturnTrue() {
    fieldTrue = true;
    return !fieldFalse;
  }

  // All of these returnFoo() methods exist so that the
  // JDT will not be able to statically evaluate with
  // the returned values. These simple methods will be
  // inlined by GWT, though, thus giving its static
  // evaluation a chance to run.

  /**
   * Return a double too large to fit in a long.
   */
  private double returnBigDouble() {
    return 10000000000000000000.0;
  }

  private char returnCharFive() {
    return (char) 5;
  }

  private char returnCharNegOne() {
    return (char) -1;
  }

  private char returnCharZero() {
    return (char) 0;
  }

  private double returnDoubleFive() {
    return 5.0;
  }

  private double returnDoubleOne() {
    return 1.0;
  }

  private double returnDoubleOneHalf() {
    return 0.5;
  }

  private double returnDoubleZero() {
    return 0.0;
  }

  private String returnEmptyString() {
    return "";
  }

  private boolean returnFalse() {
    return false;
  }

  private float returnFloatFive() {
    return 5.0F;
  }

  private float returnFloatOne() {
    return 1.0F;
  }

  private float returnFloatOneHalf() {
    return 0.5F;
  }

  private int returnIntFive() {
    return 5;
  }

  private int returnIntNegOne() {
    return -1;
  }

  private int returnIntOne() {
    return 1;
  }

  private int returnIntThree() {
    return 3;
  }

  private int returnIntZero() {
    return 0;
  }

  private long returnLongFive() {
    return 5L;
  }

  private long returnLongOne() {
    return 1L;
  }

  private double returnMinusFieldDoubleFive() {
    return -fieldDoubleFive;
  }

  private float returnMinusFieldFloatFive() {
    return -fieldFloatFive;
  }

  private int returnMinusFieldIntFive() {
    return -fieldIntFive;
  }

  private long returnMinusFieldLongFive() {
    return -fieldLongFive;
  }

  private boolean returnNotFieldTrue() {
    return !fieldTrue;
  }

  private boolean returnTrue() {
    return true;
  }

  private int throwError() {
    throw new Error();
  }
}
