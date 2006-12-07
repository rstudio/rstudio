// Copyright 2006 Google Inc. All Rights Reserved.
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

/**
 * Represents an individual browser window and all of its controls.
 */
public class BrowserWidgetMoz extends BrowserWidget {

  private class ExternalObjectImpl implements ExternalObject {

    public int resolveReference(String ident) {
      return LowLevelMoz.JSVAL_VOID;
    }

    public boolean gwtOnLoad(int scriptObject, String moduleName) {
      try {
        if (moduleName == null) {
          // Indicates the page is being unloaded.
          //
          onPageUnload();
          return true;
        }

        // Attach a new ModuleSpace to make it programmable.
        //
        ModuleSpaceHost msh = getHost().createModuleSpaceHost(
          BrowserWidgetMoz.this, moduleName);
        ModuleSpace moduleSpace = new ModuleSpaceMoz(msh, scriptObject);
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
  }

  public BrowserWidgetMoz(Shell shell, BrowserWidgetHost host) {
    super(shell, host);

    // Expose a 'window.external' object factory. The created object's
    // gwtOnLoad() method will be called when a hosted mode application's wrapper
    // HTML is done loading.
    //
    final ExternalFactory externalFactory = new ExternalFactory() {

      public ExternalObject createExternalObject() {
        if (externalObject == null) {
          externalObject = new ExternalObjectImpl();
        }
        return externalObject;
      }

      public boolean matchesDOMWindow(int domWindow) {
        nsIWebBrowser webBrowser = (nsIWebBrowser) LowLevel
          .snatchFieldObjectValue(browser.getClass(), browser, "webBrowser");
        int[] aContentDOMWindow = new int[1];
        webBrowser.GetContentDOMWindow(aContentDOMWindow);
        if (aContentDOMWindow[0] == domWindow) {
          return true;
        }
        return false;
      }

      private ExternalObject externalObject = null;

    };

    LowLevelMoz.registerExternalFactory(externalFactory);

    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        LowLevelMoz.unregisterExternalFactory(externalFactory);
      }
    });
  }

}
