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
package com.google.gwt.requestfactory.client;

import static com.google.gwt.user.client.rpc.RpcRequestBuilder.STRONG_NAME_HEADER;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestTransport;
import com.google.gwt.user.client.Window.Location;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of {@link RequestTransport} that uses a
 * {@link RequestBuilder}.
 * <p>
 * This implementation will send {@link RequestEvent} objects to the
 * {@link EventBus} passed into the constructor to provide indication of
 * transport-level activity. When an HTTP request is sent, an event with a
 * {@link State#SENT} state and a {@code null} {@link Request} will be posted.
 * When an HTTP transport completes successfully, an event will be posted with
 * state {@link State#RECEIVED} and the {@link Request} object provided to the
 * internal {@link RequestCallback#onResponseReceived()}. If an HTTP transport
 * fails (e.g. due to network malfunction), an event with state
 * {@link State#RECEIVED} and a {@code null} {@link Request} will be sent. The
 * success or failure of the HTTP transport is wholly independent from whether
 * or not the application payload was successfully executed by the server.
 */
public class DefaultRequestTransport implements RequestTransport {

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
  private static Logger wireLogger = Logger.getLogger("WireActivityLogger");
  private static final String SERVER_ERROR = "Server Error";

  private final EventBus eventBus;
  private String requestUrl = GWT.getHostPageBaseURL() + URL;

  /**
   * Construct a DefaultRequestTransport.
   * 
   * @param eventBus the same EventBus passed into
   *          {@link RequestFactory#initialize(EventBus)
   *          RequestFactory.initialize}.
   */
  public DefaultRequestTransport(EventBus eventBus) {
    if (eventBus == null) {
      throw new IllegalArgumentException("eventBus must not be null");
    }
    this.eventBus = eventBus;
  }

  /**
   * Returns the current URL used by this transport.
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
      postRequestEvent(State.SENT, null);
    } catch (RequestException e) {
      wireLogger.log(Level.SEVERE, SERVER_ERROR + " (" + e.getMessage() + ")",
          e);
    }
  }

  /**
   * Override the default URL used by this transport.
   */
  public void setRequestUrl(String url) {
    this.requestUrl = url;
  }

  /**
   * Override to change the headers sent in the HTTP request.
   */
  protected void configureRequestBuilder(RequestBuilder builder) {
    builder.setHeader("Content-Type", RequestFactory.JSON_CONTENT_TYPE_UTF8);
    builder.setHeader("pageurl", Location.getHref());
    builder.setHeader(STRONG_NAME_HEADER, GWT.getPermutationStrongName());
  }

  /**
   * Constructs a RequestBuilder using the {@link RequestBuilder#POST} method
   * sent to the URL returned from {@link #getRequestUrl()}.
   */
  protected RequestBuilder createRequestBuilder() {
    return new RequestBuilder(RequestBuilder.POST, getRequestUrl());
  }

  /**
   * Creates a RequestCallback that maps the HTTP response onto the
   * {@link com.google.gwt.requestfactory.shared.RequestTransport.TransportReceiver
   * TransportReceiver} interface.
   */
  protected RequestCallback createRequestCallback(
      final TransportReceiver receiver) {
    return new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        postRequestEvent(State.RECEIVED, null);
        wireLogger.log(Level.SEVERE, SERVER_ERROR, exception);
        receiver.onTransportFailure(exception.getMessage());
      }

      public void onResponseReceived(Request request, Response response) {
        wireLogger.finest("Response received");
        try {
          if (200 == response.getStatusCode()) {
            String text = response.getText();
            receiver.onTransportSuccess(text);
          } else if (Response.SC_UNAUTHORIZED == response.getStatusCode()) {
            String message = "Need to log in";
            wireLogger.finest(message);
            receiver.onTransportFailure(message);
          } else if (response.getStatusCode() > 0) {
            /*
             * During the redirection for logging in, we get a response with no
             * status code, but it's not an error, so we only log errors with
             * bad status codes here.
             */
            String message = SERVER_ERROR + " " + response.getStatusCode()
                + " " + response.getText();
            wireLogger.severe(message);
            receiver.onTransportFailure(message);
          }
        } finally {
          postRequestEvent(State.RECEIVED, response);
        }
      }
    };
  }

  private void postRequestEvent(State received, Response response) {
    eventBus.fireEvent(new RequestEvent(received, response));
  }
}
