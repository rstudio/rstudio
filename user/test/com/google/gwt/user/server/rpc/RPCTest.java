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
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RpcToken;
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

  @SuppressWarnings("rpc-validation")
  private static interface A extends RemoteService {
    void method1() throws SerializableException;

    int method2();

    int method3(int val);
  }

  private static interface B {
    void method1();
  }

  @SuppressWarnings("rpc-validation")
  private static interface D extends RemoteService {
    long echo(long val);
  }

  /**
   * Test error message for an out=of-range int value.
   * 
   * @see RPCTest#testDecodeBadIntegerValue()
   */
  private static class Wrapper implements IsSerializable {
    byte value1;
    char value2;
    short value3;
    int value4;
    public Wrapper() { }
  }

  @SuppressWarnings("rpc-validation")
  private static interface WrapperIF extends RemoteService {
    void method1(Wrapper w);
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
   * Call 'D.echo(0xFEDCBA9876543210L);' using V5 long format
   * (pair of doubles).
   */
  private static final String VALID_V5_ENCODED_REQUEST = "" +
      AbstractSerializationStream.SERIALIZATION_STREAM_MIN_VERSION +
      RPC_SEPARATOR_CHAR + // version
      "0" + RPC_SEPARATOR_CHAR + // flags
      "5" + RPC_SEPARATOR_CHAR + // string table count
      "moduleBaseUrl" + RPC_SEPARATOR_CHAR + // string table entry #1
      "whitelistHashCode" + RPC_SEPARATOR_CHAR + // string table entry #2
      D.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #3
      "echo" + RPC_SEPARATOR_CHAR + // string table entry #4
      "J" + RPC_SEPARATOR_CHAR + // string table entry #5
      "1" + RPC_SEPARATOR_CHAR + // moduleBaseUrl
      "2" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "3" + RPC_SEPARATOR_CHAR + // interface name
      "4" + RPC_SEPARATOR_CHAR + // method name
      "1" + RPC_SEPARATOR_CHAR + // param count
      "5" + RPC_SEPARATOR_CHAR + // 'J' == long param type
      "1.985229328E9" + RPC_SEPARATOR_CHAR + // low bits of long
      "-8.1985531201716224E16" + RPC_SEPARATOR_CHAR; // high bits of long

  /**
   * Call 'D.echo(0xFEDCBA9876543210L);' using V6 long format
   * (base-64 encoding).
   */
  private static final String VALID_V6_ENCODED_REQUEST = "" +
      AbstractSerializationStream.SERIALIZATION_STREAM_VERSION +
      RPC_SEPARATOR_CHAR + // version
      "0" + RPC_SEPARATOR_CHAR + // flags
      "5" + RPC_SEPARATOR_CHAR + // string table count
      "moduleBaseUrl" + RPC_SEPARATOR_CHAR + // string table entry #1
      "whitelistHashCode" + RPC_SEPARATOR_CHAR + // string table entry #2
      D.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #3
      "echo" + RPC_SEPARATOR_CHAR + // string table entry #4
      "J" + RPC_SEPARATOR_CHAR + // string table entry #5
      "1" + RPC_SEPARATOR_CHAR + // moduleBaseUrl
      "2" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "3" + RPC_SEPARATOR_CHAR + // interface name
      "4" + RPC_SEPARATOR_CHAR + // method name
      "1" + RPC_SEPARATOR_CHAR + // param count
      "5" + RPC_SEPARATOR_CHAR + // 'J' == long param type
      "P7cuph2VDIQ" + RPC_SEPARATOR_CHAR; // long in base-64 encoding
  
  private static final String VALID_V6_ENCODED_REQUEST_WITH_INVALID_FLAGS = ""
      + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION +
      RPC_SEPARATOR_CHAR + // version
      "123" + RPC_SEPARATOR_CHAR + // flags
      "5" + RPC_SEPARATOR_CHAR + // string table count
      "moduleBaseUrl" + RPC_SEPARATOR_CHAR + // string table entry #1
      "whitelistHashCode" + RPC_SEPARATOR_CHAR + // string table entry #2
      D.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #3
      "echo" + RPC_SEPARATOR_CHAR + // string table entry #4
      "J" + RPC_SEPARATOR_CHAR + // string table entry #5
      "1" + RPC_SEPARATOR_CHAR + // moduleBaseUrl
      "2" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "3" + RPC_SEPARATOR_CHAR + // interface name
      "4" + RPC_SEPARATOR_CHAR + // method name
      "1" + RPC_SEPARATOR_CHAR + // param count
      "5" + RPC_SEPARATOR_CHAR + // 'J' == long param type
      "P7cuph2VDIQ" + RPC_SEPARATOR_CHAR; // long in base-64 encoding

  /**
   * Tests that out-of-range or other illegal integer values generated
   * by client-side serialization get a nested exception with a reasonable
   * error message.
   */
  public void testDecodeBadIntegerValue() {
    String requestBase = "" +
        AbstractSerializationStream.SERIALIZATION_STREAM_VERSION +
        RPC_SEPARATOR_CHAR + // version
        "0" + RPC_SEPARATOR_CHAR + // flags
        "6" + RPC_SEPARATOR_CHAR + // string table entry count
        WrapperIF.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #1
        "method1" + RPC_SEPARATOR_CHAR + // string table entry #2
        "moduleBaseURL" + RPC_SEPARATOR_CHAR + // string table entry #3
        "whitelistHashcode" + RPC_SEPARATOR_CHAR + // string table entry #4
        Wrapper.class.getName() + RPC_SEPARATOR_CHAR + // string table entry #5
        Wrapper.class.getName() +
        "/316143997" + RPC_SEPARATOR_CHAR + // string table entry #6
        "3" + RPC_SEPARATOR_CHAR + // module base URL
        "4" + RPC_SEPARATOR_CHAR + // whitelist hashcode
        "1" + RPC_SEPARATOR_CHAR + // interface name
        "2" + RPC_SEPARATOR_CHAR + // method name
        "1" + RPC_SEPARATOR_CHAR + // param count
        "5" + RPC_SEPARATOR_CHAR + // IntWrapper class name
        "6" + RPC_SEPARATOR_CHAR; // IntWrapper signature
    
    // Valid values
    String goodRequest = requestBase + "12" + RPC_SEPARATOR_CHAR + // byte
    "345" + RPC_SEPARATOR_CHAR + // char
    "678" + RPC_SEPARATOR_CHAR + // short
    "9101112" + RPC_SEPARATOR_CHAR; // int
    
    RPC.decodeRequest(goodRequest); // should succeed
    
    // Create bad RPC messages with out of range, fractional, and non-numerical
    // values for byte, char, short, and int fields.
    for (int idx = 0; idx < 12; idx++) {
      String b = "12";
      String c = "345";
      String s = "678";
      String i = "9101112";
      String message = null;
      String badValue = null;
      
      // Choose type of bad value and expected error message string
      switch (idx / 4) {
        case 0:
          badValue = "123456789123456789";
          message = "out-of-range";
          break;
        case 1:
          badValue = "1.25";
          message = "fractional";
          break;
        case 2:
          badValue = "123ABC";
          message = "non-numerical";
          break;
      }
      
      // Choose field to hold bad value
      switch (idx % 4) {
        case 0: b = badValue; break;
        case 1: c = badValue; break;
        case 2: s = badValue; break;
        case 3: i = badValue; break;
      }
      
      // Form the request
      String request = requestBase + b + RPC_SEPARATOR_CHAR + // byte
          c + RPC_SEPARATOR_CHAR + // char
          s + RPC_SEPARATOR_CHAR + // short
          i + RPC_SEPARATOR_CHAR; // int
      
      // Check that request fails with the expected message
      try {
        RPC.decodeRequest(request);
        fail();
      } catch (IncompatibleRemoteServiceException e) {
        assertTrue(e.getMessage().contains(message));
      }
    }
  }

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
  
  public void testDecodeInvalidFlags() {
    try {
      RPC.decodeRequest(VALID_V6_ENCODED_REQUEST_WITH_INVALID_FLAGS);
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

  private static class TestRpcToken implements RpcToken {
    String tokenValue;
    public TestRpcToken() { }
  }

  private static final String VALID_V6_ENCODED_REQUEST_WITH_RPC_TOKEN = "" +
      AbstractSerializationStream.SERIALIZATION_STREAM_VERSION +
      RPC_SEPARATOR_CHAR + // version
      AbstractSerializationStream.FLAG_RPC_TOKEN_INCLUDED + // flags 
          RPC_SEPARATOR_CHAR +
      "7" + RPC_SEPARATOR_CHAR + // string table count
      "moduleBaseUrl" + RPC_SEPARATOR_CHAR + // string table entry #1
      "whitelistHashCode" + RPC_SEPARATOR_CHAR + // string table entry #2
      TestRpcToken.class.getName() + "/3856085925" + // string table entry #3
          RPC_SEPARATOR_CHAR +
      "RPC_TOKEN_VALUE" + RPC_SEPARATOR_CHAR + // string table entry #4 
      D.class.getName() + // string table entry #5
          RPC_SEPARATOR_CHAR + 
      "echo" + RPC_SEPARATOR_CHAR + // string table entry #6
      "J" + RPC_SEPARATOR_CHAR + // string table entry #7
      "1" + RPC_SEPARATOR_CHAR + // moduleBaseUrl
      "2" + RPC_SEPARATOR_CHAR + // whitelist hashcode
      "3" + RPC_SEPARATOR_CHAR + // RPC token class
      "4" + RPC_SEPARATOR_CHAR + // RPC token value
      "5" + RPC_SEPARATOR_CHAR + // interface name
      "6" + RPC_SEPARATOR_CHAR + // method name
      "1" + RPC_SEPARATOR_CHAR + // param count
      "7" + RPC_SEPARATOR_CHAR + // 'J' == long param type
      "P7cuph2VDIQ" + RPC_SEPARATOR_CHAR; // long in base-64 encoding
  
  public void testDecodeRpcToken() {
    class TestPolicy extends SerializationPolicy {
      @Override
      public boolean shouldDeserializeFields(Class<?> clazz) {
        return TestRpcToken.class.equals(clazz);
      }

      @Override
      public boolean shouldSerializeFields(Class<?> clazz) {
        return TestRpcToken.class.equals(clazz);
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
    class TestPolicyProvider implements SerializationPolicyProvider {      
      public SerializationPolicy getSerializationPolicy(
          String moduleBaseURL, String serializationPolicyStrongName) {
        return new TestPolicy();
      }
    }
    RPCRequest requestWithoutToken = RPC.decodeRequest(
        VALID_V6_ENCODED_REQUEST);
    assertNull(requestWithoutToken.getRpcToken());
    RPCRequest requestWithToken = RPC.decodeRequest(
        VALID_V6_ENCODED_REQUEST_WITH_RPC_TOKEN, null, new TestPolicyProvider());
    assertNotNull(requestWithToken.getRpcToken());
    TestRpcToken token = (TestRpcToken) requestWithToken.getRpcToken();
    assertEquals("RPC_TOKEN_VALUE", token.tokenValue);
  }

  public void testDecodeV5Long() {
    try {
      RPCRequest request = RPC.decodeRequest(VALID_V5_ENCODED_REQUEST,
          D.class, null);
      assertEquals(0xFEDCBA9876543210L, request.getParameters()[0]);
    } catch (IncompatibleRemoteServiceException e) {
      fail();
    }
  }

  public void testDecodeV6Long() {
    try {
      RPCRequest request = RPC.decodeRequest(VALID_V6_ENCODED_REQUEST,
          D.class, null);
      assertEquals(0xFEDCBA9876543210L, request.getParameters()[0]);
    } catch (IncompatibleRemoteServiceException e) {
      fail();
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
