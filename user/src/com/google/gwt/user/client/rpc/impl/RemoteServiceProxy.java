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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;

/**
 * Superclass for client-side
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} proxies.
 * 
 * For internal use only.
 */
public abstract class RemoteServiceProxy implements SerializationStreamFactory,
    ServiceDefTarget {

  /**
   * The content type to be used in HTTP requests.
   */
  private static final String RPC_CONTENT_TYPE = "text/x-gwt-rpc; charset=utf-8";

  /**
   * A global id to track any given request.
   */
  private static int requestId;

  public static native JavaScriptObject bytesStat(String method, int count,
      int bytes, String eventType) /*-{
    var stat = @com.google.gwt.user.client.rpc.impl.RemoteServiceProxy::timeStat(Ljava/lang/String;ILjava/lang/String;)(method, count, eventType);
    stat.bytes = bytes;
    return stat;
  }-*/;

  /**
   * Indicates if RPC statistics should be gathered.
   */
  public static native boolean isStatsAvailable() /*-{
    return !!$stats;
  }-*/;

  /**
   * Always use this as {@link #isStatsAvailable()} &amp;&amp;
   * {@link #stats(JavaScriptObject)}.
   */
  public static native boolean stats(JavaScriptObject data) /*-{
    return $stats(data);
  }-*/;

  public static native JavaScriptObject timeStat(String method, int count,
      String eventType) /*-{
    return {
      moduleName: @com.google.gwt.core.client.GWT::getModuleName()(),
      sessionId: $sessionId,
      subSystem: 'rpc',
      evtGroup: count,
      method: method,
      millis: (new Date()).getTime(),
      type: eventType
    };
  }-*/;

  protected static int getNextRequestId() {
    return requestId++;
  }

  /**
   * @deprecated Use {@link RpcRequestBuilder} instead.
   */
  @Deprecated
  protected static int getRequestId() {
    return requestId;
  }

  /**
   * Return <code>true</code> if the encoded response contains a value returned
   * by the method invocation.
   * 
   * @param encodedResponse
   * @return <code>true</code> if the encoded response contains a value returned
   *         by the method invocation
   */
  static boolean isReturnValue(String encodedResponse) {
    return encodedResponse.startsWith("//OK");
  }

  /**
   * Return <code>true</code> if the encoded response contains a checked
   * exception that was thrown by the method invocation.
   * 
   * @param encodedResponse
   * @return <code>true</code> if the encoded response contains a checked
   *         exception that was thrown by the method invocation
   */
  static boolean isThrownException(String encodedResponse) {
    return encodedResponse.startsWith("//EX");
  }

  /**
   * Returns a string that encodes the result of a method invocation.
   * Effectively, this just removes any headers from the encoded response.
   * 
   * @param encodedResponse
   * @return string that encodes the result of a method invocation
   */
  private static String getEncodedInstance(String encodedResponse) {
    if (isReturnValue(encodedResponse) || isThrownException(encodedResponse)) {
      return encodedResponse.substring(4);
    }

    return encodedResponse;
  }

  /**
   * The module base URL as specified during construction.
   */
  private final String moduleBaseURL;

  /**
   * URL of the {@link com.google.gwt.user.client.rpc.RemoteService
   * RemoteService}.
   */
  private String remoteServiceURL;

  private RpcRequestBuilder rpcRequestBuilder;

  /**
   * The name of the serialization policy file specified during construction.
   */
  private final String serializationPolicyName;

  /**
   * The {@link Serializer} instance used to serialize and deserialize
   * instances.
   */
  private final Serializer serializer;

  protected RemoteServiceProxy(String moduleBaseURL,
      String remoteServiceRelativePath, String serializationPolicyName,
      Serializer serializer) {
    this.moduleBaseURL = moduleBaseURL;
    if (remoteServiceRelativePath != null) {
      /*
       * If the module relative URL is not null we set the remote service URL to
       * be the module base URL plus the module relative remote service URL.
       * Otherwise an explicit call to
       * ServiceDefTarget.setServiceEntryPoint(String) is required.
       */
      this.remoteServiceURL = moduleBaseURL + remoteServiceRelativePath;
    }
    this.serializer = serializer;
    this.serializationPolicyName = serializationPolicyName;
  }

  /**
   * Returns a {@link com.google.gwt.user.client.rpc.SerializationStreamReader
   * SerializationStreamReader} that is ready for reading.
   * 
   * @param encoded string that encodes the response of an RPC request
   * @return {@link com.google.gwt.user.client.rpc.SerializationStreamReader
   *         SerializationStreamReader} that is ready for reading
   * @throws SerializationException
   */
  public SerializationStreamReader createStreamReader(String encoded)
      throws SerializationException {
    ClientSerializationStreamReader clientSerializationStreamReader = new ClientSerializationStreamReader(
        serializer);
    clientSerializationStreamReader.prepareToRead(getEncodedInstance(encoded));
    return clientSerializationStreamReader;
  }

  /**
   * Returns a {@link com.google.gwt.user.client.rpc.SerializationStreamWriter
   * SerializationStreamWriter} that has had
   * {@link ClientSerializationStreamWriter#prepareToWrite()} called on it and
   * it has already had had the name of the remote service interface written as
   * well.
   * 
   * @return {@link com.google.gwt.user.client.rpc.SerializationStreamWriter
   *         SerializationStreamWriter} that has had
   *         {@link ClientSerializationStreamWriter#prepareToWrite()} called on
   *         it and it has already had had the name of the remote service
   *         interface written as well
   */
  public SerializationStreamWriter createStreamWriter() {
    ClientSerializationStreamWriter clientSerializationStreamWriter = new ClientSerializationStreamWriter(
        serializer, moduleBaseURL, serializationPolicyName);
    clientSerializationStreamWriter.prepareToWrite();
    return clientSerializationStreamWriter;
  }
  
  public String getSerializationPolicyName() {
    return serializationPolicyName;
  }

  /**
   * @see ServiceDefTarget#getServiceEntryPoint()
   */
  public String getServiceEntryPoint() {
    return remoteServiceURL;
  }

  public void setRpcRequestBuilder(RpcRequestBuilder builder) {
    this.rpcRequestBuilder = builder;
  }

  /**
   * @see ServiceDefTarget#setServiceEntryPoint(String)
   */
  public void setServiceEntryPoint(String url) {
    this.remoteServiceURL = url;
  }

  protected <T> RequestCallback doCreateRequestCallback(
      ResponseReader responseReader, String methodName, int invocationCount,
      AsyncCallback<T> callback) {
    return new RequestCallbackAdapter<T>(this, methodName, invocationCount,
        callback, responseReader);
  }

  /**
   * Performs a remote service method invocation. This method is called by
   * generated proxy classes.
   * 
   * @param <T> return type for the AsyncCallback
   * @param responseReader instance used to read the return value of the
   *          invocation
   * @param requestData payload that encodes the addressing and arguments of the
   *          RPC call
   * @param callback callback handler
   * 
   * @return a {@link Request} object that can be used to track the request
   */
  protected <T> Request doInvoke(ResponseReader responseReader,
      String methodName, int invocationCount, String requestData,
      AsyncCallback<T> callback) {

    RequestBuilder rb = doPrepareRequestBuilderImpl(responseReader, methodName,
        invocationCount, requestData, callback);

    try {
      return rb.send();
    } catch (RequestException ex) {
      InvocationException iex = new InvocationException(
          "Unable to initiate the asynchronous service invocation -- check the network connection",
          ex);
      callback.onFailure(iex);
    } finally {
      if (RemoteServiceProxy.isStatsAvailable()
          && RemoteServiceProxy.stats(RemoteServiceProxy.bytesStat(methodName,
              invocationCount, requestData.length(), "requestSent"))) {
      }
    }
    return null;
  }

  /**
   * Configures a RequestBuilder to send an RPC request when the RequestBuilder
   * is intended to be returned through the asynchronous proxy interface.
   * 
   * @param <T> return type for the AsyncCallback
   * @param responseReader instance used to read the return value of the
   *          invocation
   * @param requestData payload that encodes the addressing and arguments of the
   *          RPC call
   * @param callback callback handler
   * 
   * @return a RequestBuilder object that is ready to have its
   *         {@link RequestBuilder#send()} method invoked.
   */
  protected <T> RequestBuilder doPrepareRequestBuilder(
      ResponseReader responseReader, String methodName, int invocationCount,
      String requestData, AsyncCallback<T> callback) {

    RequestBuilder rb = doPrepareRequestBuilderImpl(responseReader, methodName,
        invocationCount, requestData, callback);

    return rb;
  }

  /**
   * Configures a RequestBuilder to send an RPC request.
   * 
   * @param <T> return type for the AsyncCallback
   * @param responseReader instance used to read the return value of the
   *          invocation
   * @param requestData payload that encodes the addressing and arguments of the
   *          RPC call
   * @param callback callback handler
   * 
   * @return a RequestBuilder object that is ready to have its
   *         {@link RequestBuilder#send()} method invoked.
   */
  private <T> RequestBuilder doPrepareRequestBuilderImpl(
      ResponseReader responseReader, String methodName, int invocationCount,
      String requestData, AsyncCallback<T> callback) {

    if (getServiceEntryPoint() == null) {
      throw new NoServiceEntryPointSpecifiedException();
    }

    RequestCallback responseHandler = doCreateRequestCallback(responseReader,
        methodName, invocationCount, callback);

    ensureRpcRequestBuilder();

    rpcRequestBuilder.create(getServiceEntryPoint());
    rpcRequestBuilder.setCallback(responseHandler);
    rpcRequestBuilder.setContentType(RPC_CONTENT_TYPE);
    rpcRequestBuilder.setRequestData(requestData);
    rpcRequestBuilder.setRequestId(invocationCount);
    return rpcRequestBuilder.finish();
  }

  private void ensureRpcRequestBuilder() {
    if (rpcRequestBuilder == null) {
      rpcRequestBuilder = new RpcRequestBuilder();
    }
  }
}
