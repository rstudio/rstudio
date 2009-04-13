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
package com.google.gwt.core.client;

import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * <p>
 * Low-level support to download an extra fragment of code. This should not be
 * invoked directly by user code.
 * </p>
 * 
 * <p>
 * The fragments are numbered as follows, assuming there are <em>m</em> split
 * points:
 * 
 * <ul>
 * <li>0 -- the <em>base</em> fragment, which is initially downloaded
 * <li>1-m -- the <em>exclusively live</em> fragments, holding the code needed
 * exclusively for each split point
 * <li>m -- the <em>secondary base</em> fragment for entry point 1. It holds
 * precisely that code needed if entry point 1 is the first one reached after
 * the application downloads.
 * <li>m+1 -- the <em>leftovers fragment</em> for entry point 1. It holds all
 * code not in fragments 0-(m-1) nor in fragment m.
 * <li>(m+2)..(3m) -- secondary bases and leftovers for entry points 2..m
 * </ul>
 * 
 * <p>
 * Since the precise way to load code depends on the linker, each linker should
 * provide functions for fragment loading for any compilation that includes more
 * than one fragment. Linkers should always provide a function
 * <code>__gwtStartLoadingFragment</code>. This function is called by
 * AsyncFragmentLoader with an integer fragment number that needs to be
 * downloaded. If the mechanism for loading the contents of fragments is
 * provided by the linker, this function should return <code>null</code> or
 * <code>undefined</code>.
 * </p>
 * <p>
 * Alternatively, the function can return a URL designating from where the code
 * for the requested fragment can be downloaded. In that case, the linker should
 * also provide a function <code>__gwtInstallCode</code> for actually installing
 * the code once it is downloaded. That function will be passed the loaded code
 * once it has been downloaded.
 * </p>
 */
public class AsyncFragmentLoader {
  /**
   * An interface for handlers of load errors.
   */
  public static interface LoadErrorHandler {
    void loadFailed(Throwable reason);
  }

  /**
   * Labels used for runAsync lightweight metrics.
   */
  public static class LwmLabels {
    public static final String BEGIN = "begin";

    public static final String END = "end";

    private static final String LEFTOVERS_DOWNLOAD = "leftoversDownload";

    /**
     * @param splitPoint
     * @return
     */
    private static String downloadGroup(int splitPoint) {
      return "download" + splitPoint;
    }
  }

  /**
   * Handles a failure to download a base fragment.
   */
  private static class BaseDownloadFailed implements LoadErrorHandler {
    private final LoadErrorHandler chainedHandler;

    public BaseDownloadFailed(LoadErrorHandler chainedHandler) {
      this.chainedHandler = chainedHandler;
    }

    public void loadFailed(Throwable reason) {
      baseLoading = false;
      chainedHandler.loadFailed(reason);
    }
  }

  /**
   * An exception indicating than at HTTP download failed.
   */
  private static class HttpDownloadFailure extends RuntimeException {
    private final int statusCode;

    public HttpDownloadFailure(int statusCode) {
      super("HTTP download failed with status " + statusCode);
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }

  /**
   * Handles a failure to download a leftovers fragment.
   */
  private static class LeftoversDownloadFailed implements LoadErrorHandler {
    public void loadFailed(Throwable reason) {
      leftoversLoading = false;

      /*
       * Cancel all other pending downloads. If any exception is thrown while
       * cancelling any of them, throw only the last one.
       */
      RuntimeException lastException = null;

      assert waitingForLeftovers.size() == waitingForLeftoversErrorHandlers.size();

      // Copy the list in case a handler makes another runAsync call
      List<LoadErrorHandler> handlersToRun = new ArrayList<LoadErrorHandler>(
          waitingForLeftoversErrorHandlers);
      waitingForLeftoversErrorHandlers.clear();
      waitingForLeftovers.clear();

      for (LoadErrorHandler handler : handlersToRun) {
        try {
          handler.loadFailed(reason);
        } catch (RuntimeException e) {
          lastException = e;
        }
      }

      if (lastException != null) {
        throw lastException;
      }
    }
  }

