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
package com.google.gwt.jsonp.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;
import com.google.gwt.safehtml.shared.annotations.SuppressIsTrustedResourceUriCastCheck;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A JSONP request that is waiting for a response. The request can be canceled.
 *
 * @param <T> the type of the response object.
 */
public class JsonpRequest<T> {

  /**
   * A global JS variable that holds the next index to use.
   */
  private static final String CALLBACKS_COUNTER_NAME = "__gwt_jsonp_counter__";

  /**
   * A global JS object that contains callbacks of pending requests.
   */
  private static final String CALLBACKS_NAME = "__gwt_jsonp__";
  private static final JavaScriptObject CALLBACKS = getOrCreateCallbacksObject();

  /**
   * Prefix appended to all id's that are determined by the callbacks counter.
   */
  private static final String INCREMENTAL_ID_PREFIX = "P";

  /**
   * Prefix appended to all id's that are passed in by the user. The "P" 
   * suffix must stay in sync with ExternalTextResourceGenerator.java
   */
  private static final String PREDETERMINED_ID_PREFIX = "P";

  /**
   * Returns the next ID to use, incrementing the global counter.
   */
  private static native int getAndIncrementCallbackCounter() /*-{
    var name = @com.google.gwt.jsonp.client.JsonpRequest::CALLBACKS_NAME;
    var ctr = @com.google.gwt.jsonp.client.JsonpRequest::CALLBACKS_COUNTER_NAME;
    return $wnd[name][ctr]++;
  }-*/;

  private static Node getHeadElement() {
    return Document.get().getElementsByTagName("head").getItem(0);
  }

  /**
   * Returns a global object to store callbacks of pending requests, creating
   * it if it doesn't exist.
   */
  private static native JavaScriptObject getOrCreateCallbacksObject() /*-{
    var name = @com.google.gwt.jsonp.client.JsonpRequest::CALLBACKS_NAME;
    if (!$wnd[name]) {
      $wnd[name] = new Object();
      $wnd[name]
          [@com.google.gwt.jsonp.client.JsonpRequest::CALLBACKS_COUNTER_NAME]
          = 0;
    }
    return $wnd[name];
  }-*/;

  private static String getPredeterminedId(String suffix) {
    return PREDETERMINED_ID_PREFIX + suffix;
  }

  private static String nextCallbackId() {
    return INCREMENTAL_ID_PREFIX + getAndIncrementCallbackCounter();
  }

  private final String callbackId;

  private final int timeout;

  private final AsyncCallback<T> callback;

  /**
   * Whether the result is expected to be an integer or not.
   */
  private final boolean expectInteger;

  private final String callbackParam;

  private final String failureCallbackParam;

  private final boolean canHaveMultipleRequestsForSameId;

  /**
   * Timer which keeps track of timeouts.
   */
  private Timer timer;

  /**
   * Create a new JSONP request.
   *
   * @param callback The callback instance to notify when the response comes
   *          back
   * @param timeout Time in ms after which a {@link TimeoutException} will be
   *          thrown
   * @param expectInteger Should be true if T is {@link Integer}, false
   *          otherwise
   * @param callbackParam Name of the url param of the callback function name
   * @param failureCallbackParam Name of the url param containing the
   *          failure callback function name, or null for no failure callback
   */
  JsonpRequest(AsyncCallback<T> callback, int timeout, boolean expectInteger,
      String callbackParam, String failureCallbackParam) {
    callbackId = nextCallbackId();
    this.callback = callback;
    this.timeout = timeout;
    this.expectInteger = expectInteger;
    this.callbackParam = callbackParam;
    this.failureCallbackParam = failureCallbackParam;
    this.canHaveMultipleRequestsForSameId = false;
  }

  /**
   * Create a new JSONP request with a hardcoded id. This could be used to
   * manually control which resources are considered duplicates (by giving them
   * identical ids). Could also be used if the callback name needs to be
   * completely user controlled (since the id is part of the callback name).
   *
   * @param callback The callback instance to notify when the response comes
   *          back
   * @param timeout Time in ms after which a {@link TimeoutException} will be
   *          thrown
   * @param expectInteger Should be true if T is {@link Integer}, false
   *          otherwise
   * @param callbackParam Name of the url param of the callback function name
   * @param failureCallbackParam Name of the url param containing the
   *          failure callback function name, or null for no failure callback
   * @param id unique id for the resource that is being fetched
   */
  JsonpRequest(AsyncCallback<T> callback, int timeout, boolean expectInteger,
      String callbackParam, String failureCallbackParam, String id) {
    callbackId = getPredeterminedId(id);
    this.callback = callback;
    this.timeout = timeout;
    this.expectInteger = expectInteger;
    this.callbackParam = callbackParam;
    this.failureCallbackParam = failureCallbackParam;
    this.canHaveMultipleRequestsForSameId = true;
  }

  /**
   * Cancels a pending request.  Note that if you are using preset ID's, this
   * will not work, since there is no way of knowing if there are other
   * requests pending (or have already returned) for the same data.
   */
  public void cancel() {
    timer.cancel();
    unload();
  }

  public AsyncCallback<T> getCallback() {
    return callback;
  }

  public int getTimeout() {
    return timeout;
  }

