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
package com.google.gwt.core.shared;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the {@link SerializableThrowable} class.
 *
 * @see com.google.gwt.user.client.rpc.ExceptionsTest for serialization tests.
 */
public class SerializableThrowableTest extends GWTTestCase {

  private static final String TEST_CLASS_NAME =
      "com.google.gwt.core.shared.SerializableThrowableTest";

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testToString() throws Exception {
    SerializableThrowable t = SerializableThrowable.fromThrowable(new RuntimeException("msg"));
    t.setDesignatedType("a.A", true);
    assertEquals("a.A: msg", t.toString());
    t.setDesignatedType("a.A", false);
    assertEquals("a.A(EXACT TYPE UNKNOWN): msg", t.toString());
  }

  public void testFromThrowable() throws Exception {
    RuntimeException exception = new RuntimeException("msg");
    exception.initCause(new RuntimeException("cause"));

    SerializableThrowable serializableThrowable = SerializableThrowable.fromThrowable(exception);
    assertEquals("msg", serializableThrowable.getMessage());
    assertEquals(exception.getStackTrace().length, serializableThrowable.getStackTrace().length);
    assertEquals("java.lang.RuntimeException: msg", serializableThrowable.toString());

    SerializableThrowable cause = (SerializableThrowable) serializableThrowable.getCause();
    assertEquals("cause", cause.getMessage());
    assertEquals("java.lang.RuntimeException: cause", cause.toString());
  }

  public void testFromThrowable_alreadySerializable() {
    SerializableThrowable expected = new SerializableThrowable(null, "msg");
    assertSame(expected, SerializableThrowable.fromThrowable(expected));
  }

  public void testFromThrowable_null() {
    assertNull(SerializableThrowable.fromThrowable(null));
  }

  private static class MyException extends Exception { }
  private static class MyRuntimeException extends RuntimeException { }
  private static class MyNullPointerException extends NullPointerException { }

  public void testDesignatedType() throws Exception {
    SerializableThrowable t = SerializableThrowable.fromThrowable(new RuntimeException());
    assertEquals("java.lang.RuntimeException", t.getDesignatedType());
    assertTrue(t.isExactDesignatedTypeKnown());
  }

  public void testDesignatedType_withClassMetadata() throws Exception {
    if (!isClassMetadataAvailable()) {
      return;
    }
    SerializableThrowable t;

    t = SerializableThrowable.fromThrowable(new MyException());
    assertEquals(TEST_CLASS_NAME + "$MyException", t.getDesignatedType());
    assertTrue(t.isExactDesignatedTypeKnown());

    t = SerializableThrowable.fromThrowable(new MyRuntimeException());
    assertEquals(TEST_CLASS_NAME + "$MyRuntimeException", t.getDesignatedType());
    assertTrue(t.isExactDesignatedTypeKnown());

    t = SerializableThrowable.fromThrowable(new MyNullPointerException());
    assertEquals(TEST_CLASS_NAME + "$MyNullPointerException", t.getDesignatedType());
    assertTrue(t.isExactDesignatedTypeKnown());
  }

  public void testDesignatedType_withoutClassMetadata() throws Exception {
    if (isClassMetadataAvailable()) {
      return;
    }
    SerializableThrowable t;

    t = SerializableThrowable.fromThrowable(new MyException());
    assertEquals("java.lang.Exception", t.getDesignatedType());
    assertFalse(t.isExactDesignatedTypeKnown());

    t = SerializableThrowable.fromThrowable(new MyRuntimeException());
    assertEquals("java.lang.RuntimeException", t.getDesignatedType());
    assertFalse(t.isExactDesignatedTypeKnown());

    t = SerializableThrowable.fromThrowable(new MyNullPointerException());
    assertEquals("java.lang.NullPointerException", t.getDesignatedType());
    assertFalse(t.isExactDesignatedTypeKnown());
  }

  private static boolean isClassMetadataAvailable() {
    return SerializableThrowableTest.class.getName().endsWith(".SerializableThrowableTest");
  }
}
