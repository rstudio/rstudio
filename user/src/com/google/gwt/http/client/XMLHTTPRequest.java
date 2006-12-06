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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Utility class that serves as the one place where we interact with the
 * JavaScript <code>XmlHttpRequest</code> object.
 */
final class XMLHTTPRequest {

  public static final int UNITIALIZED = 0;
  public static final int OPEN = 1;
  public static final int SENT = 2;
  public static final int RECEIVING = 3;
  public static final int LOADED = 4;

  /*
   * NOTE: Testing discovered that for some bizarre reason, on Mozilla, the
   * JavaScript <code>XmlHttpRequest.onreadystatechange</code> handler function 
   * maybe still be called after it is deleted. The theory is that the callback 
   * is cached somewhere.  Setting the handler to null has the desired effect on 
   * Mozilla but it causes IE to crash during the assignment.
   */
  static native void abort(JavaScriptObject xmlHttpRequest) /*-{
   delete xmlHttpRequest.onreadystatechange;
   
   xmlHttpRequest.abort();
   }-*/;

  static native String getAllResponseHeaders(JavaScriptObject xmlHttpRequest) /*-{
   return xmlHttpRequest.getAllResponseHeaders();
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
        public String getName() {
          return name;
        }

        public String getValue() {
          return value;
        }

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
   } catch (ex) {
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

  /**
   * Tests if the JavaScript <code>XmlHttpRequest.status</code> property is 
   * undefined. Currently, this happens on Safari if the HTTP response does not 
   * include any response text.
   * 
   * @param xmlHttpRequest the JavaScript <code>XmlHttpRequest</code> object 
   *        to test
   * @return true if the <code>XmlHttpRequest.status</code> code is undefined
   * 
   * NOTE: Safari has a bug, <a
   * href="http://bugs.webkit.org/show_bug.cgi?id=3810">
   * http://bugs.webkit.org/show_bug.cgi?id=3810</a>, in its XmlHttpRequest
   * implementation that can result in the status code being undefined if the
   * HTTP response does not contain any text.
   * 
   */
  static native boolean hasStatusCodeUndefinedBug(
      JavaScriptObject xmlHttpRequest) /*-{
   return xmlHttpRequest.status === undefined;
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
   * @param user user to use in the URL
   * @param password password to use in the URL
   * @return error message if an exception is thrown or null if there is none 
   */
  static native String open(JavaScriptObject xmlHttpRequest, String httpMethod,
      String url, boolean async, String user, String password) /*-{
   try {
   xmlHttpRequest.open(httpMethod, url, async, user, password);
   } catch (e) {
   return e.toString();
   }
   
   return null;
   }-*/;

  /*
   * Creates a closure that calls the HTTPRequest::fireOnResponseRecieved method
   * when the server's response is received and sends the data.
   */
  static native String send(JavaScriptObject xmlHttpRequest,
      Request httpRequest, String requestData, RequestCallback callback) /*-{
   var xmlHttp = xmlHttpRequest;

   xmlHttp.onreadystatechange = function() {
   if (xmlHttp.readyState == @com.google.gwt.http.client.XMLHTTPRequest::LOADED) {
   delete xmlHttp.onreadystatechange;
   
   httpRequest.@com.google.gwt.http.client.Request::fireOnResponseReceived(Lcom/google/gwt/http/client/RequestCallback;)(callback);
   }
   };
   
   try {
   xmlHttp.send(requestData);
   } catch (e) {
   return e.toString();
   }
   
   return null;
   }-*/;

  static native String setRequestHeader(JavaScriptObject xmlHttpRequest,
      String header, String value) /*-{
   try {
   xmlHttpRequest.setRequestHeader(header, value);
   } catch (e) {
   return e.toString();
   }
   
   return null;
   }-*/;

  private XMLHTTPRequest() {
  }
}
