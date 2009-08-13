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
package com.google.gwt.user.server.rpc;

import static com.google.gwt.user.client.rpc.impl.AbstractSerializationStream.RPC_SEPARATOR_CHAR;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
import com.google.gwt.user.server.rpc.impl.TypeNameObfuscator;

import junit.framework.TestCase;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Tests for the {@link com.google.gwt.user.server.rpc.RPC RPC} class.
 */
@SuppressWarnings("deprecation")
public class RPCTest extends TestCase {

  /**
   * Test serialization class.
   * 
   * @see RPCTest#testElision()
   */
  public static class C implements Serializable {
    int i = 0;
  }

  /**
   * Test serialization class.
   * 
   * @see RPCTest#testElision()
   */
  private static interface CC {
    C c();
  }

  private static interface A extends RemoteService {
    void method1() throws SerializableException;

    int method2();

    int method3(int val);
  }

  private static interface B {
    void method1();
  }

  private static final String VALID_ENCODED_REQUEST = ""
      + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION
      + RPC_SEPARATOR_CHAR + // version
      "0" + RPC_SEPARATOR_CHAR + // flags
      "4" + RPC_SEPARATOR_CHAR + // string table entry count
      A.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #1
      "method2" + RPC_SEPARATOR_CHAR + // string table entry #2
      "moduleBaseURL" + RPC_SEPARATOR_CHAR + // string table entry #3
      "whitelistHashcode" + RPC_SEPARATOR_CHAR + // string table entry #4
      "3" + RPC_SEPARATOR_CHAR + // module base URL
      "4" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "1" + RPC_SEPARATOR_CHAR + // interface name
      "2" + RPC_SEPARATOR_CHAR + // method name
      "0" + RPC_SEPARATOR_CHAR; // param count

  private static final String INVALID_METHOD_REQUEST = ""
      + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION
      + RPC_SEPARATOR_CHAR + // version
      "0" + RPC_SEPARATOR_CHAR + // flags
      "4" + RPC_SEPARATOR_CHAR + // string table entry count
      A.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #1
      "method3" + RPC_SEPARATOR_CHAR + // string table entry #2
      "moduleBaseURL" + RPC_SEPARATOR_CHAR + // string table entry #3
      "whitelistHashcode" + RPC_SEPARATOR_CHAR + // string table entry #4
      "3" + RPC_SEPARATOR_CHAR + // module base URL
      "4" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "1" + RPC_SEPARATOR_CHAR + // interface name
      "2" + RPC_SEPARATOR_CHAR + // method name
      "0" + RPC_SEPARATOR_CHAR; // param count

  private static final String INVALID_INTERFACE_REQUEST = ""
      + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION
      + RPC_SEPARATOR_CHAR + // version
      "0" + RPC_SEPARATOR_CHAR + // flags
      "4" + RPC_SEPARATOR_CHAR + // string table entry count
      B.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #1
      "method1" + RPC_SEPARATOR_CHAR + // string table entry #2
      "moduleBaseURL" + RPC_SEPARATOR_CHAR + // string table entry #3
      "whitelistHashcode" + RPC_SEPARATOR_CHAR + // string table entry #4
      "3" + RPC_SEPARATOR_CHAR + // module base URL
      "4" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "1" + RPC_SEPARATOR_CHAR + // interface name
      "2" + RPC_SEPARATOR_CHAR + // method name
      "0" + RPC_SEPARATOR_CHAR; // param count

  private static final String STRING_QUOTE_REQUEST = ""
      + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION
      + RPC_SEPARATOR_CHAR + // version
      "0" + RPC_SEPARATOR_CHAR + // flags
      "7" + RPC_SEPARATOR_CHAR + // string table entry count
      A.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #1
      "method2" + RPC_SEPARATOR_CHAR + // string table entry #2
      "moduleBaseURL" + RPC_SEPARATOR_CHAR + // string table entry #3
      "whitelistHashcode" + RPC_SEPARATOR_CHAR + // string table entry #4
      "Raw backslash \\\\" + RPC_SEPARATOR_CHAR + // string table entry #5
      "Quoted separator \\!" + RPC_SEPARATOR_CHAR + // string table entry #6
      "\\uffff\\\\!\\\\0\\0" + RPC_SEPARATOR_CHAR + // string table entry #7
      "3" + RPC_SEPARATOR_CHAR + // module base URL
      "4" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "5" + RPC_SEPARATOR_CHAR + // begin test data
      "6" + RPC_SEPARATOR_CHAR + "7" + RPC_SEPARATOR_CHAR;

