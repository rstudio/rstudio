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
package com.google.gwt.xhr.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * The native XMLHttpRequest object. Most applications should use the higher-
 * level {@link RequestBuilder} class unless they need specific functionality
 * provided by the XMLHttpRequest object.
 * 
 * @see http://www.w3.org/TR/XMLHttpRequest/
 */
public class XMLHttpRequest extends JavaScriptObject {

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

  /**
   * When constructed, the XMLHttpRequest object must be in the UNSENT state.
   */
  public static final int UNSENT = 0;

  /**
   * The OPENED state is the state of the object when the open() method has been
   * successfully invoked. During this state request headers can be set using
   * setRequestHeader() and the request can be made using send().
   */
  public static final int OPENED = 1;

  /**
   * The HEADERS_RECEIVED state is the state of the object when all response
   * headers have been received.
   */
  public static final int HEADERS_RECEIVED = 2;

  /**
   * The LOADING state is the state of the object when the response entity body
   * is being received.
   */
  public static final int LOADING = 3;

  /**
   * The DONE state is the state of the object when either the data transfer has
   * been completed or something went wrong during the transfer (infinite
   * redirects for instance).
   */
  public static final int DONE = 4;

  /**
   * Creates an XMLHttpRequest object.
   * 
   * @return the created object
   */
  public static native XMLHttpRequest create() /*-{
    if ($wnd.XMLHttpRequest) {
      return new XMLHttpRequest();
    } else {
      try {
        return new ActiveXObject('MSXML2.XMLHTTP.3.0');
      } catch (e) {
        return new ActiveXObject("Microsoft.XMLHTTP");
      }
    }
  }-*/;

  protected XMLHttpRequest() {
  }

  /**
   * Aborts the current request.
   * 
   * @see http://www.w3.org/TR/XMLHttpRequest/#abort
   */
  public final native void abort() /*-{
    this.abort();
  }-*/;

  /**
   * Clears the {@link ReadyStateChangeHandler}.
   * 
   * @see #clearOnReadyStateChange()
   * @see http://www.w3.org/TR/XMLHttpRequest/#onreadystatechange
   */
  public final native void clearOnReadyStateChange() /*-{
    var self = this;
    $wnd.setTimeout(function() {
      self.onreadystatechange = @null::nullMethod();
    }, 0);
  }-*/;

  /**
   * Gets all the HTTP response headers, as a single string.
   * 
   * @return the response headers.
   * @see http://www.w3.org/TR/XMLHttpRequest/#getallresponseheaders
   */
  public final native String getAllResponseHeaders() /*-{
    return this.getAllResponseHeaders();
  }-*/;

  /**
   * Get's the current ready-state.
   * 
   * @return the ready-state constant
   * @see http://www.w3.org/TR/XMLHttpRequest/#readystate
   */
  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  /**
   * Gets an HTTP response header.
   * 
   * @param header the response header to be retrieved
   * @return the header value
   * @see http://www.w3.org/TR/XMLHttpRequest/#getresponseheader
   */
  public final native String getResponseHeader(String header) /*-{
    return this.getResponseHeader(header);
  }-*/;

  /**
   * Gets the response text.
   * 
   * @return the response text
   * @see http://www.w3.org/TR/XMLHttpRequest/#responsetext
   */
  public final native String getResponseText() /*-{
    return this.responseText;
  }-*/;

  /**
   * Gets the status code.
   * 
   * @return the status code
   * @see http://www.w3.org/TR/XMLHttpRequest/#status
   */
  public final native int getStatus() /*-{
    return this.status;
  }-*/;

  /**
   * Gets the status text.
   * 
   * @return the status text
   * @see http://www.w3.org/TR/XMLHttpRequest/#statustext
   */
  public final native String getStatusText() /*-{
    return this.statusText;
  }-*/;

  /**
   * Opens an asynchronous connection.
   * 
   * @param httpMethod the HTTP method to use
   * @param url the URL to be opened
   * @see http://www.w3.org/TR/XMLHttpRequest/#open
   */
  public final native void open(String httpMethod, String url) /*-{
    this.open(httpMethod, url, true);
  }-*/;

  /**
   * Opens an asynchronous connection.
   * 
   * @param httpMethod the HTTP method to use
   * @param url the URL to be opened
   * @param user user to use in the URL
   * @see http://www.w3.org/TR/XMLHttpRequest/#open
   */
  public final native void open(String httpMethod, String url, String user) /*-{
    this.open(httpMethod, url, true, user);
  }-*/;

  /**
   * Opens an asynchronous connection.
   * 
   * @param httpMethod the HTTP method to use
   * @param url the URL to be opened
   * @param user user to use in the URL
   * @param password password to use in the URL
   * @see http://www.w3.org/TR/XMLHttpRequest/#open
   */
  public final native void open(String httpMethod, String url, String user,
      String password) /*-{
    this.open(httpMethod, url, true, user, password);
  }-*/;

  /**
   * Initiates a request with no request data. This simply calls
   * {@link #send(String)} with <code>null</code> as an argument, because the
   * no-argument <code>send()</code> method is unavailable on Firefox.
   */
  public final void send() {
    send(null);
  }

  /**
   * Initiates a request with data.  If there is no data, specify null.
   * 
   * @param requestData the data to be sent with the request
   * @see http://www.w3.org/TR/XMLHttpRequest/#send
   */
  public final native void send(String requestData) /*-{
    this.send(requestData);
  }-*/;

  /**
   * Sets the {@link ReadyStateChangeHandler} to be notified when the object's
   * ready-state changes.
   * 
   * <p>
   * Note: Applications <em>must</em> call {@link #clearOnReadyStateChange()}
   * when they no longer need this object, to ensure that it is cleaned up
   * properly. Failure to do so will result in memory leaks on some browsers.
   * </p>
   * 
   * @param handler the handler to be called when the ready state changes
   * @see #clearOnReadyStateChange()
   * @see http://www.w3.org/TR/XMLHttpRequest/#onreadystatechange
   */
  public final native void setOnReadyStateChange(ReadyStateChangeHandler handler) /*-{
    // The 'this' context is always supposed to point to the xhr object in the
    // onreadystatechange handler, but we reference it via closure to be extra sure.
    var _this = this;
    this.onreadystatechange = $entry(function() {
      handler.@com.google.gwt.xhr.client.ReadyStateChangeHandler::onReadyStateChange(Lcom/google/gwt/xhr/client/XMLHttpRequest;)(_this);
    });
  }-*/;

  /**
   * Sets a request header.
   * 
   * @param header the header to be set
   * @param value the header's value
   * @see http://www.w3.org/TR/XMLHttpRequest/#setrequestheader
   */
  public final native void setRequestHeader(String header, String value) /*-{
    this.setRequestHeader(header, value);
  }-*/;
}
