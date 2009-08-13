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
package com.google.gwt.user.server.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the {@link StandardSerializationPolicy} class.
 */
public class StandardSerializationPolicyTest extends TestCase {

  // This type will be included
  class A {
    // purposely empty
  }

  // This type will not be included
  class B {
    // purposely empty
  }

  // This type is serializable but not instantiable
  class C {
    // purposely empty
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy#shouldDeserializeFields(java.lang.Class)}.
   */
  public void testShouldDerializeFields() {
    StandardSerializationPolicy ssp = getStandardSerializationPolicy();
    assertTrue(ssp.shouldDeserializeFields(A.class));
    assertFalse(ssp.shouldDeserializeFields(B.class));
    assertTrue(ssp.shouldDeserializeFields(C.class));
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy#shouldSerializeFields(java.lang.Class)}.
   */
  public void testShouldSerializeFields() {
    StandardSerializationPolicy ssp = getStandardSerializationPolicy();
    assertTrue(ssp.shouldSerializeFields(A.class));
    assertFalse(ssp.shouldSerializeFields(B.class));
    assertTrue(ssp.shouldSerializeFields(C.class));
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy#validateDeserialize(java.lang.Class)}.
   * 
   * @throws SerializationException
   */
  public void testValidateDeserialize() throws SerializationException {
    StandardSerializationPolicy ssp = getStandardSerializationPolicy();

    ssp.validateDeserialize(A.class);

    try {
      ssp.validateDeserialize(B.class);
      fail("Expected SerializationException");
    } catch (SerializationException e) {
      // should get here
    }

    try {
      ssp.validateDeserialize(C.class);
      fail("Expected SerializationException");
    } catch (SerializationException e) {
      // should get here
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy#validateSerialize(java.lang.Class)}.
   * 
   * @throws SerializationException
   */
  public void testValidateSerialize() throws SerializationException {
    StandardSerializationPolicy ssp = getStandardSerializationPolicy();

    ssp.validateSerialize(A.class);

    try {
      ssp.validateSerialize(B.class);
      fail("Expected SerializationException");
    } catch (SerializationException e) {
      // should get here
    }

    try {
      ssp.validateSerialize(C.class);
      fail("Expected SerializationException");
    } catch (SerializationException e) {
      // should get here
    }
  }

  StandardSerializationPolicy getStandardSerializationPolicy() {
    Map map = new HashMap();
    map.put(A.class, Boolean.TRUE);
    map.put(C.class, Boolean.FALSE);
    
    Map typeIds = new HashMap();
    typeIds.put(A.class, "A");
    typeIds.put(B.class, "B");

    return new StandardSerializationPolicy(map, map, typeIds, new HashMap());
  }
}
