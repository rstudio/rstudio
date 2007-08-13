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

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.client.rpc.SerializationException;

import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * Tests for the {@link com.google.gwt.user.server.rpc.RPC RPC} class.
 */
public class RPCTest extends TestCase {

  private static interface A extends RemoteService {
    void method1() throws SerializableException;

    int method2();

    int method3(int val);
  }

  private static interface B {
    void method1();
  }

  private final String VALID_ENCODED_REQUEST = "3\uffff" + // version
      "0\uffff" + // flags
      "4\uffff" + // string table entry count
      A.class.getName() + "\uffff" + // string table entry #0
      "method2" + "\uffff" + // string table entry #1
      "moduleBaseURL" + "\uffff" + // string table entry #2
      "whitelistHashcode" + "\uffff" + // string table entry #4
      "3\uffff" + // module base URL
      "4\uffff" + // whitelist hashcode
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  private final String INVALID_METHOD_REQUEST = "3\uffff" + // version
      "0\uffff" + // flags
      "4\uffff" + // string table entry count
      A.class.getName() + "\uffff" + // string table entry #0
      "method3" + "\uffff" + // string table entry #1
      "moduleBaseURL" + "\uffff" + // string table entry #2
      "whitelistHashcode" + "\uffff" + // string table entry #4
      "3\uffff" + // module base URL
      "4\uffff" + // whitelist hashcode
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  private final String INVALID_INTERFACE_REQUEST = "3\uffff" + // version
      "0\uffff" + // flags
      "4\uffff" + // string table entry count
      B.class.getName() + "\uffff" + // string table entry #0
      "method1" + "\uffff" + // string table entry #1
      "moduleBaseURL" + "\uffff" + // string table entry #2
      "whitelistHashcode" + "\uffff" + // string table entry #4
      "3\uffff" + // module base URL
      "4\uffff" + // whitelist hashcode
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  private final String VALID_PRE_RPC_RESOURCE_ENCODED_REQUEST = "2\uffff" + // version
      "0\uffff" + // flags
      "2\uffff" + // string table entry count
      A.class.getName() + "\uffff" + // string table entry #0
      "method2" + "\uffff" + // string table entry #1
      "1\uffff" + // interface name
      "2\uffff" + // method name
      "0\uffff"; // param count

  /**
   * Tests for method {@link RPC#decodeRequest(String)}
   * 
   * <p/> Cases:
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
   * Tests for method {@link RPC#decodeRequest(String, Class)}
   * 
   * <p/> Cases:
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
    assertEquals(A.class.getMethod("method2", null), request.getMethod());
    assertTrue(request.getParameters().length == 0);

    // Case 4
    request = RPC.decodeRequest(VALID_ENCODED_REQUEST, A.class);
    assertEquals(A.class.getMethod("method2", null), request.getMethod());
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

  /**
   * Tests that method
   * {@link RPC#decodeRequest(String, Class, SerializationPolicyProvider)} can
   * handle the decoding of requests from pre-RPC resource (whitelist) clients.
   * 
   * @throws SerializationException
   */
  public void testDecodeRequestPreRPCResourceFile() {
    RPCRequest rpcRequest = RPC.decodeRequest(
        VALID_PRE_RPC_RESOURCE_ENCODED_REQUEST, A.class, null);
    SerializationPolicy serializationPolicy = rpcRequest.getSerializationPolicy();
    assertEquals(RPC.getDefaultSerializationPolicy(), serializationPolicy);
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
    A_method1 = A.class.getMethod("method1", null);

    // Case 2
    try {
      RPC.encodeResponseForFailure(A_method1, null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected to get here
    }

    // Case 3
    try {
      RPC.encodeResponseForFailure(A.class.getMethod("method1", null),
          new IllegalArgumentException());
      fail("Expected UnexpectedException");
    } catch (UnexpectedException e) {
      // expected to get here
    }

    // Case 4
    String str = RPC.encodeResponseForFailure(
        A.class.getMethod("method1", null), new SerializableException());
    assertTrue(str.indexOf("SerializableException") != -1);
  }

  /**
   * Tests for {@link RPC#encodeResponseForSuccess(Method, Object)}
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
    A_method1 = A.class.getMethod("method1", null);
    A_method2 = A.class.getMethod("method2", null);

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
   * Tests for {@link RPC#invokeAndEncodeResponse(Object, Method, Object[])}
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

    Method A_method1 = A.class.getMethod("method1", null);

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
}
