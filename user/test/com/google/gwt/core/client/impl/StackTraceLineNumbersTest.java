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

package com.google.gwt.core.client.impl;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that stack traces work properly with line numbers turned on.
 */
public class StackTraceLineNumbersTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.core.StackTraceLineNumbersTest";
  }

  /**
   * Tests that the rewrites do not change a reference to a comma
   * expression, in contexts where a reference is needed and
   * not just a value.  See issue 4512.
   */
  public native void testRefBreakups() /*-{
    var assertTrue = @junit.framework.Assert::assertTrue(Ljava/lang/String;Z);

    // Check breakups in an invocation context
    var bar = {
      foo: function() {
        return this === bar;
      }
    }

    assertTrue("bar['foo']", bar['foo']());
    assertTrue("bar.foo", bar.foo());

    // Check breakups in for-in statements
    var c = null;
    for (a in [0, 1, 2]) {
      c = a;
    }
    assertTrue("for-in", c==2);

    // typeOf works on bad references
    assertTrue("typeOf", (typeof someNameThatDoesNotExist301402172) == 'undefined');

    // delete needs a reference, not a value
    delete bar.foo;
  }-*/;
}