  private static final String VALID_V2_ENCODED_REQUEST = "2\uffff" + // version
      "0\uffff" + // flags
      "2\uffff" + // string table entry count
      A.class.getName() + "\uffff" + // string table entry #1
      "method2\uffff" + // string table entry #2
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  private static final String VALID_V3_ENCODED_REQUEST = "3\uffff" + // version
      "0\uffff" + // flags
      "4\uffff" + // string table entry count
      A.class.getName() + "\uffff" + // string table entry #1
      "method2\uffff" + // string table entry #2
      "moduleBaseURL\uffff" + // string table entry #3
      "whitelistHashcode\uffff" + // string table entry #4
      "3\uffff" + // module base URL
      "4\uffff" + // whitelist hashcode
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  private static final String VALID_V4_ENCODED_REQUEST = "4\uffff" + // version
      "0\uffff" + // flags
      "4\uffff" + // string table entry count
      A.class.getName() + "\uffff" + // string table entry #1
      "method2" + "\uffff" + // string table entry #2
      "moduleBaseURL" + "\uffff" + // string table entry #3
      "whitelistHashcode" + "\uffff" + // string table entry #4
      "3\uffff" + // module base URL
      "4\uffff" + // whitelist hashcode
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  /**
   * Tests that seeing obsolete RPC formats throws an
   * {@link IncompatibleRemoteServiceException}.
   */
  public void testDecodeObsoleteFormats() {
    try {
      RPC.decodeRequest(VALID_V2_ENCODED_REQUEST, A.class, null);
      fail("Should have thrown an IncompatibleRemoteServiceException");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected
    }

    try {
      RPC.decodeRequest(VALID_V3_ENCODED_REQUEST, A.class, null);
      fail("Should have thrown an IncompatibleRemoteServiceException");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected
    }

    try {
      RPC.decodeRequest(VALID_V4_ENCODED_REQUEST, A.class, null);
      fail("Should have thrown an IncompatibleRemoteServiceException");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected
    }
  }

