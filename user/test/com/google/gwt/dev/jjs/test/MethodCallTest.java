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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests method invocations including potential inlining bugs.
 */
public class MethodCallTest extends GWTTestCase {

  private static final class MyException extends RuntimeException {
  }

  private static Object field;

  private static void clobberFieldNoInline() {
    try {
      field = null;
    } catch (Throwable e) {
      e.toString();
    }
  }

  private static int manyArgs(int i0, int i1, int i2, int i3, int i4, int i5,
      int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13,
      int i14, int i15, int i16, int i17, int i18, int i19, int i20, int i21,
      int i22, int i23, int i24, int i25, int i26, int i27, int i28, int i29,
      int i30, int i31, int i32, int i33, int i34, int i35, int i36, int i37,
      int i38, int i39, int i40, int i41, int i42, int i43, int i44, int i45,
      int i46, int i47, int i48, int i49, int i50, int i51, int i52, int i53,
      int i54, int i55, int i56, int i57, int i58, int i59, int i60, int i61,
      int i62, int i63, int i64, int i65, int i66, int i67, int i68, int i69,
      int i70, int i71, int i72, int i73, int i74, int i75, int i76, int i77,
      int i78, int i79, int i80, int i81, int i82, int i83, int i84, int i85,
      int i86, int i87, int i88, int i89, int i90, int i91, int i92, int i93,
      int i94, int i95, int i96, int i97, int i98, int i99, int i100, int i101,
      int i102, int i103, int i104, int i105, int i106, int i107, int i108,
      int i109, int i110, int i111, int i112, int i113, int i114, int i115,
      int i116, int i117, int i118, int i119, int i120, int i121, int i122,
      int i123, int i124, int i125, int i126, int i127, int i128, int i129,
      int i130, int i131, int i132, int i133, int i134, int i135, int i136,
      int i137, int i138, int i139, int i140, int i141, int i142, int i143,
      int i144, int i145, int i146, int i147, int i148, int i149, int i150,
      int i151, int i152, int i153, int i154, int i155, int i156, int i157,
      int i158, int i159, int i160, int i161, int i162, int i163, int i164,
      int i165, int i166, int i167, int i168, int i169, int i170, int i171,
      int i172, int i173, int i174, int i175, int i176, int i177, int i178,
      int i179, int i180, int i181, int i182, int i183, int i184, int i185,
      int i186, int i187, int i188, int i189, int i190, int i191, int i192,
      int i193, int i194, int i195, int i196, int i197, int i198, int i199,
      int i200, int i201, int i202, int i203, int i204, int i205, int i206,
      int i207, int i208, int i209, int i210, int i211, int i212, int i213,
      int i214, int i215, int i216, int i217, int i218, int i219, int i220,
      int i221, int i222, int i223, int i224, int i225, int i226, int i227,
      int i228, int i229, int i230, int i231, int i232, int i233, int i234,
      int i235, int i236, int i237, int i238, int i239, int i240, int i241,
      int i242, int i243, int i244, int i245, int i246, int i247, int i248,
      int i249, int i250, int i251, int i252, int i253, int i254) {
    return i0 + i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12
        + i13 + i14 + i15 + i16 + i17 + i18 + i19 + i20 + i21 + i22 + i23 + i24
        + i25 + i26 + i27 + i28 + i29 + i30 + i31 + i32 + i33 + i34 + i35 + i36
        + i37 + i38 + i39 + i40 + i41 + i42 + i43 + i44 + i45 + i46 + i47 + i48
        + i49 + i50 + i51 + i52 + i53 + i54 + i55 + i56 + i57 + i58 + i59 + i60
        + i61 + i62 + i63 + i64 + i65 + i66 + i67 + i68 + i69 + i70 + i71 + i72
        + i73 + i74 + i75 + i76 + i77 + i78 + i79 + i80 + i81 + i82 + i83 + i84
        + i85 + i86 + i87 + i88 + i89 + i90 + i91 + i92 + i93 + i94 + i95 + i96
        + i97 + i98 + i99 + i100 + i101 + i102 + i103 + i104 + i105 + i106
        + i107 + i108 + i109 + i110 + i111 + i112 + i113 + i114 + i115 + i116
        + i117 + i118 + i119 + i120 + i121 + i122 + i123 + i124 + i125 + i126
        + i127 + i128 + i129 + i130 + i131 + i132 + i133 + i134 + i135 + i136
        + i137 + i138 + i139 + i140 + i141 + i142 + i143 + i144 + i145 + i146
        + i147 + i148 + i149 + i150 + i151 + i152 + i153 + i154 + i155 + i156
        + i157 + i158 + i159 + i160 + i161 + i162 + i163 + i164 + i165 + i166
        + i167 + i168 + i169 + i170 + i171 + i172 + i173 + i174 + i175 + i176
        + i177 + i178 + i179 + i180 + i181 + i182 + i183 + i184 + i185 + i186
        + i187 + i188 + i189 + i190 + i191 + i192 + i193 + i194 + i195 + i196
        + i197 + i198 + i199 + i200 + i201 + i202 + i203 + i204 + i205 + i206
        + i207 + i208 + i209 + i210 + i211 + i212 + i213 + i214 + i215 + i216
        + i217 + i218 + i219 + i220 + i221 + i222 + i223 + i224 + i225 + i226
        + i227 + i228 + i229 + i230 + i231 + i232 + i233 + i234 + i235 + i236
        + i237 + i238 + i239 + i240 + i241 + i242 + i243 + i244 + i245 + i246
        + i247 + i248 + i249 + i250 + i251 + i252 + i253 + i254;
  }

