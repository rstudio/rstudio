// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import org.apache.commons.collections.TestSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HashSetTest extends TestSet {

  public HashSetTest() {
    super("Dummy");
  }
  public void testAddingKeys(){
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
