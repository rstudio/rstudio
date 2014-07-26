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
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * A smoke test that is isolated from the rest of tests by using different module. This provides
 * less type to exist in the compilation which can reveal some type related bugs in the
 * implementation (e.g. issues that might rise due to treating a javascript array as java array).
 */
public class HashMapSmokeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuiteIsolated";
  }

  public void testSmoke() {
    Map<Object, Object> map = new HashMap<Object, Object>();
    Object object = new Object();
    Object key = new Object();
    map.put(key, object);
    assertEquals(object, map.get(key));
  }
}