  /**
   * The first entry point reached after the program started.
   */
  private static int base = -1;

  /**
   * Whether the secondary base fragment is currently loading.
   */
  private static boolean baseLoading = false;

  private static final String HTTP_GET = "GET";

  /**
   * Some UA's like Safari will have a "0" status code when loading from file:
   * URLs.
   */
  private static final int HTTP_STATUS_LOCAL = 0;

  private static final int HTTP_STATUS_OK = 200;

  /**
   * Whether the leftovers fragment has loaded yet.
   */
  private static boolean leftoversLoaded = false;

  /**
   * Whether the leftovers fragment is currently loading.
   */
  private static boolean leftoversLoading = false;

  /**
   * The total number of split points in the program, counting the initial entry
   * as a split point. This is changed to the correct value by
   * {@link com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs}.
   */
  private static int numEntries = 1;

  /**
   * Split points that have been reached, but that cannot be downloaded until
   * the leftovers fragment finishes downloading.
   */
  private static Queue<Integer> waitingForLeftovers = new LinkedList<Integer>();

  /**
   * Error handlers for the above queue.
   */
  private static Queue<LoadErrorHandler> waitingForLeftoversErrorHandlers = new LinkedList<LoadErrorHandler>();

  /**
   * Inform the loader that the code for an entry point has now finished
   * loading.
   * 
   * @param entry The entry whose code fragment is now loaded.
   */
  public static void fragmentHasLoaded(int entry) {
    int fragment = base >= 0 ? entry : baseFragmentNumber(entry);
    logEventProgress(LwmLabels.downloadGroup(entry), LwmLabels.END, fragment,
        null);

    if (base < 0) {
      // The base fragment has loaded
      base = entry;
      baseLoading = false;

      // Go ahead and download the appropriate leftovers fragment
      startLoadingLeftovers();
    }
  }

  /**
   * Loads the specified split point.
   * 
   * @param splitPoint the fragment to load
   */
  public static void inject(int splitPoint, LoadErrorHandler loadErrorHandler) {
    if (leftoversLoaded) {
      /*
       * A base and a leftovers fragment have loaded. Load an exclusively live
       * fragment.
       */
      logEventProgress(LwmLabels.downloadGroup(splitPoint), LwmLabels.BEGIN,
          splitPoint, null);
      startLoadingFragment(splitPoint, loadErrorHandler);
      return;
    }

    if (baseLoading || leftoversLoading) {
      /*
       * Wait until the leftovers fragment has loaded before loading this one.
       */
      assert (waitingForLeftovers.size() == waitingForLeftoversErrorHandlers.size());
      waitingForLeftovers.add(splitPoint);
      waitingForLeftoversErrorHandlers.add(loadErrorHandler);

      /*
       * Also, restart the leftovers download if it previously failed.
       */
      if (!leftoversLoading) {
        startLoadingLeftovers();
      }

      return;
    }

    // Nothing has loaded or started to load. Treat this fragment as the base.
    baseLoading = true;
    logEventProgress(LwmLabels.downloadGroup(splitPoint), LwmLabels.BEGIN,
        baseFragmentNumber(splitPoint), null);
    startLoadingFragment(baseFragmentNumber(splitPoint),
        new BaseDownloadFailed(loadErrorHandler));
  }

  /**
   * Inform the loader that the "leftovers" fragment has loaded.
   */
  public static void leftoversFragmentHasLoaded() {
    leftoversLoaded = true;
    leftoversLoading = false;
    logEventProgress(LwmLabels.LEFTOVERS_DOWNLOAD, LwmLabels.END,
        leftoversFragmentNumber(), null);

    assert (waitingForLeftovers.size() == waitingForLeftoversErrorHandlers.size());
    while (!waitingForLeftovers.isEmpty()) {
      inject(waitingForLeftovers.remove(),
          waitingForLeftoversErrorHandlers.remove());
    }
  }

