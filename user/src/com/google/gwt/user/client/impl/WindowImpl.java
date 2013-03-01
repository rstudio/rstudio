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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Native implementation associated with
 * {@link com.google.gwt.user.client.Window}.
 */
public class WindowImpl {
  private JavaScriptObject oldOnResize;
  private JavaScriptObject oldOnScroll;
  private JavaScriptObject oldOnBeforeUnload;
  private JavaScriptObject oldOnUnload;

  public native void disposeWindowCloseHandlers() /*-{
    $wnd.onbeforeunload = this.@com.google.gwt.user.client.impl.WindowImpl::oldOnBeforeUnload;
    $wnd.onunload = this.@com.google.gwt.user.client.impl.WindowImpl::oldOnUnload;
  }-*/;
  
  public native void disposeWindowResizeHandlers() /*-{
     $wnd.onresize = this.@com.google.gwt.user.client.impl.WindowImpl::oldOnResize;
   }-*/;
  
  public native void disposeWindowScrollHandlers() /*-{
     $wnd.onscroll = this.@com.google.gwt.user.client.impl.WindowImpl::oldOnScroll;
   }-*/;

  public native String getHash() /*-{
    return $wnd.location.hash;
  }-*/;

  public native String getQueryString() /*-{
    return $wnd.location.search;
  }-*/;
  
  public native void initWindowCloseHandler() /*-{
    var oldOnBeforeUnload = this.@com.google.gwt.user.client.impl.WindowImpl::oldOnBeforeUnload = $wnd.onbeforeunload;
    var oldOnUnload =  this.@com.google.gwt.user.client.impl.WindowImpl::oldOnUnload = $wnd.onunload;
    
    // Old mozilla doesn't like $entry's explicit return statement and
    // will always pop up a confirmation dialog.  This is worked around by
    // just wrapping the call to onClosing(), which still has the semantics
    // that we want.
    $wnd.onbeforeunload = function(evt) {
      var ret, oldRet;
      try {
        ret = $entry(@com.google.gwt.user.client.Window::onClosing())();
      } finally {
        oldRet = oldOnBeforeUnload && oldOnBeforeUnload(evt);
      }
      // Avoid returning null as IE6 will coerce it into a string.
      // Ensure that "" gets returned properly.
      if (ret != null) {
        return ret;
      }
      if (oldRet != null) {
        return oldRet;
      }
      // returns undefined.
    };
    
    $wnd.onunload = $entry(function(evt) {
      try {
        @com.google.gwt.user.client.Window::onClosed()();
      } finally {
        oldOnUnload && oldOnUnload(evt);
        $wnd.onresize = null;
        $wnd.onscroll = null;
        $wnd.onbeforeunload = null;
        $wnd.onunload = null;
      }
    });
  }-*/;

  public native void initWindowResizeHandler() /*-{
    var oldOnResize = this.@com.google.gwt.user.client.impl.WindowImpl::oldOnResize = $wnd.onresize;
    $wnd.onresize = $entry(function(evt) {
      try {
        @com.google.gwt.user.client.Window::onResize()();
      } finally {
        oldOnResize && oldOnResize(evt);
      }
    });
  }-*/;

  public native void initWindowScrollHandler() /*-{
    var oldOnScroll =  this.@com.google.gwt.user.client.impl.WindowImpl::oldOnScroll = $wnd.onscroll;
    $wnd.onscroll = $entry(function(evt) {
      try {
        @com.google.gwt.user.client.Window::onScroll()();
      } finally {
        oldOnScroll && oldOnScroll(evt);
      }
    });
  }-*/;
}
