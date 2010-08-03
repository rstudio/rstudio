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

import static com.google.gwt.user.client.rpc.RpcRequestBuilder.STRONG_NAME_HEADER;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An abstract base class containing utility methods.
 */
public abstract class AbstractRemoteServiceServlet extends HttpServlet {

  protected transient ThreadLocal<HttpServletRequest> perThreadRequest;
  protected transient ThreadLocal<HttpServletResponse> perThreadResponse;

  public AbstractRemoteServiceServlet() {
    super();
  }

  /**
   * Standard HttpServlet method: handle the POST. Delegates to
   * {@link #processPost(HttpServletRequest, HttpServletResponse)}.
   * 
   * This doPost method swallows ALL exceptions, logs them in the
   * ServletContext, and returns a GENERIC_FAILURE_MSG response with status code
   * 500.
   */
  @Override
  public final void doPost(HttpServletRequest request,
      HttpServletResponse response) {
    // Ensure the thread-local data fields have been initialized

    try {
      // Store the request & response objects in thread-local storage.
      //
      synchronized (this) {
        validateThreadLocalData();
        perThreadRequest.set(request);
        perThreadResponse.set(response);
      }

      processPost(request, response);

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

  /**
   * Override this method to control what should happen when an exception
   * escapes the {@link #doPost} method. The default implementation will log the
   * failure and send a generic failure response to the client.
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
    try {
      getThreadLocalResponse().reset();
    } catch (IllegalStateException ex) {
      /*
       * If we can't reset the request, the only way to signal that something
       * has gone wrong is to throw an exception from here. It should be the
       * case that we call the user's implementation code before emitting data
       * into the response, so the only time that gets tripped is if the object
       * serialization code blows up.
       */
      throw new RuntimeException("Unable to report failure", e);
    }
    ServletContext servletContext = getServletContext();
    RPCServletUtils.writeResponseForUnexpectedFailure(servletContext,
        getThreadLocalResponse(), e);
  }

  /**
   * Returns the strong name of the permutation, as reported by the client that
   * issued the request, or <code>null</code> if it could not be determined.
   * This information is encoded in the
   * {@value com.google.gwt.user.client.rpc.RpcRequestBuilder#STRONG_NAME_HEADER}
   * HTTP header.
   */
  protected final String getPermutationStrongName() {
    return getThreadLocalRequest().getHeader(STRONG_NAME_HEADER);
  }

  /**
   * Gets the <code>HttpServletRequest</code> object for the current call. It is
   * stored thread-locally so that simultaneous invocations can have different
   * request objects.
   */
  protected final HttpServletRequest getThreadLocalRequest() {
    synchronized (this) {
      validateThreadLocalData();
      return perThreadRequest.get();
    }
  }

  /**
   * Gets the <code>HttpServletResponse</code> object for the current call. It
   * is stored thread-locally so that simultaneous invocations can have
   * different response objects.
   */
  protected final HttpServletResponse getThreadLocalResponse() {
    synchronized (this) {
      validateThreadLocalData();
      return perThreadResponse.get();
    }
  }

  /**
   * Override this method to examine the deserialized version of the request
   * before the call to the servlet method is made. The default implementation
   * does nothing and need not be called by subclasses.
   * 
   * @param rpcRequest
   */
  protected void onAfterRequestDeserialized(RPCRequest rpcRequest) {
  }

  /**
   * Called by {@link #doPost} for type-specific processing of the request.
   * Because <code>doPost</code> swallows all <code>Throwables</code>, this
   * method may throw any exception the implementor wishes.
   */
  protected abstract void processPost(HttpServletRequest request,
      HttpServletResponse response) throws Throwable;

  /**
   * Override this method in order to control the parsing of the incoming
   * request. For example, you may want to bypass the check of the Content-Type
   * and character encoding headers in the request, as some proxies re-write the
   * request headers. Note that bypassing these checks may expose the servlet to
   * some cross-site vulnerabilities. Your implementation should comply with the
   * HTTP/1.1 specification, which includes handling both requests which include
   * a Content-Length header and requests utilizing <code>Transfer-Encoding:
   * chuncked</code>.
   * 
   * @param request the incoming request
   * @return the content of the incoming request encoded as a string.
   */
  protected String readContent(HttpServletRequest request)
      throws ServletException, IOException {
    return RPCServletUtils.readContentAsGwtRpc(request);
  }

  /**
   * Initializes the perThreadRequest and perThreadResponse fields if they are
   * null. This will occur the first time they are accessed after an instance of
   * this class is constructed or deserialized. This method should be called
   * from within a 'synchronized(this) {}' block in order to ensure that only
   * one thread creates the objects.
   */
  private void validateThreadLocalData() {
    if (perThreadRequest == null) {
      perThreadRequest = new ThreadLocal<HttpServletRequest>();
    }
    if (perThreadResponse == null) {
      perThreadResponse = new ThreadLocal<HttpServletResponse>();
    }
  }
}
