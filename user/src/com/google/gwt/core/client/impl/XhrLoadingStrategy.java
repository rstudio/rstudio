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
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * A download strategy that uses XHRs and iss therefore not cross site compatible.
 * 
 * This is the default strategy for the IframeLinker.
 */
public class XhrLoadingStrategy extends LoadingStrategyBase {

  /**
   * Uses XHR's to download the code.
   */
  protected static class XhrDownloadStrategy implements DownloadStrategy {
    @Override
    public void tryDownload(final RequestData request) {
      final XMLHttpRequest xhr = XMLHttpRequest.create();

      xhr.open(HTTP_GET, request.getUrl());

      xhr.setOnReadyStateChange(new ReadyStateChangeHandler() {
        public void onReadyStateChange(XMLHttpRequest ignored) {
          if (xhr.getReadyState() == XMLHttpRequest.DONE) {
            xhr.clearOnReadyStateChange();
            if ((xhr.getStatus() == HTTP_STATUS_OK || xhr.getStatus() == HTTP_STATUS_NON_HTTP)
                && xhr.getResponseText() != null
                && xhr.getResponseText().length() != 0) {
              request.tryInstall(xhr.getResponseText());
            } else {
              // If the download fails
              request.onLoadError(
                  new HttpDownloadFailure(request.getUrl(), xhr.getStatus(),
                      xhr.getStatusText()), true);
            }
          }
        }
      });

      xhr.send();
    }
  }

  static final String HTTP_GET = "GET";

  /**
   * Some UA's like Safari will have a "0" status code when loading from file:
   * URLs. Additionally, the "0" status code is used sometimes if the server
   * does not respond, e.g. if there is a connection refused.
   */
  static final int HTTP_STATUS_NON_HTTP = 0;

  static final int HTTP_STATUS_OK = 200;

  public XhrLoadingStrategy() {
    super(new XhrDownloadStrategy());
  }

}