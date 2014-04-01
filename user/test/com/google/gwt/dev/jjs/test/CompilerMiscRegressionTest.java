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
import com.google.gwt.dom.client.Document;
import com.google.gwt.junit.client.GWTTestCase;

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

  @SuppressWarnings("null")
  public void testJsoShouldNullPointerExceptionInDevMode() {
    com.google.gwt.dom.client.Element foo = null;
    try {
      foo.setPropertyString("x", "y");
      fail("Should have thrown Exception");
    } catch (Exception npe) {
      // is not yet a NullPointerException in prod mode
    }
    // once foo is not null, then it works
    foo = Document.get().createDivElement();
    foo.setPropertyString("x", "y");
    assertEquals("y", foo.getPropertyString("x"));
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

  private static void assertEqualContents(float[] expected, float[] actual) {

    assertEquals("Array length mismatch", expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals("Array mismatch at element " + i , expected[i], actual[i]);
    }
  }
}
