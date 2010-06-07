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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SyncRequest;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.valuestore.client.DeltaValueStoreJsonImpl;
import com.google.gwt.valuestore.client.ValueStoreJsonImpl;
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.valuestore.shared.impl.RecordToTypeMap;

import java.util.Set;

/**
 * Base implementation of RequestFactory.
 */
public abstract class RequestFactoryJsonImpl implements RequestFactory {

  private ValueStoreJsonImpl valueStore;

  private HandlerManager handlerManager;

  public void fire(final RequestObject<?> requestObject) {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
        GWT.getHostPageBaseURL() + RequestFactory.URL);
    builder.setRequestData(requestObject.getRequestData());
    builder.setCallback(new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        postRequestEvent(State.RECEIVED);
        // shell.error.setInnerText(SERVER_ERROR);
      }

      public void onResponseReceived(Request request, Response response) {
        if (200 == response.getStatusCode()) {
          String text = response.getText();
          requestObject.handleResponseText(text);
        } else {
          // shell.error.setInnerText(SERVER_ERROR + " ("
          // + response.getStatusText() + ")");
        }
        postRequestEvent(State.RECEIVED);
      }

    });

    try {
      builder.send();
      postRequestEvent(State.SENT);
    } catch (RequestException e) {
      // shell.error.setInnerText(SERVER_ERROR + " (" + e.getMessage() +
      // ")");
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
            postRequestEvent(State.RECEIVED);
            // shell.error.setInnerText(SERVER_ERROR);
          }

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              // parse the return value.
              receiver.onSuccess(jsonDeltas.commit(response.getText()));
            } else {
              // shell.error.setInnerText(SERVER_ERROR + " ("
              // + response.getStatusText() + ")");
            }
            postRequestEvent(State.RECEIVED);
          }
        });

        try {
          builder.send();
          postRequestEvent(State.SENT);
        } catch (RequestException e) {
          // shell.error.setInnerText(SERVER_ERROR + " (" + e.getMessage() +
          // ")");
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
  }

  private void postRequestEvent(State received) {
    handlerManager.fireEvent(new RequestEvent(received));
  }
}
