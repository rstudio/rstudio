/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.HttpDownloadFailure;

/**
 * A download strategy that uses a JSONP style script tag mechanism. and is
 * therefore cross site compatible. Note that if this strategy is used, the
 * deferred fragments must be wrapped in a callback called runAsyncCallbackX()
 * where X is the fragment number.
 * 
 * This is the default strategy for the CrossSiteIframeLinker.
 * 
 * TODO(unnurg): Try to use the ScriptInjector here
 */

public class ScriptTagLoadingStrategy extends LoadingStrategyBase {

  /**
   * Uses a JSONP style script tag mechanism to download the code.
   */
  protected static class ScriptTagDownloadStrategy implements DownloadStrategy {
    @Override
    public void tryDownload(final RequestData request) {
      int fragment = request.getFragment();
      JavaScriptObject scriptTag = createScriptTag(request.getUrl());
      setOnSuccess(fragment, onSuccess(fragment, scriptTag, request));
      setOnFailure(scriptTag, onFailure(fragment, scriptTag, request));
      installScriptTag(scriptTag);
    }
  }
  
  protected static void callOnLoadError(RequestData request) {
    request.onLoadError(new HttpDownloadFailure(request.getUrl(), 404,
      "Script Tag Failure - no status available"), true);
  }

  private static native boolean clearCallbacksAndRemoveTag(
      int fragment, JavaScriptObject scriptTag) /*-{
    if (scriptTag.parentNode == null) {
      // onSuccess or onFailure must have already been called.
      return false;
    }
    var head = document.getElementsByTagName('head').item(0);
    @com.google.gwt.core.client.impl.ScriptTagLoadingStrategy::clearOnSuccess(I)(fragment);
    @com.google.gwt.core.client.impl.ScriptTagLoadingStrategy::clearOnFailure(Lcom/google/gwt/core/client/JavaScriptObject;)(
      scriptTag);
    head.removeChild(scriptTag);
    return true;
  }-*/;
  
  private static native void clearOnFailure(JavaScriptObject scriptTag) /*-{
    scriptTag.onerror = scriptTag.onload = scriptTag.onreadystatechange = function(){};
  }-*/;

  private static native void clearOnSuccess(int fragment) /*-{
    delete __gwtModuleFunction['runAsyncCallback' + fragment];
  }-*/;
  
  private static native JavaScriptObject createScriptTag(String url) /*-{
    var head = document.getElementsByTagName('head').item(0);
    var scriptTag = document.createElement('script');
    scriptTag.src = url;
    return scriptTag;
  }-*/;

  private static native void installScriptTag(JavaScriptObject scriptTag) /*-{
    var head = document.getElementsByTagName('head').item(0);
    head.appendChild(scriptTag);
  }-*/;

  private static native JavaScriptObject onFailure(
      int fragment, JavaScriptObject scriptTag, RequestData request) /*-{
    return function() {
      if (@com.google.gwt.core.client.impl.ScriptTagLoadingStrategy::clearCallbacksAndRemoveTag(ILcom/google/gwt/core/client/JavaScriptObject;)(
        fragment, scriptTag)) {
        @com.google.gwt.core.client.impl.ScriptTagLoadingStrategy::callOnLoadError(Lcom/google/gwt/core/client/impl/LoadingStrategyBase$RequestData;)(
          request)
      }
    }
  }-*/;
  
  private static native JavaScriptObject onSuccess(int fragment,
      JavaScriptObject scriptTag, RequestData request) /*-{
    return function(code, instance) {
      if (@com.google.gwt.core.client.impl.ScriptTagLoadingStrategy::clearCallbacksAndRemoveTag(ILcom/google/gwt/core/client/JavaScriptObject;)(
        fragment, scriptTag)) {
        request.@com.google.gwt.core.client.impl.LoadingStrategyBase.RequestData::tryInstall(Ljava/lang/String;)(
          code);
      }
    }
  }-*/;
  
  private static native void setOnFailure(JavaScriptObject script,
      JavaScriptObject callback) /*-{
    script.onerror = function() {
      callback();
    }
    script.onload = function() {
      callback();
    }
    script.onreadystatechange = function () {
      if (script.readyState == 'loaded' || script.readyState == 'complete') {
        script.onreadystatechange = function () { }
        callback();
      }
    }
  }-*/;
  
  private static native void setOnSuccess(int fragment, JavaScriptObject callback) /*-{
    __gwtModuleFunction['runAsyncCallback'+fragment] = callback;
  }-*/;

  public ScriptTagLoadingStrategy() {
    super(new ScriptTagDownloadStrategy());
  } 
}
