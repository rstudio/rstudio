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
package com.google.gwt.dev.shell.mac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.HostedHtmlVersion;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchMethod;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an individual browser window and all of its controls.
 */
public class BrowserWidgetSaf extends BrowserWidget {
  private class ExternalObject implements DispatchObject {

    public int getField(int jsContext, String name) {
      if ("gwtonload".equalsIgnoreCase(name)) {
        return LowLevelSaf.wrapDispatchMethod(jsContext, "gwtOnload",
            new GwtOnLoad());
      } else if ("initmodule".equalsIgnoreCase(name)) {
        return LowLevelSaf.wrapDispatchMethod(jsContext, "initModule",
            new InitModule());
      }
      // Native code eats the same ref it gave us.
      return LowLevelSaf.getJsUndefined(jsContext);
    }

    public Object getTarget() {
      return this;
    }

    public boolean gwtOnLoad(int scriptObject, String moduleName, String version) {
      if (moduleName == null) {
        // Indicates one or more modules are being unloaded.
        return handleUnload(scriptObject);
      }

      TreeLogger logger = getHost().getLogger().branch(TreeLogger.DEBUG,
          "Loading an instance of module '" + moduleName + "'");
      try {
        if (!HostedHtmlVersion.validHostedHtmlVersion(logger, version)) {
          return false;
        }

        // Attach a new ModuleSpace to make it programmable.
        //
        Integer key = new Integer(scriptObject);
        ModuleSpaceHost msh = getHost().createModuleSpaceHost(logger,
            BrowserWidgetSaf.this, moduleName);

        /*
         * The global context for each window object is recorded during the
         * windowScriptObjectAvailable event. Now that we know which window
         * belongs to this module, we can resolve the correct global context.
         */
        final int globalContext = globalContexts.get(scriptObject).intValue();

        ModuleSpace moduleSpace = new ModuleSpaceSaf(logger, msh, scriptObject,
            globalContext, moduleName, key);
        attachModuleSpace(logger, moduleSpace);
        return true;
      } catch (Throwable e) {
        // We do catch Throwable intentionally because there are a ton of things
        // that can go wrong trying to load a module, including Error-dervied
        // things like NoClassDefFoundError.
        // 
        logger.log(TreeLogger.ERROR, "Failure to load module '" + moduleName
            + "'", e);
        return false;
      }
    }

    /**
     * Causes a link to occur for the specified module.
     * 
     * @param moduleName the module name to link
     * @return <code>true</code> if this module is stale and should be
     *         reloaded
     */
    public boolean initModule(String moduleName) {
      return getHost().initModule(moduleName);
    }

    public void setField(int jsContext, String name, int value) {
      try {
        // TODO (knorton): This should produce an error. The SetProperty
        // callback on the native side should be changed to pass an exception
        // array.
      } finally {
        LowLevelSaf.gcUnprotect(jsContext, value);
      }
    }

    /**
     * Unload one or more modules.
     * 
     * @param scriptObject window to unload, 0 if all
     */
    protected boolean handleUnload(int scriptObject) {
      try {
        Integer key = null;
        if (scriptObject != 0) {
          key = new Integer(scriptObject);
        }
        doUnload(key);
        return true;
      } catch (Throwable e) {
        getHost().getLogger().log(TreeLogger.ERROR,
            "Failure to unload modules", e);
        return false;
      }
    }
  }

  private final class GwtOnLoad implements DispatchMethod {

    public int invoke(int jsContext, int jsthis, int[] jsargs, int[] exception) {
      int jsFalse = LowLevelSaf.toJsBoolean(jsContext, false);
      LowLevelSaf.pushJsContext(jsContext);
      try {
        if (!LowLevelSaf.isDispatchObject(jsContext, jsthis)) {
          return jsFalse;
        }

        Object thisObj = LowLevelSaf.unwrapDispatchObject(jsContext, jsthis);
        if (!(thisObj instanceof ExternalObject)) {
          return jsFalse;
        }

        if (jsargs.length < 3) {
          reportIncorrectInvocation("gwtOnLoad", 3, jsargs.length);
          return jsFalse;
        }

        if (!LowLevelSaf.isJsObject(jsContext, jsargs[0])) {
          return jsFalse;
        }
        if (!LowLevelSaf.isJsNull(jsContext, jsargs[1])
            && !LowLevelSaf.isJsString(jsContext, jsargs[1])) {
          return jsFalse;
        }
        String moduleName = LowLevelSaf.toString(jsContext, jsargs[1]);

        if (!LowLevelSaf.isJsString(jsContext, jsargs[2])) {
          return jsFalse;
        }
        String version = LowLevelSaf.toString(jsContext, jsargs[2]);

        boolean result = ((ExternalObject) thisObj).gwtOnLoad(jsargs[0],
            moduleName, version);
        // Native code eats the same ref it gave us.
        return LowLevelSaf.toJsBoolean(jsContext, result);
      } catch (Throwable e) {
        return jsFalse;
      } finally {
        for (int jsarg : jsargs) {
          LowLevelSaf.gcUnprotect(jsContext, jsarg);
        }
        LowLevelSaf.gcUnprotect(jsContext, jsthis);
        LowLevelSaf.popJsContext(jsContext);
      }
    }
  }

