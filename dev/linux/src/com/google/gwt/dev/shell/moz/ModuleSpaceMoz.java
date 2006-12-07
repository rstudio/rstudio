// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchObject;

/**
 * An implementation of {@link com.google.gwt.dev.shell.ModuleSpace} for
 * Mozilla.
 */
public class ModuleSpaceMoz extends ModuleSpace {

  /**
   * Constructs a browser interface for use with a Mozilla global window object.
   */
  public ModuleSpaceMoz(ModuleSpaceHost host, int scriptGlobalObject) {
    super(host);

    // Hang on to the parent window.
    //
    window = scriptGlobalObject;
    SwtGeckoGlue.addRefInt(window);
  }

  public void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    // Execute the function definition within the browser, which will define
    // a new top-level function.
    //
    String newScript = createNativeMethodInjector(jsniSignature, paramNames, js);
    LowLevelMoz.executeScriptWithInfo(window, newScript, file, line);
  }

  public void dispose() {
    SwtGeckoGlue.releaseInt(window);
    super.dispose();
  }

  public void exceptionCaught(int number, String name, String message) {
    RuntimeException thrown = (RuntimeException) sThrownJavaExceptionObject
      .get();

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
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return false;
    }
    return LowLevelMoz.coerceToBoolean(window, jsval);
  }

  public byte invokeNativeByte(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToByte(window, jsval);
  }

  public char invokeNativeChar(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToChar(window, jsval);
  }

  public double invokeNativeDouble(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToDouble(window, jsval);
  }

  public float invokeNativeFloat(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToFloat(window, jsval);
  }

  public Object invokeNativeHandle(String name, Object jthis, Class returnType,
      Class[] types, Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return null;
    }
    return SwtGeckoGlue.convertJSValToObject(window, returnType, jsval);
  }

  public int invokeNativeInt(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToInt(window, jsval);
  }

  public long invokeNativeLong(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToLong(window, jsval);
  }

  public Object invokeNativeObject(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return null;
    }
    return SwtGeckoGlue.convertJSValToObject(window, Object.class, jsval);
  }

  public short invokeNativeShort(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return 0;
    }
    return LowLevelMoz.coerceToShort(window, jsval);
  }

  public String invokeNativeString(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsval = invokeNative(name, jthis, types, args);
    if (jsval == LowLevelMoz.JSVAL_VOID && isExceptionActive()) {
      return null;
    }
    return LowLevelMoz.coerceToString(window, jsval);
  }

  public void invokeNativeVoid(String name, Object jthis, Class[] types,
      Object[] args) {
    invokeNative(name, jthis, types, args);
  }

  protected void initializeStaticDispatcher() {
    staticDispatch = new GeckoDispatchAdapter(getIsolatedClassLoader(),
      window);

    // Define the static dispatcher for use by JavaScript.
    //
    createNative("initializeStaticDispatcher", 0, "__defineStatic",
      new String[]{"__arg0"}, "window.__static = __arg0;");
    invokeNativeVoid("__defineStatic", null, new Class[]{Object.class},
      new Object[]{staticDispatch});
  }

  int wrapObjectAsJSObject(Object o) {
    return SwtGeckoGlue.wrapObjectAsJSObject(getIsolatedClassLoader(), window,
      o);
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
    HandleMoz.releaseQueuedPtrs();

    int jsthis = wrapObjectAsJSObject(jthis);

    int argc = args.length;
    int argv[] = new int[argc];
    for (int i = 0; i < argc; ++i) {
      argv[i] = SwtGeckoGlue.convertObjectToJSVal(window,
        getIsolatedClassLoader(), types[i], args[i]);
    }

    int result = LowLevelMoz.invoke(window, name, jsthis, argv);
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

  private DispatchObject staticDispatch;

  private final int window;

}
