/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.shell.mac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchMethod;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.WebKit;
import org.eclipse.swt.widgets.Shell;

/**
 * Represents an individual browser window and all of its controls.
 */
public class BrowserWidgetSaf extends BrowserWidget {
  private class ExternalObject implements DispatchObject {

    public int getField(String name) {
      if ("gwtonload".equalsIgnoreCase(name)) {
        return LowLevelSaf.wrapFunction("gwtOnload", new GwtOnLoad());
      }
      return 0;
    }

    public Object getTarget() {
      return null;
    }

    public boolean gwtOnLoad(int scriptObject, String moduleName) {
      try {
        if (moduleName == null) {
          // Indicates the page is being unloaded.
          // TODO(jat): add support to unload a single module
          onPageUnload();
          return true;
        }

        // Attach a new ModuleSpace to make it programmable.
        //
        ModuleSpaceHost msh = getHost().createModuleSpaceHost(
            BrowserWidgetSaf.this, moduleName);
        ModuleSpace moduleSpace = new ModuleSpaceSaf(msh, scriptObject);
        attachModuleSpace(moduleName, moduleSpace);
        return true;
      } catch (Throwable e) {
        // We do catch Throwable intentionally because there are a ton of things
        // that can go wrong trying to load a module, including Error-dervied
        // things like NoClassDefFoundError.
        // 
        getHost().getLogger().log(TreeLogger.ERROR,
            "Failure to load module '" + moduleName + "'", e);
        return false;
      }
    }

    public void setField(String name, int value) {
    }
  }
  private static final class GwtOnLoad implements DispatchMethod {

    public int invoke(int execState, int jsthis, int[] jsargs) {
      int jsFalse = LowLevelSaf.convertBoolean(false);
      LowLevelSaf.pushExecState(execState);
      try {
        if (!LowLevelSaf.isWrappedDispatch(jsthis)) {
          return jsFalse;
        }

        Object thisObj = LowLevelSaf.unwrapDispatch(jsthis);
        if (!(thisObj instanceof ExternalObject)) {
          return jsFalse;
        }

        if (jsargs.length < 2) {
          return jsFalse;
        }

        if (!LowLevelSaf.isObject(jsargs[0])) {
          return jsFalse;
        }
        if (!LowLevelSaf.isString(jsargs[1])) {
          return jsFalse;
        }
        String moduleName = LowLevelSaf.coerceToString(execState, jsargs[1]);

        boolean result = ((ExternalObject) thisObj).gwtOnLoad(jsargs[0],
            moduleName);
        return LowLevelSaf.convertBoolean(result);
      } catch (Throwable e) {
        return jsFalse;
      } finally {
        LowLevelSaf.popExecState(execState);
      }
    }
  }

  private static final int REDRAW_PERIOD = 250;

  static {
    LowLevelSaf.init();
  }

  public BrowserWidgetSaf(Shell shell, BrowserWidgetHost host) {
    super(shell, host);

    Browser.setWebInspectorEnabled(true);
    browser.setUserAgentApplicationName("Safari 419.3");
    browser.addWindowScriptObjectListener(new Browser.WindowScriptObjectListener() {

      public void windowScriptObjectAvailable(int windowScriptObject) {
        int sel = WebKit.sel_registerName("_imp");
        int windowObject = WebKit.objc_msgSend(windowScriptObject, sel);
        try {
          LowLevelSaf.jsLock();
          final int globalExec = LowLevelSaf.getGlobalExecState(windowObject);
          int external = LowLevelSaf.wrapDispatch(new ExternalObject());
          LowLevelSaf.executeScript(globalExec,
              "function __defineExternal(x) {" + "  window.external = x;" + "}");
          LowLevelSaf.invoke(globalExec, windowObject, "__defineExternal",
              windowObject, new int[] {external});
        } finally {
          LowLevelSaf.jsUnlock();
        }
      }

    });

    /*
     * HACK (knorton) - SWT wrapper on WebKit seems to cause unreliable repaints
     * when the DOM changes inside of WebView. To compensate for this, every
     * quarter second, we tell WebView to repaint itself fully.
     */
    getDisplay().timerExec(REDRAW_PERIOD, new Runnable() {
      public void run() {
        if (browser.isDisposed() || isDisposed()) {
          // stop running if we're disposed
          return;
        }
        // Force the browser to refresh
        browser.setNeedsDisplay(true);
        // Reschedule this object to run again
        getDisplay().timerExec(REDRAW_PERIOD, this);
      }
    });
  }

}
