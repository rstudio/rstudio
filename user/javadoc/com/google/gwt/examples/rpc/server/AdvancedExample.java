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
package com.google.gwt.examples.rpc.server;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Example demonstrating a more complex RPC integration scenario.
 */
public class AdvancedExample extends HttpServlet {
  /**
   * An example of how you could integrate GWTs RPC functionality without using
   * the {@link com.google.gwt.user.server.rpc.RemoteServiceServlet}. Note that
   * it also shows how mapping between and RPC interface and some other POJO
   * could be performed.
   */
  @Override
  public void doPost(HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    String payload = readPayloadAsUtf8(httpRequest);

    try {
      try {
        RPCRequest rpcRequest = RPC.decodeRequest(payload);

        Object targetInstance = getInstanceToHandleRequest(httpRequest,
            rpcRequest);

        Method targetMethod = maybeMapRequestedMethod(targetInstance,
            rpcRequest.getMethod());

        Object[] targetParameters = maybeMapParameters(rpcRequest.getParameters());

        try {
          Object result = targetMethod.invoke(targetInstance, targetParameters);

          result = maybeMapResult(rpcRequest.getMethod(), result);

          /*
           * Encode the object that will be given to the client code's
           * AsyncCallback::onSuccess(Object) method.
           */
          String encodedResult = RPC.encodeResponseForSuccess(
              rpcRequest.getMethod(), result);

          sendResponseForSuccess(httpResponse, encodedResult);
        } catch (IllegalArgumentException e) {
          SecurityException securityException = new SecurityException(
              "Blocked attempt to invoke method " + targetMethod);
          securityException.initCause(e);
          throw securityException;
        } catch (IllegalAccessException e) {
          SecurityException securityException = new SecurityException(
              "Blocked attempt to access inaccessible method "
                  + targetMethod
                  + (targetInstance != null ? " on target " + targetInstance
                      : ""));
          securityException.initCause(e);
          throw securityException;
        } catch (InvocationTargetException e) {
          Throwable cause = e.getCause();

          Throwable mappedThrowable = maybeMapThrowable(cause,
              rpcRequest.getMethod());

          /*
           * Encode the exception that will be passed back to the client's
           * client code's AsyncCallback::onFailure(Throwable) method.
           */
          String failurePayload = RPC.encodeResponseForFailure(
              rpcRequest.getMethod(), mappedThrowable);

          sendResponseForFailure(httpResponse, failurePayload);
        }
      } catch (IncompatibleRemoteServiceException e) {
        sendResponseForFailure(httpResponse, RPC.encodeResponseForFailure(null,
            e));
      }
    } catch (Throwable e) {
      /*
       * Return a generic error which will be passed to the client code's
       * AsyncCallback::onFailure(Throwable) method.
       */
      sendResponseForGenericFailure(httpResponse);
    }
  }

  private Object getInstanceToHandleRequest(HttpServletRequest httpRequest,
      RPCRequest rpcRequest) {
    return null;
  }

  private Method maybeMapRequestedMethod(Object targetInstance, Method method) {
    return null;
  }

  private Object[] maybeMapParameters(Object[] parameters) {
    return null;
  }

  private Object maybeMapResult(Method method, Object targetResult) {
    return null;
  }

  private Throwable maybeMapThrowable(Throwable cause, Method method) {
    return null;
  }

  private String readPayloadAsUtf8(HttpServletRequest httpRequest) {
    return null;
  }

  private void sendResponseForFailure(HttpServletResponse httpResponse,
      String failurePayload) {
  }

  private void sendResponseForGenericFailure(HttpServletResponse httpResponse) {
  }

  private void sendResponseForSuccess(HttpServletResponse httpResponse,
      String encodedResult) {
  }
}
