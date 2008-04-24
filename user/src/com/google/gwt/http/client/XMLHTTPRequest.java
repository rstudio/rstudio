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
package com.google.gwt.http.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Utility class that serves as the one place where we interact with the
 * JavaScript <code>XmlHttpRequest</code> object.
 */
final class XMLHTTPRequest {

  /*
   * NOTE: Testing discovered that for some bizarre reason, on Mozilla, the
   * JavaScript <code>XmlHttpRequest.onreadystatechange</code> handler
   * function maybe still be called after it is deleted. The theory is that the
   * callback is cached somewhere. Setting it to null or an empty function does
   * seem to work properly, though.
   * 
   * On IE, there are two problems: Setting onreadystatechange to null (as
   * opposed to an empty function) sometimes throws an exception. With
   * particular (rare) versions of jscript.dll, setting onreadystatechange from
   * within onreadystatechange causes a crash. Setting it from within a timeout
   * fixes this bug (see issue 1610).
   * 
   * End result: *always* set onreadystatechange to an empty function (never to
   * null). Never set onreadystatechange from within onreadystatechange (always
   * in a setTimeout()).
   */

  public static final int UNITIALIZED = 0;
  public static final int OPEN = 1;
  public static final int SENT = 2;
  public static final int RECEIVING = 3;
  public static final int LOADED = 4;

  static native void abort(JavaScriptObject xmlHttpRequest) /*-{
    xmlHttpRequest.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
    xmlHttpRequest.abort();
  }-*/;

  static native String getAllResponseHeaders(JavaScriptObject xmlHttpRequest) /*-{
    return xmlHttpRequest.getAllResponseHeaders();
  }-*/;

  /**
   * Tests if the JavaScript <code>XmlHttpRequest.status</code> property is
   * readable. This can return failure in two different known scenarios:
   * 
   * <ol>
   * <li>On Mozilla, after a network error, attempting to read the status code
   * results in an exception being thrown. See <a
   * href="https://bugzilla.mozilla.org/show_bug.cgi?id=238559">https://bugzilla.mozilla.org/show_bug.cgi?id=238559</a>.
   * </li>
   * <li>On Safari, if the HTTP response does not include any response text.
   * See <a
   * href="http://bugs.webkit.org/show_bug.cgi?id=3810">http://bugs.webkit.org/show_bug.cgi?id=3810</a>.
   * </li>
   * </ol>
   * 
   * @param xmlHttpRequest the JavaScript <code>XmlHttpRequest</code> object
   *          to test
   * @return a String message containing an error message if the
   *         <code>XmlHttpRequest.status</code> code is unreadable or null if
   *         the status code could be successfully read.
   */
  static native String getBrowserSpecificFailure(
      JavaScriptObject xmlHttpRequest) /*-{
    try {
      if (xmlHttpRequest.status === undefined) {
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

  /**
   * Returns an array of headers built by parsing the string of headers returned
   * by the JavaScript <code>XmlHttpRequest</code> object.
   * 
   * @param xmlHttpRequest
   * @return array of Header items
   */
  static Header[] getHeaders(JavaScriptObject xmlHttpRequest) {
    String allHeaders = getAllResponseHeaders(xmlHttpRequest);
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

  static native int getReadyState(JavaScriptObject xmlHttpRequest) /*-{
    return xmlHttpRequest.readyState;
  }-*/;

  static native String getResponseHeader(JavaScriptObject xmlHttpRequest,
      String header) /*-{
    try {
      return xmlHttpRequest.getResponseHeader(header);
    } catch (e) {
      // purposely ignored
    }
    return null;
  }-*/;

  static native String getResponseText(JavaScriptObject xmlHttpRequest) /*-{
    return xmlHttpRequest.responseText;
  }-*/;

  static native int getStatusCode(JavaScriptObject xmlHttpRequest) /*-{
    return xmlHttpRequest.status;
  }-*/;

  static native String getStatusText(JavaScriptObject xmlHttpRequest) /*-{
    return xmlHttpRequest.statusText;
  }-*/;

  static boolean isResponseReady(JavaScriptObject xmlHttpRequest) {
    return getReadyState(xmlHttpRequest) == LOADED;
  }

  /**
   * Opens the request and catches any exceptions thrown. If an exception is
   * caught, its string representation will be returned. This is the only signal
   * that an error has occurred.
   * 
   * @param xmlHttpRequest JavaScript <code>XmlHttpRequest</code> object  
   * @param httpMethod the method to use for open call
   * @param url the URL to use for the open call
   * @param async true if we should do an asynchronous open 
   * @return error message if an exception is thrown or null if there is none 
   */
  static native String open(JavaScriptObject xmlHttpRequest, String httpMethod,
      String url, boolean async) /*-{
    try {
      xmlHttpRequest.open(httpMethod, url, async);
      return null;
    } catch (e) {
      return e.message || e.toString();
    }
  }-*/;

  /**
   * Opens the request and catches any exceptions thrown. If an exception is
   * caught, its string representation will be returned. This is the only signal
   * that an error has occurred.
   * 
   * @param xmlHttpRequest JavaScript <code>XmlHttpRequest</code> object  
   * @param httpMethod the method to use for open call
   * @param url the URL to use for the open call
   * @param async true if we should do an asynchronous open 
   * @param user user to use in the URL
   * @return error message if an exception is thrown or null if there is none 
   */
  static native String open(JavaScriptObject xmlHttpRequest, String httpMethod,
      String url, boolean async, String user) /*-{
    try {
      xmlHttpRequest.open(httpMethod, url, async, user);
      return null;
    } catch (e) {
      return e.message || e.toString();
    }
  }-*/;

  /**
   * Opens the request and catches any exceptions thrown. If an exception is
   * caught, its string representation will be returned. This is the only signal
   * that an error has occurred.
   * 
   * @param xmlHttpRequest JavaScript <code>XmlHttpRequest</code> object  
   * @param httpMethod the method to use for open call
   * @param url the URL to use for the open call
   * @param async true if we should do an asynchronous open 
   * @param user user to use in the URL
   * @param password password to use in the URL
   * @return error message if an exception is thrown or null if there is none 
   */
  static native String open(JavaScriptObject xmlHttpRequest, String httpMethod,
      String url, boolean async, String user, String password) /*-{
    try {
      xmlHttpRequest.open(httpMethod, url, async, user, password);
      return null;
    } catch (e) {
      return e.message || e.toString();
    }
  }-*/;

  /*
   * Creates a closure that calls the HTTPRequest::fireOnResponseRecieved method
   * when the server's response is received and sends the data.
   */
  static native String send(JavaScriptObject xmlHttpRequest, Request httpRequest,
      String requestData, RequestCallback callback) /*-{
    xmlHttpRequest.onreadystatechange = function() {
      if (xmlHttpRequest.readyState == @com.google.gwt.http.client.XMLHTTPRequest::LOADED) {
      	$wnd.setTimeout(function() {
          xmlHttpRequest.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
      	}, 0);
        httpRequest.@com.google.gwt.http.client.Request::fireOnResponseReceived(Lcom/google/gwt/http/client/RequestCallback;)(callback);
      }
    };
    try {
      xmlHttpRequest.send(requestData);
      return null;
    } catch (e) {
      xmlHttpRequest.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
      return e.message || e.toString();
    }
  }-*/;

  static native String setRequestHeader(JavaScriptObject xmlHttpRequest,
      String header, String value) /*-{
    try {
      xmlHttpRequest.setRequestHeader(header, value);
      return null;
    } catch (e) {
      return e.message || e.toString();
    }
  }-*/;
  
  private XMLHTTPRequest() {
  }
}
