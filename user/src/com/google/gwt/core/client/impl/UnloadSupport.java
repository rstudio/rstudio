/*
 * Copyright 2012 Google Inc.
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

/**
 * Rebound class to enable/disable support for {@link com.google.gwt.core.client.GWT#unloadModule()}
 */
public class UnloadSupport {

  static native void clearInterval0(int timerId) /*-{
    window.clearInterval(timerId);
  }-*/;

  static native void clearTimeout0(int timerId) /*-{
    window.clearTimeout(timerId);
  }-*/;

  static native int setInterval0(JavaScriptObject func, int time) /*-{
    var timerId = window.setInterval(function () {
      func();
    }, time);
    return timerId;
  }-*/;

  static native int setTimeout0(JavaScriptObject func, int time, Disposable disposeable) /*-{
    var timerId = window.setTimeout(function () {
      func();
      if (disposeable != null) {
        @com.google.gwt.core.client.impl.Impl::dispose(Lcom/google/gwt/core/client/impl/Disposable;)(disposeable);
      }
    }, time);
    return timerId;
  }-*/;

  public void exportUnloadModule() {
  }

  /**
   * Return true if {@link com.google.gwt.core.client.GWT#unloadModule()} is enabled. Default is false.
   */
  public boolean isUnloadSupported() {
    return false;
  }

  void clearInterval(int timerId) {
    clearInterval0(timerId);
  }

  void clearTimeout(int timerId) {
    clearTimeout0(timerId);
  }

  void dispose(Disposable d) {
    if (d != null) {
      d.dispose();
    }
  }

  void disposeAll() {
  }

  void scheduleDispose(Disposable d) {
  }
  

  int setInterval(JavaScriptObject func, int time) {
    return setInterval0(func, time);
  }

  int setTimeout(JavaScriptObject func, int time) {
    return setTimeout0(func, time, null);
  }
}
