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

import com.google.gwt.core.client.impl.SpecializeMethod;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for @SpecializeMethod and
 * {@link com.google.gwt.dev.jjs.impl.MethodCallSpecializer}.
 */
@DoNotRunWith(Platform.Devel)
public class SpecializationTest extends GWTTestCase {

  public static final String KEY = "key";
  public static final String VALUE = "value";

  static class TestImpl<K,V> {
    @SpecializeMethod(params = {String.class, Object.class},
      target = "putString")
    public void put(K k, V v) {
      if (k instanceof String) {
        putString((String) k, v); // keeps putString from being pruned
      }
      fail("Shouldn't be called due to specialization");
    }

    public void putString(String k, V v) {
      assertEquals(KEY, k);
      assertEquals(VALUE, v);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testSpecialization() {
    TestImpl<String, Object> succ = new TestImpl<String, Object>();
    succ.put("key", VALUE);
  }

  public void testSpecializationFallback() {
    TestImpl<Integer, Object> succ = new TestImpl<Integer,
            Object>();
    try {
      succ.put(42, VALUE);
      fail();
    } catch (Throwable t) {
      // swallow failure, we expect putString is called
    }
  }
}
