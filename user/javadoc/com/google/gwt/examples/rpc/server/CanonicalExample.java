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
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Canonical RPC integration example.
 */
public class CanonicalExample extends RemoteServiceServlet {
  /**
   * Process the RPC request encoded into the payload string and return a string
   * that encodes either the method return or an exception thrown by it.
   */
  @Override
  public String processCall(String payload) throws SerializationException {
    try {
      RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass());
      return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
          rpcRequest.getParameters());
    } catch (IncompatibleRemoteServiceException ex) {
      return RPC.encodeResponseForFailure(null, ex);
    }
  }
}
