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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.SerializationException;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

/**
 * Test the {@link SerializationPolicyLoader} class.
 */
public class SerializationPolicyLoaderTest extends TestCase {
  // allowed by the policy
  static class A {
  }

  // not allowed by the policy
  static class B {
  }

  // missing the instantiable attribute
  private static String POLICY_FILE_MISSING_FIELD = A.class.getName();

  private static String POLICY_FILE_TRIGGERS_CLASSNOTFOUND = "C,false";

  private static String VALID_POLICY_FILE_CONTENTS = A.class.getName()
      + ", true";

  public static InputStream getInputStreamFromString(String content)
      throws UnsupportedEncodingException {
    return new ByteArrayInputStream(
        content.getBytes(SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING));
  }

  public void testPolicyFileMissingField() throws IOException,
      ClassNotFoundException {
    InputStream is = getInputStreamFromString(POLICY_FILE_MISSING_FIELD);
    try {
      SerializationPolicyLoader.loadFromStream(is);
      fail("Expected ParseException");
    } catch (ParseException e) {
      // expected to get here
    }
  }

  public void testPolicyFileTriggersClassNotFound() throws IOException,
      ParseException {
    InputStream is = getInputStreamFromString(POLICY_FILE_TRIGGERS_CLASSNOTFOUND);
    try {
      SerializationPolicyLoader.loadFromStream(is);
      fail("Expected ClassNotFoundException");
    } catch (ClassNotFoundException e) {
      // expected to get here
    }
  }

  /**
   * Test that a valid policy file will allow the types in the policy to be used
   * and reject those that are not.
   * 
   * @throws ClassNotFoundException
   * @throws ParseException
   */
  public void testValidSerializationPolicy() throws IOException,
      SerializationException, ParseException, ClassNotFoundException {

    InputStream is = getInputStreamFromString(VALID_POLICY_FILE_CONTENTS);
    SerializationPolicy sp = SerializationPolicyLoader.loadFromStream(is);
    assertTrue(sp.shouldDeserializeFields(A.class));
    assertTrue(sp.shouldSerializeFields(A.class));

    assertFalse(sp.shouldDeserializeFields(B.class));
    assertFalse(sp.shouldSerializeFields(B.class));

    sp.validateDeserialize(A.class);
    sp.validateSerialize(A.class);

    try {
      sp.validateDeserialize(B.class);
      fail("Expected SerializationException");
    } catch (SerializationException ex) {
      // should get here
    }

    try {
      sp.validateSerialize(B.class);
      fail("Expected SerializationException");
    } catch (SerializationException ex) {
      // should get here
    }
  }
}
