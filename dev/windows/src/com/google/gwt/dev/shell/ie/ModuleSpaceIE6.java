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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.ie.IDispatchImpl.HResultException;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.swt.widgets.Display;

/**
 * An implementation of {@link com.google.gwt.dev.shell.ModuleSpace} for
 * Internet Explorer 6.
 */
public class ModuleSpaceIE6 extends ModuleSpace {

  // CHECKSTYLE_OFF
  private static int CODE(int hresult) {
    return hresult & 0xFFFF;
  }
  // CHECKSTYLE_ON

  private Variant fStaticDispatch;

  private IDispatchProxy fStaticDispatchProxy;

  private final OleAutomation fWindow;

  /**
   * Constructs a browser interface for use with an IE6 'window' automation
   * object.
   */
  public ModuleSpaceIE6(ModuleSpaceHost host, IDispatch scriptFrameWindow) {
    super(host);

    fWindow = SwtOleGlue.wrapIDispatch(scriptFrameWindow);
  }

  public void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    // Execute the function definition within the browser, which will define
    // a new top-level function.
    //
    String newScript = createNativeMethodInjector(jsniSignature, paramNames, js);
    try {
      // TODO: somehow insert file/line info into the script
      Variant result = execute(fWindow, newScript);
      if (result != null) {
        result.dispose();
      }
    } catch (RuntimeException e) {
      throw new RuntimeException(file + "(" + line
          + "): Failed to create JSNI method with signature '" + jsniSignature
          + "'", e);
    }
  }

  public void dispose() {
    /*
     * Dispose the static dispatcher. This should be simple and straightforward,
     * but isn't, because IE (especially 7) appears to over-Release() the static
     * dispatcher on unload (less often when shutting down, but occasionally
     * then as well). Because this occurs *after* the window unload event, we
     * intentionally use Display.asyncExec() to defer it until after the browser
     * is done cleaning up. We then dispose() the static dispatcher only if it
     * has not already been disposed().
     */
    if (fStaticDispatch != null) {
      final Variant staticDispatchToDispose = fStaticDispatch;
      fStaticDispatch = null;

      Display.getCurrent().asyncExec(new Runnable() {
        public void run() {
          // If the proxy has already been disposed, don't try to do so again,
          // as this will attempt to call through a null vtable.
          if (!fStaticDispatchProxy.isDisposed()) {
            staticDispatchToDispose.dispose();
          }
        }
      });
    }

    // Dispose everything else.
    if (fWindow != null) {
      fWindow.dispose();
    }
    super.dispose();
  }

  public void exceptionCaught(int number, String name, String message) {
    RuntimeException thrown = (RuntimeException) sThrownJavaExceptionObject.get();

    // See if the caught exception matches the thrown exception
    if (thrown != null) {
      HResultException hre = new HResultException(thrown);
      if (CODE(hre.getHResult()) == CODE(number)
          && hre.getMessage().equals(message)) {
        sCaughtJavaExceptionObject.set(thrown);
        sThrownJavaExceptionObject.set(null);
        return;
      }
    }

    sCaughtJavaExceptionObject.set(createJavaScriptException(
        getIsolatedClassLoader(), name, message));
  }

  public boolean invokeNativeBoolean(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return false;
      }
      return result.getBoolean();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public byte invokeNativeByte(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return (byte) result.getInt();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public char invokeNativeChar(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return (char) result.getInt();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public double invokeNativeDouble(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return result.getDouble();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public float invokeNativeFloat(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return result.getFloat();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public Object invokeNativeHandle(String name, Object jthis, Class returnType,
      Class[] types, Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return null;
      }

      // NULL is a legal return value when expecting a handle
      if (result.getType() == COM.VT_NULL) {
        return null;
      }

      return HandleIE6.createHandle(returnType,
          result.getDispatch().getAddress());
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public int invokeNativeInt(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return result.getInt();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public long invokeNativeLong(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return (long) result.getDouble();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public Object invokeNativeObject(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return null;
      } else {
        return SwtOleGlue.convertVariantToObject(Object.class, result,
            "Returning from method '" + name + "'");
      }
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public short invokeNativeShort(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return 0;
      }
      return result.getShort();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public String invokeNativeString(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, true);
      if (result.getType() == COM.VT_EMPTY && isExceptionActive()) {
        return null;
      } else if (result.getType() == COM.VT_NULL) {
        // An explicit null return value.
        //
        return null;
      }
      return result.getString();
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  public void invokeNativeVoid(String name, Object jthis, Class[] types,
      Object[] args) {
    Variant result = null;
    try {
      result = invokeNative(name, jthis, types, args, false);
    } finally {
      if (result != null) {
        result.dispose();
      }
    }
  }

  protected void initializeStaticDispatcher() {
    fStaticDispatchProxy = new IDispatchProxy(getIsolatedClassLoader());
    IDispatch staticDispatch = new IDispatch(fStaticDispatchProxy.getAddress());
    staticDispatch.AddRef();
    fStaticDispatch = new Variant(staticDispatch);

    // Define the static dispatcher for use by JavaScript.
    //
    createNative("initializeStaticDispatcher", 0, "__defineStatic",
        new String[] {"__arg0"}, "window.__static = __arg0;");
    invokeNativeVoid("__defineStatic", null, new Class[] {Variant.class},
        new Object[] {fStaticDispatch});
  }

  private Variant execute(OleAutomation window, String code) {
    int[] dispIds = window.getIDsOfNames(new String[] {"execScript", "code"});
    Variant[] vArgs = new Variant[1];
    vArgs[0] = new Variant(code);
    int[] namedArgs = new int[1];
    namedArgs[0] = dispIds[1];
    Variant result = window.invoke(dispIds[0], vArgs, namedArgs);
    vArgs[0].dispose();
    if (result == null) {
      String lastError = window.getLastError();
      throw new RuntimeException("Error (" + lastError
          + ") executing JavaScript:\n" + code);
    }
    return result;
  }

  /**
   * Invokes a native javascript function.
   * 
   * @param name the name of the function to invoke
   * @param jthis the function's 'this' context
   * @param types the type of each argument
   * @param args the arguments to be passed
   * @return the return value as a Variant.
   */
  private Variant invokeNative(String name, Object jthis, Class[] types,
      Object[] args, boolean returnsValue) {
    // Every time a native method is invoked, release any enqueued COM objects.
    //
    HandleIE6.releaseQueuedPtrs();

    OleAutomation funcObj = null;
    Variant funcObjVar = null;
    Variant[] vArgs = null;
    try {
      // Build the argument list, including 'jthis'.
      //
      int len = args.length;
      vArgs = new Variant[len + 1];
      vArgs[0] = SwtOleGlue.wrapObjectAsVariant(getIsolatedClassLoader(), jthis);

      for (int i = 0; i < len; ++i) {
        vArgs[i + 1] = SwtOleGlue.convertObjectToVariant(
            getIsolatedClassLoader(), types[i], args[i]);
      }

      // Get the function object and its 'call' method.
      //
      int[] ids = fWindow.getIDsOfNames(new String[] {name});
      if (ids == null) {
        throw new RuntimeException(
            "Could not find a native method with the signature '" + name + "'");
      }
      int functionId = ids[0];
      funcObjVar = fWindow.getProperty(functionId);
      funcObj = funcObjVar.getAutomation();
      int callDispId = funcObj.getIDsOfNames(new String[] {"call"})[0];

      // Invoke it and return the result.
      //
      Variant result = funcObj.invoke(callDispId, vArgs);
      if (!isExceptionActive()) {
        if (returnsValue) {
          if (result.getType() == OLE.VT_EMPTY) {
            // Function was required to return something.
            //
            throw new RuntimeException(
                "JavaScript method '"
                    + name
                    + "' returned 'undefined'. This can happen either because of a "
                    + "missing return statement, or explicitly returning a value "
                    + "of 'undefined' (e.g. 'return element[nonexistent property]')");
          }
        } else {
          if (result.getType() != OLE.VT_EMPTY) {
            // Function is not allowed to return something.
            //
            getLogger().log(
                TreeLogger.WARN,
                "JavaScript method '" + name
                    + "' is not supposed to return a value", null);
          }
        }
        return result;
      }
      result.dispose();

      /*
       * The stack trace on the stored exception will not be very useful due to
       * how it was created. Using fillInStackTrace() resets the stack trace to
       * this moment in time, which is usually far more useful.
       */
      RuntimeException thrown = takeJavaException();
      thrown.fillInStackTrace();
      throw thrown;

    } finally {
      // We allocated variants for all arguments, so we must dispose them all.
      //
      for (int i = 0; i < vArgs.length; ++i) {
        if (vArgs[i] != null) {
          vArgs[i].dispose();
        }
      }

      if (funcObjVar != null) {
        funcObjVar.dispose();
      }

      if (funcObj != null) {
        funcObj.dispose();
      }
    }
  }
}
