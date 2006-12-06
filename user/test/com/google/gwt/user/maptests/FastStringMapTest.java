// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.maptests;

import org.apache.commons.collections.TestMap;

import java.util.Map;

/**
 * Test class for <code>FastStringMap</code>
 */
public class FastStringMapTest extends TestMap {


  public String getModuleName() {
    return "com.google.gwt.user.FastStringMapTest";
  }
  protected Map makeEmptyMap() {
    return com.google.gwt.user.client.ui.FastStringMapTest.makeEmptyMap();
  }

  /**
   * Override if your map does not allow a <code>null</code> key. The default
   * implementation returns <code>true</code>
   */
  protected boolean useNullKey() {
    return false;
  }

  /**
   * Override if your map does not allow <code>null</code> values. The default
   * implementation returns <code>true</code>.
   */
  protected boolean useNullValue() {
    return true;
  }

}
