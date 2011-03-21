/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadingStrategy;

/**
 * Load runAsync code using a script tag. The
 * {@link com.google.gwt.core.linker.XSLinker} sets
 * <code>__gwtModuleFunction</code> to point at the function that wraps the
 * initially downloaded code. On that function is a property
 * <code>installCode</code> that can be invoked to eval more code in a scope
 * nested somewhere within that function. The loaded script for fragment 123 is
 * expected to invoke __gwtModuleFunction.runAsyncCallback123 with the code to
 * be installed.
 */
public class CrossSiteLoadingStrategy implements LoadingStrategy {
  /**
   * A trivial JavaScript map from ints to ints.
   */
  private static final class IntToIntMap extends JavaScriptObject {
    public static IntToIntMap create() {
      return (IntToIntMap) JavaScriptObject.createArray();
    }

    protected IntToIntMap() {
    }

    /**
     * Get an entry. If there is no such entry, return 0.
     */
    public native int get(int x) /*-{
      return this[x] ? this[x] : 0;
    }-*/;
    
    public native void put(int x, int y) /*-{
      this[x] = y;
    }-*/;
  }

  private static RuntimeException LoadTerminated = new RuntimeException(
      "Code download terminated");

  /**
   * Clear callbacks on script objects. This is important on IE 6 and 7 to
   * prevent a memory leak. If the callbacks aren't cleared, there is a cyclical
   * chain of references between the script tag and the function callback, and
   * IE 6/7 can't garbage collect them.
   */
  private static native void clearCallbacks(JavaScriptObject script) /*-{
    var nop = new Function('');
    script.onerror = script.onload = script.onreadystatechange = nop;
  }-*/;

  /**
   * Clear the success callback for fragment <code>fragment</code>.
   */
  private static native void clearOnSuccess(int fragment) /*-{
    delete __gwtModuleFunction['runAsyncCallback'+fragment];
  }-*/;

  private static native JavaScriptObject createScriptTag(String url) /*-{
    var head = document.getElementsByTagName('head').item(0);
    var script = document.createElement('script');
    script.src = url;
    return script;
  }-*/;

  private static native void installScriptTag(JavaScriptObject script) /*-{
    var head = document.getElementsByTagName('head').item(0);
    head.appendChild(script);
  }-*/;

  private static native JavaScriptObject removeTagAndCallErrorHandler(
      int fragment, JavaScriptObject tag,
      LoadTerminatedHandler loadFinishedHandler) /*-{
     return function(exception) {
       if (tag.parentNode == null) {
         // onSuccess or onFailure must have already been called.
         return;
       }
       var head = document.getElementsByTagName('head').item(0);
       @com.google.gwt.core.client.impl.CrossSiteLoadingStrategy::clearOnSuccess(*)(fragment);
       @com.google.gwt.core.client.impl.CrossSiteLoadingStrategy::clearCallbacks(*)(tag);
       head.removeChild(tag);
       function callLoadTerminated() {
         loadFinishedHandler.@com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler::loadTerminated(*)(exception);
       }
       $entry(callLoadTerminated)();
     }
   }-*/;

  private static native JavaScriptObject removeTagAndEvalCode(int fragment,
      JavaScriptObject tag) /*-{
     return function(code) {
       var head = document.getElementsByTagName('head').item(0);
       @com.google.gwt.core.client.impl.CrossSiteLoadingStrategy::clearOnSuccess(*)(fragment);
       @com.google.gwt.core.client.impl.CrossSiteLoadingStrategy::clearCallbacks(*)(tag);
       head.removeChild(tag);
       __gwtModuleFunction.installCode(code);
     }
   }-*/;

  private static native void setOnFailure(JavaScriptObject script,
      JavaScriptObject callback) /*-{
    var exception = @com.google.gwt.core.client.impl.CrossSiteLoadingStrategy::LoadTerminated;
    script.onerror = function() {
      callback(exception);
    }
    script.onload = function() {
      callback(exception);
    }
    script.onreadystatechange = function () {
      if (script.readyState == 'loaded' || script.readyState == 'complete') {
        script.onreadystatechange = function () { }
        callback(exception);
      }
    }
  }-*/;

  /**
   * Set the success callback for fragment <code>fragment</code>
   * to the supplied JavaScript function.
   */
  private static native void setOnSuccess(int fragment, JavaScriptObject callback) /*-{
    __gwtModuleFunction['runAsyncCallback'+fragment] = callback;
  }-*/;

  private final IntToIntMap serialNumbers = IntToIntMap.create();

  public void startLoadingFragment(int fragment,
      LoadTerminatedHandler loadFinishedHandler) {
    JavaScriptObject tag = createScriptTag(getUrl(fragment));
    setOnSuccess(fragment, removeTagAndEvalCode(fragment, tag));
    setOnFailure(tag, removeTagAndCallErrorHandler(fragment, tag,
        loadFinishedHandler));
    installScriptTag(tag);
  }

  protected String getDeferredJavaScriptDirectory() {
    return "deferredjs/";
  }

  private int getSerial(int fragment) {
    int ser = serialNumbers.get(fragment);
    serialNumbers.put(fragment, ser + 1);
    return ser;
  }

  /**
   * The URL to retrieve a fragment of code from. NOTE: this function is not
   * stable. It tweaks the URL with each call so that browsers are not tempted
   * to cache a download failure.
   */
  private String getUrl(int fragment) {
    return GWT.getModuleBaseURL() + getDeferredJavaScriptDirectory()
        + GWT.getPermutationStrongName() + "/" + fragment + ".cache.js?serial="
        + getSerial(fragment);
  }
}
