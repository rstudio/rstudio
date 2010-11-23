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

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The servlet base class for your RPC service implementations that
 * automatically deserializes incoming requests from the client and serializes
 * outgoing responses for client/server RPCs.
 */
public class RemoteServiceServlet extends AbstractRemoteServiceServlet
    implements SerializationPolicyProvider {

  /**
   * Used by HybridServiceServlet.
   */
  static SerializationPolicy loadSerializationPolicy(HttpServlet servlet,
      HttpServletRequest request, String moduleBaseURL, String strongName) {
    // The request can tell you the path of the web app relative to the
    // container root.
    String contextPath = request.getContextPath();

    String modulePath = null;
    if (moduleBaseURL != null) {
      try {
        modulePath = new URL(moduleBaseURL).getPath();
      } catch (MalformedURLException ex) {
        // log the information, we will default
        servlet.log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
      }
    }

    SerializationPolicy serializationPolicy = null;

    /*
     * Check that the module path must be in the same web app as the servlet
     * itself. If you need to implement a scheme different than this, override
     * this method.
     */
    if (modulePath == null || !modulePath.startsWith(contextPath)) {
      String message = "ERROR: The module path requested, "
          + modulePath
          + ", is not in the same web application as this servlet, "
          + contextPath
          + ".  Your module may not be properly configured or your client and server code maybe out of date.";
      servlet.log(message);
    } else {
      // Strip off the context path from the module base URL. It should be a
      // strict prefix.
      String contextRelativePath = modulePath.substring(contextPath.length());

      String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath
          + strongName);

      // Open the RPC resource file and read its contents.
      InputStream is = servlet.getServletContext().getResourceAsStream(
          serializationPolicyFilePath);
      try {
        if (is != null) {
          try {
            serializationPolicy = SerializationPolicyLoader.loadFromStream(is,
                null);
          } catch (ParseException e) {
            servlet.log("ERROR: Failed to parse the policy file '"
                + serializationPolicyFilePath + "'", e);
          } catch (IOException e) {
            servlet.log("ERROR: Could not read the policy file '"
                + serializationPolicyFilePath + "'", e);
          }
        } else {
          String message = "ERROR: The serialization policy file '"
              + serializationPolicyFilePath
              + "' was not found; did you forget to include it in this deployment?";
          servlet.log(message);
        }
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // Ignore this error
          }
        }
      }
    }

    return serializationPolicy;
  }

  /**
   * A cache of moduleBaseURL and serialization policy strong name to
   * {@link SerializationPolicy}.
   */
  private final Map<String, SerializationPolicy> serializationPolicyCache = new HashMap<String, SerializationPolicy>();

  /**
   * The implementation of the service.
   */
  private final Object delegate;

  /**
   * The default constructor used by service implementations that
   * extend this class.  The servlet will delegate AJAX requests to
   * the appropriate method in the subclass.
   */
  public RemoteServiceServlet() {
    this.delegate = this;
  }

  /**
   * The wrapping constructor used by service implementations that are
   * separate from this class.  The servlet will delegate AJAX
   * requests to the appropriate method in the given object.
   */
  public RemoteServiceServlet(Object delegate) {
    this.delegate = delegate;
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

  /**
   * Process a call originating from the given request. Uses the
   * {@link RPC#invokeAndEncodeResponse(Object, java.lang.reflect.Method, Object[])}
   * method to do the actual work.
   * <p>
   * Subclasses may optionally override this method to handle the payload in any
   * way they desire (by routing the request to a framework component, for
   * instance). The {@link HttpServletRequest} and {@link HttpServletResponse}
   * can be accessed via the {@link #getThreadLocalRequest()} and
   * {@link #getThreadLocalResponse()} methods.
   * </p>
   * This is public so that it can be unit tested easily without HTTP.
   * 
   * @param payload the UTF-8 request payload
   * @return a string which encodes either the method's return, a checked
   *         exception thrown by the method, or an
   *         {@link IncompatibleRemoteServiceException}
   * @throws SerializationException if we cannot serialize the response
   * @throws UnexpectedException if the invocation throws a checked exception
   *           that is not declared in the service method's signature
   * @throws RuntimeException if the service method throws an unchecked
   *           exception (the exception will be the one thrown by the service)
   */
  public String processCall(String payload) throws SerializationException {
    // First, check for possible XSRF situation
    checkPermutationStrongName();

    try {
      RPCRequest rpcRequest = RPC.decodeRequest(payload, delegate.getClass(), this);
      onAfterRequestDeserialized(rpcRequest);
      return RPC.invokeAndEncodeResponse(delegate, rpcRequest.getMethod(),
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
   * Standard HttpServlet method: handle the POST.
   * 
   * This doPost method swallows ALL exceptions, logs them in the
   * ServletContext, and returns a GENERIC_FAILURE_MSG response with status code
   * 500.
   * 
   * @throws ServletException
   * @throws SerializationException
   */
  @Override
  public final void processPost(HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException,
      SerializationException {
    // Read the request fully.
    //
    String requestPayload = readContent(request);

    // Let subclasses see the serialized request.
    //
    onBeforeRequestDeserialized(requestPayload);

    // Invoke the core dispatching logic, which returns the serialized
    // result.
    //
    String responsePayload = processCall(requestPayload);

    // Let subclasses see the serialized response.
    //
    onAfterResponseSerialized(responsePayload);

    // Write the response.
    //
    writeResponse(request, response, responsePayload);
  }

  /**
   * This method is called by {@link #processCall(String)} and will throw a
   * SecurityException if {@link #getPermutationStrongName()} returns
   * <code>null</code>. This method can be overridden to be a no-op if there are
   * clients that are not expected to provide the
   * {@value com.google.gwt.user.client.rpc.RpcRequestBuilder#STRONG_NAME_HEADER}
   * header.
   * 
   * @throws SecurityException if {@link #getPermutationStrongName()} returns
   *           <code>null</code>
   */
  protected void checkPermutationStrongName() throws SecurityException {
    if (getPermutationStrongName() == null) {
      throw new SecurityException(
          "Blocked request without GWT permutation header (XSRF attack?)");
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
    return loadSerializationPolicy(this, request, moduleBaseURL, strongName);
  }

  /**
   * Override this method to examine the serialized response that will be
   * returned to the client. The default implementation does nothing and need
   * not be called by subclasses.
   * 
   * @param serializedResponse
   */
  protected void onAfterResponseSerialized(String serializedResponse) {
  }

  /**
   * Override this method to examine the serialized version of the request
   * payload before it is deserialized into objects. The default implementation
   * does nothing and need not be called by subclasses.
   * 
   * @param serializedRequest
   */
  protected void onBeforeRequestDeserialized(String serializedRequest) {
  }

  /**
   * Determines whether the response to a given servlet request should or should
   * not be GZIP compressed. This method is only called in cases where the
   * requester accepts GZIP encoding.
   * <p>
   * This implementation currently returns <code>true</code> if the response
   * string's estimated byte length is longer than 256 bytes. Subclasses can
   * override this logic.
   * </p>
   * 
   * @param request the request being served
   * @param response the response that will be written into
   * @param responsePayload the payload that is about to be sent to the client
   * @return <code>true</code> if responsePayload should be GZIP compressed,
   *         otherwise <code>false</code>.
   */
  protected boolean shouldCompressResponse(HttpServletRequest request,
      HttpServletResponse response, String responsePayload) {
    return RPCServletUtils.exceedsUncompressedContentLengthLimit(responsePayload);
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

  private void writeResponse(HttpServletRequest request,
      HttpServletResponse response, String responsePayload) throws IOException {
    boolean gzipEncode = RPCServletUtils.acceptsGzipEncoding(request)
        && shouldCompressResponse(request, response, responsePayload);

    RPCServletUtils.writeResponse(getServletContext(), response,
        responsePayload, gzipEncode);
  }
}
