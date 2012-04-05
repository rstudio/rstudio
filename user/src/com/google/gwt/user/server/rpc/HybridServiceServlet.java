/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.rpc.server.RpcServlet;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * This RemoteServiceServlet provides support for both legacy and deRPC clients
 * at the cost of additional runtime overhead and API complexity.
 */
public class HybridServiceServlet extends RpcServlet implements
    SerializationPolicyProvider {

  /**
   * Records permutations for which {@link #getClientOracle()} should return
   * <code>null</code>.
   */
  private final Set<String> legacyPermutations = new HashSet<String>();

  /**
   * A cache of moduleBaseURL and serialization policy strong name to
   * {@link SerializationPolicy}.
   */
  private final Map<String, SerializationPolicy> serializationPolicyCache = new HashMap<String, SerializationPolicy>();

  /**
   * This method will return <code>null</code> instead of throwing an exception.
   */
  @Override
  public ClientOracle getClientOracle() {
    String strongName = getPermutationStrongName();
    if (legacyPermutations.contains(strongName)) {
      return null;
    }
    try {
      return super.getClientOracle();
    } catch (SerializationException e) {
      legacyPermutations.add(strongName);
      return null;
    }
  }

  public final SerializationPolicy getSerializationPolicy(String moduleBaseURL,
      String strongName) {

    SerializationPolicy serializationPolicy = getCachedSerializationPolicy(
        moduleBaseURL, strongName);
    if (serializationPolicy != null) {
      return serializationPolicy;
    }

    serializationPolicy = doGetSerializationPolicy(getThreadLocalRequest(),
        moduleBaseURL, strongName);

    if (serializationPolicy == null) {
      // Failed to get the requested serialization policy; use the default
      log(
          "WARNING: Failed to get the SerializationPolicy '"
              + strongName
              + "' for module '"
              + moduleBaseURL
              + "'; a legacy, 1.3.3 compatible, serialization policy will be used.  You may experience SerializationExceptions as a result.");
      serializationPolicy = RPC.getDefaultSerializationPolicy();
    }

    // This could cache null or an actual instance. Either way we will not
    // attempt to lookup the policy again.
    putCachedSerializationPolicy(moduleBaseURL, strongName, serializationPolicy);

    return serializationPolicy;
  }

  @Override
  public void processCall(ClientOracle clientOracle, String payload,
      OutputStream stream) throws SerializationException {

    if (!Character.isDigit(payload.charAt(0))) {
      // Picking up null returned from getClientOracle()
      if (clientOracle == null) {
        throw new SerializationException("No ClientOracle for permutation "
            + getPermutationStrongName());
      }
      super.processCall(clientOracle, payload, stream);
    } else {
      String toReturn = processCall(payload);
      onAfterResponseSerialized(toReturn);

      try {
        stream.write(toReturn.getBytes(RPCServletUtils.CHARSET_UTF8));
      } catch (IOException e) {
        throw new SerializationException("Unable to commit bytes", e);
      }
    }
  }

  public String processCall(String payload) throws SerializationException {
    try {
      RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
      onAfterRequestDeserialized(rpcRequest);
      return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
          rpcRequest.getParameters(), rpcRequest.getSerializationPolicy(),
          rpcRequest.getFlags());
    } catch (IncompatibleRemoteServiceException ex) {
      log(
          "An IncompatibleRemoteServiceException was thrown while processing this call.",
          ex);
      return RPC.encodeResponseForFailure(null, ex);
    } catch (RpcTokenException tokenException) {
      log("An RpcTokenException was thrown while processing this call.",
          tokenException);
      return RPC.encodeResponseForFailure(null, tokenException);
    }
  }

  /**
   * Gets the {@link SerializationPolicy} for given module base URL and strong
   * name if there is one.
   * 
   * Override this method to provide a {@link SerializationPolicy} using an
   * alternative approach.
   * 
   * @param request the HTTP request being serviced
   * @param moduleBaseURL as specified in the incoming payload
   * @param strongName a strong name that uniquely identifies a serialization
   *          policy file
   * @return a {@link SerializationPolicy} for the given module base URL and
   *         strong name, or <code>null</code> if there is none
   */
  protected SerializationPolicy doGetSerializationPolicy(
      HttpServletRequest request, String moduleBaseURL, String strongName) {
    return RemoteServiceServlet.loadSerializationPolicy(this, request,
        moduleBaseURL, strongName);
  }

  /**
   * @param serializedResponse
   */
  protected void onAfterResponseSerialized(String serializedResponse) {
  }

  private SerializationPolicy getCachedSerializationPolicy(
      String moduleBaseURL, String strongName) {
    synchronized (serializationPolicyCache) {
      return serializationPolicyCache.get(moduleBaseURL + strongName);
    }
  }

  private void putCachedSerializationPolicy(String moduleBaseURL,
      String strongName, SerializationPolicy serializationPolicy) {
    synchronized (serializationPolicyCache) {
      serializationPolicyCache.put(moduleBaseURL + strongName,
          serializationPolicy);
    }
  }
}
