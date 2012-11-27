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

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.HttpDownloadFailure;

/**
 * A download strategy that uses a JSONP style script tag mechanism. and is
 * therefore cross site compatible. Note that if this strategy is used, the
 * deferred fragments must be wrapped in a callback called runAsyncCallbackX()
 * where X is the fragment number.
 *
 * This is the default strategy for the CrossSiteIframeLinker.
 */

public class ScriptTagLoadingStrategy extends LoadingStrategyBase {

  /**
   * Uses a JSONP style script tag mechanism to download the code.
   */
  protected static class ScriptTagDownloadStrategy implements DownloadStrategy {
    @Override
    public void tryDownload(final RequestData request) {
      setAsyncCallback(request.getFragment(), request);

      ScriptInjector.fromUrl(request.getUrl()).setRemoveTag(true).setCallback(
        new Callback<Void, Exception>() {
          @Override
          public void onFailure(Exception reason) {
            cleanup(request);
          }

          @Override
          public void onSuccess(Void result) {
            cleanup(request);
          }
      }).inject();
    }
  }

  private static void asyncCallback(RequestData request, String code) {
    boolean firstTimeCalled = clearAsyncCallback(request.getFragment());
    if (firstTimeCalled) {
      request.tryInstall(code);
    }
  }

  private static void cleanup(RequestData request) {
    boolean neverCalled = clearAsyncCallback(request.getFragment());
    if (neverCalled) {
      request.onLoadError(new HttpDownloadFailure(request.getUrl(), 404,
        "Script Tag Failure - no status available"), true);
    }
  }

  /**
   * Returns true if the callback existed.
   */
  private static native boolean clearAsyncCallback(int fragment) /*-{
    if (!__gwtModuleFunction['runAsyncCallback' + fragment]) {
      return false;
    }
    delete __gwtModuleFunction['runAsyncCallback' + fragment];
    return true;
  }-*/;

  private static native void setAsyncCallback(int fragment, RequestData request) /*-{
    __gwtModuleFunction['runAsyncCallback' + fragment] = $entry(function(code, instance) {
      @com.google.gwt.core.client.impl.ScriptTagLoadingStrategy::asyncCallback(Lcom/google/gwt/core/client/impl/LoadingStrategyBase$RequestData;Ljava/lang/String;)(
        request, code);
    });
  }-*/;

  public ScriptTagLoadingStrategy() {
    super(new ScriptTagDownloadStrategy());
  }
}
