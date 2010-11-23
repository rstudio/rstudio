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

import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * Tests the deserialized RPC request object.
 */
public class RPCRequestTest extends TestCase {

  /**
   * Just a class to get a dummy method from for the request.
   */
  private class MockRequestImplementation {
    public String doSomething(String a, int b) {
      // This is never called, we just reflect on it
      return null;
    }
  }

  /**
   * Tests getting the equivalent string of an RPC request.
   */
  public void testToString() throws Exception {
    Method method = MockRequestImplementation.class.getMethod("doSomething",
        String.class, int.class);

    // Test simple case
    Object params[] = new Object[] {"abcdefg", 1234};
    RPCRequest request = new RPCRequest(method, params, null, null, 0);
    String strRequest = request.toString();
    assertEquals("com.google.gwt.user.server.rpc.RPCRequestTest$"
        + "MockRequestImplementation.doSomething(\"abcdefg\", 1234)",
        strRequest);

    // Test case with a string that needs escaping
    Object params2[] = new Object[] {"ab\"cde\"fg", 1234};
    RPCRequest request2 = new RPCRequest(method, params2, null, null, 0);
    String strRequest2 = request2.toString();
    assertEquals("com.google.gwt.user.server.rpc.RPCRequestTest$"
        + "MockRequestImplementation.doSomething(\"ab\\\"cde\\\"fg\", 1234)",
        strRequest2);
  }
}