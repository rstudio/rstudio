/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.client.rpc;

/**
 * An interface implemented by client-side RPC proxy objects. Cast the object
 * returned from {@link com.google.gwt.core.client.GWT#create(Class)} on a
 * {@link RemoteService} to this interface to set {@link RpcToken} and
 * {@link RpcTokenExceptionHandler}.
 */
public interface HasRpcToken {

  /**
   * Return RPC token used with this RPC instance.
   *
   * @return RPC token or {@code null} if none set.
   */
  RpcToken getRpcToken();

  /**
   * Return RPC token exception handler used with this RPC instance.
   *
   * @return Exception handler or {@code null} if none set.
   */
  RpcTokenExceptionHandler getRpcTokenExceptionHandler();

  /**
   * Sets the {@link RpcToken} to be included with each RPC call.
   */
  void setRpcToken(RpcToken token);

  /**
   * Sets the handler for exceptions that occurred during RPC token processing.
   */
  void setRpcTokenExceptionHandler(RpcTokenExceptionHandler handler);
}
