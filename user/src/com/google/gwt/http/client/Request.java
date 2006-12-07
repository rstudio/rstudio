/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.Timer;

/**
 * An HTTP request that is waiting for a response. Requests can be queried for
 * their pending status or they can be canceled.
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 * 
 */
public class Request {
  /**
   * Creates a {@link Response} instance for the given JavaScript XmlHttpRequest
   * object.
   * 
   * @param xmlHttpRequest xmlHttpRequest object for which we need a response
   * @return a {@link Response} object instance
   */
  private static Response createResponse(final JavaScriptObject xmlHttpRequest) {
    assert (XMLHTTPRequest.isResponseReady(xmlHttpRequest));
    Response response = new Response() {
      public String getHeader(String header) {
        StringValidator.throwIfEmptyOrNull("header", header);

        return XMLHTTPRequest.getResponseHeader(xmlHttpRequest, header);
      }

      public Header[] getHeaders() {
        return XMLHTTPRequest.getHeaders(xmlHttpRequest);
      }

      public String getHeadersAsString() {
        return XMLHTTPRequest.getAllResponseHeaders(xmlHttpRequest);
      }

      public int getStatusCode() {
        return XMLHTTPRequest.getStatusCode(xmlHttpRequest);
      }

      public String getStatusText() {
        return XMLHTTPRequest.getStatusText(xmlHttpRequest);
      }

      public String getText() {
        return XMLHTTPRequest.getResponseText(xmlHttpRequest);
      }
    };
    return response;
  }

  /**
   * The number of milliseconds to wait for this HTTP request to complete.
   */
  private final int timeoutMillis;

  /*
   * Timer used to force HTTPRequest timeouts. If the user has not requested a
   * timeout then this field is null.
   */
  private final Timer timer;

  /*
   * JavaScript XmlHttpRequest object that this Java class wraps. This field is
   * not final because we transfer ownership of it to the HTTPResponse object
   * and set this field to null.
   */
  private JavaScriptObject xmlHttpRequest;

  /**
   * Constructs an instance of the Request object.
   * 
   * @param xmlHttpRequest JavaScript XmlHttpRequest object instance
   * @param timeoutMillis number of milliseconds to wait for a response
   * @param callback callback interface to use for notification
   * 
   * @throws IllegalArgumentException if timeoutMillis &lt; 0
   * @throws NullPointerException if xmlHttpRequest, or callback are null
   */
  Request(JavaScriptObject xmlHttpRequest, int timeoutMillis,
      final RequestCallback callback) {
    if (xmlHttpRequest == null) {
      throw new NullPointerException();
    }

    if (callback == null) {
      throw new NullPointerException();
    }

    if (timeoutMillis < 0) {
      throw new IllegalArgumentException();
    }

    this.timeoutMillis = timeoutMillis;

    this.xmlHttpRequest = xmlHttpRequest;

    if (timeoutMillis > 0) {
      // create and start a Timer
      timer = new Timer() {
        public void run() {
          fireOnTimeout(callback);
        }
      };

      timer.schedule(timeoutMillis);
    } else {
      // no Timer required
      timer = null;
    }
  }

  /**
   * Cancels a pending request. If the request has already been canceled or if
   * it has timed out no action is taken.
   */
  public void cancel() {
    /*
     * There is a strange race condition that occurs on Mozilla when you cancel
     * a request while the response is coming in. It appears that in some cases
     * the onreadystatechange handler is still called after the handler function
     * has been deleted and during the call to XmlHttpRequest.abort(). So we
     * null the xmlHttpRequest here and that will prevent the
     * fireOnResponseReceived method from calling the callback function.
     * 
     * Setting the onreadystatechange handler to null gives us the correct
     * behavior in Mozilla but crashes IE. That is why we have chosen to fixed
     * this in Java by nulling out our reference to the XmlHttpRequest object.
     */
    if (xmlHttpRequest != null) {
      JavaScriptObject xmlHttp = xmlHttpRequest;
      xmlHttpRequest = null;

      XMLHTTPRequest.abort(xmlHttp);

      cancelTimer();
    }
  }

  /**
   * Returns true if this request is waiting for a response.
   * 
   * @return true if this request is waiting for a response
   */
  public boolean isPending() {
    if (xmlHttpRequest == null) {
      return false;
    }

    int readyState = XMLHTTPRequest.getReadyState(xmlHttpRequest);

    /*
     * Because we are doing asynchronous requests it is possible that we can
     * call XmlHttpRequest.send and still have the XmlHttpRequest.getReadyState
     * method return the state as XmlHttpRequest.OPEN. That is why we include
     * open although it is not *technically* true since open implies that the
     * request has not been sent.
     */
    switch (readyState) {
      case XMLHTTPRequest.OPEN:
      case XMLHTTPRequest.SENT:
      case XMLHTTPRequest.RECEIVING:
        return true;
    }

    return false;
  }

  /*
   * Stops the current HTTPRequest timer if there is one.
   */
  private void cancelTimer() {
    if (timer != null) {
      timer.cancel();
    }
  }

  /*
   * Method called when the JavaScript XmlHttpRequest object's readyState
   * reaches 4 (LOADED).
   * 
   * NOTE: this method is called from JSNI
   */
  private void fireOnResponseReceived(RequestCallback callback) {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireOnResponseReceivedAndCatch(handler, callback);
    } else {
      fireOnResponseReceivedImpl(callback);
    }
  }

  private void fireOnResponseReceivedAndCatch(UncaughtExceptionHandler handler,
      RequestCallback callback) {
    try {
      fireOnResponseReceivedImpl(callback);
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private void fireOnResponseReceivedImpl(RequestCallback callback) {
    if (xmlHttpRequest == null) {
      // the request has timed out at this point
      return;
    }

    cancelTimer();

    /*
     * We cannot use cancel here because it would clear the contents of the
     * JavaScript XmlHttpRequest object so we manually null out our reference to
     * the JavaScriptObject
     */
    final JavaScriptObject xmlHttp = xmlHttpRequest;
    xmlHttpRequest = null;

    if (XMLHTTPRequest.hasStatusCodeUndefinedBug(xmlHttp)) {
      Throwable exception = new RuntimeException(
          "XmlHttpRequest.status == undefined, please see Safari bug http://bugs.webkit.org/show_bug.cgi?id=3810 for more details");
      callback.onError(this, exception);
    } else {
      Response response = createResponse(xmlHttp);
      callback.onResponseReceived(this, response);
    }
  }

  /*
   * Method called when this request times out.
   * 
   * NOTE: this method is called from JSNI
   */
  private final void fireOnTimeout(RequestCallback callback) {
    if (xmlHttpRequest == null) {
      // the request has been received at this point
      return;
    }

    cancel();

    callback.onError(this, new RequestTimeoutException(this, timeoutMillis));
  }
}
