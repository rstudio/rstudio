// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Various low-level helper methods for dealing with Gecko.
 */
public class LowLevelMoz {

  /**
   * Provides interface for methods to be exposed
   * on javascript side.
   */
  public interface DispatchMethod {
    int invoke(int jsthis, int[] jsargs);
  }

  /**
   * Provides interface for objects to be exposed
   * on javascript side.
   */
  public interface DispatchObject {
    int getField(String name);

    Object getTarget();

    void setField(String name, int value);
  }

  interface ExternalFactory {
    ExternalObject createExternalObject();

    boolean matchesDOMWindow(int domWindow);
  }

  /**
   * TODO: rip this whole thing out if possible and use DispatchObject like on
   * Safari.
   */
  interface ExternalObject {
    boolean gwtOnLoad(int scriptGlobalObject, String moduleName);

    /**
     * TODO: rip this out.
     */
    int resolveReference(String ident);
  }

  public static final int JSVAL_NULL = 0;
  public static final int JSVAL_VOID = 0x80000001;
  private static final int JSVAL_OBJECT = 0;
  private static final int JSVAL_STRING = 4;
  private static final int JSVAL_TAGMASK = 0x7;

  private static Vector sExternalFactories = new Vector();
  private static boolean sInitialized = false;

  public static boolean coerceToBoolean(int scriptObject, int jsval) {
    boolean[] rval = new boolean[1];
    if (!_coerceToBoolean(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to boolean value.");
    }
    return rval[0];
  }

  public static byte coerceToByte(int scriptObject, int jsval) {
    int[] rval = new int[1];
    if (!_coerceTo31Bits(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to byte value");
    }
    return (byte) rval[0];
  }

  public static char coerceToChar(int scriptObject, int jsval) {
    int[] rval = new int[1];
    if (!_coerceTo31Bits(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to char value");
    }
    return (char) rval[0];
  }

  public static double coerceToDouble(int scriptObject, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to double value");
    }
    return rval[0];
  }

  public static float coerceToFloat(int scriptObject, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to double value");
    }
    return (float) rval[0];
  }

  public static int coerceToInt(int scriptObject, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to int value");
    }
    return (int) rval[0];
  }

