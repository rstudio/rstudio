/*
 * Copyright 2013 Google Inc.
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tests <code>LinkedHashSet</code>.
 */
public class LinkedHashSetTest extends TestSet {

  private static final String VALUE_1 = "val1";
  private static final String VALUE_2 = "val2";
  private static final String VALUE_3 = "val3";
  private static final String VALUE_4 = "val4";

  /**
   * Check the state of a newly constructed, empty LinkedHashSet.
   * 
   * @param hashSet
   */
  private static void checkEmptyLinkedHashSetAssumptions(LinkedHashSet<?> hashSet) {
    assertNotNull(hashSet);
    assertTrue(hashSet.isEmpty());
  }

  public LinkedHashSetTest() {
    super("LinkedHashSet");
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /*
   * Test method for 'java.util.LinkedHashSet.clone()'
   */
  @SuppressWarnings("unchecked")
  public void testClone() {
    LinkedHashSet<String> srcSet = new LinkedHashSet<String>();
    checkEmptyLinkedHashSetAssumptions(srcSet);

    // Check empty clone behavior
    LinkedHashSet<String> dstSet = (LinkedHashSet<String>) srcSet.clone();
    assertNotNull(dstSet);
    assertEquals(dstSet.size(), srcSet.size());
    assertEquals(dstSet.toArray(), srcSet.toArray());

    // Check non-empty clone behavior
    srcSet.add(VALUE_1);
    srcSet.add(VALUE_2);
    srcSet.add(VALUE_3);
    srcSet.add(VALUE_4);
    dstSet = (LinkedHashSet<String>) srcSet.clone();
    assertNotNull(dstSet);
    assertEquals(dstSet.size(), srcSet.size());
    assertEquals(dstSet.toArray(), srcSet.toArray());
  }

  @Override
  protected Set<?> makeEmptySet() {
    return new LinkedHashSet<Object>();
  }
}
