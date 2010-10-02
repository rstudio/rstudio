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

/**
 * Provides access to the changes that occurred on the server.
 */
public final class SideEffects extends JavaScriptObject {
  protected SideEffects() {
  }

  public native JsArray<ReturnRecord> getDelete() /*-{
    return this['DELETE'];
  }-*/;

  public native JsArray<ReturnRecord> getPersist() /*-{
    return this['PERSIST'];
  }-*/;

  public native JsArray<ReturnRecord> getUpdate() /*-{
    return this['UPDATE'];
  }-*/;
}