  /**
   * Log an event with the lightweight metrics framework.
   */
  public static void logEventProgress(String eventGroup, String type) {
    logEventProgress(eventGroup, type, null, null);
  }

  /**
   * Compute the fragment number for the base fragment of
   * <code>splitPoint</code>.
   */
  private static int baseFragmentNumber(int splitPoint) {
    return numEntries + 2 * (splitPoint - 1);
  }

  private static native JavaScriptObject createStatsEvent(String eventGroup,
      String type, Integer fragment, Integer size) /*-{
    var evt = {
      moduleName: @com.google.gwt.core.client.GWT::getModuleName()(), 
      subSystem: 'runAsync',
      evtGroup: eventGroup,
      millis: (new Date()).getTime(),
      type: type
    };
    if (fragment != null) {
      evt.fragment = fragment.@java.lang.Integer::intValue()();
    }
    if (size != null) {
      evt.size = size.@java.lang.Integer::intValue()();
    }
    return evt;
  }-*/;

  /**
   * Use the linker-supplied __gwtStartLoadingFragment function. It should
   * either start the download and return null or undefined, or it should return
   * a URL that should be downloaded to get the code. If it starts the download
   * itself, it can synchronously load it, e.g. from cache, if that makes sense.
   */
  private static native String gwtStartLoadingFragment(int fragment) /*-{
    return __gwtStartLoadingFragment(fragment);
  }-*/;

  private static native void installCode(String text) /*-{
    __gwtInstallCode(text);
  }-*/;

  private static native boolean isStatsAvailable() /*-{
    return !!$stats;
  }-*/;

  /**
   * Compute the leftovers fragment number. This method can only be called once
   * <code>base</code> has been set.
   */
  private static int leftoversFragmentNumber() {
    assert (base >= 0);
    return numEntries + 2 * (base - 1) + 1;
  }

  /**
   * Log an event with the lightweight metrics framework. The
   * <code>fragment</code> and <code>size</code> objects are allowed to be
   * <code>null</code>.
   */
  private static void logEventProgress(String eventGroup, String type,
      Integer fragment, Integer size) {
    @SuppressWarnings("unused")
    boolean toss = isStatsAvailable()
        && stats(createStatsEvent(eventGroup, type, fragment, size));
  }

  private static void startLoadingFragment(int fragment,
      final LoadErrorHandler loadErrorHandler) {
    String fragmentUrl = gwtStartLoadingFragment(fragment);

    if (fragmentUrl != null) {
      // use XHR
      final XMLHttpRequest xhr = XMLHttpRequest.create();

      xhr.open(HTTP_GET, fragmentUrl);

      xhr.setOnReadyStateChange(new ReadyStateChangeHandler() {
        public void onReadyStateChange(XMLHttpRequest xhr) {
          if (xhr.getReadyState() == XMLHttpRequest.DONE) {
            xhr.clearOnReadyStateChange();
            if (xhr.getStatus() == HTTP_STATUS_OK
                || xhr.getStatus() == HTTP_STATUS_LOCAL) {
              try {
                installCode(xhr.getResponseText());
              } catch (RuntimeException e) {
                loadErrorHandler.loadFailed(e);
              }
            } else {
              loadErrorHandler.loadFailed(new HttpDownloadFailure(
                  xhr.getStatus()));
            }
          }
        }
      });

      xhr.send();
    }
  }

  private static void startLoadingLeftovers() {
    leftoversLoading = true;
    logEventProgress(LwmLabels.LEFTOVERS_DOWNLOAD, LwmLabels.BEGIN,
        leftoversFragmentNumber(), null);
    startLoadingFragment(leftoversFragmentNumber(),
        new LeftoversDownloadFailed());
  }

  /**
   * Always use this as {@link isStatsAvailable} &amp;&amp;
   * {@link #stats(JavaScriptObject)}.
   */
  private static native boolean stats(JavaScriptObject data) /*-{
    return $stats(data);
  }-*/;
}