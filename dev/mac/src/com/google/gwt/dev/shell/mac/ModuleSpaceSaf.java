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
package com.google.gwt.dev.shell.mac;

import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

/**
 * An implementation of {@link com.google.gwt.dev.shell.ModuleSpace} for Safari.
 */
public class ModuleSpaceSaf extends ModuleSpace {

  /**
   * Constructs a browser interface for use with a Mozilla global window object.
   */
  public ModuleSpaceSaf(ModuleSpaceHost host, int scriptGlobalObject) {
    super(host);

    // Hang on to the global execution state.
    //
    this.fWindow = scriptGlobalObject;
    LowLevelSaf.gcLock(scriptGlobalObject);
  }

  public void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    // Execute the function definition within the browser, which will define
    // a new top-level function.
    //
    String newScript = createNativeMethodInjector(jsniSignature, paramNames, js);
    LowLevelSaf.executeScriptWithInfo(LowLevelSaf.getGlobalExecState(fWindow),
        newScript, file, line);
  }

  public void dispose() {
    LowLevelSaf.gcUnlock(fWindow);
    super.dispose();
  }

  public void exceptionCaught(int number, String name, String message) {
    RuntimeException thrown = (RuntimeException) sThrownJavaExceptionObject.get();

    // See if the caught exception is null (thus thrown by us)
    if (thrown != null) {
      if (name == null && message == null) {
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
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return false;
    }
    return LowLevelSaf.coerceToBoolean(LowLevelSaf.getExecState(), jsval);
  }

  public byte invokeNativeByte(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToByte(LowLevelSaf.getExecState(), jsval);
  }

  public char invokeNativeChar(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToChar(LowLevelSaf.getExecState(), jsval);
  }

  public double invokeNativeDouble(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToDouble(LowLevelSaf.getExecState(), jsval);
  }

  public float invokeNativeFloat(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToFloat(LowLevelSaf.getExecState(), jsval);
  }

  public Object invokeNativeHandle(String name, Object jthis, Class returnType,
      Class[] types, Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return null;
    }
    return SwtWebKitGlue.convertJSValToObject(returnType, jsval);
  }

  public int invokeNativeInt(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToInt(LowLevelSaf.getExecState(), jsval);
  }

  public long invokeNativeLong(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToLong(LowLevelSaf.getExecState(), jsval);
  }

  public Object invokeNativeObject(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return null;
    }
    return SwtWebKitGlue.convertJSValToObject(Object.class, jsval);
  }

  public short invokeNativeShort(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return 0;
    }
    return LowLevelSaf.coerceToShort(LowLevelSaf.getExecState(), jsval);
  }

  public String invokeNativeString(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (LowLevelSaf.isUndefined(jsval) && isExceptionActive()) {
      return null;
    }
    return LowLevelSaf.coerceToString(LowLevelSaf.getExecState(), jsval);
  }

  public void invokeNativeVoid(String name, Object jthis, Class[] types,
      Object[] args) {
    invokeNative(name, jthis, types, args);
  }

  protected void initializeStaticDispatcher() {
    fStaticDispatch = new WebKitDispatchAdapter(getIsolatedClassLoader(),
        fWindow);

    // Define the static dispatcher for use by JavaScript.
    //
    createNative("initializeStaticDispatcher", 0, "__defineStatic",
        new String[] {"__arg0"}, "window.__static = __arg0;");
    invokeNativeVoid("__defineStatic", null, new Class[] {Object.class},
        new Object[] {fStaticDispatch});
  }

  int wrapObjectAsJSObject(Object o) {
    return SwtWebKitGlue.wrapObjectAsJSObject(getIsolatedClassLoader(),
        fWindow, o);
  }

  /**
   * Invokes a native javascript function.
   * 
   * @param name the name of the function to invoke
   * @param jthis the function's 'this' context
   * @param types the type of each argument
   * @param args the arguments to be passed
   * @return the return value as a Object.
   */
  private int invokeNative(String name, Object jthis, Class[] types,
      Object[] args) {
    // Every time a native method is invoked, release any enqueued COM objects.
    //
    HandleSaf.releaseQueuedPtrs();

    int jsthis = wrapObjectAsJSObject(jthis);
    int curExecState = LowLevelSaf.getExecState();
    int argc = args.length;
    int argv[] = new int[argc];
    for (int i = 0; i < argc; ++i) {
      argv[i] = SwtWebKitGlue.convertObjectToJSVal(curExecState,
          getIsolatedClassLoader(), types[i], args[i]);
    }

    int result = LowLevelSaf.invoke(curExecState, fWindow, name, jsthis, argv);
    if (!isExceptionActive()) {
      return result;
    }

    /*
     * The stack trace on the stored exception will not be very useful due to
     * how it was created. Using fillInStackTrace() resets the stack trace to
     * this moment in time, which is usually far more useful.
     */
    RuntimeException thrown = takeJavaException();
    thrown.fillInStackTrace();
    throw thrown;
  }

  private DispatchObject fStaticDispatch;

  private final int fWindow;

}
