/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the $entry() method in JSNI.
 */
public class EntryTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  /**
   * Tests that methods with primitive return types correctly return JS values
   * when wrapped with {@code $entry} (rather than JS objects).
   * <p>
   * We test this with a boolean {@code false} that we coerce to a boolean. If the
   * $entry-wrapped function returns it as a JS Boolean object, it'll coerce to
   * {@code true} because it's non-null.
   *
   * @see <a href="https://code.google.com/p/google-web-toolkit/issues/detail?id=8548">issue 8548</a>
   */
  public native void testPrimitiveReturnType() /*-{
    var assertStringEquals = @junit.framework.Assert::assertEquals(Ljava/lang/String;Ljava/lang/String;);
    var assertFalse = @junit.framework.Assert::assertFalse(Z);
    var assertTrue = @junit.framework.Assert::assertTrue(Z);

    var assertIsBooleanValueFalse = function(shouldBeBooleanValueFalse) {
      assertStringEquals("boolean", typeof shouldBeBooleanValueFalse);
      assertFalse(!!shouldBeBooleanValueFalse);
    };
    var assertIsBooleanObjectFalse = function(shouldBeBooleanObjectFalse) {
      assertStringEquals("object", typeof shouldBeBooleanObjectFalse);
      assertTrue(shouldBeBooleanObjectFalse instanceof Boolean);
      assertFalse(shouldBeBooleanObjectFalse.valueOf());
      // that was the failing code in issue 8548, so test it explicitly:
      assertTrue(!!shouldBeBooleanObjectFalse);
    }

    // Make sure we don't erroneously wrap values
    var returnsBooleanValueFalse = $entry(function() { return false; });
    assertIsBooleanValueFalse(returnsBooleanValueFalse());
    // try if with a Java method returning a Java primitive boolean (issue 8548)
    var returnsJavaPrimitiveBooleanFalse = $entry(@com.google.gwt.core.client.EntryTest::returnsFalse());
    assertIsBooleanValueFalse(returnsJavaPrimitiveBooleanFalse());

    // Make sure we don't erroneously unwrap objects
    var returnsBooleanObjectFalse = $entry(function() { return new Boolean(false); });
    assertIsBooleanObjectFalse(returnsBooleanObjectFalse());

    // Just to be sure, make sure we round-trip values correctly:
    var returnsFirstArgument = $entry(function(a) { return a; });
    assertIsBooleanValueFalse(returnsFirstArgument(false));
    assertIsBooleanObjectFalse(returnsFirstArgument(new Boolean(false)));
  }-*/;

  private static boolean returnsFalse() {
    return false;
  }
}