  @Override
  public String toString() {
    return "JsonpRequest(id=" + callbackId + ")";
  }

  // @VisibleForTesting
  String getCallbackId() {
    return callbackId;
  }

  /**
   * Sends a request using the JSONP mechanism.
   *
   * @param baseUri To be sent to the server.
   */
  @SuppressIsTrustedResourceUriCastCheck
  void send(@IsTrustedResourceUri final String baseUri) {
    registerCallbacks(CALLBACKS, canHaveMultipleRequestsForSameId);
    StringBuilder uri = new StringBuilder(baseUri);
    uri.append(baseUri.contains("?") ? "&" : "?");
    String prefix = CALLBACKS_NAME + "." + callbackId;

    uri.append(callbackParam).append("=").append(prefix).append(
        ".onSuccess");
    if (failureCallbackParam != null) {
      uri.append("&");
      uri.append(failureCallbackParam).append("=").append(prefix).append(
          ".onFailure");
    }
    ScriptElement script = Document.get().createScriptElement();
    script.setType("text/javascript");
    script.setId(callbackId);
    script.setSrc(uri.toString());
    timer = new Timer() {
      @Override
      public void run() {
        onFailure(new TimeoutException("Timeout while calling " + baseUri));
      }
    };
    timer.schedule(timeout);
    getHeadElement().appendChild(script);
  }
  
  private void onFailure(String message) {
    onFailure(new Exception(message));
  }

  private void onFailure(Throwable ex) {
    timer.cancel();
    try {
      if (callback != null) {
        callback.onFailure(ex);
      }
    } finally {
      unload();
    }
  }

  private void onSuccess(T data) {
    timer.cancel();
    try {
      if (callback != null) {
        callback.onSuccess(data);
      }
    } finally {
      unload();
    }
  }

  /**
   * Registers the callback methods that will be called when the JSONP response
   * comes back. 2 callbacks are created, one to return the value, and one to
   * notify a failure.
   *
   * @param callbacks the global JS object which stores callbacks
   */
  private native void registerCallbacks(JavaScriptObject callbacks, boolean canHaveMultipleRequestsForId) /*-{
    var self = this;
    var callback = new Object();
    callback.onSuccess = $entry(function(data) {
      // Box primitive types
      if (typeof data == 'boolean') {
        data = @java.lang.Boolean::new(Z)(data);
      } else if (typeof data == 'number') {
        if (self.@com.google.gwt.jsonp.client.JsonpRequest::expectInteger) {
          data = @java.lang.Integer::new(I)(data);
        } else {
          data = @java.lang.Double::new(D)(data);
        }
      }
      self.@com.google.gwt.jsonp.client.JsonpRequest::onSuccess(Ljava/lang/Object;)(data);
    });
    if (this.@com.google.gwt.jsonp.client.JsonpRequest::failureCallbackParam) {
      callback.onFailure = $entry(function(message) {
        self.@com.google.gwt.jsonp.client.JsonpRequest::onFailure(Ljava/lang/String;)(message);
      });
    }
    
    if (canHaveMultipleRequestsForId) {
      // In this case, we keep a wrapper, with a list of callbacks.  Since the
      // response for the request is the same each time, we call all of the
      // callbacks as soon as any response comes back.
      var callbackWrapper =
        callbacks[this.@com.google.gwt.jsonp.client.JsonpRequest::callbackId];
      if (!callbackWrapper) {
        callbackWrapper = new Object();
        callbackWrapper.callbackList = new Array();

        callbackWrapper.onSuccess = function(data) {
          while (callbackWrapper.callbackList.length > 0) {
            callbackWrapper.callbackList.shift().onSuccess(data);
          }
        } 
        callbackWrapper.onFailure = function(data) {
          while (callbackWrapper.callbackList.length > 0) {
            callbackWrapper.callbackList.shift().onFailure(data);
          }
        } 
        callbacks[this.@com.google.gwt.jsonp.client.JsonpRequest::callbackId] =
          callbackWrapper;
      }
      callbackWrapper.callbackList.push(callback);
    } else {
      // In this simple case, just associate the callback directly with the
      // particular id in the callbacks object
      callbacks[this.@com.google.gwt.jsonp.client.JsonpRequest::callbackId] = callback;
    }
  }-*/;

  /**
   * Cleans everything once the response has been received: deletes the script
   * tag and unregisters the callback.
   */
  private void unload() {
    /*
     * Some browsers (IE7) require the script tag to be deleted outside the
     * scope of the script itself. Therefore, we need to defer the delete
     * statement after the callback execution.
     */
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        if (!canHaveMultipleRequestsForSameId) {
          // If there can me multiple requests for a particular ID, then we
          // don't want to unregister the callback since there may be pending
          // requests that have not yet come back and we don't want them to
          // have an undefined callback function.
          unregisterCallbacks(CALLBACKS);
        }
        Node script = Document.get().getElementById(callbackId);
        if (script != null) {
          // The script may have already been deleted
          getHeadElement().removeChild(script);
        }
      }
    });
  }

  private native void unregisterCallbacks(JavaScriptObject callbacks) /*-{
    delete callbacks[this.@com.google.gwt.jsonp.client.JsonpRequest::callbackId];
  }-*/;
}
