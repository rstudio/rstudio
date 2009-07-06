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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;

/**
 * This class encapsulates the logic necessary to configure a RequestBuilder for
 * use with an RPC proxy object. Users who wish to alter the specifics of the
 * HTTP requests issued by RPC proxy objects may override the protected
 * <code>doXyz</code> methods and pass an instance of the subclass to
 * {@link ServiceDefTarget#setRpcRequestBuilder}.
 */
public class RpcRequestBuilder {
  /**
   * Used by {@link #doSetContentType}.
   */
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  /**
   * Used by {@link #doFinish}.
   */
  /*
   * NB: Also used by RpcServlet.
   */
  public static final String MODULE_BASE_HEADER = "X-GWT-Module-Base";

  /**
   * Used by {@link #doFinish}.
   */
  /*
   * NB: Also used by AbstractRemoteServiceServlet.
   */
  public static final String STRONG_NAME_HEADER = "X-GWT-Permutation";

  /**
   * Not exposed directly to the subclass.
   */
  private RequestBuilder builder;

  /**
   * Initialize the RpcRequestBuilder. This method must be called before any of
   * the other methods in this class may be called. Calling <code>create</code>
   * before calling {@link #finish()} will reset the state of the
   * RpcRequestBuilder.
   * <p>
   * This method delegates to {@link #doCreate} to instantiate the
   * RequestBuilder.
   * 
   * @param serviceEntryPoint The URL entry point
   * @return <code>this</code>
   * @see ServiceDefTarget#setServiceEntryPoint(String)
   */
  public final RpcRequestBuilder create(String serviceEntryPoint) {
    builder = doCreate(serviceEntryPoint);
    assert builder != null : "doCreate failed to return a RequestBuilder";
    return this;
  }

  /**
   * This method must be called to return the RequestBuilder that the RPC
   * request will be made with.
   * <p>
   * This method will call {@link #doFinish} before returning the current
   * RequestBuilder.
   */
  public final RequestBuilder finish() {
    try {
      assert builder != null : "Call create() first";
      doFinish(builder);
      return builder;
    } finally {
      builder = null;
    }
  }

  /**
   * Sets the RequestCallback to be used by the RequestBuilder. Delegates to
   * {@link #doSetCallback}.
   * 
   * @param callback the RequestCallback to be used by the RequestBuilder
   * @return <code>this</code>
   */
  public final RpcRequestBuilder setCallback(RequestCallback callback) {
    assert builder != null : "Call create() first";
    doSetCallback(builder, callback);
    return this;
  }

  /**
   * Sets the MIME content type to be used by the RequestBuilder. Delegates to
   * {@link #doSetContentType}.
   * 
   * @param contentType the MIME content type to be used in the request
   * @return <code>this</code>
   */
  public final RpcRequestBuilder setContentType(String contentType) {
    assert builder != null : "Call create() first";
    doSetContentType(builder, contentType);
    return this;
  }

  /**
   * Sets the request data to be sent in the request. Delegates to
   * {@link #doSetRequestData}.
   * 
   * @param data the data to send
   * @return <code>this</code>
   */
  public final RpcRequestBuilder setRequestData(String data) {
    assert builder != null : "Call create() first";
    doSetRequestData(builder, data);
    return this;
  }

  /**
   * Sets the request id of the request. Delegates to {@link #doSetRequestId}.
   * 
   * @param id the issue number of the request
   * @return <code>this</code>
   */
  public final RpcRequestBuilder setRequestId(int id) {
    assert builder != null : "Call create() first";
    doSetRequestId(builder, id);
    return this;
  }

  /**
   * Called by {@link #create} to instantiate the RequestBuilder object.
   * <p>
   * The default implementation creates a <code>POST</code> RequestBuilder with
   * the given entry point.
   * 
   * @param serviceEntryPoint the URL to which the request should be issued
   * @return the RequestBuilder that should be ultimately passed to the
   *         RpcRequestBuilder's caller.
   */
  protected RequestBuilder doCreate(String serviceEntryPoint) {
    return new RequestBuilder(RequestBuilder.POST, serviceEntryPoint);
  }

  /**
   * Called by {@link #finish()} prior to returning the RequestBuilder to the
   * caller.
   * <p>
   * The default implementation sets the {@value #STRONG_NAME_HEADER} header to
   * the value returned by {@link GWT#getPermutationStrongName()}.
   * 
   * @param rb The RequestBuilder that is currently being configured
   */
  protected void doFinish(RequestBuilder rb) {
    rb.setHeader(STRONG_NAME_HEADER, GWT.getPermutationStrongName());
    rb.setHeader(MODULE_BASE_HEADER, GWT.getModuleBaseURL());
  }

  /**
   * Called by {@link #setCallback}.
   * <p>
   * The default implementation calls
   * {@link RequestBuilder#setCallback(RequestCallback)}.
   * 
   * @param rb the RequestBuilder that is currently being configured
   * @param callback the user-provided callback
   */
  protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
    rb.setCallback(callback);
  }

  /**
   * Called by {@link #setContentType}.
   * <p>
   * The default implementation sets the {@value #CONTENT_TYPE_HEADER} header to
   * the value specified by <code>contentType</code> by calling
   * {@link RequestBuilder#setHeader(String, String)}.
   * 
   * @param rb the RequestBuilder that is currently being configured
   * @param contentType the desired MIME type of the request's contents
   */
  protected void doSetContentType(RequestBuilder rb, String contentType) {
    rb.setHeader(CONTENT_TYPE_HEADER, contentType);
  }

  /**
   * Called by {@link #setRequestData}.
   * <p>
   * The default implementation invokes
   * {@link RequestBuilder#setRequestData(String)}.
   * 
   * @param rb the RequestBuilder that is currently being configured
   * @param data the data to send
   */
  protected void doSetRequestData(RequestBuilder rb, String data) {
    rb.setRequestData(data);
  }

  /**
   * Called by {@link #setRequestId}.
   * <p>
   * The default implementation is a no-op.
   * 
   * @param rb the RequestBuilder that is currently being configured
   * @param id the request's issue id
   */
  protected void doSetRequestId(RequestBuilder rb, int id) {
  }
}
