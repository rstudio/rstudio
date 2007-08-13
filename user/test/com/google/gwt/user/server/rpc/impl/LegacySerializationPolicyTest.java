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

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import junit.framework.TestCase;

import java.io.Serializable;

/**
 * Tests {@link LegacySerializationPolicy}.
 */
public class LegacySerializationPolicyTest extends TestCase {

  private static class Bar implements Serializable {
  }

  private static class Baz {
  }

  private static class Foo implements IsSerializable {
  }

  public void testSerializability() throws SerializationException {

    SerializationPolicy serializationPolicy = LegacySerializationPolicy.getInstance();

    assertDeserializeFields(serializationPolicy, Foo.class);
    assertValidDeserialize(serializationPolicy, Foo.class);

    assertDeserializeFields(serializationPolicy, Bar.class);
    assertNotValidDeserialize(serializationPolicy, Bar.class);

    assertNotDeserializeFields(serializationPolicy, Baz.class);
    assertNotValidDeserialize(serializationPolicy, Baz.class);
  }

  private void assertDeserializeFields(SerializationPolicy policy, Class clazz) {
    assertTrue(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotDeserializeFields(SerializationPolicy policy,
      Class clazz) {
    assertFalse(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotValidDeserialize(SerializationPolicy policy, Class clazz) {
    try {
      policy.validateDeserialize(clazz);
      fail("assertNotValidDeserialize: " + clazz.getName()
          + " failed to throw an exception");
    } catch (SerializationException e) {
      // expected
    }
  }

  private void assertValidDeserialize(SerializationPolicy policy, Class clazz)
      throws SerializationException {
    policy.validateDeserialize(clazz);
  }
}
