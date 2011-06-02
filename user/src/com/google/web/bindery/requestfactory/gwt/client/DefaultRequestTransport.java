/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.requestfactory.gwt.client;

import static com.google.gwt.user.client.rpc.RpcRequestBuilder.STRONG_NAME_HEADER;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window.Location;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of {@link RequestTransport} that uses a
 * {@link RequestBuilder}.
 */
public class DefaultRequestTransport implements RequestTransport {
  private static final String SERVER_ERROR = "Server Error";

  /**
   * The default URL for a DefaultRequestTransport is
   * <code>{@link GWT#getHostPageBaseURL()} + {@value #URL}</code> which may be
   * overridden by calling {@link #setRequestUrl(String)}.
   */
  public static final String URL = "gwtRequest";

  /*
   * A separate logger for wire activity, which does not get logged by the
   * remote log handler, so we avoid infinite loops. All log messages that could
   * happen every time a request is made from the server should be logged to
   * this logger.
   */
  private static final Logger wireLogger = Logger.getLogger("WireActivityLogger");

  private String requestUrl = GWT.getHostPageBaseURL() + URL;

  /**
   * Returns the current URL used by this transport.
   * 
   * @return the URL as a String
   * @see #setRequestUrl(String)
   */
  public String getRequestUrl() {
    return requestUrl;
  }

  public void send(String payload, TransportReceiver receiver) {
    RequestBuilder builder = createRequestBuilder();
    configureRequestBuilder(builder);

    builder.setRequestData(payload);
    builder.setCallback(createRequestCallback(receiver));

    try {
      wireLogger.finest("Sending fire request");
      builder.send();
    } catch (RequestException e) {
      wireLogger.log(Level.SEVERE, SERVER_ERROR + " (" + e.getMessage() + ")", e);
    }
  }

  /**
   * Override the default URL used by this transport.
   * 
   * @param url a String URL
   * @see #getRequestUrl()
   */
  public void setRequestUrl(String url) {
    this.requestUrl = url;
  }

  /**
   * Override to change the headers sent in the HTTP request.
   * 
   * @param builder a {@link RequestBuilder} instance
   */
  protected void configureRequestBuilder(RequestBuilder builder) {
    builder.setHeader("Content-Type", RequestFactory.JSON_CONTENT_TYPE_UTF8);
    builder.setHeader("pageurl", Location.getHref());
    builder.setHeader(STRONG_NAME_HEADER, GWT.getPermutationStrongName());
  }

  /**
   * Constructs a {@link RequestBuilder} using the {@link RequestBuilder#POST}
   * method sent to the URL returned from {@link #getRequestUrl()}.
   * 
   * @return a {@link RequestBuilder} instance
   */
  protected RequestBuilder createRequestBuilder() {
    return new RequestBuilder(RequestBuilder.POST, getRequestUrl());
  }

  /**
   * Creates a RequestCallback that maps the HTTP response onto the
   * {@link com.google.web.bindery.requestfactory.shared.RequestTransport.TransportReceiver
   * TransportReceiver} interface.
   * 
   * @param receiver a
   *          {@link com.google.web.bindery.requestfactory.shared.RequestTransport.TransportReceiver
   *          TransportReceiver}
   * @return a {@link RequestCallback} instance
   */
  protected RequestCallback createRequestCallback(final TransportReceiver receiver) {
    return new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        wireLogger.log(Level.SEVERE, SERVER_ERROR, exception);
        receiver.onTransportFailure(new ServerFailure(exception.getMessage()));
      }

      public void onResponseReceived(Request request, Response response) {
        wireLogger.finest("Response received");
        if (Response.SC_OK == response.getStatusCode()) {
          String text = response.getText();
          receiver.onTransportSuccess(text);
        } else {
          String message = SERVER_ERROR + " " + response.getStatusCode() + " " + response.getText();
          wireLogger.severe(message);
          receiver.onTransportFailure(new ServerFailure(message));
        }
      }
    };
  }
}
