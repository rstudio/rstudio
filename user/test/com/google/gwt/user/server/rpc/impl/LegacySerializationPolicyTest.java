/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import junit.framework.TestCase;

import java.io.Serializable;

/**
 * Tests {@link LegacySerializationPolicy}.
 */
public class LegacySerializationPolicyTest extends TestCase {

  /**
   * This class should be serializable by a LegacySerializationPolicy.
   */
  private static class LegacySerializable implements IsSerializable {
  }

  /**
   * This class should not be serializable by a LegacySerializationPolicy.
   */
  private static class NotLegacySerializable implements Serializable {
  }

  /**
   * This class should be serializable by a LegacySerializationPolicy.
   */
  private static class NotSerializable {
  }

  public void testSerializability() throws SerializationException {

    SerializationPolicy serializationPolicy = LegacySerializationPolicy.getInstance();

    assertDeserializeFields(serializationPolicy, LegacySerializable.class);
    assertValidDeserialize(serializationPolicy, LegacySerializable.class);

    assertDeserializeFields(serializationPolicy, NotLegacySerializable.class);
    assertNotValidDeserialize(serializationPolicy, NotLegacySerializable.class);

    assertNotDeserializeFields(serializationPolicy, NotSerializable.class);
    assertNotValidDeserialize(serializationPolicy, NotSerializable.class);

    /*
     * Ensure that the LegacySerializationPolicy can fully serialize
     * IncompatibleRemoteServiceExceptions that can be sent back to a client.
     */
    assertValidSerialize(serializationPolicy,
        IncompatibleRemoteServiceException.class);
    assertSerializeFields(serializationPolicy,
        IncompatibleRemoteServiceException.class);

    /*
     * Make sure that the supretypes of IncompatibleRemoteServiceException,
     * excluding Object, should not be serialized as leaf types but their fields
     * should be serializable.
     */
    for (Class<?> clazz = IncompatibleRemoteServiceException.class.getSuperclass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      assertNotValidSerialize(serializationPolicy, clazz);
      assertSerializeFields(serializationPolicy, clazz);
    }
  }

  private void assertDeserializeFields(SerializationPolicy policy,
      Class<?> clazz) {
    assertTrue(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotDeserializeFields(SerializationPolicy policy,
      Class<?> clazz) {
    assertFalse(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotValidDeserialize(SerializationPolicy policy,
      Class<?> clazz) {
    try {
      policy.validateDeserialize(clazz);
      fail("assertNotValidDeserialize: " + clazz.getName()
          + " failed to throw an exception");
    } catch (SerializationException e) {
      // expected
    }
  }

  private void assertNotValidSerialize(SerializationPolicy policy,
      Class<?> clazz) {
    try {
      policy.validateSerialize(clazz);
      fail("assertNotValidSerialize: " + clazz.getName()
          + " failed to throw an exception");
    } catch (SerializationException e) {
      // expected
    }
  }

  private void assertSerializeFields(SerializationPolicy policy, Class<?> clazz) {
    assertTrue(policy.shouldSerializeFields(clazz));
  }

  private void assertValidDeserialize(SerializationPolicy policy, Class<?> clazz)
      throws SerializationException {
    policy.validateDeserialize(clazz);
  }

  private void assertValidSerialize(SerializationPolicy policy, Class<?> clazz)
      throws SerializationException {
    policy.validateSerialize(clazz);
  }
}
