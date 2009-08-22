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
package com.google.gwt.dev.shell.ie;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.HostedHtmlVersion;
import com.google.gwt.dev.shell.MethodAdaptor;
import com.google.gwt.dev.shell.ModuleSpaceHost;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.ole.win32.OleAutomation;
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
     * @param moduleName the name of the module to load, null if this is being
     *          unloaded
     */
    public boolean gwtOnLoad(IDispatch frameWnd, String moduleName,
        String version) {
      if (moduleName == null) {
        // Indicates one or more modules are being unloaded.
        return handleUnload(frameWnd);
      }

      TreeLogger logger = getHost().getLogger().branch(TreeLogger.DEBUG,
          "Loading an instance of module '" + moduleName + "'");
      try {
        if (!HostedHtmlVersion.validHostedHtmlVersion(logger, version)) {
          throw new HResultException(COM.E_INVALIDARG);
        }

        // set the module ID
        int moduleID = ++nextModuleID;
        Integer key = new Integer(moduleID);
        setIntProperty(frameWnd, "__gwt_module_id", moduleID);

        // Attach a new ModuleSpace to make it programmable.
        //
        ModuleSpaceHost msh = getHost().createModuleSpaceHost(logger,
            BrowserWidgetIE6.this, moduleName);
        ModuleSpaceIE6 moduleSpace = new ModuleSpaceIE6(logger, msh, frameWnd,
            moduleName, key);
        attachModuleSpace(logger, moduleSpace);
        return true;
      } catch (Throwable e) {
        // We do catch Throwable intentionally because there are a ton of things
        // that can go wrong trying to load a module, including Error-derived
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

    @Override
    protected void getIDsOfNames(String[] names, int[] ids)
        throws HResultException {

      if (names.length >= 2) {
        throw new HResultException(DISP_E_UNKNOWNNAME);
      }

      String name = names[0].toLowerCase();
      if (name.equals("gwtonload")) {
        ids[0] = 1;
        return;
      } else if (name.equals("initmodule")) {
        ids[0] = 2;
        return;
      }

      throw new HResultException(DISP_E_UNKNOWNNAME);
    }

    /**
     * Unload one or more modules.
     * 
     * @param frameWnd window to unload, null if all
     */
    protected boolean handleUnload(IDispatch frameWnd) {
      try {
        Integer key = null;
        if (frameWnd != null) {
          key = new Integer(getIntProperty(frameWnd, "__gwt_module_id"));
        }
        doUnload(key);
        return true;
      } catch (Throwable e) {
        getHost().getLogger().log(TreeLogger.ERROR,
            "Failure to unload modules", e);
        return false;
      }
    }

    @Override
    protected Variant invoke(int dispId, int flags, Variant[] params)
        throws HResultException, InvocationTargetException {

      if (dispId == 0 && (flags & COM.DISPATCH_PROPERTYGET) != 0) {
        // MAGIC: this is the default property, let's just do toString()
        return new Variant(toString());
      } else if (dispId == 1) {
        if ((flags & COM.DISPATCH_METHOD) != 0) {
          try {
            if (params.length < 3) {
              reportIncorrectInvocation("gwtOnLoad", 3, params.length);
              throw new HResultException(COM.E_INVALIDARG);
            }
            IDispatch frameWnd = (params[0].getType() == COM.VT_DISPATCH)
                ? params[0].getDispatch() : null;
            String moduleName = (params[1].getType() == COM.VT_BSTR)
                ? params[1].getString() : null;
            String version = (params[2].getType() == COM.VT_BSTR)
                ? params[2].getString() : null;
            boolean success = gwtOnLoad(frameWnd, moduleName, version);

            // boolean return type
            return new Variant(success);
          } catch (SWTException e) {
            throw new HResultException(COM.E_INVALIDARG);
          }
        } else if ((flags & COM.DISPATCH_PROPERTYGET) != 0) {
          // property get on the method itself
          try {
            Method gwtOnLoadMethod = getClass().getMethod("gwtOnLoad",
                new Class[] {IDispatch.class, String.class, String.class});
            MethodAdaptor methodAdaptor = new MethodAdaptor(gwtOnLoadMethod);
            IDispatchImpl funcObj = new MethodDispatch(null, methodAdaptor);
            IDispatch disp = new IDispatch(funcObj.getAddress());
            disp.AddRef();
            return new Variant(disp);
          } catch (Exception e) {
            // just return VT_EMPTY
            return new Variant();
          }
        }
        throw new HResultException(COM.E_NOTSUPPORTED);
      } else if (dispId == 2) {
        if ((flags & COM.DISPATCH_METHOD) != 0) {
          try {
            if (params.length < 1) {
              reportIncorrectInvocation("initModule", 1, params.length);
              throw new HResultException(COM.E_INVALIDARG);
            }
            String moduleName = (params[0].getType() == COM.VT_BSTR)
                ? params[0].getString() : null;
            boolean reload = initModule(moduleName);

            // boolean return type
            return new Variant(reload);
          } catch (SWTException e) {
            throw new HResultException(COM.E_INVALIDARG);
          }
        } else if ((flags & COM.DISPATCH_PROPERTYGET) != 0) {
          // property get on the method itself
          try {
            Method gwtOnLoadMethod = getClass().getMethod("initModule",
                new Class[] {String.class});
            MethodAdaptor methodAdaptor = new MethodAdaptor(gwtOnLoadMethod);
            IDispatchImpl funcObj = new MethodDispatch(null, methodAdaptor);
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

  // counter to generate unique module IDs
  private static int nextModuleID = 0;

  /**
   * Get a property off a window object as an integer.
   * 
   * @param frameWnd inner code frame
   * @param propName name of the property to get
   * @return the property value as an integer
   * @throws RuntimeException if the property does not exist
   */
  private static int getIntProperty(IDispatch frameWnd, String propName) {
    OleAutomation window = null;
    try {
      window = new OleAutomation(frameWnd);
      int[] dispID = window.getIDsOfNames(new String[] {propName});
      if (dispID == null) {
        throw new RuntimeException("No such property " + propName);
      }
      Variant value = null;
      try {
        value = window.getProperty(dispID[0]);
        return value.getInt();
      } finally {
        if (value != null) {
          value.dispose();
        }
      }
    } finally {
      if (window != null) {
        window.dispose();
      }
    }
  }

  /**
   * Set a property off a window object from an integer value.
   * 
   * @param frameWnd inner code frame
   * @param propName name of the property to set
   * @param intValue the value to set
   * @throws RuntimeException if the property does not exist
   */
  private static void setIntProperty(IDispatch frameWnd, String propName,
      int intValue) {
    OleAutomation window = null;
    try {
      window = new OleAutomation(frameWnd);
      int[] dispID = window.getIDsOfNames(new String[] {propName});
      if (dispID == null) {
        throw new RuntimeException("No such property " + propName);
      }
      Variant value = null;
      try {
        value = new Variant(intValue);
        window.setProperty(dispID[0], value);
      } finally {
        if (value != null) {
          value.dispose();
        }
      }
    } finally {
      if (window != null) {
        window.dispose();
      }
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

  @Override
  public String getUserAgent() {
    // See UserAgent.gwt.xml
    return "ie6";
  }
}
