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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Command;

/**
 * IE implementation of {@link com.google.gwt.user.client.impl.WindowImpl}.
 */
public class WindowImplIE extends WindowImpl {

  /**
   * For IE6, reading from $wnd.location.hash drops part of the fragment if the
   * fragment contains a '?'. To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getHash() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.lastIndexOf("#");
    return (hashLoc > 0) ? href.substring(hashLoc) : "";
  }-*/;

  /**
   * For IE6, reading from $wnd.location.search gets confused if hash contains
   * a '?'. To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getQueryString() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.lastIndexOf("#");
    if (hashLoc >= 0) {
      // strip off any hash first
      href = href.substring(0, hashLoc);
    }
    var questionLoc = href.lastIndexOf("?");
    return (questionLoc > 0) ? href.substring(questionLoc) : "";
  }-*/;

  @Override
  public void initWindowCloseHandler() {
    initHandler(getWindowCloseHandlerMethodString(),
        "__gwt_initWindowCloseHandler", new Command() {
          public void execute() {
            initWindowCloseHandlerImpl();
          }
        });
  }

  @Override
  public void initWindowResizeHandler() {
    initHandler(getWindowResizeHandlerMethodString(),
        "__gwt_initWindowResizeHandler", new Command() {
          public void execute() {
            initWindowResizeHandlerImpl();
          }
        });
  }

  @Override
  public void initWindowScrollHandler() {
    initHandler(getWindowScrollHandlerMethodString(),
        "__gwt_initWindowScrollHandler", new Command() {
          public void execute() {
            initWindowScrollHandlerImpl();
          }
        });
  }

  /**
   * This method defines a function that sinks an event on the Window.  However,
   * this method returns the function as a String so it can be added to the
   * outer window.
   * 
   * We need to declare this method on the outer window because you cannot
   * attach Window listeners from within an iframe on IE6.
   * 
   * Per ECMAScript 262 spec 15.3.4.2, Function.prototype.toString() returns a
   * string representation of the function that has the syntax of the function.
   */
  private native String getWindowCloseHandlerMethodString() /*-{
    return function(beforeunload, unload) {
      var wnd = window
      , oldOnBeforeUnload = wnd.onbeforeunload
      , oldOnUnload = wnd.onunload;
      
      wnd.onbeforeunload = function(evt) {
        var ret, oldRet;
        try {
          ret = beforeunload();
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
      
      wnd.onunload = function(evt) {
        try {
          unload();
        } finally {
          oldOnUnload && oldOnUnload(evt);
          wnd.onresize = null;
          wnd.onscroll = null;
          wnd.onbeforeunload = null;
          wnd.onunload = null;
        }
      };
      
      // Remove the reference once we've initialize the handler
      wnd.__gwt_initWindowCloseHandler = undefined;
    }.toString();
  }-*/;

  /**
   * @see #getWindowCloseHandlerMethodString()
   */
  private native String getWindowResizeHandlerMethodString() /*-{
    return function(resize) {
      var wnd = window, oldOnResize = wnd.onresize;
      
      wnd.onresize = function(evt) {
        try {
          resize();
        } finally {
          oldOnResize && oldOnResize(evt);
        }
      };
      
      // Remove the reference once we've initialize the handler
      wnd.__gwt_initWindowResizeHandler = undefined;
    }.toString();
  }-*/;

  /**
   * @see #getWindowCloseHandlerMethodString()
   */
  private native String getWindowScrollHandlerMethodString() /*-{
    return function(scroll) {
      var wnd = window, oldOnScroll = wnd.onscroll;
      
      wnd.onscroll = function(evt) {
        try {
          scroll();
        } finally {
          oldOnScroll && oldOnScroll(evt);
        }
      };
      
      // Remove the reference once we've initialize the handler
      wnd.__gwt_initWindowScrollHandler = undefined;
    }.toString();
  }-*/;

  /**
   * IE6 does not allow direct access to event handlers on the parent window,
   * so we must embed a script in the parent window that will set the event
   * handlers in the correct context.
   * 
   * @param initFunc the string representation of the init function
   * @param funcName the name to assign to the init function
   * @param cmd the command to execute the init function
   */
  private void initHandler(String initFunc, String funcName, Command cmd) {
    if (GWT.isClient()) {
      // Embed the init script on the page
      initFunc = initFunc.replaceFirst("function", "function " + funcName);
      ScriptElement scriptElem = Document.get().createScriptElement(initFunc);
      Document.get().getBody().appendChild(scriptElem);
  
      // Initialize the handler
      cmd.execute();
  
      // Remove the script element
      Document.get().getBody().removeChild(scriptElem);
    }
  }

  private native void initWindowCloseHandlerImpl() /*-{
    $wnd.__gwt_initWindowCloseHandler(
      function() {
        return @com.google.gwt.user.client.Window::onClosing()();
      },
      function() {
        @com.google.gwt.user.client.Window::onClosed()();
      }
    );
  }-*/;

  private native void initWindowResizeHandlerImpl() /*-{
    $wnd.__gwt_initWindowResizeHandler(
      function() {
        @com.google.gwt.user.client.Window::onResize()();
      }
    );
  }-*/;

  private native void initWindowScrollHandlerImpl() /*-{
    $wnd.__gwt_initWindowScrollHandler(
      function() {
        @com.google.gwt.user.client.Window::onScroll()();
      }
    );
  }-*/;

}
