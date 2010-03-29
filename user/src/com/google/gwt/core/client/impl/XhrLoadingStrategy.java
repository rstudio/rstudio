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
import com.google.gwt.core.client.impl.AsyncFragmentLoader.HttpInstallFailure;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadingStrategy;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * The standard loading strategy used in a web browser. The linker it is used
 * with should provide JavaScript-level functions to indicate how to handle
 * downloading and installing code. There is support to use XHR for the
 * download.
 * 
 * Linkers should always provide a function
 * <code>__gwtStartLoadingFragment</code>. This function is called by
 * AsyncFragmentLoader with two arguments: an integer fragment number that needs
 * to be downloaded, and a one-argument loadFinished function. If the load
 * fails, that function should be called with a descriptive exception as the
 * argument. If the load succeeds, that function may also be called, so long as
 * it isn't called until the downloaded code has been installed.
 * 
 * 
 * If the mechanism for loading the contents of fragments is provided by the
 * linker, the <code>__gwtStartLoadingFragment</code> function should return
 * <code>null</code> or <code>undefined</code>.
 * 
 * Alternatively, the function can return a URL designating from where the code
 * for the requested fragment can be downloaded. In that case, the linker should
 * also provide a function <code>__gwtInstallCode</code> for actually installing
 * the code once it is downloaded. That function will be passed the loaded code
 * once it has been downloaded.
 */
public class XhrLoadingStrategy implements LoadingStrategy {
  
  /**
   * A {@link MockableXMLHttpRequest} that is really just a vanilla
   * XMLHttpRequest.  This wrapper (and thus {@code MockableXMLHttpRequest} is
   * needed because so much of {@link XMLHttpRequest} is final, which in turn
   * is because it extends {@code JavaScriptObject} and is subject to its
   * restrictions.
   * 
   * It is important that these methods be simple enough to be inlined away.
   */
  class DelegatingXMLHttpRequest implements MockableXMLHttpRequest {
    private final XMLHttpRequest delegate;

    public DelegatingXMLHttpRequest(XMLHttpRequest xmlHttpRequest) {
      delegate = xmlHttpRequest;
    }

    public void clearOnReadyStateChange() {
      delegate.clearOnReadyStateChange();
    }

    public int getReadyState() {
      return delegate.getReadyState();
    }

    public String getResponseText() {
      return delegate.getResponseText();
    }

    public int getStatus() {
      return delegate.getStatus();
    }

    public String getStatusText() {
      return delegate.getStatusText();
    }

    public void open(String method, String url) {
      delegate.open(method, url);
    }

    public void send() {
      delegate.send();
    }

    public void setOnReadyStateChange(ReadyStateChangeHandler handler) {
      delegate.setOnReadyStateChange(handler);
    }

    public void setRequestHeader(String header, String value) {
      delegate.setRequestHeader(header, value);
    }
  }

  /**
   * Delegates to the real XMLHttpRequest, except in test when we make a mock
   * to jump through error/retry hoops.
   */
  interface MockableXMLHttpRequest {
    void clearOnReadyStateChange();
    int getReadyState();
    String getResponseText();
    int getStatus();
    String getStatusText();
    void open(String method, String url);
    void send();
    void setOnReadyStateChange(ReadyStateChangeHandler handler);
    void setRequestHeader(String header, String value);
  }

  /**
   * Since LoadingStrategy must support concurrent requests, including figuring
   * which is which in the onLoadError handling, we need to keep track of this
   * data for each outstanding request, which we index by xhr object.
   */
  protected class RequestData {
    String url;
    int retryCount;
    LoadTerminatedHandler errorHandler = null;
    
