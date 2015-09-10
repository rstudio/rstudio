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

import javaemul.internal.annotations.DoNotInline;
import javaemul.internal.annotations.HasNoSideEffects;

/**
 * Tests for {@link HasNoSideEffects}.
 */
@DoNotRunWith(Platform.Devel)
public class HasNoSideEffectsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  @DoNotInline
  private static void execute() {
    throw new RuntimeException("I lied, I have a side effect but compiler should have trusted me!");
  }

  @HasNoSideEffects
  private static boolean sideEffectFree() {
    // @HasNoSideEffects should propagate to next call otherwise the exception will be thrown.
    execute();
    return true;
  }

  // Carefully crafted to test that we propagate 'side effect' data while inlining.
  public void testMethodRemoval() {
    assertTrue(sideEffectFree());
  }
}
