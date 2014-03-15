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
import com.google.gwt.core.client.impl.AsyncFragmentLoader.HttpInstallFailure;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadTerminatedHandler;
import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadingStrategy;

/**
 * Base for a standard loading strategy used in a web browser. Subclasses
 * provide an implementation of DownloadStrategy, thereby controlling how
 * the download of the code will be done, while the base class controls how
 * to interact with the linker, handle download failures, etc.
 *
 * The linker it is used with should provide JavaScript-level functions to
 * indicate how to handle downloading and installing code.
 *
 * Linkers should always provide a function
 * <code>__gwtStartLoadingFragment</code>. This function is called by
 * this class with two arguments: an integer fragment number that needs
 * to be downloaded, and a one-argument loadFinished function. If the load
 * fails, that function should be called with a descriptive exception as the
 * argument. If the load succeeds, that function may also be called, so long as
 * it isn't called until the downloaded code has been installed.
 *
 * If the mechanism for loading the contents of fragments is provided by the
 * linker, the <code>__gwtStartLoadingFragment</code> function should return
 * <code>null</code> or <code>undefined</code>, and the linker should handle
 * installing the code as well.  Note that in this case, all the code in this
 * class is pretty much moot.
 *
 * Alternatively, the function can return a URL designating from where the code
 * for the requested fragment can be downloaded. In that case, the linker should
 * also provide a function <code>__gwtInstallCode</code> for actually installing
 * the code once it is downloaded. That function will be passed the loaded code
 * once it has been downloaded.
 */
public class LoadingStrategyBase implements LoadingStrategy {
  /**
   * Subclasses will need to implement this and pass it in in the constructor.
   * This is how they control how the download will be done (XHR, Script tag, etc.)
   * If the download succeeds, the DownloadStrategy should call it's
   * RequestData's tryInstall() function, and if it fails, it should call it's
   * RequestData's onLoadError() function.
   */
  protected interface DownloadStrategy {
    void tryDownload(final RequestData request);
  }

  /**
   * A trivial JavaScript map from ints to ints.  Used to keep a global count of
   * how many times a user has manually retried fetching a fragment.
   */
  private static final class FragmentReloadTracker extends JavaScriptObject {
    public static FragmentReloadTracker create() {
      return (FragmentReloadTracker) JavaScriptObject.createArray();
    }

    protected FragmentReloadTracker() { }

    public native int get(int x) /*-{
      return this[x] ? this[x] : 0;
    }-*/;

    public native void put(int x, int y) /*-{
      this[x] = y;
    }-*/;
  }

  /**
   * Since LoadingStrategy must support concurrent requests, we keep most of the
   * relevant info in the RequestData, and pass it around.  Once created, a
   * RequestData interacts primarily with it's DownloadStrategy, which will
   * attempt call out to the RequestData's tryInstall function if the download
   * succeeds, or it's onLoadError if the download fails.
   */
  protected static class RequestData {
    private static final int MAX_LOG_LENGTH = 200;

    private DownloadStrategy downloadStrategy;
    private LoadTerminatedHandler errorHandler = null;
    private int fragment;
    private int maxRetryCount;
    private String originalUrl;
    private int retryCount;
    private String url;

    public RequestData(String url, LoadTerminatedHandler errorHandler,
        int fragment, DownloadStrategy downloadStrategy, int maxRetryCount) {
      this.url = url;
      this.originalUrl = url;
      this.errorHandler = errorHandler;
      this.maxRetryCount = maxRetryCount;
      this.retryCount = 0;
      this.fragment = fragment;
      this.downloadStrategy = downloadStrategy;
    }

    public LoadTerminatedHandler getErrorHandler() { return errorHandler; }

    public int getFragment() { return fragment; }

    public int getRetryCount() { return retryCount; }

    protected void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
    }

    public String getUrl() { return url; }

    protected void setUrl(String url) {
      this.url = url;
    }

    public String getOriginalUrl() { return originalUrl; }

    public void onLoadError(Throwable e, boolean mayRetry) {
      if (mayRetry) {
        retryCount++;
        if (retryCount <= maxRetryCount) {
          char connector = originalUrl.contains("?") ? '&' : '?';
          url = originalUrl + connector + "autoRetry=" + retryCount;
          downloadStrategy.tryDownload(this);
          return;
        }
      }
      errorHandler.loadTerminated(e);
    }

    public void tryDownload() {
      downloadStrategy.tryDownload(this);
    }

    public void tryInstall(String code) {
      try {
        gwtInstallCode(code);
      } catch (RuntimeException e) {
        String textIntro = code;
        if (textIntro != null && textIntro.length() > MAX_LOG_LENGTH) {
          textIntro = textIntro.substring(0, MAX_LOG_LENGTH) + "...";
        }
        onLoadError(new HttpInstallFailure(url, textIntro, e), false);
      }
    }
  }

  /**
   * The number of times that we will retry a download. Note that if the install
   * fails, we do not retry, since there's no reason to expect a different result.
   */
  public static int MAX_AUTO_RETRY_COUNT = 3;

  /**
   * Call the linker-supplied <code>__gwtInstallCode</code> method. This method
   * will attempt to install the code, and throw a runtime exception if it fails,
   * which will get caught by the RequestData.tryInstall() function.
   */
  protected static native void gwtInstallCode(String text) /*-{
    __gwtInstallCode(text);
  }-*/;

  /**
   * Call the linker-supplied __gwtStartLoadingFragment function. It should
   * either start the download and return null or undefined, or it should
   * return a URL that should be downloaded to get the code. If it starts the
   * download itself, it can synchronously load it, e.g. from cache, if that
   * makes sense.
   */
  protected static native String gwtStartLoadingFragment(int fragment,
      LoadTerminatedHandler loadErrorHandler) /*-{
    function loadFailed(e) {
      loadErrorHandler.@com.google.gwt.core.client.impl.AsyncFragmentLoader$LoadTerminatedHandler::loadTerminated(Ljava/lang/Throwable;)(e);
    }
    return __gwtStartLoadingFragment(fragment, $entry(loadFailed));
  }-*/;

  private DownloadStrategy downloadStrategy;
  private final FragmentReloadTracker manualRetryNumbers = FragmentReloadTracker.create();

  public DownloadStrategy getDownloadStrategy() {
    return downloadStrategy;
  }

  /**
   * Subclasses should create a DownloadStrategy and pass it into this constructor.
   */
  public LoadingStrategyBase(DownloadStrategy downloadStrategy) {
    this.downloadStrategy = downloadStrategy;
  }

  @Override
  public void startLoadingFragment(int fragment,
      final LoadTerminatedHandler loadErrorHandler) {
    String url = gwtStartLoadingFragment(fragment, loadErrorHandler);
    if (url == null) {
      // The linker is going to handle this fetch - nothing more to do
      return;
    }
    // Browsers will ignore too many script tags if it has previously failed
    // to download that url, so we add a parameter to the url if
    // this is not the first time we've tried to download this fragment.
    int manualRetry = getManualRetryNum(fragment);
    if (manualRetry > 0) {
      char connector = url.contains("?") ? '&' : '?';
      url += connector + "manualRetry=" + manualRetry;
    }
    RequestData request = new RequestData(url, loadErrorHandler,
        fragment, downloadStrategy, getMaxAutoRetryCount());
    request.tryDownload();
  }

  protected int getMaxAutoRetryCount() { return MAX_AUTO_RETRY_COUNT; }

  private int getManualRetryNum(int fragment) {
    int ser = manualRetryNumbers.get(fragment);
    manualRetryNumbers.put(fragment, ser + 1);
    return ser;
  }
}
