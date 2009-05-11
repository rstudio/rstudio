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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * <li>1..m -- fragments for each split point
 * <li>m+1 -- the <em>leftovers</em> fragment of code that goes nowhere else
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

    private static String downloadGroup(int splitPoint) {
      return "download" + splitPoint;
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
   * Handles a failure to download a fragment in the initial sequence.
   */
  private static class InitialFragmentDownloadFailed implements
      LoadErrorHandler {
    public void loadFailed(Throwable reason) {
      initialFragmentsLoading = false;

      // Cancel all pending downloads.

      /*
       * Make a local list of the handlers to run, in case one of them calls
       * another runAsync
       */
      List<LoadErrorHandler> handlersToRun = new ArrayList<LoadErrorHandler>();

      // add handlers that are waiting pending the initials download
      assert waitingForInitialFragments.length() == waitingForInitialFragmentsErrorHandlers.size();
      while (waitingForInitialFragments.length() > 0) {
        handlersToRun.add(waitingForInitialFragmentsErrorHandlers.remove());
        waitingForInitialFragments.shift();
      }

      // add handlers for pending initial fragment downloads
      handlersToRun.addAll(initialFragmentErrorHandlers.values());
      initialFragmentErrorHandlers.clear();

      /*
       * If an exception is thrown while canceling any of them, remember and
       * throw the last one.
       */
      RuntimeException lastException = null;

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

  private static final String HTTP_GET = "GET";

  /**
   * Some UA's like Safari will have a "0" status code when loading from file:
   * URLs. Additionally, the "0" status code is used sometimes if the server
   * does not respond, e.g. if there is a connection refused.
   */
  private static final int HTTP_STATUS_NON_HTTP = 0;

  private static final int HTTP_STATUS_OK = 200;

  /**
   * Error handlers for failure to download an initial fragment.
   * 
   * TODO(spoon) make it a lightweight integer map
   */
  private static Map<Integer, LoadErrorHandler> initialFragmentErrorHandlers = new HashMap<Integer, LoadErrorHandler>();

  /**
   * Indicates that the next fragment in {@link #remainingInitialFragments} is
   * currently downloading.
   */
  private static boolean initialFragmentsLoading = false;

  /**
   * The sequence of fragments to load initially, before anything else can be
   * loaded. This array will hold the initial sequence of bases followed by the
   * leftovers fragment. It is filled in by
   * {@link com.google.gwt.dev.jjs.impl.CodeSplitter}.  It does *not* include
   * the leftovers fragment, which must be loaded once all of these are finished.
   */
  private static int[] initialLoadSequence = new int[] { };

  /**
   * The total number of split points in the program, counting the initial entry
   * as an honorary split point. This is changed to the correct value by
   * {@link com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs}.
   */
  private static int numEntries = 1;

  /**
   * Base fragments that remain to be downloaded. It is lazily initialized in
   * the first call to {@link #startLoadingNextInitial()}.  It does include
   * the leftovers fragment.
   */
  private static JsArrayInteger remainingInitialFragments = null;

  /**
   * Split points that have been reached, but that cannot be downloaded until
   * the initial fragments finish downloading.
   */
  private static JsArrayInteger waitingForInitialFragments = createJsArrayInteger();

  /**
   * Error handlers for the above queue.
   * 
   * TODO(spoon) change this to a lightweight JS collection
   */
  private static Queue<LoadErrorHandler> waitingForInitialFragmentsErrorHandlers = new LinkedList<LoadErrorHandler>();

  /**
   * Inform the loader that a fragment has now finished loading.
   */
  public static void fragmentHasLoaded(int fragment) {
    logFragmentLoaded(fragment);

    if (isInitial(fragment)) {
      assert (fragment == remainingInitialFragments.get(0));
      remainingInitialFragments.shift();
      initialFragmentErrorHandlers.remove(fragment);

      startLoadingNextInitial();
    }
  }

  /**
   * Loads the specified split point.
   * 
   * @param splitPoint the split point whose code needs to be loaded
   */
  public static void inject(int splitPoint, LoadErrorHandler loadErrorHandler) {
    if (haveInitialFragmentsLoaded()) {
      /*
       * The initial fragments has loaded. Immediately start loading the
       * requested code.
       */
      logEventProgress(LwmLabels.downloadGroup(splitPoint), LwmLabels.BEGIN,
          splitPoint, null);
      startLoadingFragment(splitPoint, loadErrorHandler);
      return;
    }

    if (isInitial(splitPoint)) {
      /*
       * The loading of an initial fragment will happen via
       * startLoadingNextInitial(), so don't start it here. Do, however, record
       * the error handler.
       */
      initialFragmentErrorHandlers.put(splitPoint, loadErrorHandler);
    } else {
      /*
       * For a non-initial fragment, queue it for later loading, once the
       * initial fragments have all been loaded.
       */

      assert (waitingForInitialFragments.length() == waitingForInitialFragmentsErrorHandlers.size());
      waitingForInitialFragments.push(splitPoint);
      waitingForInitialFragmentsErrorHandlers.add(loadErrorHandler);
    }

    /*
     * Start the initial downloads if they aren't running already.
     */
    if (!initialFragmentsLoading) {
      startLoadingNextInitial();
    }

    return;
  }
  
  public static void leftoversFragmentHasLoaded() {
    fragmentHasLoaded(leftoversFragment());
  }

  /**
   * Log an event with the lightweight metrics framework.
   */
  public static void logEventProgress(String eventGroup, String type) {
    logEventProgress(eventGroup, type, null, null);
  }

  private static native JsArrayInteger createJsArrayInteger() /*-{
    return [];
  }-*/;

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

  private static native void gwtInstallCode(String text) /*-{
    __gwtInstallCode(text);
  }-*/;

  /**
   * Call the linker-supplied __gwtStartLoadingFragment function. It should
   * either start the download and return null or undefined, or it should return
   * a URL that should be downloaded to get the code. If it starts the download
   * itself, it can synchronously load it, e.g. from cache, if that makes sense.
   */
  private static native String gwtStartLoadingFragment(int fragment) /*-{
    return __gwtStartLoadingFragment(fragment);
  }-*/;

  /**
   * Return whether all initial fragments have completed loading.
   */
  private static boolean haveInitialFragmentsLoaded() {
    return remainingInitialFragments != null
        && remainingInitialFragments.length() == 0;
  }

  private static boolean isInitial(int splitPoint) {
    if (splitPoint == leftoversFragment()) {
      return true;
    }
    for (int sp : initialLoadSequence) {
      if (sp == splitPoint) {
        return true;
      }
    }
    return false;
  }

  private static native boolean isStatsAvailable() /*-{
    return !!$stats;
  }-*/;

  private static int leftoversFragment() {
    return numEntries;
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

  private static void logFragmentLoaded(int fragment) {
    String logGroup = (fragment == leftoversFragment())
        ? LwmLabels.LEFTOVERS_DOWNLOAD : LwmLabels.downloadGroup(fragment);
    logEventProgress(logGroup, LwmLabels.END, fragment, null);
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
            if ((xhr.getStatus() == HTTP_STATUS_OK || xhr.getStatus() == HTTP_STATUS_NON_HTTP)
                && xhr.getResponseText() != null
                && xhr.getResponseText().length() != 0) {
              try {
                gwtInstallCode(xhr.getResponseText());
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

  /**
   * Start downloading the next fragment in the initial sequence, if there are
   * any left.
   */
  private static void startLoadingNextInitial() {
    if (remainingInitialFragments == null) {
      // first call, so initialize remainingInitialFragments
      remainingInitialFragments = createJsArrayInteger();
      for (int sp : initialLoadSequence) {
        remainingInitialFragments.push(sp);
      }
      remainingInitialFragments.push(leftoversFragment());
    }

    if (remainingInitialFragments.length() > 0) {
      // start loading the next initial fragment
      initialFragmentsLoading = true;
      int nextSplitPoint = remainingInitialFragments.get(0);
      logEventProgress(LwmLabels.downloadGroup(nextSplitPoint), LwmLabels.BEGIN,
          nextSplitPoint, null);
      startLoadingFragment(nextSplitPoint, new InitialFragmentDownloadFailed());
      return;
    }

    // all initials are finished
    initialFragmentsLoading = false;
    assert (haveInitialFragmentsLoaded());
    
    // start loading any pending fragments
    assert (waitingForInitialFragments.length() == waitingForInitialFragmentsErrorHandlers.size());
    while (waitingForInitialFragments.length() > 0) {
      startLoadingFragment(waitingForInitialFragments.shift(),
          waitingForInitialFragmentsErrorHandlers.remove());
    }
  }

  /**
   * Always use this as {@link isStatsAvailable} &amp;&amp;
   * {@link #stats(JavaScriptObject)}.
   */
  private static native boolean stats(JavaScriptObject data) /*-{
    return $stats(data);
  }-*/;
}
