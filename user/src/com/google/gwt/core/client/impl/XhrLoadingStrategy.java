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

import com.google.gwt.core.client.impl.AsyncFragmentLoader.HttpDownloadFailure;
import com.google.gwt.core.client.impl.LoadingStrategyBase.RequestData;

/**
 * A download strategy that uses XHRs and iss therefore not cross site compatible.
 * 
 * This is the default strategy for the IframeLinker.
 */
public class XhrLoadingStrategy extends LoadingStrategyBase {

  /**
   * Uses XHR's to download the code.
   */
  public static class XhrDownloadStrategy implements DownloadStrategy {
    @Override
    public native void tryDownload(final RequestData request)/*-{
      var xhr = new $wnd.XMLHttpRequest()
      xhr.open("GET", request.@RequestData::getUrl()());
      xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
          // Clearing onreadystatechange otherwise it may cause memory leak (e.g. in IE8).
          xhr.onreadystatechange = function() {}; // Clear callback
          @XhrLoadingStrategy::onLoad(*)(request, xhr.status, xhr.statusText, xhr.responseText);
        }
      };
      xhr.send(null);
    }-*/;
  }

  /**
   * Some UA's like Safari will have a "0" status code when loading from file:
   * URLs. Additionally, the "0" status code is used sometimes if the server
   * does not respond, e.g. if there is a connection refused.
   */
  private static final int HTTP_STATUS_NON_HTTP = 0;

  private static final int HTTP_STATUS_OK = 200;

  @SuppressWarnings("unused") // Called via JSNI
  private static void onLoad(RequestData request, int status, String statusText, String response) {
    if ((status == HTTP_STATUS_OK || status == HTTP_STATUS_NON_HTTP)
        && response != null
        && response.length() != 0) {
      request.tryInstall(response);
    } else {
      // If the download fails
      request.onLoadError(new HttpDownloadFailure(request.getUrl(), status, statusText), true);
    }
  }

  public XhrLoadingStrategy() {
    super(new XhrDownloadStrategy());
  }

}
