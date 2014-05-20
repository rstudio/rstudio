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

import com.google.gwt.user.client.rpc.RemoteServiceServletTestService;

import javax.servlet.http.HttpServletRequest;

/**
 * A RemoteService for testing the details of the "over-HTTP" part of
 * RPC-over-HTTP.
 */
public class RemoteServiceServletTestServiceImplBase extends
    RemoteServiceServlet implements RemoteServiceServletTestService {

  /**
   * A RuntimeException the client code shouldn't know anything about.
   */
  public static class FooException extends RuntimeException {
    public FooException() {
      super("This is OK.  Simulating random backend code exception.");
    }
  }

  @Override
  public void test() {
  }

  @Override
  public void testExpectCustomHeader() {
    HttpServletRequest req = getThreadLocalRequest();
    if (!Boolean.parseBoolean(req.getHeader("X-Custom-Header"))) {
      throw new RuntimeException("Missing header");
    }
  }

  @Override
  public void testExpectPermutationStrongName(String expectedStrongName) {
    if (getPermutationStrongName() == null) {
      throw new NullPointerException("getPermutationStrongName()");
    }

    if (!expectedStrongName.equals(getPermutationStrongName())) {
      throw new RuntimeException(expectedStrongName + " != "
          + getPermutationStrongName());
    }
  }

  @Override
  public void throwDeclaredRuntimeException() {
    throw new NullPointerException("expected");
  }

  @Override
  public void throwUnknownRuntimeException() {
    throw new FooException();
  }
}
