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
package com.google.gwt.emultest.java.util;

import org.apache.commons.collections.TestSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: document me.
 */
public class HashSetTest extends TestSet {

  public HashSetTest() {
    super("Dummy");
  }

  public void testAddingKeys() {
    Map map = new HashMap();
    Set keys = new HashSet(map.keySet());
    keys.add(new Object()); // Throws exception in IE6 (web-mode) but not GWT
  }

  public void testAddWatch() {
    HashSet s = new HashSet();
    s.add("watch");
    assertTrue(s.contains("watch"));
  }

  protected Set makeEmptySet() {
    return new HashSet();
  }

  public Object makeObject() {
    return new HashSet();
  }

}
