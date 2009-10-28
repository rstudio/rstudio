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
package com.google.gwt.dev.util.collect;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.collections.map.TestIdentityMap;

import java.util.Map;

/**
 * Test for {@link IdentityHashMap}.
 */
public class IdentityHashMapTest {

  public static class HashMapExtTest extends HashMapTest {
    public HashMapExtTest(String testName) {
      super(testName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map makeConfirmedMap() {
      return new java.util.IdentityHashMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map makeEmptyMap() {
      return new IdentityHashMap();
    }

    @Override
    protected boolean skipSerializedCanonicalTests() {
      return true;
    }
  }

  public static class IdentityMapExtTest extends TestIdentityMap {
    public static Test suite() {
      return new TestSuite(IdentityMapExtTest.class);
    }

    public IdentityMapExtTest(String testName) {
      super(testName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object makeObject() {
      return new IdentityHashMap();
    }

    @Override
    protected boolean skipSerializedCanonicalTests() {
      return true;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(IdentityHashMapTest.class.getName());
    suite.addTestSuite(IdentityMapExtTest.class);
    suite.addTestSuite(HashMapExtTest.class);
    return suite;
  }
}
