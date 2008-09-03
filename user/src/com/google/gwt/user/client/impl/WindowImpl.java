/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.Command;

/**
 * Native implementation associated with
 * {@link com.google.gwt.user.client.Window}.
 */
public class WindowImpl {
  public native void enableScrolling(boolean enable) /*-{
   @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.style.overflow =
       enable ? "" : "hidden";
  }-*/;

  public native int getClientHeight() /*-{
   return @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientHeight;
  }-*/;

  public native int getClientWidth() /*-{
   return @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientWidth;
  }-*/;

  public native String getHash() /*-{
    return $wnd.location.hash;
  }-*/;

  public native String getQueryString() /*-{
    return $wnd.location.search;
  }-*/;
  
  public native int getScrollLeft() /*-{
   return @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollLeft;
  }-*/;

  public native int getScrollTop() /*-{
   return @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollTop;
  }-*/;

  public void initHandler(String initFunc, String funcName, Command cmd) {
    // Eval the init script
    initFunc = initFunc.replaceFirst("function", "function " + funcName);
    eval(initFunc);

    // Initialize the handler
    cmd.execute();
  }

  private native void eval(String expr) /*-{
    $wnd.eval(expr);
  }-*/;
}
