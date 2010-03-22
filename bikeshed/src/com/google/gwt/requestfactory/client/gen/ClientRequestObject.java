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
package com.google.gwt.requestfactory.client.gen;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Map;

/**
 * A convenience class to convert a Map<String, String> to a Json string on the
 * client side.
 */
public class ClientRequestObject {

  public static String getRequestString(Map<String, String> requestData) {
    ClientRequestObject requestObject = new ClientRequestObject();
    requestObject.init();
    for (String key : requestData.keySet()) {
      requestObject.put(key, requestData.get(key));
    }
    return requestObject.toJsonString();
  }

  private JavaScriptObject map;

  ClientRequestObject() {
    init();
  }

  private native void init()/*-{
    this.@com.google.gwt.requestfactory.client.gen.ClientRequestObject::map = {};
  }-*/;

  private native void put(String key, String value)/*-{
    this.@com.google.gwt.requestfactory.client.gen.ClientRequestObject::map[key] = value;
  }-*/;

  private native String toJsonString()/*-{
    return JSON.stringify(this.@com.google.gwt.requestfactory.client.gen.ClientRequestObject::map);
  }-*/;
}