  /**
   * If this gets inlined into {@link #testShouldNotInline()}, the value of
   * <code>o</code> will be seen to be cleared. This is because the parameter
   * <code>o</code> will have been replaced by a direct reference to
   * {@link #field}. Both the Java and JS inliners must not inline this.
   */
  private static void shouldNotInline(Object o) {
    field = null;
    o.toString();
  }

  /**
   * Same as {@link #shouldNotInline(Object)}, except the field clobber is done
   * indirectly in a non-inlinable method.
   */
  private static void shouldNotInline2(Object o) {
    clobberFieldNoInline();
    o.toString();
  }

  private int value;

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  /**
   * Ensure that call-site side-effects happen before expressions in the callee
   * are evaluated.
   */
  public void testArgumentEffectPreceedsCalleeEffect() {
    value = 10;
    int result = doubleValueAndAdd(value += 2);
    assertEquals(24, value);
    assertEquals(36, result);
  }

  /**
   * Ensure that call-site side-effects happen before expressions in the callee
   * are evaluated.
   */
  public void testArgumentEffectPreceedsCalleeValue() {
    value = 0;
    int result = addToValue(value = 10);
    assertEquals(10, value);
    assertEquals(20, result);
  }

  /**
   * Ensure that side-effects are processed in the correct order.
   */
  public void testArgumentsEvalInCorrectOrder() {
    value = 10;
    int result = checkOrder(value, ++value);
    assertEquals(11, value);
    assertEquals(11010, result);

    int localValue = 20;
    result = checkOrder(localValue, ++localValue);
    assertEquals(21, localValue);
    assertEquals(21020, result);
  }

  /**
   * Ensure that call-site evaluation happens before effects in the callee
   * occur.
   */
  public void testArgumentValuePreceedsCalleeEffect() {
    value = 10;
    int result = resetValueAndAdd(value);
    assertEquals(0, value);
    assertEquals(10, result);
  }

  public void testAssignsToParam() {
    value = 10;
    int result = assignsToParam(value);
    assertEquals(10, value);
    assertEquals(11, result);

    int localValue = 20;
    result = assignsToParam(localValue);
    assertEquals(20, localValue);
    assertEquals(21, result);
  }

  /**
   * Ensure that local variable assigned in invocation arguments are evaluated
   * in the correct order.
   */
  public void testLocalAssignmentInArgs() {
    int local = 0;
    assertEquals(5, addMultReverseOrder(local, local = 1, local));
  }

  public void testManyArgs() {
    assertEquals(32385, manyArgs(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67,
        68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85,
        86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102,
        103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116,
        117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130,
        131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144,
        145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158,
        159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172,
        173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186,
        187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200,
        201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214,
        215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228,
        229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242,
        243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254));
  }

  /**
   * Ensure that we correctly inline a method that ignores its parameters.
   */
  public void testNoParameterRefs() {
    int value = 100;
    assertEquals(1, ignoreParams(value++, value++));
    assertEquals(102, value);
  }

  public void testRecursion() {
    assertEquals(210, recursiveSum(20));
  }

  /**
   * Tests that {@link #shouldNotInline(Object)} does not get inlined. Inlining
   * would cause a read of {@link #field} after its value has been clobbered.
   */
  public void testShouldNotInline() {
    field = new Object();
    shouldNotInline(field);
  }

