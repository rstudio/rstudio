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
package com.google.gwt.user.client;

/**
 * This class allows you to make asynchronous HTTP requests to the originating
 * server.
 * 
 * @deprecated As of GWT 1.5, replaced by
 *             {@link com.google.gwt.http.client.RequestBuilder RequestBuilder}.
 */
@Deprecated
public class HTTPRequest {

  /**
   * Makes an asynchronous HTTP GET to a remote server.
   * 
   * @param url the absolute url to GET
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncGet(String url, ResponseTextHandler handler) {
    return asyncGetImpl(null, null, url, handler);
  }

  /**
   * Makes an asynchronous HTTP GET to a remote server.
   * 
   * @param url the absolute url to GET
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncGet(String user, String pwd, String url,
      ResponseTextHandler handler) {
    return asyncGetImpl(user, pwd, url, handler);
  }

  /**
   * Makes an asynchronous HTTP POST to a remote server.
   * 
   * @param url the absolute url to which the POST data is delivered
   * @param postData the data to post
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncPost(String url, String postData,
      ResponseTextHandler handler) {
    return asyncPostImpl(null, null, url, postData, handler);
  }

  /**
   * Makes an asynchronous HTTP POST to a remote server.
   * 
   * @param url the absolute url to which the POST data is delivered
   * @param postData the data to post
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncPost(String user, String pwd, String url,
      String postData, ResponseTextHandler handler) {
    return asyncPostImpl(user, pwd, url, postData, handler);
  }

  private static native boolean asyncGetImpl(String user, String pwd, String url,
      ResponseTextHandler handler) /*-{
    var xmlHttp = @com.google.gwt.xhr.client.XMLHttpRequest::create()();
    try {
      xmlHttp.open("GET", url, true);
      xmlHttp.setRequestHeader("Content-Type", "text/plain; charset=utf-8");
      xmlHttp.onreadystatechange = $entry(function() {
        if (xmlHttp.readyState == 4) {
          $wnd.setTimeout(function() {
            xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
          }, 0);
          handler.@com.google.gwt.user.client.ResponseTextHandler::onCompletion(Ljava/lang/String;)(xmlHttp.responseText || "");
        }
      });
      xmlHttp.send('');
      return true;
    } catch (e) {
      xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
      return false;
    }
  }-*/;

  private static native boolean asyncPostImpl(String user, String pwd, String url,
      String postData, ResponseTextHandler handler) /*-{
    var xmlHttp = @com.google.gwt.xhr.client.XMLHttpRequest::create()();
    try {
      xmlHttp.open("POST", url, true);
      xmlHttp.setRequestHeader("Content-Type", "text/plain; charset=utf-8");
      xmlHttp.onreadystatechange = $entry(function() {
        if (xmlHttp.readyState == 4) {
          $wnd.setTimeout(function() {
            xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
          }, 0);
          handler.@com.google.gwt.user.client.ResponseTextHandler::onCompletion(Ljava/lang/String;)(xmlHttp.responseText || "");
        }
      });
      xmlHttp.send(postData);
      return true;
    }
    catch (e) {
      xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
      return false;
    }
  }-*/;
}