    public RequestData(String url, LoadTerminatedHandler errorHandler) {
      this.url = url;
      this.errorHandler = errorHandler;
      this.retryCount = 0;
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

  /**
   * For error logging, max length of fragment response text to include in
   * failed-to-install exception message.
   */
  private static final int MAX_LOG_LENGTH = 200;
  
  /**
   * Number of retry attempts for a single fragment.  If a fragment download
   * fails, we try again this many times before "really" failing out to user
   * error-handling code.  If a fragment downloads but doesn't install, we
   * don't retry at all.
   */
  private static final int MAX_RETRY_COUNT = 3;

  public void startLoadingFragment(int fragment,
      final LoadTerminatedHandler loadErrorHandler) {
    String url = gwtStartLoadingFragment(fragment, loadErrorHandler);
    if (url == null) {
      // The download has already started; nothing more to do
      return;
    }

    RequestData request = new RequestData(url, loadErrorHandler);
    tryLoad(request);
  }
  
  /**
   * Overridable for tests.
   */
  protected MockableXMLHttpRequest createXhr() {
    return new DelegatingXMLHttpRequest(XMLHttpRequest.create());
  }

  /**
   * Call the linker-supplied <code>__gwtInstallCode</code> method. See the
   * {@link AsyncFragmentLoader class comment} for more details.
   */
  protected native void gwtInstallCode(String text) /*-{
    __gwtInstallCode(text);
  }-*/;
  
  /**
   * Call the linker-supplied __gwtStartLoadingFragment function. It should
   * either start the download and return null or undefined, or it should
   * return a URL that should be downloaded to get the code. If it starts the
   * download itself, it can synchronously load it, e.g. from cache, if that
   * makes sense.
   */
  protected native String gwtStartLoadingFragment(int fragment,
      LoadTerminatedHandler loadErrorHandler) /*-{
    function loadFailed(e) {
      loadErrorHandler.@com.google.gwt.core.client.impl.AsyncFragmentLoader$LoadTerminatedHandler::loadTerminated(*)(e);
    }
    return __gwtStartLoadingFragment(fragment, loadFailed);
  }-*/;

  /**
   * Error recovery from loading or installing code.
   * @param request the requestData of this request
   * @param e exception of the error
   * @param mayRetry {@code true} if retrying might be helpful
   */
  protected void onLoadError(RequestData request, Throwable e, boolean mayRetry) {
    if (mayRetry) {
      request.retryCount++;
      if (request.retryCount < MAX_RETRY_COUNT) {
        tryLoad(request);
        return;
      }
    }
    request.errorHandler.loadTerminated(e); 
  }

  /**
   * Makes a single load-and-install attempt.
   */
  protected void tryLoad(final RequestData request) {
    final MockableXMLHttpRequest xhr = createXhr();
    
    xhr.open(HTTP_GET, request.url);
    if (request.retryCount > 0) {
      // disable caching if we have to retry; one cause could be bad cache
      xhr.setRequestHeader("Cache-Control", "no-cache");
    }

    xhr.setOnReadyStateChange(new ReadyStateChangeHandler() {
      public void onReadyStateChange(XMLHttpRequest ignored) {
        if (xhr.getReadyState() == XMLHttpRequest.DONE) {
          xhr.clearOnReadyStateChange();
          if ((xhr.getStatus() == HTTP_STATUS_OK || xhr.getStatus() == HTTP_STATUS_NON_HTTP)
              && xhr.getResponseText() != null
              && xhr.getResponseText().length() != 0) {
            try {
              gwtInstallCode(xhr.getResponseText());
            } catch (RuntimeException e) {
              String textIntro = xhr.getResponseText();
              if (textIntro != null && textIntro.length() > MAX_LOG_LENGTH) {
                textIntro = textIntro.substring(0, MAX_LOG_LENGTH) + "...";
              }
              onLoadError(request, 
                  new HttpInstallFailure(request.url, textIntro, e), false);
            }
          } else {
            onLoadError(request,
                new HttpDownloadFailure(request.url, xhr.getStatus(),
                    xhr.getStatusText()), true);
          }
        }
      }
    });

    xhr.send();
  }
}