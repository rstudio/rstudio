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
package com.google.gwt.dev.jjs.optimized;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import javaemul.internal.annotations.SpecializeMethod;

/**
 * Tests for {@link SpecializeMethod}.
 */
@DoNotRunWith(Platform.Devel)
public class SpecializationTest extends GWTTestCase {

  private static final String KEY = "key";
  private static final String VALUE = "value";
  private static final String MSG = "Shouldn't be called due to specialization";

  static class TestImplChild<K, V> extends TestImpl<K, V> {
    @Override
    public void put(K k, V v) {
      if (Math.random() > 0) {
        return;
      }
    }
  }

  static class TestImpl<K, V> {
    @SpecializeMethod(params = {String.class, Object.class}, target = "putString")
    public void put(K k, V v) {
      if (k instanceof String) {
        putString((String) k, v); // keeps putString from being pruned
      }
      throw new RuntimeException(MSG);
    }

    private void putString(String k, V v) {
      assertEquals(KEY, k);
      assertEquals(VALUE, v);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuiteOptimized";
  }

  public void testSpecialization() {
    // Existence of a live subclass with an overriding method makes sure that the specializaion is
    // applied in realistic scenarios.
    TestImplChild<String, Object> dummy = new TestImplChild<>();
    dummy.put(KEY, VALUE);

    TestImpl<String, Object> succ = new TestImpl<>();
    try {
      succ.put("key", VALUE);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public void testSpecializationFallback() {
    TestImpl<Integer, Object> succ = new TestImpl<>();
    try {
      succ.put(42, VALUE);
      fail();
    } catch (Exception t) {
      // swallow failure, we expect put() is called normally
      assertEquals(MSG, t.getMessage());
    }
  }
}