  private final class InitModule implements DispatchMethod {

    public int invoke(int jsContext, int jsthis, int[] jsargs, int[] exception) {
      int jsFalse = LowLevelSaf.toJsBoolean(jsContext, false);
      LowLevelSaf.pushJsContext(jsContext);
      try {
        if (!LowLevelSaf.isDispatchObject(jsContext, jsthis)) {
          return jsFalse;
        }

        Object thisObj = LowLevelSaf.unwrapDispatchObject(jsContext, jsthis);
        if (!(thisObj instanceof ExternalObject)) {
          return jsFalse;
        }

        if (jsargs.length < 1) {
          reportIncorrectInvocation("initModule", 1, jsargs.length);
          return jsFalse;
        }

        if (!LowLevelSaf.isJsString(jsContext, jsargs[0])) {
          return jsFalse;
        }
        String moduleName = LowLevelSaf.toString(jsContext, jsargs[0]);

        boolean result = ((ExternalObject) thisObj).initModule(moduleName);
        // Native code eats the same ref it gave us.
        return LowLevelSaf.toJsBoolean(jsContext, result);
      } catch (Throwable e) {
        return jsFalse;
      } finally {
        for (int jsarg : jsargs) {
          LowLevelSaf.gcUnprotect(jsContext, jsarg);
        }
        LowLevelSaf.gcUnprotect(jsContext, jsthis);
        LowLevelSaf.popJsContext(jsContext);
      }
    }
  }

  private static final int REDRAW_PERIOD = 250;

  static {
    LowLevelSaf.init();
  }

  private final Map<Integer, Integer> globalContexts = new HashMap<Integer, Integer>();

  public BrowserWidgetSaf(Shell shell, BrowserWidgetHost host) {
    super(shell, host);

    Browser.setWebInspectorEnabled(true);
    browser.addWindowScriptObjectListener(new Browser.WindowScriptObjectListener() {

      public void windowScriptObjectAvailable(int windowScriptObject) {
        /*
         * When GwtOnLoad fires we may not be able to get to the JSGlobalContext
         * that corresponds to our module frame (since the call to GwtOnLoad
         * could originate in the main page. So as each frame fires a
         * windowScriptObjectAvailable event, we must store all window,
         * globalContext pairs in a HashMap so we can later look up the global
         * context by window object when GwtOnLoad is called.
         */
        int jsGlobalContext = browser.getGlobalContextForWindowObject(windowScriptObject);
        int jsGlobalObject = LowLevelSaf.getGlobalJsObject(jsGlobalContext);
        LowLevelSaf.pushJsContext(jsGlobalContext);

        try {
          globalContexts.put(Integer.valueOf(jsGlobalObject),
              Integer.valueOf(jsGlobalContext));

          int external = LowLevelSaf.wrapDispatchObject(jsGlobalContext,
              new ExternalObject());
          LowLevelSaf.executeScript(jsGlobalContext,
              "function __defineExternal(x) {" + "  window.external = x;" + "}");
          int ignoredResult = LowLevelSaf.invoke(jsGlobalContext,
              jsGlobalObject, "__defineExternal", jsGlobalObject,
              new int[] {external});
          LowLevelSaf.gcUnprotect(jsGlobalContext, ignoredResult);
        } finally {
          LowLevelSaf.popJsContext(jsGlobalContext);
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

  @Override
  public String getUserAgent() {
    // See UserAgent.gwt.xml
    return "safari";
  }
}
