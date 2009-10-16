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
package com.google.gwt.http.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * An HTTP request that is waiting for a response. Requests can be queried for
 * their pending status or they can be canceled.
 * 
 * <h3>Required Module</h3> Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include
 * com/google/gwt/examples/http/InheritsExample.gwt.xml}
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
  private static Response createResponse(final XMLHttpRequest xmlHttpRequest) {
    assert (isResponseReady(xmlHttpRequest));
    Response response = new Response() {
      @Override
      public String getHeader(String header) {
        StringValidator.throwIfEmptyOrNull("header", header);

        return xmlHttpRequest.getResponseHeader(header);
      }

      @Override
      public Header[] getHeaders() {
        return Request.getHeaders(xmlHttpRequest);
      }

      @Override
      public String getHeadersAsString() {
        return xmlHttpRequest.getAllResponseHeaders();
      }

      @Override
      public int getStatusCode() {
        return xmlHttpRequest.getStatus();
      }

      @Override
      public String getStatusText() {
        return xmlHttpRequest.getStatusText();
      }

      @Override
      public String getText() {
        return xmlHttpRequest.getResponseText();
      }
    };
    return response;
  }

  /**
   * Returns an array of headers built by parsing the string of headers returned
   * by the JavaScript <code>XmlHttpRequest</code> object.
   * 
   * @param xmlHttpRequest
   * @return array of Header items
   */
  private static Header[] getHeaders(XMLHttpRequest xmlHttp) {
    String allHeaders = xmlHttp.getAllResponseHeaders();
    String[] unparsedHeaders = allHeaders.split("\n");
    Header[] parsedHeaders = new Header[unparsedHeaders.length];

    for (int i = 0, n = unparsedHeaders.length; i < n; ++i) {
      String unparsedHeader = unparsedHeaders[i];

      if (unparsedHeader.length() == 0) {
        continue;
      }

      int endOfNameIdx = unparsedHeader.indexOf(':');
      if (endOfNameIdx < 0) {
        continue;
      }

      final String name = unparsedHeader.substring(0, endOfNameIdx).trim();
      final String value = unparsedHeader.substring(endOfNameIdx + 1).trim();
      Header header = new Header() {
        @Override
        public String getName() {
          return name;
        }

        @Override
        public String getValue() {
          return value;
        }

        @Override
        public String toString() {
          return name + " : " + value;
        }
      };

      parsedHeaders[i] = header;
    }

    return parsedHeaders;
  }

  private static boolean isResponseReady(XMLHttpRequest xhr) {
    return xhr.getReadyState() == XMLHttpRequest.DONE;
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
  private XMLHttpRequest xmlHttpRequest;

  /**
   * Only used for building a
   * {@link com.google.gwt.user.client.rpc.impl.FailedRequest}.
   */
  protected Request() {
    timeoutMillis = 0;
    xmlHttpRequest = null;
    timer = null;
  }

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
  Request(XMLHttpRequest xmlHttpRequest, int timeoutMillis,
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
        @Override
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
      XMLHttpRequest xmlHttp = xmlHttpRequest;
      xmlHttpRequest = null;

      xmlHttp.clearOnReadyStateChange();
      xmlHttp.abort();

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

    int readyState = xmlHttpRequest.getReadyState();

    /*
     * Because we are doing asynchronous requests it is possible that we can
     * call XmlHttpRequest.send and still have the XmlHttpRequest.getReadyState
     * method return the state as XmlHttpRequest.OPEN. That is why we include
     * open although it is nottechnically true since open implies that the
     * request has not been sent.
     */
    switch (readyState) {
      case XMLHttpRequest.OPENED:
      case XMLHttpRequest.HEADERS_RECEIVED:
      case XMLHttpRequest.LOADING:
        return true;
    }

    return false;
  }

  /*
   * Method called when the JavaScript XmlHttpRequest object's readyState
   * reaches 4 (LOADED).
   */
  void fireOnResponseReceived(RequestCallback callback) {
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
    final XMLHttpRequest xhr = xmlHttpRequest;
    xmlHttpRequest = null;

    String errorMsg = getBrowserSpecificFailure(xhr);
    if (errorMsg != null) {
      Throwable exception = new RuntimeException(errorMsg);
      callback.onError(this, exception);
    } else {
      Response response = createResponse(xhr);
      callback.onResponseReceived(this, response);
    }
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
   * Method called when this request times out.
   * 
   * NOTE: this method is called from JSNI
   */
  private void fireOnTimeout(RequestCallback callback) {
    if (xmlHttpRequest == null) {
      // the request has been received at this point
      return;
    }

    cancel();

    callback.onError(this, new RequestTimeoutException(this, timeoutMillis));
  }

  /**
   * Tests if the JavaScript <code>XmlHttpRequest.status</code> property is
   * readable. This can return failure in two different known scenarios:
   * 
   * <ol>
   * <li>On Mozilla, after a network error, attempting to read the status code
   * results in an exception being thrown. See <a
   * href="https://bugzilla.mozilla.org/show_bug.cgi?id=238559"
   * >https://bugzilla.mozilla.org/show_bug.cgi?id=238559</a>.</li>
   * <li>On Safari, if the HTTP response does not include any response text. See
   * <a
   * href="http://bugs.webkit.org/show_bug.cgi?id=3810">http://bugs.webkit.org
   * /show_bug.cgi?id=3810</a>.</li>
   * </ol>
   * 
   * @param xhr the JavaScript <code>XmlHttpRequest</code> object to test
   * @return a String message containing an error message if the
   *         <code>XmlHttpRequest.status</code> code is unreadable or null if
   *         the status code could be successfully read.
   */
  private native String getBrowserSpecificFailure(XMLHttpRequest xhr) /*-{
    try {
      if (xhr.status === undefined) {
        return "XmlHttpRequest.status == undefined, please see Safari bug " +
               "http://bugs.webkit.org/show_bug.cgi?id=3810 for more details";
      }
      return null;
    } catch (e) {
      return "Unable to read XmlHttpRequest.status; likely causes are a " +
             "networking error or bad cross-domain request. Please see " +
             "https://bugzilla.mozilla.org/show_bug.cgi?id=238559 for more " +
             "details";
    }
  }-*/;
}
