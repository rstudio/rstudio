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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Sends stats events for RPC calls. 
 */
public class RpcStatsContext {
  /**
   * A global counter/generator for ids to track any given request.
   */
  private static int requestIdCounter;

  static int getNextRequestId() {
    return requestIdCounter++;
  }

  static int getLastRequestId() {
    return requestIdCounter;
  }

  private int requestId;

  public RpcStatsContext() {
    this(getNextRequestId());
  }

  public RpcStatsContext(int requestId) {
    this.requestId = requestId;
  }

  public native JavaScriptObject bytesStat(String method, int bytes, String eventType) /*-{
    var stat = this.@com.google.gwt.user.client.rpc.impl.RpcStatsContext::timeStat(Ljava/lang/String;Ljava/lang/String;)(method, eventType);
    stat.bytes = bytes;
    return stat;
  }-*/;

  public int getRequestId() {
    return requestId;
  }

  /**
   * Indicates if RPC statistics should be gathered.
   */
  public native boolean isStatsAvailable() /*-{
    return !!$stats;
  }-*/;

  /**
   * Always use this as {@link #isStatsAvailable()} &amp;&amp;
   * {@link #stats(JavaScriptObject)}.
   */
  public native boolean stats(JavaScriptObject data) /*-{
    return $stats(data);
  }-*/;

  public native JavaScriptObject timeStat(String method, String eventType) /*-{
    return {
      moduleName: @com.google.gwt.core.client.GWT::getModuleName()(),
      sessionId: $sessionId,
      subSystem: 'rpc',
      evtGroup: this.@com.google.gwt.user.client.rpc.impl.RpcStatsContext::requestId,
      method: method,
      millis: (new Date()).getTime(),
      type: eventType
    };
  }-*/;

  /**
   * @param method
   * @param result
   * @param eventType
   */
  public JavaScriptObject timeStat(String method, Object result, String eventType) {
    return timeStat(method, eventType);
  }
}
