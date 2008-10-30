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

/**
 * <p>
 * Low-level support to download an extra fragment of code. This should not be
 * invoked directly by user code.
 * </p>
 * 
 * <p>
 * Different linkers have different requirements about how the code is
 * downloaded and installed. Thus, when it is time to actually download the
 * code, this class defers to a JavaScript function named
 * <code>__gwtStartLoadingFragment</code>. Linkers must arrange that a
 * suitable <code>__gwtStartLoadingFragment</code> function is in scope.
 */
public class AsyncFragmentLoader {
  /**
   * Inform the loader that the code for an entry point has now finished
   * loading.
   * 
   * @param entry The entry whose code fragment is now loaded.
   */
  public static void fragmentHasLoaded(int entry) {
    // There is nothing to do with the current fragmentation strategy
  }

  /**
   * Loads the specified fragment asynchronously.
   * 
   * @param fragment the fragment to load
   */
  public static void inject(int fragment) {
    logEventProgress("download" + fragment, "begin");
    startLoadingFragment(fragment);
  }

  /**
   * Logs an event with the GWT lightweight metrics framework.
   */
  public static void logEventProgress(String eventGroup, String type) {
    @SuppressWarnings("unused")
    boolean toss = isStatsAvailable()
        && stats(createStatsEvent(eventGroup, type));
  }

  /**
   * Create an event object suitable for submitting to the lightweight metrics
   * framework.
   */
  private static native JavaScriptObject createStatsEvent(String eventGroup,
      String type) /*-{
    return {
      moduleName: @com.google.gwt.core.client.GWT::getModuleName()(), 
      subSystem: 'runAsync',
      evtGroup: eventGroup,
      millis: (new Date()).getTime(),
      type: type
    };
  }-*/;

  private static native boolean isStatsAvailable() /*-{
    return !!$stats;
  }-*/;

  private static native void startLoadingFragment(int fragment) /*-{
    __gwtStartLoadingFragment(fragment);
  }-*/;

  /**
   * Always use this as {@link isStatsAvailable} &amp;&amp;
   * {@link #stats(JavaScriptObject)}.
   */
  private static native boolean stats(JavaScriptObject data) /*-{
    return $stats(data);
  }-*/;
}