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
public class UnloadSupportEnabled extends com.google.gwt.core.client.impl.UnloadSupport {

  static class TimerDisposable implements com.google.gwt.core.client.impl.Disposable {

    int timerId;
    private Map<Integer, com.google.gwt.core.client.impl.Disposable> timeoutMap;
    private boolean timeout;

    public TimerDisposable(Map<Integer, com.google.gwt.core.client.impl.Disposable> timeoutMap,
        boolean isTimeout) {
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

  private Map<Integer, com.google.gwt.core.client.impl.Disposable> timeouts =
      new HashMap<Integer, com.google.gwt.core.client.impl.Disposable>();
  private Map<Integer, com.google.gwt.core.client.impl.Disposable> intervals =
      new HashMap<Integer, com.google.gwt.core.client.impl.Disposable>();
  private Set<com.google.gwt.core.client.impl.Disposable> disposables =
      new LinkedHashSet<com.google.gwt.core.client.impl.Disposable>();

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
        // null out prototypesByTypeId and class entries
        for (key in @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId) {
          var obj = @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId[key];
          obj.prototype.@java.lang.Object::___clazz = null;
        }
        @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId = null;
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
    if (!com.google.gwt.core.client.impl.Impl.isModuleUnloaded()) {
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

  void dispose(com.google.gwt.core.client.impl.Disposable d) {
    if (d != null) {
      try {
        d.dispose();
      } catch (Throwable e) {
        GWT.reportUncaughtException(e);
      }
      disposables.remove(d);
    }
  }

  void disposeAll() {
    LinkedHashSet<com.google.gwt.core.client.impl.Disposable> copy =
        new LinkedHashSet<com.google.gwt.core.client.impl.Disposable>(disposables);
    for (com.google.gwt.core.client.impl.Disposable d : copy) {
      dispose(d);
    }
  }

  void scheduleDispose(com.google.gwt.core.client.impl.Disposable d) {
    disposables.add(d);
  }

  int setTimeout(JavaScriptObject func, int time) {
    if (!com.google.gwt.core.client.impl.Impl.isModuleUnloaded()) {
      TimerDisposable disposable = new TimerDisposable(timeouts, true);
      final int timerId = com.google.gwt.core.client.impl.UnloadSupport
          .setTimeout0(func, time, disposable);
      timeouts.put(timerId, disposable);
      disposable.timerId = timerId;
      scheduleDispose(disposable);
      return timerId;
    }
    return -1;
  }
}
