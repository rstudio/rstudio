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


public class HostEntry extends JavaScriptObject{  
  
  protected HostEntry() {}
  
  public static HostEntry create(String url, boolean include) {
    HostEntry entry = JavaScriptObject.createObject().cast();
    entry.setUrl(url);
    entry.setInclude(include);
    return entry;
  }
    
  public final native String getUrl() /*-{
    return this.url;
  }-*/;
  
  public final native void setUrl(String url) /*-{
    this.url = url;
  }-*/;
  
  public final native void setInclude(boolean include) /*-{
    this.include = include;
  }-*/;
  
  public final native boolean include() /*-{
    return this.include;
  }-*/;

  public final boolean isEqual(HostEntry host) {
    return this.getUrl().equals(host.getUrl());
  }
}
