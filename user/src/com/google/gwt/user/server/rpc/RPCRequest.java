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

import com.google.gwt.user.client.rpc.RpcToken;

import java.lang.reflect.Method;

/**
 * Describes an incoming RPC request in terms of a resolved {@link Method} and
 * an array of arguments.
 */
public final class RPCRequest {

  /**
   * The flags associated with the RPC request.
   */
  private final int flags;

  /**
   * The method for this request.
   */
  private final Method method;

  /**
   * The parameters for this request.
   */
  private final Object[] parameters;
  
  /**
   * The RPC token for this request.
   */
  private final RpcToken rpcToken;

  /**
   * {@link SerializationPolicy} used for decoding this request and for encoding
   * the responses.
   */
  private final SerializationPolicy serializationPolicy;

  /**
   * Construct an RPCRequest.
   */
  public RPCRequest(Method method, Object[] parameters,
      SerializationPolicy serializationPolicy, int flags) {
    this(method, parameters, null, serializationPolicy, flags);
  }

  /**
   * Construct an RPCRequest.
   */
  public RPCRequest(Method method, Object[] parameters,
      RpcToken rpcToken, SerializationPolicy serializationPolicy, int flags) {
    this.method = method;
    this.parameters = parameters;
    this.rpcToken = rpcToken;
    this.serializationPolicy = serializationPolicy;
    this.flags = flags;
  }

  public int getFlags() {
    return flags;
  }

  /**
   * Get the request's method.
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Get the request's parameters.
   */
  public Object[] getParameters() {
    return parameters;
  }

  /**
   * Get the request's RPC token.
   */
  public RpcToken getRpcToken() {
    return rpcToken;
  }
  
  /**
   * Returns the {@link SerializationPolicy} used to decode this request. This
   * is also the <code>SerializationPolicy</code> that should be used to encode
   * responses.
   * 
   * @return {@link SerializationPolicy} used to decode this request
   */
  public SerializationPolicy getSerializationPolicy() {
    return serializationPolicy;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    StringBuilder callSignature = new StringBuilder();

    // Add the class and method names
    callSignature.append(method.getDeclaringClass().getName());
    callSignature.append('.');
    callSignature.append(method.getName());

    // Add the list of parameters
    callSignature.append('(');
    for (Object param : parameters) {
      if (param instanceof String) {
        // Put it within quotes and escape quotes, for readability
        callSignature.append('"');
        String strParam = (String) param;
        String escapedStrParam = strParam.replaceAll("\\\"", "\\\\\"");
        callSignature.append(escapedStrParam);
        callSignature.append('"');
      } else if (param == null) {
        callSignature.append("null");
      } else {
        // We assume that anyone who wants to use this method will implement
        // toString on his serializable objects.
        callSignature.append(param.toString());
      }
      callSignature.append(", ");
    }

    // Remove the last ", "
    int length = callSignature.length();
    callSignature.delete(length - 2, length);
    callSignature.append(')');

    return callSignature.toString();
  }
}