  /**
   * Tests that {@link #shouldNotInline2(Object)} does not get inlined. Inlining
   * would cause a read of {@link #field} after its value has been clobbered.
   */
  public void testShouldNotInline2() {
    field = new Object();
    shouldNotInline2(field);
  }

  /**
   * Ensure that side-effects always execute.
   */
  public void testSideEffectsAlwaysExecute1() {
    value = 1;
    assertEquals(0, conditional1(value++));
    assertEquals(2, value);

    int localValue = 1;
    assertEquals(0, conditional1(localValue++));
    assertEquals(2, localValue);
  }

  /**
   * Ensure that side-effects always execute.
   */
  public void testSideEffectsAlwaysExecute2() {
    value = 1;
    assertEquals(false, conditional2(value++));
    assertEquals(2, value);

    int localValue = 1;
    assertEquals(false, conditional2(localValue++));
    assertEquals(2, localValue);
  }

  /**
   * Ensure that side-effects always execute.
   */
  public void testSideEffectsAlwaysExecute3() {
    value = 1;
    assertEquals(true, conditional3(value++));
    assertEquals(2, value);

    int localValue = 1;
    assertEquals(true, conditional3(localValue++));
    assertEquals(2, localValue);
  }

  /**
   * Ensure that call-site side-effects happen before callee side-effects.
   */
  public void testSideEffectsInFields() {
    value = 10;
    int result = add(++value, ++value);
    assertEquals(23, result);
  }

  /**
   * Ensure that side-effects always happen before the exception.
   */
  public void testSideEffectsVersusExceptions1() {
    int value = 10;
    try {
      // Use the return value so it doesn't get pruned
      assertEquals(0, addCorrectOrder(++value, throwMyException()));
      fail();
    } catch (MyException e) {
      // expected
      assertEquals(11, value);
    }

    try {
      // Use the return value so it doesn't get pruned
      assertEquals(0, addReverseOrder(++value, throwMyException()));
      fail();
    } catch (MyException e) {
      // expected
      assertEquals(12, value);
    }
  }

  /**
   * Ensure that the exception always happens before the side-effects.
   */
  public void testSideEffectsVersusExceptions2() {
    int value = 10;

    try {
      // Use the return value so it doesn't get pruned
      assertEquals(0, addCorrectOrder(throwMyException(), ++value));
      fail();
    } catch (MyException e) {
      // expected
      assertEquals(10, value);
    }

    // Use the return value so it doesn't get pruned
    try {
      assertEquals(0, addReverseOrder(throwMyException(), ++value));
      fail();
    } catch (MyException e) {
      // expected
      assertEquals(10, value);
    }
  }

  /**
   * Ensure that the exception always happens before the side-effects.
   */
  public void testSideEffectsVersusExceptions3() {
    int value = 10;

    try {
      // Use the return value so it doesn't get pruned
      assertEquals(0, throwExceptionAndReturn(++value));
      fail();
    } catch (MyException e) {
      // expected
      assertEquals(11, value);
    }
  }

  private int add(int i, int j) {
    return (value = 0) + i + j;
  }

  private int addCorrectOrder(int i, int j) {
    return i + j;
  }

  private int addMultReverseOrder(int a, int b, int c) {
    return c * 4 + b + a;
  }

  private int addReverseOrder(int i, int j) {
    return j + i;
  }

  private int addToValue(int i) {
    return value + i;
  }

  private int assignsToParam(int x) {
    return ++x;
  }

  private int checkOrder(int x, int y) {
    return y * 1000 + x;
  }

  private int conditional1(int i) {
    return (this.value != Integer.MAX_VALUE) ? 0 : i;
  }

  private boolean conditional2(int i) {
    return (this.value == Integer.MAX_VALUE) && (i == 1);
  }

  private boolean conditional3(int i) {
    return (this.value != Integer.MAX_VALUE) || (i == 1);
  }

  private int doubleValueAndAdd(int i) {
    return (value *= 2) + i;
  }

  private int ignoreParams(int i, int j) {
    return 1;
  }

  private int recursiveSum(int x) {
    if (x == 0) {
      return 0;
    } else {
      return x + recursiveSum(x - 1);
    }
  }

  private int resetValueAndAdd(int i) {
    return (value = 0) + i;
  }

  private int throwExceptionAndReturn(int i) {
    return throwMyException() + i;
  }

  private int throwMyException() {
    throw new MyException();
  }
}
