// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.CompilingClassLoader;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.ole.win32.Variant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps an arbitrary Java Method as an Automation-compatible server. The class
 * was motivated by the need to expose Java objects into JavaScript.
 * 
 * <p>
 * <b>Features</b>
 * </p>
 * <ul>
 * <li>Implements the <code>IDispatch</code> interface for you</li>
 * <li>If the COM client keeps a reference to this object, this object is
 * prevented from being garbage collected</li>
 * </ul>
 * 
 * <p>
 * <b>Limitations</b>
 * </p>
 * <ul>
 * <li>Only late-bound dispatch is supported</li>
 * <li>Named arguments are not supported (see {@link #GetIDsOfNames)).</li>
 * </ul>
 */
class MethodDispatch extends IDispatchImpl {

  private final CompilingClassLoader classLoader;

  private final Method method;

  public MethodDispatch(CompilingClassLoader classLoader, Method method) {
    this.classLoader = classLoader;
    this.method = method;
  }

  public String toString() {
    return "\nfunction  " + method.toString() + "(){\n    [native code]\n}\n";
  }

  /**
   * ID 0 is magic. It can either mean toString or invoke, depending on the
   * flags. So we start with ID 1 for toString. {@link IDispatchProxy} and
   * {@link BrowserWidgetIE6.External} should be fixed to do the same.
   */
  protected void getIDsOfNames(String[] names, int[] ids)
      throws HResultException {
    if (names[0].equalsIgnoreCase("toString")) {
      ids[0] = 1;
    } else if (names[0].equalsIgnoreCase("call")) {
      ids[0] = 2;
    } else {
      throw new HResultException(IDispatchProxy.DISP_E_UNKNOWNNAME);
    }
  }

  /*
   * Handles all the things the browser can do to a function object.
   */
  protected Variant invoke(int id, int flags, Variant[] params)
      throws HResultException, InvocationTargetException {
    switch (id) {
      case 0:
        if ((flags & COM.DISPATCH_METHOD) != 0) {
          // implicit call -- "m()"
          return callMethod(classLoader, null, params, method);
        } else if ((flags & COM.DISPATCH_PROPERTYGET) != 0) {
          // implicit toString -- "'foo' + m"
          return new Variant(toString());
        }
        break;
      case 1:
        // "m.toString()"
        if ((flags & (COM.DISPATCH_METHOD | COM.DISPATCH_PROPERTYGET)) != 0) {
          return new Variant(toString());
        }
        break;
      case 2:
        // "m.call(thisObj, arg)"
        if ((flags & COM.DISPATCH_METHOD) != 0) {
          /*
           * First param must be a this object of the correct type (for instance
           * methods). If method is static, it can be null.
           */
          Object jthis = SwtOleGlue.convertVariantToObject(
              method.getDeclaringClass(), params[0], "this");
          Variant[] otherParams = new Variant[params.length - 1];
          System.arraycopy(params, 1, otherParams, 0, otherParams.length);
          return callMethod(classLoader, jthis, otherParams, method);
        }
        break;
      default:
        // The specified member id is out of range.
        throw new HResultException(COM.DISP_E_MEMBERNOTFOUND);
    }
    throw new HResultException(COM.E_NOTSUPPORTED);
  }
}