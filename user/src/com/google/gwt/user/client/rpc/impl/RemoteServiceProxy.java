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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
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
   * Return <code>true</code> if the encoded response contains a value
   * returned by the method invocation.
   * 
   * @param encodedResponse
   * @return <code>true</code> if the encoded response contains a value
   *         returned by the method invocation
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
   * URL of the
   * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}.
   */
  private String remoteServiceURL;

  /**
   * The name of the serialization policy file specified during construction.
   */
  private final String serializationPolicyName;

  /**
   * The {@link Serializer} instance used to serialize and deserialize
   * instances.
   */
  private final Serializer serializer;

  protected RemoteServiceProxy(String moduleBaseURL, String remoteServiceURL,
      String serializationPolicyName, Serializer serializer) {
    this.moduleBaseURL = moduleBaseURL;
    this.remoteServiceURL = remoteServiceURL;
    this.serializer = serializer;
    this.serializationPolicyName = serializationPolicyName;
  }

  /**
   * Returns a
   * {@link com.google.gwt.user.client.rpc.SerializationStreamReader SerializationStreamReader}
   * that is ready for reading.
   * 
   * @param encoded string that encodes the response of an RPC request
   * @return {@link com.google.gwt.user.client.rpc.SerializationStreamReader SerializationStreamReader}
   *         that is ready for reading
   * @throws SerializationException
   */
  public ClientSerializationStreamReader createStreamReader(String encoded)
      throws SerializationException {
    ClientSerializationStreamReader clientSerializationStreamReader = new ClientSerializationStreamReader(
        serializer);
    clientSerializationStreamReader.prepareToRead(getEncodedInstance(encoded));
    return clientSerializationStreamReader;
  }

  /**
   * Returns a
   * {@link com.google.gwt.user.client.rpc.SerializationStreamWriter SerializationStreamWriter}
   * that has had {@link ClientSerializationStreamWriter#prepareToWrite()}
   * called on it and it has already had had the name of the remote service
   * interface written as well.
   * 
   * @return {@link com.google.gwt.user.client.rpc.SerializationStreamWriter SerializationStreamWriter}
   *         that has had
   *         {@link ClientSerializationStreamWriter#prepareToWrite()} called on
   *         it and it has already had had the name of the remote service
   *         interface written as well
   */
  public ClientSerializationStreamWriter createStreamWriter() {
    ClientSerializationStreamWriter clientSerializationStreamWriter = new ClientSerializationStreamWriter(
        serializer, moduleBaseURL, serializationPolicyName);
    clientSerializationStreamWriter.prepareToWrite();
    return clientSerializationStreamWriter;
  }

  /**
   * @see ServiceDefTarget#getServiceEntryPoint()
   */
  public String getServiceEntryPoint() {
    return remoteServiceURL;
  }

  /**
   * @see ServiceDefTarget#setServiceEntryPoint(String)
   */
  public void setServiceEntryPoint(String url) {
    this.remoteServiceURL = url;
  }

  /**
   * Performs a remote service method invocation.
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
      String requestData, AsyncCallback<T> callback) {

    if (getServiceEntryPoint() == null) {
      throw new NoServiceEntryPointSpecifiedException();
    }

    RequestCallbackAdapter<T> responseHandler = new RequestCallbackAdapter<T>(
        this, callback, responseReader);
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST,
        getServiceEntryPoint());
    try {
      return rb.sendRequest(requestData, responseHandler);
    } catch (RequestException ex) {
      InvocationException iex = new InvocationException(
          "Unable to initiate the asynchronous service invocation -- check the network connection",
          ex);
      callback.onFailure(iex);
    }

    return null;
  }
}
