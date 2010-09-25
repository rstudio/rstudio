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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.client.impl.json.ClientJsonUtil;

/**
 * JSO to hold result and related objects.
 */
class JsonResults extends JavaScriptObject {

  static JsonResults fromResults(String json) {
    return ClientJsonUtil.parse(json).cast();
  }

  protected JsonResults() {
  }

  final native JsonServerException getException() /*-{
    return this.exception || null;
  }-*/;

  final native JavaScriptObject getRelated() /*-{
    return this.related;
  }-*/;

  final native Object getResult() /*-{
    return Object(this.result);
  }-*/;

  final native JavaScriptObject getSideEffects() /*-{
    return this.sideEffects;
  }-*/;
  
  final native JsArray<DeltaValueStoreJsonImpl.ReturnRecord> getViolations()/*-{
    return this.violations || null;
  }-*/;
  
  /**
   * Looks for an explicit <code>{result: null}</code> in the payload.
   */
  final native boolean isNullResult() /*-{
    return this.result === null;
  }-*/;
}