  public static long coerceToLong(int scriptObject, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to long value");
    }
    return (long) rval[0];
  }

  public static short coerceToShort(int scriptObject, int jsval) {
    int[] rval = new int[1];
    if (!_coerceTo31Bits(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to short value");
    }
    return (short) rval[0];
  }

  public static String coerceToString(int scriptObject, int jsval) {
    String[] rval = new String[1];
    if (!_coerceToString(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to String value");
    }
    return rval[0];
  }

  public static int convertBoolean(int scriptObject, boolean v) {
    int[] rval = new int[1];
    if (!_convertBoolean(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Boolean value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertByte(int scriptObject, byte v) {
    int[] rval = new int[1];
    if (!_convert31Bits(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Byte value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertChar(int scriptObject, char v) {
    int[] rval = new int[1];
    if (!_convert31Bits(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Char value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertDouble(int scriptObject, double v) {
    int[] rval = new int[1];
    if (!_convertDouble(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Double value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertFloat(int scriptObject, float v) {
    int[] rval = new int[1];
    if (!_convertDouble(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Float value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertInt(int scriptObject, int v) {
    int[] rval = new int[1];
    if (!_convertDouble(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Int value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertLong(int scriptObject, long v) {
    int[] rval = new int[1];
    if (!_convertDouble(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Long value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertShort(int scriptObject, short v) {
    int[] rval = new int[1];
    if (!_convert31Bits(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert Short value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertString(int scriptObject, String v) {
    int[] rval = new int[1];
    if (!_convertString(scriptObject, v, rval)) {
      throw new RuntimeException("Failed to convert String value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  /**
   * Executes JavaScript code.
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param code The JavaScript code to execute
   */
  public static void executeScript(int scriptObject, String code) {
    if (!_executeScript(scriptObject, code)) {
      throw new RuntimeException("Failed to execute script: " + code);
    }
  }

  /**
   * Executes JavaScript code, retaining file and line information.
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param code The JavaScript code to execute
   * @param file A file name associated with the code
   * @param line A line number associated with the code.
   */
  public static void executeScriptWithInfo(int scriptObject, String code,
      String file, int line) {
    if (!_executeScriptWithInfo(scriptObject, code, file, line)) {
      throw new RuntimeException(file + "(" + line
          + "): Failed to execute script: " + code);
    }
  }

  public static String getMozillaDirectory() {
    String installPath = Utility.getInstallPath();
    try {
      // try to make absolute
      installPath = new File(installPath).getCanonicalPath();
    } catch (IOException e) {
      // ignore problems, failures will occur when the libs try to load
    }
    return installPath + "/mozilla-1.7.12";
  }

  public static synchronized void init() {
    // Force LowLevel initialization to load gwt-ll
    LowLevel.init();
    if (!sInitialized) {
      if (!_registerExternalFactoryHandler()) {
        throw new RuntimeException(
            "Failed to register external factory handler.");
      }
      sInitialized = true;
    }
  }

  /**
   * Invokes a method implemented in JavaScript.
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param methodName the method name on jsthis to call
   * @param jsthis A wrapped java object as a jsval
   * @param jsargs the arguments to pass to the method
   * @return the result of the invocation
   */
  public static int invoke(int scriptObject, String methodName, int jsthis,
      int[] jsargs) {
    int[] rval = new int[1];
    if (!_invoke(scriptObject, methodName, jsthis, jsargs.length, jsargs, rval)) {
      throw new RuntimeException("Failed to invoke native method: "
          + methodName + " with " + jsargs.length + " arguments.");
    }
    return rval[0];
  }

  /**
   * Is the jsval a JSObject?
   * 
   * @param jsval the value
   * @return true if jsval is a JSObject
   */
  public static boolean isJSObject(int jsval) {
    return (jsval & JSVAL_TAGMASK) == JSVAL_OBJECT;
  }

  /**
   * Is the jsval a string primitive?
   * 
   * @param jsval the value
   * @return true if the jsval is a string primitive
   */
  public static boolean isString(int jsval) {
    return (jsval & JSVAL_TAGMASK) == JSVAL_STRING;
  }

  /**
   * Is the jsval JSObject a wrapped DispatchObject?
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param jsval the value
   * @return true if the JSObject is a wrapped DispatchObject
   */
  public static boolean isWrappedDispatch(int scriptObject, int jsval) {
    boolean[] rval = new boolean[1];
    if (!_isWrappedDispatch(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed isWrappedDispatch.");
    }
    return rval[0];
  }

  /**
   * Call this to raise an exception in JavaScript before returning control.
   * 
   * @param scriptObject An opaque handle to the script frame window
   */
  public static void raiseJavaScriptException(int scriptObject, int jsval) {
    if (!_raiseJavaScriptException(scriptObject, jsval)) {
      throw new RuntimeException(
          "Failed to raise Java Exception into JavaScript.");
    }
  }

  /**
   * BrowserWindows register here so that if their contained window gets a call
   * to window.external, the call can be routed correctly by nsIDOMWindow
   * pointer.
   * 
   * @param externalFactory the factory to register
   */
  public static void registerExternalFactory(ExternalFactory externalFactory) {
    synchronized (sExternalFactories) {
      sExternalFactories.add(externalFactory);
    }
  }

  /**
   * Unregisters an existing registration.
   * 
   * @param externalFactory the factory to unregister
   */
  public static void unregisterExternalFactory(ExternalFactory externalFactory) {
    synchronized (sExternalFactories) {
      sExternalFactories.remove(externalFactory);
    }
  }

  /**
   * Unwraps a wrapped DispatchObject.
   * 
   * @param scriptObject An opaque handle to the script frame window
   * @param jsval a value previously returned from wrapDispatch
   * @return the original DispatchObject
   */
  public static DispatchObject unwrapDispatch(int scriptObject, int jsval) {
    DispatchObject[] rval = new DispatchObject[1];
    if (!_unwrapDispatch(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to unwrapDispatch.");
    }
    return rval[0];
  }

  /**
   * Unwraps a wrapped JSObject.
   * 
   * @param nsISupports a value previously returned from wrapJSObject
   * @return the original jsval JSObject
   */
  public static int unwrapJSObject(int nsISupports) {
    int[] rval = new int[1];
    if (!_unwrapJSObject(nsISupports, rval)) {
      throw new RuntimeException("Failed to unwrapJSObject.");
    }
    return rval[0];
  }

  /**
   * @param scriptObject An opaque handle to the script frame window
   * @param dispObj the DispatchObject to wrap
   * @return the wrapped object as a jsval JSObject
   */
  public static int wrapDispatch(int scriptObject, DispatchObject dispObj) {
    int[] rval = new int[1];
    if (!_wrapDispatch(scriptObject, dispObj, rval)) {
      throw new RuntimeException("Failed to wrapDispatch.");
    }
    return rval[0];
  }

  /**
   * @param scriptObject An opaque handle to the script frame window
   * @param dispObj the DispatchMethod to wrap
   * @return the wrapped method as a jsval JSObject
   */
  public static int wrapFunction(int scriptObject, String name,
      DispatchMethod dispMeth) {
    int[] rval = new int[1];
    if (!_wrapFunction(scriptObject, name, dispMeth, rval)) {
      throw new RuntimeException("Failed to wrapFunction.");
    }
    return rval[0];
  }

  /**
   * Creates an nsISupports interface locking the contained JSObject jsval, so
   * that we can lock/free it correctly via reference counting.
   * 
   * @param scriptObject the global script window
   * @param jsval the JSObject to wrap
   * @return an nsISupports wrapper object
   */
  public static int wrapJSObject(int scriptObject, int jsval) {
    int[] rval = new int[1];
    if (!_wrapJSObject(scriptObject, jsval, rval)) {
      throw new RuntimeException("Failed to createJSObjectHolder.");
    }
    return rval[0];
  }

  /**
   * Called from native code to create an external object for a particular
   * window.
   * 
   * @param domWindow an nsIDOMWindow to check against our ExternalFactories map
   * @return a new ExternalObject
   */
  protected static ExternalObject createExternalObjectForDOMWindow(int domWindow) {
    for (Iterator iter = sExternalFactories.iterator(); iter.hasNext();) {
      ExternalFactory fac = (ExternalFactory) iter.next();
      if (fac.matchesDOMWindow(domWindow)) {
        return fac.createExternalObject();
      }
    }
    return null;
  }

  /**
   * Called from native code to do tracing.
   * 
   * @param s the string to trace
   */
  protected static void trace(String s) {
    System.out.println(s);
    System.out.flush();
  }

  private static native boolean _coerceTo31Bits(int scriptObject, int jsval,
      int[] rval);

  private static native boolean _coerceToBoolean(int scriptObject, int jsval,
      boolean[] rval);

  private static native boolean _coerceToDouble(int scriptObject, int jsval,
      double[] rval);

  private static native boolean _coerceToString(int scriptObject, int jsval,
      String[] rval);

  private static native boolean _convert31Bits(int scriptObject, int v,
      int[] rval);

  private static native boolean _convertBoolean(int scriptObject, boolean v,
      int[] rval);

  private static native boolean _convertDouble(int scriptObject, double v,
      int[] rval);

  private static native boolean _convertString(int scriptObject, String v,
      int[] rval);

  private static native boolean _executeScript(int scriptObject, String code);

  private static native boolean _executeScriptWithInfo(int scriptObject,
      String newScript, String file, int line);

  private static native boolean _invoke(int scriptObject, String methodName,
      int jsthis, int jsargCount, int[] jsargs, int[] rval);

  private static native boolean _isWrappedDispatch(int scriptObject, int jsval,
      boolean[] rval);

  private static native boolean _raiseJavaScriptException(int scriptObject,
      int jsval);

  private static native boolean _registerExternalFactoryHandler();

  private static native boolean _unwrapDispatch(int scriptObject, int jsval,
      DispatchObject[] rval);

  private static native boolean _unwrapJSObject(int nsISupports, int[] rval);

  private static native boolean _wrapDispatch(int scriptObject,
      DispatchObject dispObj, int[] rval);

  private static native boolean _wrapFunction(int scriptObject, String name,
      DispatchMethod dispMeth, int[] rval);

  private static native boolean _wrapJSObject(int scriptObject, int jsval,
      int[] rval);

  /**
   * Not instantiable.
   */
  private LowLevelMoz() {
  }

}
