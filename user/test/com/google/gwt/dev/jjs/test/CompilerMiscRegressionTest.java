/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.core.client.js.JsType;
import com.google.gwt.dev.jjs.test.overrides.package1.Caller;
import com.google.gwt.dev.jjs.test.overrides.package1.ClassExposingM;
import com.google.gwt.dev.jjs.test.overrides.package1.SomeParent;
import com.google.gwt.dev.jjs.test.overrides.package1.SomeParentParent;
import com.google.gwt.dev.jjs.test.overrides.package1.SomeParentParentParent;
import com.google.gwt.dev.jjs.test.overrides.package1.SubClassExposingM;
import com.google.gwt.dev.jjs.test.overrides.package2.SomeSubClassInAnotherPackage;
import com.google.gwt.dev.jjs.test.overrides.package2.SomeSubSubClassInAnotherPackage;
import com.google.gwt.dev.jjs.test.overrides.package3.SomeInterface;
import com.google.gwt.dev.jjs.test.overrides.package3.SomePackageConfusedParent;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javaemul.internal.annotations.DoNotInline;

/**
 * Tests Miscelaneous fixes.
 */
public class CompilerMiscRegressionTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  native double toNumber(String value) /*-{
    return +value;
  }-*/;

  native double addAndConvert(double v1, String v2) /*-{
    return v1 + +v2;
  }-*/;

  native double minusAndDecrement(double val) /*-{
    var lhs = val;
    return - --lhs;
  }-*/;

  /**
   * The array {@code map.get("one")[0]} gets normalized (by {@link ImplementCastsAndTypeChecks}) to
   * {@code Cast.dynamicCast(map.get("one"), ...)[0]}. The expression resulting from dynamiCast
   * would have type Object and that would not be a valid type for an array access operation.
   */
  public void testOverridingReturnType() {
    Map<String, String[]> map = new HashMap();
    map.put("one", new String[10]);

    map.get("one")[0] = "one";
    assertEquals("one", map.get("one")[0]);
  }

  /**
   * Test for issues 6373 and 3942.
   */
  public void testUnaryPlus() {
    // With the unary + operator stripped the first assertion only fails in
    // dev mode, in web mode the  comparison made by assertEquals masks
    // the error; whereas the second fails in both dev and web modes.
    assertEquals(11.0, toNumber("11"));
    assertEquals(12.0, toNumber("10") + toNumber("2"));
    assertEquals(12.0, addAndConvert(10, "2"));
    assertEquals(-10.0, minusAndDecrement(11));
  }
  private static float[] copy(float[] src, float[] dest) {
    System.arraycopy(src, 0, dest, 0, Math.min(src.length, dest.length));
    return dest;
  }

  private void throwE(String message) {
    throw new RuntimeException(message);
  }

  /**
   * Test for issue 8243.
   */
  public void testAddAllLargeNumberOfElements() {

    int dstLength = 10;
    // Some browser have a limit on the number of parameters a function can have and 130000 barely
    // exceeds Chrome limit (as of V34).
    // This limit also applies when functions are called through apply().
    int srcLength =  130000;
    List<String> original = new ArrayList<String>();
    for (int i = 0; i < dstLength; i++) {
      original.add("foo");
    }
    List<String> src = new ArrayList<String>();
    for (int i = 0; i < srcLength; i++) {
      src.add("bar");
    }

    original.addAll(src);
    final int totalLength = srcLength + dstLength;
    assertEquals(totalLength, original.size());

    // Check the result sampling as iterating through large arrays seems costly in IE.
    for (int i = 0; i < totalLength; i += 1000) {
      if (i < dstLength) {
        assertEquals("foo", original.get(i));
      } else {
        assertEquals("bar", original.get(i));
      }
    }
  }

  /**
   * Test for issue 7253.
   */
  public void testNestedTryFollowedByTry() {
    try {
      throwE("1");
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertEquals("1", e.getMessage());
      try {
        throwE("2");
        fail("Should have thrown RuntimeException");
      } catch (RuntimeException e2) {
        assertEquals("2", e2.getMessage());
      }
    }
    try {
      throwE("3");
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertEquals("3", e.getMessage());
    }
  }

  /**
   * Test for issue 6638.
   */
  public void testNewArrayInlining() {
    float[] src = new float[]{1,1,1};
    float[] dest = copy(src, new float[3]);

    assertEqualContents(src, dest);
  }

  /**
   * Tests complex overriding patterns involving package private methods.
   * <p>
   * Test for issue 8654.
   */
  public void testOverride() {
    Caller aCaller = new Caller();
    assertEquals("SomeParentParent", aCaller.callPackagePrivatem(new SomeParentParent()));
    assertEquals("SomeParent", aCaller.callPackagePrivatem(new SomeParent()));
    assertEquals("SomeParent", aCaller.callPackagePrivatem(
        new SomeSubClassInAnotherPackage()));

    assertEquals("SomeSubClassInAnotherPackage",
        SomeSubClassInAnotherPackage.pleaseCallm(new SomeSubClassInAnotherPackage()));
    assertEquals("SomeSubSubClassInAnotherPackage",
        SomeSubClassInAnotherPackage.pleaseCallm(new SomeSubSubClassInAnotherPackage()));

    assertEquals("ClassExposingM",
        aCaller.callPackagePrivatem(new ClassExposingM()));

    SomeInterface i = new ClassExposingM();
    assertEquals("ClassExposingM", i.m());
    assertEquals("live at ClassExposingM", new ClassExposingM().f());

    // Confirm that both calling m through SomeInterface and through SomeParentParentParent
    // dispatch to the right implementation.
    SomeInterface i1 = new SubClassExposingM();
    assertEquals("SubClassExposingM", i1.m());

    assertEquals("SubClassExposingM",
        SomeParentParentParent.callSomeParentParentParentM(new SubClassExposingM()));

    assertEquals("SomeParentParentParent",
        SomeParentParentParent.callSomeParentParentParentM(new SomeParentParentParent()));
    assertEquals("SomeParentParentParent",
        SomeParentParentParent.callSomeParentParentParentM(new SomePackageConfusedParent()));
    assertEquals("SomeParentParent",
        SomeParentParentParent.callSomeParentParentParentM(new SomeParentParent()));
    assertEquals("SomeParent",
        SomeParentParentParent.callSomeParentParentParentM(new SomeParent()));
    assertEquals("SomeParent",
        SomeParentParentParent.callSomeParentParentParentM(new SomeSubClassInAnotherPackage()));
    assertEquals("SomeParent",
        SomeParentParentParent.callSomeParentParentParentM(new SomeSubSubClassInAnotherPackage()));
  }

  enum MyEnum {
    A,
    B,
    C;

    public final static MyEnum[] VALUES = values();

    public int getPriority() {
      return VALUES.length - ordinal();
    }
  }

  /**
   * Tests that enum ordinalizer does not incorrectly optimize {@code MyEnum}.
   * <p>
   * Test for issue 8846.
   */
  public void testMyEnum() {
    assertEquals(2, MyEnum.B.getPriority());
  }

  enum OrderingProblem {
    A,
    B;

    public static OrderingProblem getPriority1() {
      if (new Integer(1).toString().isEmpty()) {
        return B;
      }
      return A;
    }
  }

  /**
   * Test for regression introduced in patch https://gwt-review.googlesource.com/#/c/9083; where
   * depending on the order in which references to the enum class were encountered, some instances
   * were not correctly replaced .
   */
  public void testOrderingProblem() {
    assertEquals(OrderingProblem.A.ordinal(), OrderingProblem.getPriority1().ordinal());
  }

  /**
   * Tests that regexes are not incorrectly internalized.
   *
   * Test for issue 8865.
   */
  public native void testJavaScriptRegExps() /*-{
    // Make regexes large enough so that the will be interned (if regex interning was enabled).
    var regExp1 = /this is a string where the search/g;
    var regExp2 = /this is a string where the search/g;
    var str = "this is a string where the search occurs";
    @junit.framework.Assert::assertEquals(ZZ)(
       regExp1.test(str), regExp2.test(str));
  }-*/;

  private static final double MINUTES_IN_DAY = 24 * 60;

  @DoNotInline
  public void assertStaticEvaluationRegression(int hour, int minute) {
    // Do not inline this method so that the problematic expression reaches JsStaticEval.
    double expected = hour * 60 + minute;
    expected /= MINUTES_IN_DAY;
    expected *= 100;
    assertEquals(expected , (hour * 60 + minute) / MINUTES_IN_DAY * 100);
  }

  /**
   * Test for issue 8934.
   */
  public void testStaticEvaluationRegression() {
    // Perform two calls with different constant values to make sure the assertStaticEvaluation does
    // not get the constant parameters propagated and statically evaluated in the Java AST.
    assertStaticEvaluationRegression(10, 20);
    assertStaticEvaluationRegression(20, 10);
  }

  /**
   * Test for issue 8909.
   * <p>
   * DevMode does not conform to JS arithmetic semantics and this method tests exactly that.
   */
  @DoNotRunWith(Platform.Devel)
  public void testStaticEvaluationSematics() {
    float num = getRoundedValue(1.005f);
    assertEquals(1.00, num, 0.001);
  }

  private float getRoundedValue(float parameter) {
    float local = parameter;
    local = local * 100f;
    return Math.round(local) / 100f;
  }

  /**
   * Test for issue 9043.
   */
  public native void testMultipleClassLiteralReferences() /*-{
    var a = @com.google.gwt.dev.jjs.test.CompilerMiscRegressionTest::class;
    var b = @com.google.gwt.dev.jjs.test.CompilerMiscRegressionTest::class;
  }-*/;

  /**
   * Test for issue 9153.
   * <p>
   * Typetightener used to incorrectly tighten method calls marked with STATIC_DISPATCH_ONLY.
   */
  public void testIncorrectDispatch() {
    final int[] state = new int[1];

    @JsType
    abstract class A {
      public void m() {
        state[0] = 1;
      }
    }

    @JsType
    class B extends A {
      public void m() {
        super.m();
      }
    }

    new B().m();
    assertEquals(1, state[0]);
  }

  private static void assertEqualContents(float[] expected, float[] actual) {

    assertEquals("Array length mismatch", expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals("Array mismatch at element " + i , expected[i], actual[i]);
    }
  }
}
