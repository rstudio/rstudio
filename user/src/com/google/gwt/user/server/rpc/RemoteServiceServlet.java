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
import com.google.gwt.user.client.rpc.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The servlet base class for your RPC service implementations that
 * automatically deserializes incoming requests from the client and serializes
 * outgoing responses for client/server RPCs.
 */
public class RemoteServiceServlet extends HttpServlet implements
    SerializationPolicyProvider {

  private final ThreadLocal<HttpServletRequest> perThreadRequest = new ThreadLocal<HttpServletRequest>();

  private final ThreadLocal<HttpServletResponse> perThreadResponse = new ThreadLocal<HttpServletResponse>();

  /**
   * A cache of moduleBaseURL and serialization policy strong name to
   * {@link SerializationPolicy}.
   */
  private final Map<String, SerializationPolicy> serializationPolicyCache = new HashMap<String, SerializationPolicy>();

  /**
   * The default constructor.
   */
  public RemoteServiceServlet() {
  }

  /**
   * Standard HttpServlet method: handle the POST.
   * 
   * This doPost method swallows ALL exceptions, logs them in the
   * ServletContext, and returns a GENERIC_FAILURE_MSG response with status code
   * 500.
   */
  @Override
  public final void doPost(HttpServletRequest request,
      HttpServletResponse response) {
    try {
      // Store the request & response objects in thread-local storage.
      //
      perThreadRequest.set(request);
      perThreadResponse.set(response);

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
      return;
    } catch (Throwable e) {
      // Give a subclass a chance to either handle the exception or rethrow it
      //
      doUnexpectedFailure(e);
    } finally {
      // null the thread-locals to avoid holding request/response
      //
      perThreadRequest.set(null);
      perThreadResponse.set(null);
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
              + "'; a legacy, 1.3.3 compatible, serialization policy will be used.  You may experience SerializationExceptions as a result.",
          null);
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
    try {
      RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
      onAfterRequestDeserialized(rpcRequest);
      return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
          rpcRequest.getParameters(), rpcRequest.getSerializationPolicy());
    } catch (IncompatibleRemoteServiceException ex) {
      log(
          "An IncompatibleRemoteServiceException was thrown while processing this call.",
          ex);
      return RPC.encodeResponseForFailure(null, ex);
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
    // The request can tell you the path of the web app relative to the
    // container root.
    String contextPath = request.getContextPath();

    String modulePath = null;
    if (moduleBaseURL != null) {
      try {
        modulePath = new URL(moduleBaseURL).getPath();
      } catch (MalformedURLException ex) {
        // log the information, we will default
        log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
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
      log(message, null);
    } else {
      // Strip off the context path from the module base URL. It should be a
      // strict prefix.
      String contextRelativePath = modulePath.substring(contextPath.length());

      String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath
          + strongName);

      // Open the RPC resource file read its contents.
      InputStream is = getServletContext().getResourceAsStream(
          serializationPolicyFilePath);
      try {
        if (is != null) {
          try {
            serializationPolicy = SerializationPolicyLoader.loadFromStream(is,
                null);
          } catch (ParseException e) {
            log("ERROR: Failed to parse the policy file '"
                + serializationPolicyFilePath + "'", e);
          } catch (IOException e) {
            log("ERROR: Could not read the policy file '"
                + serializationPolicyFilePath + "'", e);
          }
        } else {
          String message = "ERROR: The serialization policy file '"
              + serializationPolicyFilePath
              + "' was not found; did you forget to include it in this deployment?";
          log(message, null);
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
   * Override this method to control what should happen when an exception
   * escapes the {@link #processCall(String)} method. The default implementation
   * will log the failure and send a generic failure response to the client.
   * <p>
   * An "expected failure" is an exception thrown by a service method that is
   * declared in the signature of the service method. These exceptions are
   * serialized back to the client, and are not passed to this method. This
   * method is called only for exceptions or errors that are not part of the
   * service method's signature, or that result from SecurityExceptions,
   * SerializationExceptions, or other failures within the RPC framework.
   * <p>
   * Note that if the desired behavior is to both send the GENERIC_FAILURE_MSG
   * response AND to rethrow the exception, then this method should first send
   * the GENERIC_FAILURE_MSG response itself (using getThreadLocalResponse), and
   * then rethrow the exception. Rethrowing the exception will cause it to
   * escape into the servlet container.
   * 
   * @param e the exception which was thrown
   */
  protected void doUnexpectedFailure(Throwable e) {
    ServletContext servletContext = getServletContext();
    RPCServletUtils.writeResponseForUnexpectedFailure(servletContext,
        getThreadLocalResponse(), e);
  }

  /**
   * Gets the <code>HttpServletRequest</code> object for the current call. It is
   * stored thread-locally so that simultaneous invocations can have different
   * request objects.
   */
  protected final HttpServletRequest getThreadLocalRequest() {
    return perThreadRequest.get();
  }

  /**
   * Gets the <code>HttpServletResponse</code> object for the current call. It
   * is stored thread-locally so that simultaneous invocations can have
   * different response objects.
   */
  protected final HttpServletResponse getThreadLocalResponse() {
    return perThreadResponse.get();
  }

  /**
   * Override this method to examine the deserialized version of the request
   * before the call to the servlet method is made. The default implementation
   * does nothing and need not be called by subclasses.
   */
  protected void onAfterRequestDeserialized(RPCRequest rpcRequest) {
  }

  /**
   * Override this method to examine the serialized response that will be
   * returned to the client. The default implementation does nothing and need
   * not be called by subclasses.
   */
  protected void onAfterResponseSerialized(String serializedResponse) {
  }

  /**
   * Override this method to examine the serialized version of the request
   * payload before it is deserialized into objects. The default implementation
   * does nothing and need not be called by subclasses.
   */
  protected void onBeforeRequestDeserialized(String serializedRequest) {
  }

  /**
   * Override this method in order to control the parsing of the incoming
   * request. For example, you may want to bypass the check of the Content-Type
   * and character encoding headers in the request, as some proxies re-write the
   * request headers. Note that bypassing these checks may expose the servlet to
   * some cross-site vulnerabilities.
   * 
   * @param request the incoming request
   * @return the content of the incoming request encoded as a string.
   */
  protected String readContent(HttpServletRequest request)
      throws ServletException, IOException {
    return RPCServletUtils.readContentAsUtf8(request, true);
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
