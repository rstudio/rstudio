/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for the WeakMapping class.
 */
public class WeakMappingTest extends GWTTestCase {

  public WeakMappingTest() {
  }
  
  public void testSetAndGet() {
    Set<Integer> stronglyReferencedObjects = new HashSet<Integer>();
    for (int i = 0; i < 1000; i++) {
      Integer instance = new Integer(i);
      if ((i % 5) == 0) {
        stronglyReferencedObjects.add(instance);
      }
      WeakMapping.set(instance, "key", new Float(i));
    }
    
    System.gc();
    
    for (Integer instance : stronglyReferencedObjects) {
      Object value = WeakMapping.get(instance, "key");
      assertNotNull(value);
      assertTrue(value instanceof Float);
      assert(((Float) value).floatValue() == instance.intValue());
    }
  }
  
  public void testNoStringsAllowed() {
    boolean gotException = false;
    try {
      WeakMapping.set("A String", "key", "value");
    } catch (IllegalArgumentException e) {
      gotException = true;
    }
    
    assertTrue(gotException);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }
}
