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
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Class to send cross domain requests to an http server. The server will receive a request
 * including a callback url parameter, which should be used to return the response as following:
 *
 * <pre>&lt;callback&gt;(&lt;json&gt;);</pre>
 *
 * where &lt;callback&gt; is the url parameter (see {@link #setCallbackParam(String)}), and
 * &lt;json&gt; is the response to the request in json format.
 *
 * This will result on the client to call the corresponding {@link AsyncCallback#onSuccess(Object)}
 * method.
 *
 * <p>
 * If needed, errors can be handled by a separate callback:
 *
 * <pre>&lt;failureCallback&gt;(&lt;error&gt;);</pre>
 *
 * where &lt;error&gt; is a string containing an error message. This will result on the client to
 * call the corresponding {@link AsyncCallback#onFailure(Throwable)} method. See
 * {@link #setFailureCallbackParam(String)}.
 *
 * <p>
 * Example using <a href="http://code.google.com/apis/gdata/json.html#Request">JSON Google Calendar
 * GData API</a>:
 *
 * <pre>
 * String url = "http://www.google.com/calendar/feeds/developer-calendar@google.com/public/full" +
 *     "?alt=json-in-script";
 * JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
 * jsonp.requestObject(url,
 *     new AsyncCallback&lt;Feed&gt;() {
 *       public void onFailure(Throwable throwable) {
 *         Log.severe("Error: " + throwable);
 *       }
 *
 *       public void onSuccess(Feed feed) {
 *         JsArray&lt;Entry&gt; entries = feed.getEntries();
 *         for (int i = 0; i &lt; entries.length(); i++) {
 *           Entry entry = entries.get(i);
 *           Log.info(entry.getTitle() +
 *                    " (" + entry.getWhere() + "): " +
 *                    entry.getStartTime() + " -> " +
 *                    entry.getEndTime());
 *         }
 *       }
 *     });
 * </pre>
 *
 * This example uses these overlay types:
 *
 * <pre>
 * class Entry extends JavaScriptObject {
 *   protected Entry() {}
 *
 *   public final native String getTitle() &#47;*-{
 *     return this.title.$t;
 *   }-*&#47;;
 *
 *   public final native String getWhere() &#47;*-{
 *     return this.gd$where[0].valueString;
 *   }-*&#47;;
 *
 *   public final native String getStartTime() &#47;*-{
 *     return this.gd$when ? this.gd$when[0].startTime : null;
 *   }-*&#47;;
 *
 *   public final native String getEndTime() &#47;*-{
 *     return this.gd$when ? this.gd$when[0].endTime : null;
 *   }-*&#47;;
 * }
 *
 * class Feed extends JavaScriptObject {
 *   protected Feed() {}
 *
 *   public final native JsArray&lt;Entry&gt; getEntries() &#47;*-{
 *     return this.feed.entry;
 *   }-*&#47;;
 * }
 * </pre>
 *
 * </p>
 */
public class JsonpRequestBuilder {
  private int timeout = 10000;
  private String callbackParam = "callback";
  private String failureCallbackParam = null;
  private String predeterminedId = null;

  /**
   * Returns the name of the callback url parameter to send to the server. The
   * default value is "callback".
   */
  public String getCallbackParam() {
    return callbackParam;
  }

  /**
   * Returns the name of the failure callback url parameter to send to the
   * server. The default is null.
   */
  public String getFailureCallbackParam() {
    return failureCallbackParam;
  }

  /**
   * Returns the expected timeout (ms) for this request.
   */
  public int getTimeout() {
    return timeout;
  }

  public JsonpRequest<Boolean> requestBoolean(
      @IsTrustedResourceUri String url, AsyncCallback<Boolean> callback) {
    return send(url, callback, false);
  }

  public JsonpRequest<Double> requestDouble(
      @IsTrustedResourceUri String url, AsyncCallback<Double> callback) {
    return send(url, callback, false);
  }

  public JsonpRequest<Integer> requestInteger(
      @IsTrustedResourceUri String url, AsyncCallback<Integer> callback) {
    return send(url, callback, true);
  }

  /**
   * Sends a JSONP request and expects a JavaScript object as a result. The caller can either use
   * {@link com.google.gwt.json.client.JSONObject} to parse it, or use a JavaScript overlay class.
   */
  public <T extends JavaScriptObject> JsonpRequest<T> requestObject(
      @IsTrustedResourceUri String url, AsyncCallback<T> callback) {
    return send(url, callback, false);
  }

  public JsonpRequest<String> requestString(
      @IsTrustedResourceUri String url, AsyncCallback<String> callback) {
    return send(url, callback, false);
  }

  /**
   * Sends a JSONP request and does not expect any results.
   */
  public void send(@IsTrustedResourceUri String url) {
    send(url, null, false);
  }

  /**
   * Sends a JSONP request, does not expect any result, but still allows to be notified when the
   * request has been executed on the server.
   */
  public JsonpRequest<Void> send(@IsTrustedResourceUri String url, AsyncCallback<Void> callback) {
    return send(url, callback, false);
  }

  /**
   * @param callbackParam The name of the callback url parameter to send to the server. The default
   *     value is "callback".
   */
  public void setCallbackParam(String callbackParam) {
    this.callbackParam = callbackParam;
  }

  /**
   * @param failureCallbackParam The name of the failure callback url parameter to send to the
   *     server. The default is null.
   */
  public void setFailureCallbackParam(String failureCallbackParam) {
    this.failureCallbackParam = failureCallbackParam;
  }

  public void setPredeterminedId(String id) {
    this.predeterminedId = id;
  }
  
  /**
   * @param timeout The expected timeout (ms) for this request. The default is 10s.
   */
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  private <T> JsonpRequest<T> send(
      @IsTrustedResourceUri String url, AsyncCallback<T> callback, boolean expectInteger) {
    JsonpRequest<T> request;
    if (predeterminedId != null) {
      request = new JsonpRequest<T>(callback, timeout, expectInteger, callbackParam,
          failureCallbackParam, predeterminedId);
    } else {
      request = new JsonpRequest<T>(callback, timeout, expectInteger, callbackParam,
          failureCallbackParam);
    }
    request.send(url);
    return request;
  }
}
