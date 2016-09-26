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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.UncheckedCast;

/**
 * Tests for {@link UncheckedCast}.
 */
@DoNotRunWith(Platform.Devel)
public class UncheckedCastTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  @UncheckedCast
  private static <T> T uncheckedCast(Object object) {
    return (T) object;
  }

  public void testUnsafeCast() {
    Integer boxedInt = uncheckedCast(new Object());
    assertNotNull(boxedInt);
    Double d = uncheckedCast(new Object());
    assertNotNull(d);
    boolean unboxedBoolean = uncheckedCast(new Double(12));
    assertTrue(unboxedBoolean);
  }
}
