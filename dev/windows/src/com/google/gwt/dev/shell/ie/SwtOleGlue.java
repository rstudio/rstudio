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
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.dev.util.TypeInfo;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.COMObject;
import org.eclipse.swt.internal.ole.win32.DISPPARAMS;
import org.eclipse.swt.internal.ole.win32.EXCEPINFO;
import org.eclipse.swt.internal.ole.win32.GUID;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

/**
 * A bag of static helper methods for mucking about with low-level SWT and COM
 * constructs. Much of this is necessary simply to do things that the SWT
 * implementers weren't really thinking about when they wrote the COM layer.
 */
class SwtOleGlue {

  /**
   * Wrapper for the OS' IUnknown::AddRef().
   */
  public static int addRefInt(int iUnknown) {
    return OS.VtblCall(1, iUnknown);
  }

  /**
   * Converts a java object to its equivalent variant. A ClassLoader is passed
   * here so that Handles can be manipulated properly.
   */
  public static Variant convertObjectToVariant(CompilingClassLoader cl,
      Class type, Object o) {

    if (o == null) {
      return new Variant(0, COM.VT_NULL);
    }

    if (type.equals(String.class)) {
      return new Variant((String) o);
    } else if (type.equals(boolean.class)) {
      return new Variant(((Boolean) o).booleanValue());
    } else if (type.equals(byte.class)) {
      return new Variant(((Byte) o).byteValue());
    } else if (type.equals(short.class)) {
      return new Variant(((Short) o).shortValue());
    } else if (type.equals(char.class)) {
      return new Variant(((Character) o).charValue());
    } else if (type.equals(int.class)) {
      return new Variant(((Integer) o).intValue());
    } else if (type.equals(long.class)) {
      return new Variant((double) ((Long) o).longValue());
    } else if (type.equals(float.class)) {
      return new Variant(((Float) o).floatValue());
    } else if (type.equals(double.class)) {
      return new Variant(((Double) o).doubleValue());
    } else if (type.equals(Variant.class)) {
      return (Variant) o;
    }

    // Handle
    try {
      Class jso = Class.forName(HandleIE6.HANDLE_CLASS, true, cl);
      if (jso.isAssignableFrom(type) && jso.isAssignableFrom(o.getClass())) {
        // Variant never AddRef's its contents.
        //
        IDispatch iDispatch = HandleIE6.getDispatchFromHandle(o);
        iDispatch.AddRef();
        return new Variant(iDispatch);
      }
    } catch (ClassNotFoundException e) {
      // Ignore the exception, if we can't find the class then obviously we
      // don't have to worry about o being one
    }

    // Fallthrough case: Object.
    //
    return wrapObjectAsVariant(cl, o);
  }

  /**
   * Converts an array of variants to their equivalent java objects.
   */
  public static Object[] convertVariantsToObjects(Class[] argTypes,
      Variant[] varArgs, String msgPrefix) {
    Object[] javaArgs = new Object[varArgs.length];
    for (int i = 0; i < javaArgs.length; i++) {
      try {
        Object javaArg = convertVariantToObject(argTypes[i], varArgs[i],
            msgPrefix);
        javaArgs[i] = javaArg;
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Error converting argument "
            + (i + 1) + ": " + e.getMessage());
      }
    }

