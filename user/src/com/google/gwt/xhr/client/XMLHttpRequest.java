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
import com.google.gwt.typedarrays.shared.ArrayBuffer;

/**
 * The native XMLHttpRequest object. Most applications should use the higher-
 * level {@link com.google.gwt.http.client.RequestBuilder} class unless they
 * need specific functionality provided by the XMLHttpRequest object.
 * 
 * See <a href="http://www.w3.org/TR/XMLHttpRequest/"
 * >http://www.w3.org/TR/XMLHttpRequest/</a>/
 */
public class XMLHttpRequest extends JavaScriptObject {

  /**
   * The type of response expected from the XHR.
   */
  public enum ResponseType {
    /**
     * The default response type -- use {@link XMLHttpRequest#getResponseText()}
     * for the return value.
     */
    Default(""),

    /**
     * The default response type -- use
     * {@link XMLHttpRequest#getResponseArrayBuffer()} for the return value.
     * This value may only be used if
     * {@link com.google.gwt.typedarrays.shared.TypedArrays#isSupported()}
     * returns true.
     */
    ArrayBuffer("arraybuffer");

    // not implemented yet
    /*
    Blob("blob"),
    
    Document("document"),
    
    Text("text");
    */

    protected final String responseTypeString;

    private ResponseType(String responseTypeString) {
      this.responseTypeString = responseTypeString;
    }
  }
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
  public static XMLHttpRequest create() {
    return create(ResponseType.Default);
  }

  /**
   * Creates an XMLHttpRequest object.
   * 
   * @param responseType the type of response desired.  See {@link ResponseType}
   *     for limitations on using the different values
   * @return the created object
   */
  public static XMLHttpRequest create(ResponseType responseType) {
    return create(responseType.responseTypeString);
  }

  /**
   * Creates an XMLHttpRequest object.
   * 
   * @return the created object
   */
  private static native XMLHttpRequest create(String responseType) /*-{
    // Don't check window.XMLHttpRequest, because it can
    // cause cross-site problems on IE8 if window's URL
    // is javascript:'' .
    var xhr;
    if ($wnd.XMLHttpRequest) {
      xhr = new $wnd.XMLHttpRequest();
    } else {
      try {
        xhr = new $wnd.ActiveXObject('MSXML2.XMLHTTP.3.0');
      } catch (e) {
        xhr = new $wnd.ActiveXObject("Microsoft.XMLHTTP");
      }
    }
    if (xhr && responseType) {
      xhr.responsetype = responseType;
    }
    return xhr;
  }-*/;

  protected XMLHttpRequest() {
  }

  /**
   * Aborts the current request.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#abort"
   * >http://www.w3.org/TR/XMLHttpRequest/#abort</a>.
   */
  public final native void abort() /*-{
    this.abort();
  }-*/;

  /**
   * Clears the {@link ReadyStateChangeHandler}.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#onreadystatechange"
   * >http://www.w3.org/TR/XMLHttpRequest/#onreadystatechange</a>.
   * 
   * @see #clearOnReadyStateChange()
   */
  public final native void clearOnReadyStateChange() /*-{
    var self = this;
    $wnd.setTimeout(function() {
      // Using a function literal here leaks memory on ie6
      // Using the same function object kills HtmlUnit
      self.onreadystatechange = new Function();
    }, 0);
  }-*/;

  /**
   * Gets all the HTTP response headers, as a single string.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#getallresponseheaders"
   * >http://www.w3.org/TR/XMLHttpRequest/#getallresponseheaders</a>.
   * 
   * @return the response headers.
   */
  public final native String getAllResponseHeaders() /*-{
    return this.getAllResponseHeaders();
  }-*/;

  /**
   * Get's the current ready-state.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#readystate"
   * >http://www.w3.org/TR/XMLHttpRequest/#readystate</a>.
   * 
   * @return the ready-state constant
   */
  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  /**
   * Get the response as an {@link ArrayBuffer}.
   * 
   * @return an {@link ArrayBuffer} containing the response, or null if the
   *     request is in progress or failed
   */
  public final native ArrayBuffer getResponseArrayBuffer() /*-{
    return this.response;
  }-*/;

  /**
   * Gets an HTTP response header.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#getresponseheader"
   * >http://www.w3.org/TR/XMLHttpRequest/#getresponseheader</a>.
   * 
   * @param header the response header to be retrieved
   * @return the header value
   */
  public final native String getResponseHeader(String header) /*-{
    return this.getResponseHeader(header);
  }-*/;

  /**
   * Gets the response text.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#responsetext"
   * >http://www.w3.org/TR/XMLHttpRequest/#responsetext</a>.
   * 
   * @return the response text
   */
  public final native String getResponseText() /*-{
    return this.responseText;
  }-*/;

  /**
   * Gets the status code.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#status"
   * >http://www.w3.org/TR/XMLHttpRequest/#status</a>.
   * 
   * @return the status code
   */
  public final native int getStatus() /*-{
    return this.status;
  }-*/;

  /**
   * Gets the status text.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#statustext"
   * >http://www.w3.org/TR/XMLHttpRequest/#statustext</a>.
   * 
   * @return the status text
   */
  public final native String getStatusText() /*-{
    return this.statusText;
  }-*/;

  /**
   * Opens an asynchronous connection.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#open"
   * >http://www.w3.org/TR/XMLHttpRequest/#open</a>.
   * 
   * @param httpMethod the HTTP method to use
   * @param url the URL to be opened
   */
  public final native void open(String httpMethod, String url) /*-{
    this.open(httpMethod, url, true);
  }-*/;

  /**
   * Opens an asynchronous connection.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#open"
   * >http://www.w3.org/TR/XMLHttpRequest/#open</a>.
   * 
   * @param httpMethod the HTTP method to use
   * @param url the URL to be opened
   * @param user user to use in the URL
   */
  public final native void open(String httpMethod, String url, String user) /*-{
    this.open(httpMethod, url, true, user);
  }-*/;

  /**
   * Opens an asynchronous connection.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#open"
   * >http://www.w3.org/TR/XMLHttpRequest/#open</a>.
   * 
   * @param httpMethod the HTTP method to use
   * @param url the URL to be opened
   * @param user user to use in the URL
   * @param password password to use in the URL
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
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#send"
   * >http://www.w3.org/TR/XMLHttpRequest/#send</a>.
   * 
   * @param requestData the data to be sent with the request
   */
  public final native void send(String requestData) /*-{
    this.send(requestData);
  }-*/;

  /**
   * Sets the {@link ReadyStateChangeHandler} to be notified when the object's
   * ready-state changes.
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#onreadystatechange"
   * >http://www.w3.org/TR/XMLHttpRequest/#onreadystatechange</a>.
   * 
   * <p>
   * Note: Applications <em>must</em> call {@link #clearOnReadyStateChange()}
   * when they no longer need this object, to ensure that it is cleaned up
   * properly. Failure to do so will result in memory leaks on some browsers.
   * </p>
   * 
   * @param handler the handler to be called when the ready state changes
   * @see #clearOnReadyStateChange()
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
   * <p>
   * See <a href="http://www.w3.org/TR/XMLHttpRequest/#setrequestheader"
   * >http://www.w3.org/TR/XMLHttpRequest/#setrequestheader</a>.
   * 
   * @param header the header to be set
   * @param value the header's value
   */
  public final native void setRequestHeader(String header, String value) /*-{
    this.setRequestHeader(header, value);
  }-*/;
}
