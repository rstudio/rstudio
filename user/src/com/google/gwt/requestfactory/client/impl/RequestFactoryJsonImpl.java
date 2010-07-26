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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.requestfactory.client.RequestFactoryLogHandler;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SyncRequest;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.valuestore.client.DeltaValueStoreJsonImpl;
import com.google.gwt.valuestore.client.ValueStoreJsonImpl;
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.valuestore.shared.impl.RecordToTypeMap;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Base implementation of RequestFactory.
 */
public abstract class RequestFactoryJsonImpl implements RequestFactory {

  private static Logger logger =
    Logger.getLogger(RequestFactory.class.getName());
  
  private static String SERVER_ERROR = "Server Error";
  
  private HandlerManager handlerManager;
  
  private ValueStoreJsonImpl valueStore;
  
  public void fire(final RequestObject<?> requestObject) {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
        GWT.getHostPageBaseURL() + RequestFactory.URL);
    builder.setRequestData(requestObject.getRequestData());
    builder.setCallback(new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        postRequestEvent(State.RECEIVED, null);
        logger.log(Level.SEVERE, SERVER_ERROR, exception);
      }

      public void onResponseReceived(Request request, Response response) {
        logger.finest("Response received");
        if (200 == response.getStatusCode()) {
          String text = response.getText();
          requestObject.handleResponseText(text);
        } else {
          logger.severe(SERVER_ERROR + " (" + response.getStatusText() + ")");
        }
        postRequestEvent(State.RECEIVED, response);
      }

    });

    try {
      logger.finest("Sending fire request");
      builder.send();
      postRequestEvent(State.SENT, null);
    } catch (RequestException e) {
      logger.log(Level.SEVERE, SERVER_ERROR + " (" + e.getMessage() +  ")", e);
    }
  }

  public ValueStoreJsonImpl getValueStore() {
    return valueStore;
  }

  public SyncRequest syncRequest(DeltaValueStore deltas) {
    assert deltas instanceof DeltaValueStoreJsonImpl;
    final DeltaValueStoreJsonImpl jsonDeltas = (DeltaValueStoreJsonImpl) deltas;

    return new SyncRequest() {

      Receiver<Set<SyncResult>> receiver = null;
      public void fire() {
        assert null != receiver : "to(Receiver) was not called";

        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
            GWT.getHostPageBaseURL() + RequestFactory.URL);

        builder.setRequestData(ClientRequestHelper.getRequestString(RequestDataManager.getRequestMap(
            RequestFactory.SYNC, null, jsonDeltas.toJson())));
        builder.setCallback(new RequestCallback() {

          public void onError(Request request, Throwable exception) {
            postRequestEvent(State.RECEIVED, null);
            logger.log(Level.SEVERE, SERVER_ERROR, exception);
          }

          public void onResponseReceived(Request request, Response response) {
            logger.finest("Response received");
            if (200 == response.getStatusCode()) {
              // parse the return value.
              receiver.onSuccess(jsonDeltas.commit(response.getText()));
            } else {
              logger.severe(SERVER_ERROR + " (" + response.getStatusText() + ")");
            }
            postRequestEvent(State.RECEIVED, response);
          }
        });

        try {
          logger.finest("Sending sync request");
          builder.send();
          postRequestEvent(State.SENT, null);
        } catch (RequestException e) {
          logger.log(Level.SEVERE, SERVER_ERROR + " (" + e.getMessage() +  ")", e);
        }
      }

      public SyncRequest to(Receiver<Set<SyncResult>> receiver) {
        this.receiver = receiver;
        return this;
      }
    };
  }

  /**
   * @param handlerManager
   */
  protected void init(HandlerManager handlerManager, RecordToTypeMap map) {
    this.valueStore = new ValueStoreJsonImpl(handlerManager, map);
    this.handlerManager = handlerManager;
    // This Handler should really get added to the Root Logger here, but until
    // App Engine Dev Mode logging is fixed, we can't use client side handlers
    // on the Root Logger. Instead, just add it to our own logger as a proof of
    // concept, and then log a Severe message to it to prove that it's working
    // All the "finest" messages that this class normally logs are not logged
    // to this handler since it would cause an infinite loop.
    // TODO(unnurg): Once this is all set up, ensure that the severe messages
    // in this class do not cause infinite loops during legitimate errors.
    logger.addHandler(new RequestFactoryLogHandler(this));
    logger.severe("Successful initialization!");
  }

  private void postRequestEvent(State received, Response response) {
    handlerManager.fireEvent(new RequestEvent(received, response));
  }
}
