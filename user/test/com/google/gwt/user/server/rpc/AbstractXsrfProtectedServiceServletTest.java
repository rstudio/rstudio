/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.user.client.rpc.RpcToken;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.XsrfProtectedService;

import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * Tests {@link AbstractXsrfProtectedServiceServlet}'s XSRF enforcement logic.
 */
public class AbstractXsrfProtectedServiceServletTest extends TestCase {

  private boolean isValidateCalled;

  @Override
  public void setUp() {
    isValidateCalled = false;
  }

  private MockXsrfProtectedServiceServlet mockServlet =
    new MockXsrfProtectedServiceServlet();

  private class MockXsrfProtectedServiceServlet extends
      AbstractXsrfProtectedServiceServlet {

    @Override
    protected void validateXsrfToken(RpcToken token, Method method)
        throws RpcTokenException {
      isValidateCalled = true;
    }
  }

  @XsrfProtect
  private interface RpcWithXsrfProtect {
    void foo();
  }

  @XsrfProtect
  private interface RpcWithXsrfProtectAndMethodOverride {
    @NoXsrfProtect
    void foo();
  }

  @NoXsrfProtect
  private interface RpcWithNoXsrfProtect {
    void foo();
  }

  @NoXsrfProtect
  private interface RpcWithNoXsrfProtectAndMethodOverride {
    @XsrfProtect
    void foo();
  }

  private interface RpcWithoutAnnotationAndMethodXsrfProtect {
    @XsrfProtect
    void foo();
  }

  private interface RpcWithoutAnnotationAndMethodNoXsrfProtect {
    @NoXsrfProtect
    void foo();
  }

  private interface RpcWithoutAnnotationsAndWithRpcTokenMethod {
    void foo();
    RpcToken getToken();
  }

  @NoXsrfProtect
  private interface
      RpcWithoutAnnotationsAndWithRpcTokenMethodAndNoProtectOverride {
    void foo();
    RpcToken getToken();
  }

  @XsrfProtect
  private interface
      RpcWithoutAnnotationsAndWithRpcTokenMethodAndProtectOverride {
    void foo();
    RpcToken getToken();
  }

  @SuppressWarnings("rpc-validation")
  private interface XsrfProtectedRpc extends XsrfProtectedService {
    void foo();
  }

  @SuppressWarnings("rpc-validation")
  private interface XsrfProtectedRpcWithOverride extends XsrfProtectedRpc {
    void fooBar();
    @NoXsrfProtect
    void insecure();
  }

  private interface RpcWithoutAnyAnnotations {
    void foo();
  }

  public void testShouldValidatedXsrfToken() throws Exception {
    checkXsrfValidationLogic(RpcWithXsrfProtect.class, "foo", true);
    checkXsrfValidationLogic(RpcWithXsrfProtectAndMethodOverride.class, "foo",
        false);
    checkXsrfValidationLogic(RpcWithNoXsrfProtect.class, "foo", false);
    checkXsrfValidationLogic(RpcWithNoXsrfProtectAndMethodOverride.class, "foo",
        true);
    checkXsrfValidationLogic(RpcWithoutAnnotationAndMethodXsrfProtect.class,
        "foo", true);
    checkXsrfValidationLogic(RpcWithoutAnnotationAndMethodNoXsrfProtect.class,
        "foo", false);
    checkXsrfValidationLogic(RpcWithoutAnnotationsAndWithRpcTokenMethod.class,
        "foo", true);
    checkXsrfValidationLogic(
        RpcWithoutAnnotationsAndWithRpcTokenMethodAndNoProtectOverride.class,
        "foo", false);
    checkXsrfValidationLogic(
        RpcWithoutAnnotationsAndWithRpcTokenMethodAndNoProtectOverride.class,
        "getToken", false);
    checkXsrfValidationLogic(
        RpcWithoutAnnotationsAndWithRpcTokenMethodAndProtectOverride.class,
        "foo", true);
    checkXsrfValidationLogic(
        RpcWithoutAnnotationsAndWithRpcTokenMethodAndProtectOverride.class,
        "getToken", true);
    checkXsrfValidationLogic(RpcWithoutAnyAnnotations.class, "foo", false);
    checkXsrfValidationLogic(RpcWithoutAnnotationsAndWithRpcTokenMethod.class,
        "getToken", false);
    checkXsrfValidationLogic(XsrfProtectedRpc.class, "foo", true);
    checkXsrfValidationLogic(XsrfProtectedRpcWithOverride.class, "foo", true);
    checkXsrfValidationLogic(XsrfProtectedRpcWithOverride.class, "insecure",
        false);
  }

  private void checkXsrfValidationLogic(Class<?> rpcClass, String methodName,
      boolean mustCallValidate) throws Exception {
    isValidateCalled = false;
    Method method = rpcClass.getMethod(methodName, new Class[] {});
    RPCRequest request = new RPCRequest(method, new Object[] {}, null, 0);
    mockServlet.onAfterRequestDeserialized(request);
    assertEquals(mustCallValidate, isValidateCalled);
  }
}
