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
import com.google.gwt.user.server.Util;

import java.lang.reflect.Method;

/**
 * An abstract class for XSRF protected RPC service implementations, which
 * decides if XSRF protection should be enforced on a method invocation based
 * on the following logic:
 * <ul>
 *  <li>RPC interface or method can be annotated with either {@link XsrfProtect}
 *      or {@link NoXsrfProtect} annotation to enable or disable XSRF protection
 *      on all methods of an RPC interface or a single method correspondingly.
 *  <li>RPC interface level annotation can be overridden by a method level
 *      annotation.
 *  <li>If no annotations are present and RPC interface contains method that
 *      returns {@link RpcToken} or its implementation, then XSRF token
 *      validation is performed on all methods of that interface except for the
 *      method returning {@link RpcToken}.
 * </ul>
 *
 * @see XsrfProtectedServiceServlet
 */
public abstract class AbstractXsrfProtectedServiceServlet extends
    RemoteServiceServlet {

  @Override
  protected void onAfterRequestDeserialized(RPCRequest rpcRequest) {
    if (shouldValidateXsrfToken(rpcRequest.getMethod())) {
      validateXsrfToken(rpcRequest.getRpcToken(), rpcRequest.getMethod());
    }
  }

  /**
   * Override this method to change default XSRF enforcement logic.
   *
   * @param method Method being invoked
   * @return {@code true} if XSRF token should be verified, {@code false}
   *         otherwise
   */
  protected boolean shouldValidateXsrfToken(Method method) {
    Class<?> servletClass = method.getDeclaringClass();

    if (method.getAnnotation(NoXsrfProtect.class) != null ||
          (Util.getClassAnnotation(
              servletClass, NoXsrfProtect.class) != null &&
          method.getAnnotation(XsrfProtect.class) == null)) {
      // XSRF protection is disabled
      return false;
    }

    if (Util.getClassAnnotation(servletClass, XsrfProtect.class) != null ||
          method.getAnnotation(XsrfProtect.class) != null) {
      return true;
    }

    // if no explicit annotation is given no XSRF token verification is done,
    // unless there's a method returning RpcToken in which case XSRF token
    // verification is performed for all methods
    Method[] classMethods = servletClass.getMethods();
    for (Method classMethod : classMethods) {
      if (RpcToken.class.isAssignableFrom(classMethod.getReturnType()) &&
          !method.equals(classMethod)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Override this method to perform XSRF token verification.
   *
   * @param token {@link RpcToken} included with an RPC request.
   * @param method method being invoked via this RPC call.
   * @throws RpcTokenException if token verification failed.
   */
  protected abstract void validateXsrfToken(RpcToken token, Method method)
      throws RpcTokenException;
}
