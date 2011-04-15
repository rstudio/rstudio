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

package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadingStrategy;

/**
 * Load runAsync code using a script tag. Intended for use with the
 * {@link com.google.gwt.core.linker.CrossSiteIframeLinker}.
 */
public class CrossSiteIframeLoadingStrategy implements LoadingStrategy {
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
         // This function must have already been called.
         return;
       }
       var head = document.getElementsByTagName('head').item(0);
       @com.google.gwt.core.client.impl.CrossSiteIframeLoadingStrategy::clearCallbacks(*)(tag);
       head.removeChild(tag);
       // It seems unintuitive to call the error function every time, but
       // it appears that AsyncFragmentLoader::fragmentHasLoaded (which is
       // called by each fragment) will set the fragmentLoading variable to
       // -1 when the code in this fragment executes, so this
       // loadTerminated call will fail the (fragmentLoading == fragment) check
       // and will immediately exit, so no errors are actually fired.
       function callLoadTerminated() {
         loadFinishedHandler.@com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler::loadTerminated(*)(exception);
       }
       $entry(callLoadTerminated)();
     }
   }-*/;
  
  private static native void setOnTerminated(JavaScriptObject script,
      JavaScriptObject callback) /*-{
    script.onerror = function() {
      var error = "Code download terminated, onerror called. Script src = " + script.src;
      callback(@com.google.gwt.core.client.CodeDownloadException::new(Ljava/lang/String;)(error));
    }
    script.onload = function() {
      var error = "Code download terminated, onload called. Script src = " + script.src;
      callback(@com.google.gwt.core.client.CodeDownloadException::new(Ljava/lang/String;)(error));
    }
    script.onreadystatechange = function () {
      if (script.readyState == 'loaded') {
        var error = "Code download terminated, readystate is loaded. Script src = " + script.src;
        callback(@com.google.gwt.core.client.CodeDownloadException::new(Ljava/lang/String;)(error));
      }
      if (script.readyState == 'complete') {
        var error = "Code download terminated, readystate is complete. Script src = " + script.src;
        callback(@com.google.gwt.core.client.CodeDownloadException::new(Ljava/lang/String;)(error));
      }
    }
  }-*/;

  private final IntToIntMap serialNumbers = IntToIntMap.create();

  public void startLoadingFragment(int fragment,
      LoadTerminatedHandler loadFinishedHandler) {
    JavaScriptObject tag = createScriptTag(getUrl(fragment));
    setOnTerminated(tag, removeTagAndCallErrorHandler(fragment, tag,
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
    // Not appending serial=N to the first attempt improves proxy caching
    // and makes it easier to write HTML5 AppCache manifests. 
    int serial = getSerial(fragment);
    String parameters = ((serial == 0) ? "" : ("?serial=" + serial));
    return GWT.getModuleBaseURL() + getDeferredJavaScriptDirectory()
        + GWT.getPermutationStrongName() + "/" + fragment + ".cache.js"
        + parameters;
  }
}
