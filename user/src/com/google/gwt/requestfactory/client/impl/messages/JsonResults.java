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
package com.google.gwt.requestfactory.client.impl.messages;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;

/**
 * JSO to hold result and related objects.
 */
public class JsonResults extends JavaScriptObject {

  public static JsonResults fromResults(String json) {
    return JsonUtils.safeEval(json).cast();
  }

  protected JsonResults() {
  }

  public final native JsonServerException getException() /*-{
    return this.exception || null;
  }-*/;

  public final native RelatedObjects getRelated() /*-{
    return this.related;
  }-*/;

  public final native Object getResult() /*-{
    return Object(this.result);
  }-*/;

  public final native SideEffects getSideEffects() /*-{
    return this.sideEffects;
  }-*/;

  public final native JsArray<ReturnRecord> getViolations()/*-{
    return this.violations || null;
  }-*/;

  /**
   * Looks for an explicit <code>{result: null}</code> in the payload.
   */
  public final native boolean isNullResult() /*-{
    return this.result === null;
  }-*/;
}