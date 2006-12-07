// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.ie;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.ModuleSpaceHost;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents an individual browser window and all of its controls.
 */
public class BrowserWidgetIE6 extends BrowserWidget {

  /**
   * IDispatch implementation of the window.external object.
   */
  public class External extends IDispatchImpl {

    /**
     * Called by the loaded HTML page to activate a new module.
     * 
     * @param frameWnd a reference to the IFRAME in which the module's injected
     *          JavaScript will live
     */
    public boolean gwtOnLoad(IDispatch frameWnd, String moduleName) {
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
          BrowserWidgetIE6.this, moduleName);
        ModuleSpaceIE6 moduleSpace = new ModuleSpaceIE6(msh, frameWnd);
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

    protected void getIDsOfNames(String[] names, int[] ids)
        throws HResultException {

      if (names.length >= 2) {
        throw new HResultException(DISP_E_UNKNOWNNAME);
      }

      String name = names[0].toLowerCase();
      if (name.equals("gwtonload")) {
        ids[0] = 1;
        return;
      }

      throw new HResultException(DISP_E_UNKNOWNNAME);
    }

    protected Variant invoke(int dispId, int flags, Variant[] params)
        throws HResultException, InvocationTargetException {

      if (dispId == 0 && (flags & COM.DISPATCH_PROPERTYGET) != 0) {
        // MAGIC: this is the default property, let's just do toString()
        return new Variant(toString());
      } else if (dispId == 1) {
        if ((flags & COM.DISPATCH_METHOD) != 0) {
          // Invoke
          Object[] javaParams = SwtOleGlue.convertVariantsToObjects(
            new Class[]{
              IDispatch.class, String.class, String.class, String.class},
            params, "Calling method 'gwtOnLoad'");

          IDispatch frameWnd = (IDispatch) javaParams[0];
          String moduleName = (String) javaParams[1];
          boolean success = gwtOnLoad(frameWnd, moduleName);

          // boolean return type
          return new Variant(success);
        } else if ((flags & COM.DISPATCH_PROPERTYGET) != 0) {
          // property get on the method itself
          try {
            Method gwtOnLoadMethod = getClass().getMethod("gwtOnLoad",
              new Class[]{IDispatch.class, String.class});
            IDispatchImpl funcObj = new MethodDispatch(null, gwtOnLoadMethod);
            IDispatch disp = new IDispatch(funcObj.getAddress());
            disp.AddRef();
            return new Variant(disp);
          } catch (Exception e) {
            // just return VT_EMPTY
            return new Variant();
          }
        }
        throw new HResultException(COM.E_NOTSUPPORTED);
      }

      // The specified member id is out of range.
      throw new HResultException(COM.DISP_E_MEMBERNOTFOUND);
    }
  }

  public BrowserWidgetIE6(Shell shell, BrowserWidgetHost host) {
    super(shell, host);

    // Expose a 'window.external' object. This object's onLoad() method will
    // be called when a hosted mode application's wrapper HTML is done loading.
    //
    SwtOleGlue.injectBrowserScriptExternalObject(browser, new External());

    // Make sure that the LowLevelIE6 magic is properly initialized.
    //
    LowLevelIE6.init();
  }

}
