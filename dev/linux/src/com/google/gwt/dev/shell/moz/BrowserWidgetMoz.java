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
import com.google.gwt.dev.shell.HostedHtmlVersion;
import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.moz.LowLevelMoz.ExternalFactory;
import com.google.gwt.dev.shell.moz.LowLevelMoz.ExternalObject;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.internal.mozilla.nsIWebBrowser;
import org.eclipse.swt.widgets.Shell;

/**
 * Represents an individual browser window and all of its controls.
 */
public class BrowserWidgetMoz extends BrowserWidget {

  private class ExternalObjectImpl implements ExternalObject {

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

        Object key = Integer.valueOf(scriptObject);
        // Attach a new ModuleSpace to make it programmable.
        ModuleSpaceHost msh = getHost().createModuleSpaceHost(logger,
            BrowserWidgetMoz.this, moduleName);
        ModuleSpace moduleSpace = new ModuleSpaceMoz(logger, msh, scriptObject,
            moduleName, key);
        attachModuleSpace(logger, moduleSpace);
        return true;
      } catch (Throwable e) {
        // We do catch Throwable intentionally because there are a ton of things
        // that can go wrong trying to load a module, including Error-derived
        // things like NoClassDefFoundError.
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

    /**
     * Unload one or more modules.
     * 
     * @param scriptObject window to unload, 0 if all
     */
    protected boolean handleUnload(int scriptObject) {
      try {
        Integer key = null;
        if (scriptObject != 0) {
          key = Integer.valueOf(scriptObject);
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

  public BrowserWidgetMoz(Shell shell, BrowserWidgetHost host) {
    super(shell, host);
    host.getLogger().log(TreeLogger.DEBUG,
        "Using Mozilla install at " + MozillaInstall.getLoaded().getPath(),
        null);

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

  @Override
  public String getUserAgent() {
    // OLD Mozilla. See UserAgent.gwt.xml
    return "gecko";
  }
}
