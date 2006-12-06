// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.CheckForUpdates;

/**
 * Mozilla specific implementation of CheckForUpdates.
 */
public class CheckForUpdatesMoz extends CheckForUpdates {

  protected byte[] doHttpGet(String userAgent, String url) {
    // Don't attempt to support proxies on Linux.
    //
    return httpGetNonNative(userAgent, url);
  }
}
