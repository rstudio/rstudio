/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.maptests;

import org.apache.commons.collections.TestMap;

import java.util.Map;

/**
 * Test class for <code>FastStringMap</code>.
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
