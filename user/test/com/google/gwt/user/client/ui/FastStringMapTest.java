// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Map;

/**
 * Tests <code>FastStringMap</code>Right now, no tests are directly run here,
 * because the tests are run in mapTest.FastStringMapTest. This is because
 * otherwise the inclusion of the map testing code causes the system to generate
 * many compiler errors during unit testing, thereby making real errors harder
 * to spot.
 */
public class FastStringMapTest extends GWTTestCase {

  /**
   * These is an example of two correctly formatted java API specification
   */

  public static Map makeEmptyMap() {
    return new FastStringMap();
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void test() {
    // Only FastStringMap specific tests should go here. Look in
    // com.google.gwt.user.maptests.FastStringMapTest for all apache Map tests.
  }

}
