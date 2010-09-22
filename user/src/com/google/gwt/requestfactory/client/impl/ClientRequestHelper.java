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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Map;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * A convenience class to convert a Map<String, String> to a JSON string on the
 * client side.
 */
public class ClientRequestHelper {

  private static class MyJSO extends JavaScriptObject {
    static native MyJSO create() /*-{
      return {};
    }-*/;

    @SuppressWarnings("unused")
    protected MyJSO() {
    }

    private native void put(String key, String value)/*-{
      // TODO(jgw): Find a better way to do this. Occasionally a js-wrapped
      // string ends up in 'value', which breaks the json2.js implementation
      // of JSON.stringify().
      this[key] = String(value);
    }-*/;

    private native String toJsonString()/*-{
      var gwt = this.__gwt_ObjectId;
      delete this.__gwt_ObjectId;
      var rtn = $wnd.JSON.stringify(this);
      this.__gwt_ObjectId = gwt;
      return rtn;
    }-*/;
  }

  public static String getRequestString(Map<String, String> requestData) {
    MyJSO request = MyJSO.create();
    for (String key : requestData.keySet()) {
      request.put(key, requestData.get(key));
    }
    return request.toJsonString();
  }
}
