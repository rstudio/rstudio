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
package com.google.gwt.dev.shell.moz;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.moz.LowLevelMoz.ExternalFactory;
import com.google.gwt.dev.shell.moz.LowLevelMoz.ExternalObject;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.internal.mozilla.nsIWebBrowser;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an individual browser window and all of its controls.
 */
public class BrowserWidgetMoz extends BrowserWidget {

  private class ExternalObjectImpl implements ExternalObject {

    private Map modulesByScriptObject = new HashMap();
    private Map namesByScriptObject = new HashMap();

    public boolean gwtOnLoad(int scriptObject, String moduleName) {
      try {
        if (moduleName == null) {
          // Indicates one or more modules are being unloaded.
          //
          handleUnload(scriptObject);
          return true;
        }

        // Attach a new ModuleSpace to make it programmable.
        //
        ModuleSpaceHost msh = getHost().createModuleSpaceHost(
            BrowserWidgetMoz.this, moduleName);
        ModuleSpace moduleSpace = new ModuleSpaceMoz(msh, scriptObject);
        attachModuleSpace(moduleName, moduleSpace);
        modulesByScriptObject.put(new Integer(scriptObject), moduleSpace);
        namesByScriptObject.put(new Integer(scriptObject), moduleName);
        return true;
      } catch (Throwable e) {
        // We do catch Throwable intentionally because there are a ton of things
        // that can go wrong trying to load a module, including Error-derived
        // things like NoClassDefFoundError.
        // 
        getHost().getLogger().log(TreeLogger.ERROR,
            "Failure to load module '" + moduleName + "'", e);
        return false;
      }
    }

    /**
     * Unload one or more modules.
     * 
     * TODO(jat): Note that currently the JS code does not unload individual
     * modules, so this change is in preparation for when the JS code is
     * fixed. 
     * 
     * @param scriptObject window to unload, 0 if all
     */
    protected void handleUnload(int scriptObject) {
      // TODO(jat): true below restores original behavior of always
      // unloading all modules until the resulting GC issues (the
      // order of destruction is undefined so JS_RemoveRoot gets
      // called on a non-existent JSContext)
      if (true || scriptObject == 0) {
        onPageUnload();
        modulesByScriptObject.clear();
        namesByScriptObject.clear();
        return;
      }
      Integer key = new Integer(scriptObject);
      ModuleSpace moduleSpace = (ModuleSpace)modulesByScriptObject.get(key);
      String moduleName = (String)namesByScriptObject.get(key);
      unloadModule(moduleSpace, moduleName);
      modulesByScriptObject.remove(key);
      namesByScriptObject.remove(key);
    }
  }

  public BrowserWidgetMoz(Shell shell, BrowserWidgetHost host) {
    super(shell, host);

    // Expose a 'window.external' object factory. The created object's
    // gwtOnLoad() method will be called when a hosted mode application's
    // wrapper
    // HTML is done loading.
    //
    final ExternalFactory externalFactory = new ExternalFactory() {

      private ExternalObject externalObject = null;

      public ExternalObject createExternalObject() {
        if (externalObject == null) {
          externalObject = new ExternalObjectImpl();
        }
        return externalObject;
      }

      public boolean matchesDOMWindow(int domWindow) {
        nsIWebBrowser webBrowser = (nsIWebBrowser) LowLevel.snatchFieldObjectValue(
            browser.getClass(), browser, "webBrowser");
        int[] aContentDOMWindow = new int[1];
        webBrowser.GetContentDOMWindow(aContentDOMWindow);
        if (aContentDOMWindow[0] == domWindow) {
          return true;
        }
        return false;
      }

    };

    LowLevelMoz.registerExternalFactory(externalFactory);

    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        LowLevelMoz.unregisterExternalFactory(externalFactory);
      }
    });
  }

}
