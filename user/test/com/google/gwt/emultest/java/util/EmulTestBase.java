// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.List;

public class EmulTestBase extends GWTTestCase {

  public static void assertEquals(Object[] x, Object[] y) {
    assertEquals(x.length, y.length);
    for (int i = 0; i < y.length; i++) {
      assertEquals(x[i], y[i]);
    }
  }

  /** Easy way to test what should be in a list */
  protected static void assertEquals(Object[] array, List target) {
    assertEquals(array.length, target.size());
    for (int i = 0; i < array.length; i++) {
      assertEquals(target.get(i), array[i]);
    }
  }

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }
}
