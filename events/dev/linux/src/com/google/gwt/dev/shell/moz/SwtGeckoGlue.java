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

import org.eclipse.swt.internal.mozilla.XPCOM;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A bag of static helper methods for mucking about with low-level SWT and Gecko
 * constructs.
 * 
 * TODO(jat): remove this class by replacing the nsISupports code with
 * JsRootedValue references.
 */
class SwtGeckoGlue {

  private static boolean areMethodsInitialized;
  private static final String ERRMSG_CANNOT_ACCESS = "Unable to find or access necessary SWT XPCOM methods.  Ensure that the correct version of swt.jar is being used.";
  private static final String ERRMSG_CANNOT_INVOKE = "Unable to invoke a necessary SWT XPCOM method.  Ensure that the correct version of swt.jar is being used.";
  private static Method XPCOMVtblCall;

  /**
   * Wrapper for XPCOM's nsISupports::AddRef().
   */
  public static int addRefInt(int nsISupports) {
    ensureMethodsInitialized();
    Throwable rethrow = null;
    try {
      Object retVal = XPCOMVtblCall.invoke(null, new Object[] {
          new Integer(1), new Integer(nsISupports)});
      return ((Integer) retVal).intValue();
    } catch (IllegalArgumentException e) {
      rethrow = e;
    } catch (IllegalAccessException e) {
      rethrow = e;
    } catch (InvocationTargetException e) {
      rethrow = e;
    }
    throw new RuntimeException(ERRMSG_CANNOT_INVOKE, rethrow);
  }

  /**
   * Wrapper for XPCOM's nsISupports::Release().
   */
  public static int releaseInt(int nsISupports) {
    ensureMethodsInitialized();
    Throwable rethrow = null;
    try {
      Object retVal = XPCOMVtblCall.invoke(null, new Object[] {
          new Integer(2), new Integer(nsISupports)});
      return ((Integer) retVal).intValue();
    } catch (IllegalArgumentException e) {
      rethrow = e;
    } catch (IllegalAccessException e) {
      rethrow = e;
    } catch (InvocationTargetException e) {
      rethrow = e;
    }
    throw new RuntimeException(ERRMSG_CANNOT_INVOKE, rethrow);
  }

  private static void ensureMethodsInitialized() {
    if (!areMethodsInitialized) {
      Throwable rethrow = null;

      // Never try more than once to initialize, even if there's an exception.
      areMethodsInitialized = true;
      try {
        XPCOMVtblCall = XPCOM.class.getDeclaredMethod("VtblCall", new Class[] {
            int.class, int.class});
        XPCOMVtblCall.setAccessible(true);
      } catch (SecurityException e) {
        rethrow = e;
      } catch (NoSuchMethodException e) {
        rethrow = e;
      }

      if (rethrow != null) {
        throw new RuntimeException(ERRMSG_CANNOT_ACCESS, rethrow);
      }
    }
  }
}