    return javaArgs;
  }

  /**
   * Try to convert based on the Java method parameter type. Note that we try to
   * be more strict than is typical for automation. It would be just make a mess
   * to let sloppy conversions happen.
   */
  public static Object convertVariantToObject(Class paramType, Variant varArg,
      String msgPrefix) {

    short vt = varArg.getType();
    if ((vt == COM.VT_NULL) || (vt == COM.VT_EMPTY)) {
      // It is actually a null reference.
      return null;
    }

    if (vt == COM.VT_DISPATCH) {
      Object translated = translateDispatchArg(paramType, varArg);

      // Make sure that the method we are going to call matches on this
      // parameter.
      if (paramType.isAssignableFrom(translated.getClass())) {
        return translated;
      }
    }

    switch (TypeInfo.classifyType(paramType)) {
      case TypeInfo.TYPE_WRAP_BOOLEAN:
      case TypeInfo.TYPE_PRIM_BOOLEAN:
        return Boolean.valueOf(varArg.getBoolean());

      case TypeInfo.TYPE_WRAP_BYTE:
      case TypeInfo.TYPE_PRIM_BYTE:
        return new Byte(varArg.getByte());

      case TypeInfo.TYPE_WRAP_CHAR:
      case TypeInfo.TYPE_PRIM_CHAR:
        return new Character(varArg.getChar());

      case TypeInfo.TYPE_WRAP_DOUBLE:
      case TypeInfo.TYPE_PRIM_DOUBLE:
        return new Double(varArg.getDouble());

      case TypeInfo.TYPE_WRAP_FLOAT:
      case TypeInfo.TYPE_PRIM_FLOAT:
        return new Float(varArg.getFloat());

      case TypeInfo.TYPE_WRAP_INT:
      case TypeInfo.TYPE_PRIM_INT:
        return new Integer(varArg.getInt());

      case TypeInfo.TYPE_WRAP_LONG:
      case TypeInfo.TYPE_PRIM_LONG:
        return new Long((long) varArg.getDouble());

      case TypeInfo.TYPE_WRAP_SHORT:
      case TypeInfo.TYPE_PRIM_SHORT:
        return new Short(varArg.getShort());

      case TypeInfo.TYPE_WRAP_STRING:
        return varArg.getString();

      case TypeInfo.TYPE_USER:
        if (varArg.getType() == COM.VT_BSTR) {
          return varArg.getString();
        }
        // if it isn't a String, it's an error, break to error
        break;
    }

    // Just don't know what do to with this.
    throw new IllegalArgumentException(msgPrefix + ": Cannot convert to type "
        + TypeInfo.getSourceRepresentation(paramType, ""));
  }

  /**
   * Copies a IDispatchImpl into an (IDispatch**).
   */
  public static void copyIDispatchImpl(IDispatchImpl o, int ppDispatch) {
    OS.MoveMemory(ppDispatch, new int[] {o.getAddress()}, 4);
    o.AddRef();
  }

  /**
   * Extracts an array of strings from an (OLECHAR**) type (useful for
   * implementing GetIDsOfNames()).
   */
  public static String[] extractStringArrayFromOleCharPtrPtr(int ppchar,
      int count) {
    String[] strings = new String[count];
    for (int i = 0; i < count; ++i) {
      int[] pchar = new int[1];
      OS.MoveMemory(pchar, ppchar + 4 * i, 4);
      strings[i] = extractStringFromOleCharPtr(pchar[0]);
    }
    return strings;
  }

  /**
   * Extracts a string from an (OLECHAR*) type.
   */
  public static String extractStringFromOleCharPtr(int pOleChar) {
    // TODO: double-check the encoding (is it UTF-16?, what)
    int size = COM.SysStringByteLen(pOleChar);
    if (size > 8192) {
      size = 8192;
    }
    char[] buffer = new char[(size + 1) / 2];
    COM.MoveMemory(buffer, pOleChar, size);

    String s = new String(buffer);
    if (s.indexOf('\0') != -1) {
      return s.substring(0, s.indexOf('\0'));
    } else {
      return s;
    }
  }

  /**
   * Gets the browser's OleAutomation object.
   */
  public static OleAutomation getBrowserAutomationObject(Browser browser) {
    return (OleAutomation) LowLevel.snatchFieldObjectValue(browser, "auto");
  }

  /**
   * Injects an object into the Browser class that resolves to IE's
   * 'window.external' object.
   */
  public static void injectBrowserScriptExternalObject(Browser browser,
      final IDispatchImpl external) {
    // Grab the browser's 'site.iDocHostUIHandler' field.
    //
    Object webSite = LowLevel.snatchFieldObjectValue(browser, "site");
    COMObject iDocHostUIHandler = (COMObject) LowLevel.snatchFieldObjectValue(
        webSite, "iDocHostUIHandler");

    // Create a COMObjectProxy that will override GetExternal().
    //
    COMObjectProxy webSiteProxy = new COMObjectProxy(new int[] {
        2, 0, 0, 4, 1, 5, 0, 0, 1, 1, 1, 3, 3, 2, 2, 1, 3, 2}) {

      {
        // make sure we hold onto a ref on the external object
        external.AddRef();
      }

      public int method15(int[] args) {
        // GetExternal() is method 15.
        //
        return GetExternal(args[0]);
      }

      public int method2(int[] args) {
        int result = super.method2(args);
        if (result == 0) {
          external.Release();
        }
        return result;
      }

      // CHECKSTYLE_OFF
      int GetExternal(int ppDispatch) {
        // CHECKSTYLE_ON
        if (ppDispatch != 0) {
          try {
            // Return the 'external' object.
            //
            copyIDispatchImpl(external, ppDispatch);
            return COM.S_OK;
          } catch (Throwable e) {
            e.printStackTrace();
            return COM.E_FAIL;
          }
        } else {
          OS.MoveMemory(ppDispatch, new int[] {0}, 4);
          return COM.E_NOTIMPL;
        }
      }

    };

    // Interpose the proxy in front of the browser's iDocHostUiHandler.
    //
    webSiteProxy.interpose(iDocHostUIHandler);
  }

  /**
   * Wrapper for the OS' IUnknown::Release().
   */
  public static int releaseInt(int iUnknown) {
    return OS.VtblCall(2, iUnknown);
  }

  /**
   * Builds an EXCEPINFO structure.
   */
  public static void setEXCEPINFO(int pEXCEPINFO, int wcode, String source,
      String desc, int scode) {
    // 0: wCode (size = 2)
    // 4: bstrSource (size = 4)
    // 8: bstrDescription (size = 4)
    // 28: scode (size = 4)
    //
    OS.MoveMemory(pEXCEPINFO + 0, new short[] {(short) wcode}, 2);

    if (source != null) {
      int bstrSource = sysAllocString(source);
      OS.MoveMemory(pEXCEPINFO + 4, new int[] {bstrSource}, 4);
    }

    if (desc != null) {
      int bstrDesc = sysAllocString(desc);
      OS.MoveMemory(pEXCEPINFO + 8, new int[] {bstrDesc}, 4);
    }

    OS.MoveMemory(pEXCEPINFO + 28, new int[] {scode}, 4);
  }

  /**
   * Wrapper for the OS' SysAllocString().
   */
  public static int sysAllocString(String s) {
    int len = s.length();
    char[] chars = new char[len + 1];
    s.getChars(0, len, chars, 0);
    return COM.SysAllocString(chars);
  }

  /**
   * Gets an OleAutomation interface from an IDispatch wrapper.
   */
  public static OleAutomation wrapIDispatch(IDispatch disp) {
    return new OleAutomation(disp);
  }

  /**
   * Wraps a Java object in an IDispatchProxy and packages it in a variant.
   */
  public static Variant wrapObjectAsVariant(CompilingClassLoader cl, Object o) {
    if (o == null) {
      return new Variant(0, COM.VT_NULL);
    }

    IDispatch disp = new IDispatch(new IDispatchProxy(cl, o).getAddress());
    disp.AddRef();
    return new Variant(disp);
  }

  /**
   * Decides what to do with an incoming IDispatch arg. Two possibilities here:
   * (1) We received a true javascript object (e.g. DOM object), in which case
   * we wrap it in a Handle. (2) We received a Java object that was passed
   * through the outside world and back, in which case we use black magic to get
   * it back.
   */
  private static Object translateDispatchArg(Class type, Variant varArg) {
    assert (varArg.getType() == COM.VT_DISPATCH);

    Variant result = null;
    try {
      /*
       * This implemention copied from OleAutomation.invoke(). We used to have a
       * varArg.getAutomation().invoke() implementation, but it turns out the
       * querying for typeInfo that occurs in the OleAutomation(IDispatch)
       * constructor will cause a VM crash on some kinds of JavaScript objects,
       * such as the window.alert function. So we do it by hand.
       */
      IDispatch dispatch = varArg.getDispatch();
      result = new Variant();
      int pVarResultAddress = 0;
      int globalRef = 0;
      try {
        pVarResultAddress = OS.GlobalAlloc(OS.GMEM_FIXED | OS.GMEM_ZEROINIT,
            Variant.sizeof);
        int[] pArgErr = new int[1];
        int hr = dispatch.Invoke(IDispatchProxy.DISPID_MAGIC_GETGLOBALREF,
            new GUID(), COM.LOCALE_USER_DEFAULT, COM.DISPATCH_METHOD,
            new DISPPARAMS(), pVarResultAddress, new EXCEPINFO(), pArgErr);

        if (hr >= COM.S_OK) {
          result = Variant.win32_new(pVarResultAddress);
          globalRef = result.getInt();
        }
      } finally {
        if (pVarResultAddress != 0) {
          COM.VariantClear(pVarResultAddress);
          OS.GlobalFree(pVarResultAddress);
        }
      }

      // Result will be null if the dispid wasn't found.
      if (globalRef != 0) {
        // This is really a Java object being passed back via an IDispatchProxy.
        IDispatchProxy proxy = (IDispatchProxy) LowLevel.objFromGlobalRefInt(globalRef);
        return proxy.getTarget();
      } else if (type == OleAutomation.class) {
        // return the automation object
        return varArg.getAutomation();
      } else {
        // This is a true JavaScript object, so wrap it.
        return HandleIE6.createHandle(type, dispatch.getAddress());
      }
    } finally {

      if (result != null) {
        result.dispose();
      }
    }
  }
}
