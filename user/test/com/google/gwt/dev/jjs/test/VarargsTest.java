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

import java.util.Arrays;

/**
 * Tests the new JDK 1.5 varargs functionality.
 */
public class VarargsTest extends GWTTestCase {

  private static class Foo { }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  @SuppressWarnings("all")
  public void testNullEmpty() {
    assertNotNull(vararg());
    assertNull(vararg(null));
    assertNotNull(vararg((String) null));
    assertNull(vararg((String[]) null));
  }

  public void testVararg() {
    String[] expected = new String[] {"1", "2", "3"};
    String[] actual = vararg("1", "2", "3");
    assertTrue(Arrays.equals(expected, actual));

    expected = new String[] {};
    actual = vararg();
    assertTrue(Arrays.equals(expected, actual));
  }

  public void testVarargBoxing() {
    int[] expected = new int[] {1, 2, 3};
    int[] actual = varargUnboxed(1, 2, 3);
    assertTrue(Arrays.equals(expected, actual));
    actual = varargUnboxed(new Integer(1), 2, new Integer(3));
    assertTrue(Arrays.equals(expected, actual));

    expected = new int[] {};
    actual = varargUnboxed();
    assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * Test for issue 8736.
   */
  public void testNullEmptyUninstantiable_Varargs() {
    assertNotNull(fooVararg());
  }

  public void testNullEmptyUninstantiable_NoVarargs() {
    assertNotNull(fooIdent(new Foo[]{}));
  }

  private String[] vararg(String... args) {
    return args;
  }

  private Foo[] fooVararg(Foo... args) {
    return args;
  }

  private Foo[] fooIdent(Foo[] args) {
    return args;
  }

  private int[] varargUnboxed(int... args) {
    return args;
  }
}
