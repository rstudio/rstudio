// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import org.apache.commons.collections.TestMap;

import java.util.HashMap;
import java.util.Map;

public class ApacheMapTest extends TestMap {

  public ApacheMapTest() {
   }



  public Object makeObject() {
    return new HashMap();
  }


   protected Map makeEmptyMap() {
    return new HashMap();
  }

}
