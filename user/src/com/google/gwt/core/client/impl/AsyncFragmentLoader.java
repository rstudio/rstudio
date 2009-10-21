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
 * AsyncFragmentLoader with two arguments: an integer fragment number that needs
 * to be downloaded, and a one-argument loadFailed function that can be called
 * if the load fails. If the load fails, that function should be called with a
 * descriptive exception as the argument. If the mechanism for loading the
 * contents of fragments is provided by the linker, the
 * <code>__gwtStartLoadingFragment</code> function should return
 * <code>null</code> or <code>undefined</code>.
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
   * A strategy for loading code fragments.
   */
  public interface LoadingStrategy {
    void startLoadingFragment(int fragment, LoadErrorHandler loadErrorHandler);
  }

  /**
   * A strategy for logging progress.
   */
  public interface Logger {
    /**
     * Log an event. The <code>fragment</code> and <code>size</code> are boxed
     * so that they can be optional. A value of <code>null</code> for either one
     * means that they are not specified.
     */
    void logEventProgress(String eventGroup, String type, Integer fragment,
        Integer size);
  }

  /**
   * Labels used for runAsync lightweight metrics.
   */
  public static class LwmLabels {
    public static final String BEGIN = "begin";

    public static final String END = "end";

    private static final String LEFTOVERS_DOWNLOAD = "leftoversDownload";

    private static String downloadGroupForExclusive(int splitPoint) {
      return "download" + splitPoint;
    }
  }

  /**
   * A trivial queue of int's that should compile much better than a
   * LinkedList&lt;Integer&gt;. It assumes that it has a bound on the number of
   * items added to the queue. Removing items does not free up more space, but
   * calling <code>clear()</code> does.
   */
  private static class BoundedIntQueue {
    private final int[] array;
    private int read = 0;
    private int write = 0;

    public BoundedIntQueue(int maxPuts) {
      array = new int[maxPuts];
    }

    public void add(int x) {
      assert (write < array.length);
      array[write++] = x;
    }

    /**
     * Removes all elements, and also makes all space in the queue available
     * again.
     */
    public void clear() {
      read = 0;
      write = 0;
    }

    public int peek() {
      assert read < write;
      return array[read];
    }

    public int remove() {
      assert read < write;
      return array[read++];
    }

    public int size() {
      return write - read;
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
  private class InitialFragmentDownloadFailed implements LoadErrorHandler {
    public void loadFailed(Throwable reason) {
      initialFragmentsLoading = false;

      // Cancel all pending downloads.

      /*
       * Make a local list of the handlers to run, in case one of them calls
       * another runAsync
       */
      List<LoadErrorHandler> handlersToRun = new ArrayList<LoadErrorHandler>();

      // add handlers that are waiting pending the initials download
      assert waitingForInitialFragments.size() == waitingForInitialFragmentsErrorHandlers.size();
      while (waitingForInitialFragments.size() > 0) {
        handlersToRun.add(waitingForInitialFragmentsErrorHandlers.remove());
        waitingForInitialFragments.remove();
      }

      /*
       * Call clear() here so that waitingForInitialFragments makes all of its
       * space available for later requests.
       */
      waitingForInitialFragments.clear();

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

  /**
   * The standard logger used in a web browser. It uses the lightweight metrics
   * system.
   */
  private static class StandardLogger implements Logger {
    /**
     * Always use this as {@link isStatsAvailable} &amp;&amp;
     * {@link #stats(JavaScriptObject)}.
     */
    private static native boolean stats(JavaScriptObject data) /*-{
      return $stats(data);
    }-*/;

    public void logEventProgress(String eventGroup, String type,
        Integer fragment, Integer size) {
      @SuppressWarnings("unused")
      boolean toss = isStatsAvailable()
          && stats(createStatsEvent(eventGroup, type, fragment, size));
    }

    private native JavaScriptObject createStatsEvent(String eventGroup,
        String type, Integer fragment, Integer size) /*-{
      var evt = {
       moduleName: @com.google.gwt.core.client.GWT::getModuleName()(), 
        sessionId: $sessionId,
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

    private native boolean isStatsAvailable() /*-{
      return !!$stats;
    }-*/;
  }

  /**
   * The standard loading strategy used in a web browser.
   */
  private static class XhrLoadingStrategy implements LoadingStrategy {
    public void startLoadingFragment(int fragment,
        final LoadErrorHandler loadErrorHandler) {
      String fragmentUrl = gwtStartLoadingFragment(fragment, loadErrorHandler);

      if (fragmentUrl == null) {
        // The download has already started; nothing more to do
        return;
      }

      // use XHR to download it

      final XMLHttpRequest xhr = XMLHttpRequest.create();

      xhr.open(HTTP_GET, fragmentUrl);

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

    /**
     * Call the linker-supplied <code>__gwtInstallCode</code> method. See the
     * {@link AsyncFragmentLoader class comment} for more details.
     */
    private native void gwtInstallCode(String text) /*-{
      __gwtInstallCode(text);
    }-*/;
 
    /**
     * Call the linker-supplied __gwtStartLoadingFragment function. It should
     * either start the download and return null or undefined, or it should return
     * a URL that should be downloaded to get the code. If it starts the download
     * itself, it can synchronously load it, e.g. from cache, if that makes sense.
     */
    private native String gwtStartLoadingFragment(int fragment,
        LoadErrorHandler loadErrorHandler) /*-{
      function loadFailed(e) {
        loadErrorHandler.@com.google.gwt.core.client.impl.AsyncFragmentLoader$LoadErrorHandler::loadFailed(Ljava/lang/Throwable;)(e);
      }
      return __gwtStartLoadingFragment(fragment, loadFailed);
    }-*/;
  }

  /**
   * The standard instance of AsyncFragmentLoader used in a web browser. The
   * parameters to this call are filled in by
   * {@link com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs}.
   */
  public static AsyncFragmentLoader BROWSER_LOADER = new AsyncFragmentLoader(1,
      new int[] {}, new XhrLoadingStrategy(), new StandardLogger());

  private static final String HTTP_GET = "GET";

  /**
   * Some UA's like Safari will have a "0" status code when loading from file:
   * URLs. Additionally, the "0" status code is used sometimes if the server
   * does not respond, e.g. if there is a connection refused.
   */
  private static final int HTTP_STATUS_NON_HTTP = 0;

  private static final int HTTP_STATUS_OK = 200;

  /**
   * A helper static method that invokes
   * BROWSER_LOADER.leftoversFragmentHasLoaded(). Such a call is generated by
   * the compiler, as it is much simpler if there is a static method to wrap up
   * the call.
   */
  public static void browserLoaderLeftoversFragmentHasLoaded() {
    BROWSER_LOADER.leftoversFragmentHasLoaded();
  }

  /**
   * Error handlers for failure to download an initial fragment.
   * 
   * TODO(spoon) make it a lightweight integer map
   */
  private Map<Integer, LoadErrorHandler> initialFragmentErrorHandlers = new HashMap<Integer, LoadErrorHandler>();

  /**
   * Indicates that the next fragment in {@link #remainingInitialFragments} is
   * currently downloading.
   */
  private boolean initialFragmentsLoading = false;

  /**
   * The sequence of fragments to load initially, before anything else can be
   * loaded. This array will hold the initial sequence of bases followed by the
   * leftovers fragment. It is filled in by
   * {@link com.google.gwt.dev.jjs.impl.CodeSplitter} modifying the initializer
   * to {@link #INSTANCE}. The list does <em>not</em> include the leftovers
   * fragment, which must be loaded once all of these are finished.
   */
  private final int[] initialLoadSequence;

  private LoadingStrategy loadingStrategy = new XhrLoadingStrategy();

  private final Logger logger;

  /**
   * The total number of entry points in the program, which is the number of
   * split points plus one for the main entry point of the program.
   */
  private final int numEntries;

  /**
   * Base fragments that remain to be downloaded. It is lazily initialized in
   * the first call to {@link #startLoadingNextInitial()}. It does include the
   * leftovers fragment.
   */
  private BoundedIntQueue remainingInitialFragments = null;

  /**
   * Split points that have been reached, but that cannot be downloaded until
   * the initial fragments finish downloading. TODO(spoon) use something lighter
   * than a LinkedList
   */
  private final BoundedIntQueue waitingForInitialFragments;

  /**
   * Error handlers for the above queue.
   * 
   * TODO(spoon) change this to a lightweight JS collection
   */
  private Queue<LoadErrorHandler> waitingForInitialFragmentsErrorHandlers = new LinkedList<LoadErrorHandler>();

  public AsyncFragmentLoader(int numEntries, int[] initialLoadSequence,
      LoadingStrategy loadingStrategy, Logger logger) {
    this.numEntries = numEntries;
    this.initialLoadSequence = initialLoadSequence;
    this.loadingStrategy = loadingStrategy;
    this.logger = logger;
    waitingForInitialFragments = new BoundedIntQueue(numEntries + 1);
  }

  /**
   * Inform the loader that a fragment has now finished loading.
   */
  public void fragmentHasLoaded(int fragment) {
    logFragmentLoaded(fragment);

    if (isInitial(fragment)) {
      assert (fragment == remainingInitialFragments.peek());
      remainingInitialFragments.remove();
      initialFragmentErrorHandlers.remove(fragment);

      startLoadingNextInitial();
    }
  }

  /**
   * Loads the specified split point.
   * 
   * @param splitPoint the split point whose code needs to be loaded
   */
  public void inject(int splitPoint, LoadErrorHandler loadErrorHandler) {

    if (haveInitialFragmentsLoaded()) {
      /*
       * The initial fragments has loaded. Immediately start loading the
       * requested code.
       */
      logDownloadStart(splitPoint);
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

      assert (waitingForInitialFragments.size() == waitingForInitialFragmentsErrorHandlers.size());
      waitingForInitialFragments.add(splitPoint);
      waitingForInitialFragmentsErrorHandlers.add(loadErrorHandler);
    }

    /*
     * Start the initial downloads if they aren't running already.
     */
    if (!initialFragmentsLoading) {
      startLoadingNextInitial();
    }
  }

  public void leftoversFragmentHasLoaded() {
    fragmentHasLoaded(leftoversFragment());
  }

  /**
   * Log an event with the {@Logger} this instance was provided.
   */
  public void logEventProgress(String eventGroup, String type) {
    logEventProgress(eventGroup, type, null, null);
  }

  private String downloadGroup(int fragment) {
    return (fragment == leftoversFragment()) ? LwmLabels.LEFTOVERS_DOWNLOAD
        : LwmLabels.downloadGroupForExclusive(fragment);
  }

  /**
   * Return whether all initial fragments have completed loading.
   */
  private boolean haveInitialFragmentsLoaded() {
    return remainingInitialFragments != null
        && remainingInitialFragments.size() == 0;
  }

  private boolean isInitial(int splitPoint) {
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

  private int leftoversFragment() {
    return numEntries;
  }

  private void logDownloadStart(int fragment) {
    logEventProgress(downloadGroup(fragment), LwmLabels.BEGIN, fragment, null);
  }

  /**
   * Log event progress via the {@link Logger} this instance was provided. The
   * <code>fragment</code> and <code>size</code> objects are allowed to be
   * <code>null</code>.
   */
  private void logEventProgress(String eventGroup, String type,
      Integer fragment, Integer size) {
    logger.logEventProgress(eventGroup, type, fragment, size);
  }

  private void logFragmentLoaded(int fragment) {
    String logGroup = downloadGroup(fragment);
    logEventProgress(logGroup, LwmLabels.END, fragment, null);
  }

  private void startLoadingFragment(int fragment,
      final LoadErrorHandler loadErrorHandler) {
    loadingStrategy.startLoadingFragment(fragment, loadErrorHandler);
  }

  /**
   * Start downloading the next fragment in the initial sequence, if there are
   * any left.
   */
  private void startLoadingNextInitial() {
    if (remainingInitialFragments == null) {
      // first call, so initialize remainingInitialFragments
      remainingInitialFragments = new BoundedIntQueue(
          initialLoadSequence.length + 1);
      for (int sp : initialLoadSequence) {
        remainingInitialFragments.add(sp);
      }
      remainingInitialFragments.add(leftoversFragment());
    }

    if (initialFragmentErrorHandlers.isEmpty()
        && waitingForInitialFragmentsErrorHandlers.isEmpty()
        && remainingInitialFragments.size() > 1) {
      /*
       * No further requests are pending, and more than the leftovers fragment
       * is left outstanding. Stop loading stuff for now.
       */
      initialFragmentsLoading = false;
      return;
    }

    if (remainingInitialFragments.size() > 0) {
      // start loading the next initial fragment
      initialFragmentsLoading = true;
      int nextSplitPoint = remainingInitialFragments.peek();
      logDownloadStart(nextSplitPoint);
      startLoadingFragment(nextSplitPoint, new InitialFragmentDownloadFailed());
      return;
    }

    // all initials are finished
    initialFragmentsLoading = false;
    assert (haveInitialFragmentsLoaded());

    // start loading any pending fragments
    assert (waitingForInitialFragments.size() == waitingForInitialFragmentsErrorHandlers.size());
    while (waitingForInitialFragments.size() > 0) {
      int nextSplitPoint = waitingForInitialFragments.remove();
      LoadErrorHandler handler = waitingForInitialFragmentsErrorHandlers.remove();
      logDownloadStart(nextSplitPoint);
      startLoadingFragment(nextSplitPoint, handler);
    }
  }
}
