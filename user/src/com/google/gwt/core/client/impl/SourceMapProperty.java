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

import com.google.gwt.core.client.GWT;

/**
 * Helper class to query if SourceMaps are enabled and capable of working on the current user
 * agent.
 */
public class SourceMapProperty {

  static class SourceMapEnabled extends SourceMapImpl {

    public native boolean doesBrowserSupportSourceMaps() /*-{
      // Chrome only for now, future Firefoxes have promised support
      return navigator.userAgent.indexOf('Chrome') > -1;
    }-*/;

    public boolean isSourceMapGenerationOn() {
      return true;
    }
  }

  static class SourceMapEmulated extends SourceMapEnabled {

    public boolean shouldUseSourceMaps() {
      // pretend emulated stack is a sourcemap
      return true;
    }
  }

  /**
   * Interface to provide both the compile time and runtime <code>user.agent</code> selection
   * property value.
   */
  static class SourceMapImpl {

    public boolean doesBrowserSupportSourceMaps() {
      return false;
    }

    public boolean isSourceMapGenerationOn() {
      return false;
    }

    public boolean shouldUseSourceMaps() {
      return isSourceMapGenerationOn() && doesBrowserSupportSourceMaps();
    }
  }

  private static final SourceMapImpl IMPL = GWT.create(SourceMapImpl.class);

  public static boolean doesBrowserSupportSourceMaps() {
    return IMPL.doesBrowserSupportSourceMaps();
  }

  /**
   * True if fully accurate stack traces are possible. True for DevMode, emulated stack traces, and
   * cases where sourceMaps can work with detailed browser stack trace support.
   */
  public static boolean isDetailedDeobfuscatedStackTraceSupported() {
    return !GWT.isScript() || shouldUseSourceMaps();
  }

  public static boolean isSourceMapGenerationOn() {
    return IMPL.isSourceMapGenerationOn();
  }

  public static boolean shouldUseSourceMaps() {
    return IMPL.shouldUseSourceMaps();
  }
}
