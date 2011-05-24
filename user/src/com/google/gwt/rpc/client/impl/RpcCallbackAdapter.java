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
package com.google.gwt.rpc.client.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;

/**
 * Adapter from a {@link RequestCallback} interface to an {@link AsyncCallback}
 * interface.
 * 
 * For internal use only.
 * 
 * @param <T> the type parameter for the {@link AsyncCallback}
 */
public class RpcCallbackAdapter<T> implements RequestCallback {

  /**
   * {@link AsyncCallback} to notify or success or failure.
   */
  private final AsyncCallback<T> callback;

  /**
   * Used for stats recording.
   */
  private final String methodName;

  /**
   * Used for stats recording.
   */
  private final RpcStatsContext statsContext;

  private final SerializationStreamFactory streamFactory;

  public RpcCallbackAdapter(SerializationStreamFactory streamFactory,
      String methodName, RpcStatsContext statsContext, AsyncCallback<T> callback) {
    assert (streamFactory != null);
    assert (callback != null);

    this.streamFactory = streamFactory;
    this.callback = callback;
    this.methodName = methodName;
    this.statsContext = statsContext;
  }

  public void onError(Request request, Throwable exception) {
    callback.onFailure(exception);
  }

  @SuppressWarnings(value = {"unchecked", "unused"})
  public void onResponseReceived(Request request, Response response) {
    T result = null;
    Throwable caught = null;
    try {
      String encodedResponse = response.getText();
      int statusCode = response.getStatusCode();
      boolean toss = statsContext.isStatsAvailable()
          && statsContext.stats(
              statsContext.bytesStat(methodName, encodedResponse.length(), "responseReceived"));

      if (statusCode != Response.SC_OK) {
        caught = new StatusCodeException(statusCode, encodedResponse);
      } else if (encodedResponse == null) {
        // This can happen if the XHR is interrupted by the server dying
        caught = new InvocationException("No response payload from " + methodName);
      } else {
        result = (T) streamFactory.createStreamReader(encodedResponse).readObject();
      }
    } catch (RemoteException e) {
      caught = e.getCause();
    } catch (SerializationException e) {
      caught = new IncompatibleRemoteServiceException(
          "The response could not be deserialized", e);
    } catch (Throwable e) {
      caught = e;
    } finally {
      boolean toss = statsContext.isStatsAvailable()
          && statsContext.stats(statsContext.timeStat(methodName, "responseDeserialized"));
    }

    try {
      if (caught == null) {
        callback.onSuccess(result);
      } else {
        callback.onFailure(caught);
      }
    } finally {
      boolean toss = statsContext.isStatsAvailable()
          && statsContext.stats(statsContext.timeStat(methodName, "end"));
    }
  }
}