  /**
   * Tests for method {@link RPC#decodeRequest(String)}.
   * 
   * <p/>
   * Cases:
   * <ol>
   * <li>String == null</li>
   * <li>String == ""</li>
   * <li>Valid request
   * </ol>
   */
  public void testDecodeRequestString() {
    // Case 1
    try {
      RPC.decodeRequest(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected to get here
    }

    // Case 2
    try {
      RPC.decodeRequest("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected to get here
    }

    // Case 3
    RPC.decodeRequest(VALID_ENCODED_REQUEST);
  }

  /**
   * Tests for method {@link RPC#decodeRequest(String, Class)}.
   * 
   * <p/>
   * Cases:
   * <ol>
   * <li>String == null</li>
   * <li>String == ""</li>
   * <li>Class is null</li>
   * <li>Class implements RemoteService subinterface</li>
   * <li>Class implements the requested interface but it is not a subtype of
   * RemoteService</li>
   * <li>Class implements RemoteService derived interface but the method does
   * not exist
   * </ol>
   * 
   * @throws NoSuchMethodException
   * @throws SecurityException
   */
  public void testDecodeRequestStringClass() throws SecurityException,
      NoSuchMethodException {
    // Case 1
    try {
      RPC.decodeRequest(null, null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected to get here
    }

    // Case 2
    try {
      RPC.decodeRequest("", null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected to get here
    }

    // Case 3
    RPCRequest request;
    request = RPC.decodeRequest(VALID_ENCODED_REQUEST, null);
    assertEquals(A.class.getMethod("method2"), request.getMethod());
    assertTrue(request.getParameters().length == 0);

    // Case 4
    request = RPC.decodeRequest(VALID_ENCODED_REQUEST, A.class);
    assertEquals(A.class.getMethod("method2"), request.getMethod());
    assertTrue(request.getParameters().length == 0);

    // Case 5
    try {
      request = RPC.decodeRequest(INVALID_INTERFACE_REQUEST, B.class);
      fail("Expected IncompatibleRemoteServiceException");
    } catch (IncompatibleRemoteServiceException e) {
      // should get here
    }
    // Case 6
    try {
      request = RPC.decodeRequest(INVALID_METHOD_REQUEST, A.class);
      fail("Expected IncompatibleRemoteServiceException");
    } catch (IncompatibleRemoteServiceException e) {
      // should get here
    }
  }

  public void testElision() throws SecurityException, SerializationException,
      NoSuchMethodException {
    class TestPolicy extends SerializationPolicy implements TypeNameObfuscator {
      private static final String C_NAME = "__c__";

      public String getClassNameForTypeId(String id)
          throws SerializationException {
        assertEquals(C_NAME, id);
        return C.class.getName();
      }

      public String getTypeIdForClass(Class<?> clazz)
          throws SerializationException {
        assertEquals(C.class, clazz);
        return C_NAME;
      }

      @Override
      public boolean shouldDeserializeFields(Class<?> clazz) {
        return C.class.equals(clazz);
      }

      @Override
      public boolean shouldSerializeFields(Class<?> clazz) {
        return C.class.equals(clazz);
      }

      @Override
      public void validateDeserialize(Class<?> clazz)
          throws SerializationException {
      }

      @Override
      public void validateSerialize(Class<?> clazz)
          throws SerializationException {
      }

      @Override
      public Set<String> getClientFieldNamesForEnhancedClass(Class<?> clazz) {
        return null;
      }
    }

    String rpc = RPC.encodeResponseForSuccess(CC.class.getMethod("c"), new C(),
        new TestPolicy(), AbstractSerializationStream.FLAG_ELIDE_TYPE_NAMES);
    assertTrue(rpc.contains(TestPolicy.C_NAME));
    assertFalse(rpc.contains(C.class.getName()));
  }

  public void testElisionWithNoObfuscator() throws SecurityException,
      NoSuchMethodException {
    class TestPolicy extends SerializationPolicy {
      @Override
      public boolean shouldDeserializeFields(Class<?> clazz) {
        return C.class.equals(clazz);
      }

      @Override
      public boolean shouldSerializeFields(Class<?> clazz) {
        return C.class.equals(clazz);
      }

      @Override
      public void validateDeserialize(Class<?> clazz)
          throws SerializationException {
      }

      @Override
      public void validateSerialize(Class<?> clazz)
          throws SerializationException {
      }

      @Override
      public Set<String> getClientFieldNamesForEnhancedClass(Class<?> clazz) {
        return null;
      }
    }

    try {
      RPC.encodeResponseForSuccess(CC.class.getMethod("c"), new C(),
          new TestPolicy(), AbstractSerializationStream.FLAG_ELIDE_TYPE_NAMES);
      fail("Should have thrown a SerializationException");
    } catch (SerializationException e) {
      // OK
    }
  }

  /**
   * Tests for method {@link RPC#encodeResponseForFailure(Method, Throwable)}.
   * 
   * Cases:
   * <ol>
   * <li>Method == null</li>
   * <li>Object == null</li>
   * <li>Method is not specified to throw an exception of the given type</li>
   * <li>Method is specified to throw an exception of the given type</li>
   * </ol>
   * 
   * @throws NoSuchMethodException
   * @throws SecurityException
   * @throws SerializationException
   * 
   */
  public void testEncodeResponseForFailure() throws SecurityException,
      NoSuchMethodException, SerializationException {
    // Case 1
    RPC.encodeResponseForFailure(null, new SerializableException());

    Method A_method1 = null;
    A_method1 = A.class.getMethod("method1");

    // Case 2
    try {
      RPC.encodeResponseForFailure(A_method1, null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected to get here
    }

    // Case 3
    try {
      RPC.encodeResponseForFailure(A.class.getMethod("method1"),
          new IllegalArgumentException());
      fail("Expected UnexpectedException");
    } catch (UnexpectedException e) {
      // expected to get here
    }

    // Case 4
    String str = RPC.encodeResponseForFailure(A.class.getMethod("method1"),
        new SerializableException());
    assertTrue(str.indexOf("SerializableException") != -1);
  }

  /**
   * Tests for {@link RPC#encodeResponseForSuccess(Method, Object)}.
   * 
   * Cases:
   * <ol>
   * <li>Method == null</li>
   * <li>Object == null</li>
   * <li>Method is not specified to return the given type</li>
   * <li>Method is specified to return the given type</li>
   * </ol>
   * 
   * @throws SerializationException
   * @throws NoSuchMethodException
   * @throws SecurityException
   */
  public void testEncodeResponseForSuccess() throws SerializationException,
      SecurityException, NoSuchMethodException {
    Method A_method1 = null;
    Method A_method2 = null;
    A_method1 = A.class.getMethod("method1");
    A_method2 = A.class.getMethod("method2");

    // Case 1
    try {
      RPC.encodeResponseForSuccess(null, new Object());
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected to get here
    }

    // Case 2
    RPC.encodeResponseForSuccess(A_method1, null);

    // Case 3
    try {
      RPC.encodeResponseForSuccess(A_method2, new SerializableException());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected to get here
    }

    // Case 4
    RPC.encodeResponseForSuccess(A_method2, new Integer(1));
  }

  /**
   * Tests for {@link RPC#invokeAndEncodeResponse(Object, Method, Object[])}.
   * 
   * Cases:
   * <ol>
   * <li>Method == null</li>
   * <li>Object does not implement Method</li>
   * <li>Method parameters do not match given parameters
   * <li>Method throws exception that it is not specified to
   * <li>Method throws exception that it is specified to throw
   * </ol>
   * 
   * @throws NoSuchMethodException
   * @throws SecurityException
   * @throws SerializationException
   * 
   */
  public void testInvokeAndEncodeResponse() throws SecurityException,
      NoSuchMethodException, SerializationException {
    // Case 1
    try {
      RPC.invokeAndEncodeResponse(null, null, null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected to get here
    }

    Method A_method1 = A.class.getMethod("method1");

    // Case 2
    try {
      RPC.invokeAndEncodeResponse(new B() {
        public void method1() {
        }
      }, A_method1, null);
      fail("Expected a SecurityException");
    } catch (SecurityException e) {
      // expected to get here
    }

    // Case 3
    try {
      RPC.invokeAndEncodeResponse(new A() {
        public void method1() throws SerializableException {
        }

        public int method2() {
          return 0;
        }

        public int method3(int val) {
          return 0;
        }
      }, A_method1, new Integer[] {new Integer(1)});
      fail("Expected a SecurityException");
    } catch (SecurityException e) {
      // expected to get here
    }

    // Case 4
    try {
      RPC.invokeAndEncodeResponse(new A() {
        public void method1() throws SerializableException {
          throw new IllegalArgumentException();
        }

        public int method2() {
          return 0;
        }

        public int method3(int val) {
          return 0;
        }
      }, A_method1, null);
      fail("Expected an UnexpectedException");
    } catch (UnexpectedException e) {
      // expected to get here
    }

    // Case 5
    RPC.invokeAndEncodeResponse(new A() {
      public void method1() throws SerializableException {
        throw new SerializableException();
      }

      public int method2() {
        return 0;
      }

      public int method3(int val) {
        return 0;
      }
    }, A_method1, null);
  }

  public void testSerializationStreamDequote() throws SerializationException {
    ServerSerializationStreamReader reader = new ServerSerializationStreamReader(
        null, null);
    reader.prepareToRead(STRING_QUOTE_REQUEST);
    assertEquals("Raw backslash \\", reader.readString());
    assertEquals("Quoted separator " + RPC_SEPARATOR_CHAR, reader.readString());
    assertEquals("\uffff\\!\\0\u0000", reader.readString());
  }
}
