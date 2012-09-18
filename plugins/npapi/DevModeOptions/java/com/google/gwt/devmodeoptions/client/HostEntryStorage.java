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
import com.google.gwt.core.client.JsArray;

public class HostEntryStorage {
  private static HostEntryStorage singleton = new HostEntryStorage();
  private static final String HOST_ENTRY_KEY = "GWT_DEV_HOSTENTRY";
  
  public static HostEntryStorage get() {
    return singleton;
  }
  
  private final LocalStorage localStorage;
  
  private HostEntryStorage() {
    localStorage = getLocalStorage();
  }

  private static native LocalStorage getLocalStorage() /*-{
    return $wnd.localStorage;
  }-*/;
  
  public JsArray<HostEntry> getHostEntries() {
    JsArray<HostEntry> entries = localStorage.getItem(HOST_ENTRY_KEY).cast();
    if (entries == null) {
      return JavaScriptObject.createArray().cast();
    } else {
      return entries;
    }
  }
  
  public void saveEntries(JsArray<HostEntry> entries) {
    localStorage.setItem(HOST_ENTRY_KEY, entries.cast());
  }
  
}
