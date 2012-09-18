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

package com.google.gwt.devmodeoptions.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Simple wrapper around HTML5 <a
 * href="http://dev.w3.org/html5/webstorage/#the-localstorage-attribute">local
 * storage</a> API.
 */
public class LocalStorage extends JavaScriptObject {
  protected LocalStorage() {
  }

  public final native void clear() /*-{
    this.clear();
  }-*/;

  public final native JavaScriptObject getItem(String key) /*-{
    return JSON.parse(this.getItem(key));
  }-*/;

  public final native String getKey(int index) /*-{
    return this.key(index);
  }-*/;

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native String getStringItem(String key) /*-{
    return this.getItem(key);
  }-*/;

  public final native void removeItem(String key) /*-{
    this.removeItem(key);
  }-*/;

  public final native void setItem(String key, JavaScriptObject dataObject) /*-{
    // Note, as of FF3.6, gecko does not support storing an object (only strings).
    this.setItem(key, JSON.stringify(dataObject));
  }-*/;

  public final native void setStringItem(String key, String dataString) /*-{
    this.setItem(key, dataString);
  }-*/;
}
