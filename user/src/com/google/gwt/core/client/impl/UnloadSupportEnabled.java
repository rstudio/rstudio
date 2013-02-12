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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rebound version switches on unload support.
 */
@GwtScriptOnly
public class UnloadSupportEnabled extends UnloadSupport {

  static class TimerDisposable implements Disposable {

    int timerId;
    private Map<Integer, Disposable> timeoutMap;
    private boolean timeout;

    public TimerDisposable(Map<Integer, Disposable> timeoutMap, boolean isTimeout) {
      this.timeoutMap = timeoutMap;
      timeout = isTimeout;
    }

    @Override
    public void dispose() {
      timeoutMap.remove(timerId);
      if (timeout) {
        clearTimeout0(timerId);
      } else {
        clearInterval0(timerId);
      }
    }
  }

  private Map<Integer, Disposable> timeouts = new HashMap<Integer, Disposable>();
  private Map<Integer, Disposable> intervals = new HashMap<Integer, Disposable>();
  private Set<Disposable> disposables = new LinkedHashSet<Disposable>();

  public native void exportUnloadModule() /*-{
    if (!$wnd.__gwt_activeModules) {
      $wnd.__gwt_activeModules = {};
    }
    var $moduleName = __gwtModuleFunction.__moduleName;
    // unfortunately GWT.getModuleName() isn't set up yet when this is called
    var activeModule = $wnd.__gwt_activeModules[$moduleName];
    if (!activeModule) {
      activeModule = {};
      $wnd.__gwt_activeModules[$moduleName] = activeModule;
    }
    activeModule.unloadModule = function () {
      @com.google.gwt.core.client.GWT::unloadModule()();

      delete $wnd.__gwt_activeModules[$moduleName];
      var modFunc = $moduleName.replace(/\./g, '_');
      $wnd[modFunc] = null;
      setTimeout(function () {
        // Browsers without Object.keys don't benefit from nulling window
        var keys = Object.keys ? Object.keys(window) : [];
        // Browsers without window.frameElement don't benefit from removing the iframe
        var frame = window.frameElement;
        var i;
        // null out seedTable and class entries
        for (key in @com.google.gwt.lang.SeedUtil::seedTable) {
          var obj = @com.google.gwt.lang.SeedUtil::seedTable[key];
          obj.prototype.@java.lang.Object::___clazz = null;
        }
        @com.google.gwt.lang.SeedUtil::seedTable = null;
        // String is special cased
        String.prototype.@java.lang.Object::___clazz = null;
        for (i = 0; i < keys.length; i++) {
          try {
            window[keys[i]] = null;
          } catch (e) {
          }
        }
        if ($wnd != window && frame) {
          frame.parentNode.removeChild(frame);
        }
      }, 1);
    };
  }-*/;

  public boolean isUnloadSupported() {
    return true;
  }

  public int setInterval(JavaScriptObject func, int time) {
    if (!Impl.isModuleUnloaded()) {
      TimerDisposable disposable = new TimerDisposable(intervals, false);
      final int timerId = setInterval0(func, time);
      intervals.put(timerId, disposable);
      disposable.timerId = timerId;
      scheduleDispose(disposable);
      return timerId;
    }
    return -1;
  }

  void clearInterval(int timerId) {
    if (timerId != -1) {
      dispose(intervals.get(timerId));
    }
  }

  void clearTimeout(int timerId) {
    if (timerId != -1) {
      dispose(timeouts.get(timerId));
    }
  }

  void dispose(Disposable d) {
    if (d != null) {
      try {
        d.dispose();
      } catch (Throwable e) {
        GWT.UncaughtExceptionHandler uncaughtExceptionHandler = GWT.getUncaughtExceptionHandler();
        if (uncaughtExceptionHandler != null) {
          uncaughtExceptionHandler.onUncaughtException(e);
        }
      }
      disposables.remove(d);
    }
  }

  void disposeAll() {
    LinkedHashSet<Disposable> copy = new LinkedHashSet<Disposable>(disposables);
    for (Disposable d : copy) {
      dispose(d);
    }
  }

  void scheduleDispose(Disposable d) {
    disposables.add(d);
  }

  int setTimeout(JavaScriptObject func, int time) {
    if (!Impl.isModuleUnloaded()) {
      TimerDisposable disposable = new TimerDisposable(timeouts, true);
      final int timerId = UnloadSupport.setTimeout0(func, time, disposable);
      timeouts.put(timerId, disposable);
      disposable.timerId = timerId;
      scheduleDispose(disposable);
      return timerId;
    }
    return -1;
  }
}
