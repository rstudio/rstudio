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
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.LowLevel;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.COMObject;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.ole.win32.Variant;

/**
 * A bag of static helper methods for mucking about with low-level SWT and COM
 * constructs. Much of this is necessary simply to do things that the SWT
 * implementers weren't really thinking about when they wrote the COM layer.
 */
class SwtOleGlue {

  /**
   * Converts a java object to its equivalent variant. A ClassLoader is passed
   * here so that Handles can be manipulated properly.
   */
  public static Variant convertObjectToVariant(CompilingClassLoader cl,
      Class type, Object o) {
    if (type.equals(Variant.class)) {
      return (Variant) o;
    }
    JsValueIE6 jsValue = new JsValueIE6();
    JsValueGlue.set(jsValue, cl, type, o);
    return jsValue.getVariant();
  }

  /**
   * Converts an array of variants to their equivalent java objects.
   */
  public static Object[] convertVariantsToObjects(Class[] argTypes,
      Variant[] varArgs, String msgPrefix) {
    Object[] javaArgs = new Object[Math.min(varArgs.length, argTypes.length)];
    for (int i = 0; i < javaArgs.length; i++) {
      try {
        Object javaArg = JsValueGlue.get(new JsValueIE6(varArgs[i]),
            argTypes[i], msgPrefix);
        javaArgs[i] = javaArg;
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Error converting argument "
            + (i + 1) + ": " + e.getMessage());
      }
    }

    return javaArgs;
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
            external.AddRef();
            OS.MoveMemory(ppDispatch, new int[] {external.getAddress()}, 4);
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
   * Convert a Java string to a COM BSTR.
   * 
   * Wrapper for the OS' SysAllocStringLen(), since SysAllocString() is not
   * safe for embedded nulls.
   */
  public static int sysAllocString(String s) {
    return COM.SysAllocStringLen(s.toCharArray(), s.length());
  }
 
}
