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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

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
    void logEventProgress(String eventGroup, String type, int fragment,
        int size);
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
  static class HttpDownloadFailure extends RuntimeException {
    private final int statusCode;

    public HttpDownloadFailure(String url, int statusCode, String statusText) {
      super("Download of " + url + " failed with status " + statusCode + "("
          + statusText + ")");
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }

  /**
   * An exception indicating than at HTTP download succeeded, but installing
   * its body failed.
   */
  static class HttpInstallFailure extends RuntimeException {
    public HttpInstallFailure(String url, String text, Throwable rootCause) {
      super("Install of " + url + " failed with text " + text, rootCause);
    }
  }

  /**
   * Internal load error handler. This calls all user-provided error handlers
   * and cancels all pending downloads.
   */
  private class ResetAfterDownloadFailure implements LoadErrorHandler {
    public void loadFailed(Throwable reason) {
      assert fragmentLoading >= 0;

      // Cancel all pending downloads.

      /*
       * Make a local list of the handlers to run, in case one of them calls
       * another runAsync
       */
      LoadErrorHandler[] handlersToRun = pendingDownloadErrorHandlers;
      pendingDownloadErrorHandlers = new LoadErrorHandler[numEntries + 1];

      /*
       * Call clear() here so that requestedExclusives makes all of its space
       * available for later requests.
       */
      requestedExclusives.clear();

      fragmentLoading = -1;

      /*
       * Run the handlers. If an exception is thrown while canceling any of
       * them, remember and throw the last one.
       */
      RuntimeException lastException = null;

      for (LoadErrorHandler handler : handlersToRun) {
        if (handler != null) {
          try {
            handler.loadFailed(reason);
          } catch (RuntimeException e) {
            lastException = e;
          }
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
        int fragment, int size) {
      @SuppressWarnings("unused")
      boolean toss = isStatsAvailable()
          && stats(createStatsEvent(eventGroup, type, fragment, size));
    }

    private native JavaScriptObject createStatsEvent(String eventGroup,
        String type, int fragment, int size) /*-{
      var evt = {
       moduleName: @com.google.gwt.core.client.GWT::getModuleName()(), 
        sessionId: $sessionId,
        subSystem: 'runAsync',
        evtGroup: eventGroup,
        millis: (new Date()).getTime(),
        type: type
      };
      if (fragment >= 0) {
        evt.fragment = fragment;
      }
      if (size >= 0) {
        evt.size = size;
      }
      return evt;
    }-*/;

    private native boolean isStatsAvailable() /*-{
      return !!$stats;
    }-*/;
  }

  /**
   * The standard instance of AsyncFragmentLoader used in a web browser.  If
   * not in GWT (i.e our vanilla JUnit tests, or if referenced in a server
   * context), this filed is {@code null}.  In GWT, the parameters to this call
   * are rewritten by {@link com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs}.  So
   * this must be a method call of exactly two arguments, or that magic fails.
   */
  public static AsyncFragmentLoader BROWSER_LOADER =
    makeBrowserLoader(1, new int[] {});
  
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
   * Creates the loader stored as {@link #BROWSER_LOADER}.
   * @returns {@code null} if not in GWT client code, where
   *   {@link GWT#create(Class)} cannot be used, or a fragment loader for
   *   the user's application otherwise.
   */
  private static AsyncFragmentLoader makeBrowserLoader(int numFragments,
      int initialLoad[]) {
    if (GWT.isClient()) {
      return new AsyncFragmentLoader(numFragments, initialLoad,
          (LoadingStrategy) GWT.create(LoadingStrategy.class),
          (Logger) GWT.create(Logger.class));
    } else {
      return null;
    }
  }

  /**
   * The fragment currently loading, or -1 if there aren't any.
   */
  private int fragmentLoading = -1;

  /**
   * The sequence of fragments to load initially, before anything else can be
   * loaded. This array will hold the initial sequence of bases followed by the
   * leftovers fragment. It is filled in by
   * {@link com.google.gwt.dev.jjs.impl.CodeSplitter} modifying the initializer
   * to {@link #INSTANCE}. The list does <em>not</em> include the leftovers
   * fragment, which must be loaded once all of these are finished.
   */
  private final int[] initialLoadSequence;

  /**
   * This array indicates which fragments have been successfully loaded.
   */
  private final boolean[] isLoaded;

  private final LoadingStrategy loadingStrategy;

  private final Logger logger;

  /**
   * The total number of entry points in the program, which is the number of
   * split points plus one for the main entry point of the program.
   */
  private final int numEntries;

  /**
   * Externally provided handlers for all outstanding and queued download
   * requests.
   */
  private LoadErrorHandler[] pendingDownloadErrorHandlers;

  /**
   * Whether prefetching is currently enabled.
   */
  private boolean prefetching = false;

  /**
   * This queue has fragments that have been requested to be prefetched. If it's
   * <code>null</code>, that indicates no prefetch requests, which should cause
   * all of this class's prefetching code to drop out of the compiled output.
   */
  private BoundedIntQueue prefetchQueue = null;

  /**
   * Base fragments that remain to be downloaded. It is lazily initialized in
   * the first call to {@link #startLoadingNextFragment()}. It does include the
   * leftovers fragment.
   */
  private BoundedIntQueue remainingInitialFragments = null;

  /**
   * Exclusive fragments that have been requested but that are not yet
   * downloading.
   */
  private final BoundedIntQueue requestedExclusives;

  public AsyncFragmentLoader(int numEntries, int[] initialLoadSequence,
      LoadingStrategy loadingStrategy, Logger logger) {
    this.numEntries = numEntries;
    this.initialLoadSequence = initialLoadSequence;
    this.loadingStrategy = loadingStrategy;
    this.logger = logger;
    int numEntriesPlusOne = numEntries + 1;
    requestedExclusives = new BoundedIntQueue(numEntriesPlusOne);
    isLoaded = new boolean[numEntriesPlusOne];
    pendingDownloadErrorHandlers = new LoadErrorHandler[numEntriesPlusOne];
  }

  /**
   * Inform the loader that a fragment has now finished loading.
   */
  public void fragmentHasLoaded(int fragment) {
    logFragmentLoaded(fragment);
    if (fragment < pendingDownloadErrorHandlers.length) {
      pendingDownloadErrorHandlers[fragment] = null;
    }

    if (isInitial(fragment)) {
      assert (fragment == remainingInitialFragments.peek());
      remainingInitialFragments.remove();
    }

    assert (fragment == fragmentLoading);
    fragmentLoading = -1;

    assert !isLoaded[fragment];
    isLoaded[fragment] = true;

    startLoadingNextFragment();
  }

  /**
   * Requests a load of the code for the specified split point. If the load
   * fails, <code>loadErrorHandler</code> will be invoked. If it succeeds, then
   * the code will be installed, and the code is expected to invoke its own
   * on-success hooks, including a call to either
   * {@link #leftoversFragmentHasLoaded()} or {@link #fragmentHasLoaded(int)}.
   * 
   * @param splitPoint the split point whose code needs to be loaded
   */
  public void inject(int splitPoint, LoadErrorHandler loadErrorHandler) {
    pendingDownloadErrorHandlers[splitPoint] = loadErrorHandler;
    if (!isInitial(splitPoint)) {
      requestedExclusives.add(splitPoint);
    }
    startLoadingNextFragment();
  }

  public boolean isAlreadyLoaded(int splitPoint) {
   return isLoaded[splitPoint];
  }

  public boolean isLoading(int splitPoint) {
    return pendingDownloadErrorHandlers[splitPoint] != null;
  }

  public void leftoversFragmentHasLoaded() {
    fragmentHasLoaded(leftoversFragment());
  }

  /**
   * Log an event with the {@Logger} this instance was provided.
   */
  public void logEventProgress(String eventGroup, String type) {
    logEventProgress(eventGroup, type, -1, -1);
  }

  /**
   * Request that a sequence of split points be prefetched. Code for the split
   * points in <code>splitPoints</code> will be downloaded and installed
   * whenever there is nothing else to download. Each call to this method
   * overwrites the entire prefetch queue with the newly specified one.
   */
  public void setPrefetchQueue(int... runAsyncSplitPoints) {
    if (prefetchQueue == null) {
      prefetchQueue = new BoundedIntQueue(numEntries);
    }
    prefetchQueue.clear();
    for (int sp : runAsyncSplitPoints) {
      prefetchQueue.add(sp);
    }
    startLoadingNextFragment();
  }

  public void startPrefetching() {
    prefetching = true;
    startLoadingNextFragment();
  }

  public void stopPrefetching() {
    prefetching = false;
  }

  private boolean anyPrefetchesRequested() {
    return prefetching && prefetchQueue != null && prefetchQueue.size() > 0;
  }

  /**
   * Clear out any inject and prefetch requests that are already loaded. Only
   * remove items from the head of each queue; any stale entries later in the
   * queue will be removed later.
   */
  private void clearRequestsAlreadyLoaded() {
    while (requestedExclusives.size() > 0
        && isLoaded[requestedExclusives.peek()]) {
      int offset = requestedExclusives.remove();
      if (offset < pendingDownloadErrorHandlers.length) {
        pendingDownloadErrorHandlers[offset] = null;
      }
    }

    if (prefetchQueue != null) {
      while (prefetchQueue.size() > 0 && isLoaded[prefetchQueue.peek()]) {
        prefetchQueue.remove();
      }
    }
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

  /**
   * Initialize {@link #remainingInitialFragments} if it isn't already.
   */
  private void initializeRemainingInitialFragments() {
    if (remainingInitialFragments == null) {
      remainingInitialFragments = new BoundedIntQueue(
          initialLoadSequence.length + 1);
      for (int sp : initialLoadSequence) {
        remainingInitialFragments.add(sp);
      }
      remainingInitialFragments.add(leftoversFragment());
    }
  }

  /**
   * Returns <code>true</code> if array contains only <code>null</code>
   * elements.
   */
  private boolean isEmpty(Object[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        return false;
      }
    }
    return true;
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
    logEventProgress(downloadGroup(fragment), LwmLabels.BEGIN, fragment, -1);
  }

  /**
   * Log event progress via the {@link Logger} this instance was provided. The
   * <code>fragment</code> and <code>size</code> objects are allowed to be
   * <code>null</code>.
   */
  private void logEventProgress(String eventGroup, String type, int fragment,
      int size) {
    logger.logEventProgress(eventGroup, type, fragment, size);
  }

  private void logFragmentLoaded(int fragment) {
    String logGroup = downloadGroup(fragment);
    logEventProgress(logGroup, LwmLabels.END, fragment, -1);
  }

  private void startLoadingFragment(int fragment) {
    assert (fragmentLoading < 0);
    fragmentLoading = fragment;
    logDownloadStart(fragment);
    loadingStrategy.startLoadingFragment(fragment,
        new ResetAfterDownloadFailure());
  }

  /**
   * Start downloading the next fragment queued up, if there are any.
   */
  private void startLoadingNextFragment() {
    if (fragmentLoading >= 0) {
      // Already loading something
      return;
    }

    initializeRemainingInitialFragments();
    clearRequestsAlreadyLoaded();

    if (isEmpty(pendingDownloadErrorHandlers) && !anyPrefetchesRequested()) {
      /*
       * Don't load anything if there aren't any requests outstanding.
       */
      return;
    }

    // Check if an initial needs downloading
    if (remainingInitialFragments.size() > 0) {
      startLoadingFragment(remainingInitialFragments.peek());
      return;
    }

    assert (haveInitialFragmentsLoaded());

    // Check if an exclusive is pending
    if (requestedExclusives.size() > 0) {
      startLoadingFragment(requestedExclusives.remove());
      return;
    }

    // Check the prefetch queue
    if (anyPrefetchesRequested()) {
      startLoadingFragment(prefetchQueue.remove());
      return;
    }

    // Nothing needed downloading after all?!
    assert false;
  }
}
