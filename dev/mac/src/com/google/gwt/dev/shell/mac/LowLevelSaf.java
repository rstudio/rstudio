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

import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

/**
 * Various low-level helper methods for dealing with Safari.
 */
public class LowLevelSaf {

  /**
   * Provides interface for methods to be exposed on javascript side.
   */
  public interface DispatchMethod {
    int invoke(int execState, int jsthis, int[] jsargs);
  }

  /**
   * Provides interface for objects to be exposed on javascript side.
   */
  public interface DispatchObject {
    int getField(String name);

    Object getTarget();

    void setField(String name, int value);
  }

  private static boolean sInitialized = false;

  private static ThreadLocal stateStack = new ThreadLocal();

  public static boolean coerceToBoolean(int execState, int jsval) {
    boolean[] rval = new boolean[1];
    if (!_coerceToBoolean(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to boolean value.");
    }
    return rval[0];
  }

  public static byte coerceToByte(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to byte value");
    }
    return (byte) rval[0];
  }

  public static char coerceToChar(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to char value");
    }
    return (char) rval[0];
  }

  public static double coerceToDouble(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to double value");
    }
    return rval[0];
  }

  public static float coerceToFloat(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to double value");
    }
    return (float) rval[0];
  }

  public static int coerceToInt(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to int value");
    }
    return (int) rval[0];
  }

  public static long coerceToLong(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to long value");
    }
    return (long) rval[0];
  }

  public static short coerceToShort(int execState, int jsval) {
    double[] rval = new double[1];
    if (!_coerceToDouble(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to short value");
    }
    return (short) rval[0];
  }

  public static String coerceToString(int execState, int jsval) {
    String[] rval = new String[1];
    if (!_coerceToString(execState, jsval, rval)) {
      throw new RuntimeException("Failed to coerce to String value");
    }
    return rval[0];
  }

  public static int convertBoolean(boolean v) {
    int[] rval = new int[1];
    if (!_convertBoolean(v, rval)) {
      throw new RuntimeException("Failed to convert Boolean value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertDouble(double v) {
    int[] rval = new int[1];
    if (!_convertDouble(v, rval)) {
      throw new RuntimeException("Failed to convert Double value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  public static int convertString(String v) {
    int[] rval = new int[1];
    if (!_convertString(v, rval)) {
      throw new RuntimeException("Failed to convert String value: "
          + String.valueOf(v));
    }
    return rval[0];
  }

  /**
   * Executes JavaScript code.
   * 
   * @param execState An opaque handle to the script frame window
   * @param code The JavaScript code to execute
   */
  public static void executeScript(int execState, String code) {
    if (!_executeScript(execState, code)) {
      throw new RuntimeException("Failed to execute script: " + code);
    }
  }

  /**
   * Executes JavaScript code, retaining file and line information.
   * 
   * @param execState An opaque handle to the script frame window
   * @param code The JavaScript code to execute
   * @param file A file name associated with the code
   * @param line A line number associated with the code.
   */
  public static void executeScriptWithInfo(int execState, String code,
      String file, int line) {
    if (!_executeScriptWithInfo(execState, code, file, line)) {
      throw new RuntimeException(file + "(" + line
          + "): Failed to execute script: " + code);
    }
  }

  public static void gcLock(int jsval) {
    _gcLock(jsval);
  }

  public static void gcUnlock(int jsval) {
    _gcUnlock(jsval);
  }

  public static int getExecState() {
    Stack stack = (Stack) stateStack.get();
    if (stack == null) {
      throw new RuntimeException("No thread local execState stack!");
    }
    Integer top = (Integer) stack.peek();
    return top.intValue();
  }

  public static int getGlobalExecState(int scriptObject) {
    int[] rval = new int[1];
    if (!_getGlobalExecState(scriptObject, rval)) {
      throw new RuntimeException("Failed to getGlobalExecState.");
    }
    return rval[0];
  }

  public static String[] getProcessArgs() {
    int argc = _getArgc();
    String[] result = new String[argc];
    for (int i = 0; i < argc; ++i) {
      result[i] = _getArgv(i);
    }
    return result;
  }

  public static native String getTypeString(int jsval);
  
  public static synchronized void init() {
    // Force LowLevel initialization to load gwt-ll
    LowLevel.init();
    String libName = "gwt-webkit";
    if (!sInitialized) {
      try {
        String installPath = Utility.getInstallPath();
        try {
          // try to make absolute
          installPath = new File(installPath).getCanonicalPath();
        } catch (IOException e) {
          // ignore problems, failures will occur when the libs try to load
        }

        System.load(installPath + '/' + System.mapLibraryName(libName));
        if (!_initNative(DispatchObject.class, DispatchMethod.class)) {
          throw new RuntimeException("Unable to initialize " + libName);
        }
      } catch (UnsatisfiedLinkError e) {
        StringBuffer sb = new StringBuffer();
        sb.append("Unable to load required native library '" + libName + "'");
        sb.append("\n\tYour GWT installation may be corrupt");
        System.err.println(sb.toString());
        throw new UnsatisfiedLinkError(sb.toString());
      }
      sInitialized = true;
    }
  }

  /**
   * Invokes a method implemented in JavaScript.
   * 
   * @param execState an opaque handle to the script frame window
   * @param methodName the method name on jsthis to call
   * @param jsthis a wrapped java object as a jsval
   * @param jsargs the arguments to pass to the method
   * @return the result of the invocation
   */
  public static int invoke(int execState, int scriptObject, String methodName,
      int jsthis, int[] jsargs) {
    int[] rval = new int[1];
    if (!_invoke(execState, scriptObject, methodName, jsthis, jsargs.length,
        jsargs, rval)) {
      throw new RuntimeException("Failed to invoke native method: "
          + methodName + " with " + jsargs.length + " arguments.");
    }
    return rval[0];
  }

  /**
   * @param jsval the js value in question
   * @return <code>true</code> if the value is a boolean value
   */
  public static native boolean isBoolean(int jsval);

  /**
   * @param jsval the js value in question
   * @return <code>true</code> if the value is the null value
   */
  public static native boolean isNull(int jsval);

  /**
   * @param jsval the js value in question
   * @return <code>true</code> if the value is a boolean value
   */
  public static native boolean isNumber(int jsval);

  /**
   * Is the jsval a JSObject?
   * 
   * @param jsval the value
   * @return true if jsval is a JSObject
   */
  public static boolean isObject(int jsval) {
    return _isObject(jsval);
  }

  /**
   * Is the jsval a string primitive?
   * 
   * @param jsval the value
   * @return true if the jsval is a string primitive
   */
  public static boolean isString(int jsval) {
    return _isString(jsval);
  }

  /**
   * @param jsval the js value in question
   * @return <code>true</code> if the value is the undefined value
   */
  public static native boolean isUndefined(int jsval);

  /**
   * Is the jsval JSObject a wrapped DispatchObject?
   * 
   * @param jsval the value
   * @return true if the JSObject is a wrapped DispatchObject
   */
  public static boolean isWrappedDispatch(int jsval) {
    boolean[] rval = new boolean[1];
    if (!_isWrappedDispatch(jsval, rval)) {
      throw new RuntimeException("Failed isWrappedDispatch.");
    }
    return rval[0];
  }

  /**
   * Locks the JavaScript interpreter into this thread; prevents the garbage
   * collector from running. DON'T CALL THIS THREAD WITHOUT PUTTING A CALL TO
   * JSUNLOCK INSIDE OF A FINALLY BLOCK OR YOU WILL LOCK THE BROWSER.
   */
  public static void jsLock() {
    _jsLock();
  }

  /**
   * @return the null value
   */
  public static native int jsNull();

  /**
   * @return the undefined value
   */
  public static native int jsUndefined();

  /**
   * Unlocks the JavaScript interpreter. Call this method from a finally block
   * whenever you call jsLock.
   */
  public static void jsUnlock() {
    _jsUnlock();
  }

  public static void popExecState(int execState) {
    Stack stack = (Stack) stateStack.get();
    if (stack == null) {
      throw new RuntimeException("No thread local execState stack!");
    }
    Integer old = (Integer) stack.pop();
    if (old.intValue() != execState) {
      throw new RuntimeException("The wrong execState was popped.");
    }
  }

  public static void pushExecState(int execState) {
    Stack stack = (Stack) stateStack.get();
    if (stack == null) {
      stack = new Stack();
      stateStack.set(stack);
    }
    stack.push(new Integer(execState));
  }

  /**
   * Call this to raise an exception in JavaScript before returning control.
   * 
   * @param execState An opaque handle to the script frame window
   */
  public static void raiseJavaScriptException(int execState, int jsval) {
    if (!_raiseJavaScriptException(execState, jsval)) {
      throw new RuntimeException(
          "Failed to raise Java Exception into JavaScript.");
    }
  }

  /**
   * Unwraps a wrapped DispatchObject.
   * 
   * @param jsval a value previously returned from wrapDispatch
   * @return the original DispatchObject
   */
  public static DispatchObject unwrapDispatch(int jsval) {
    DispatchObject[] rval = new DispatchObject[1];
    if (!_unwrapDispatch(jsval, rval)) {
      throw new RuntimeException("Failed to unwrapDispatch.");
    }
    return rval[0];
  }

  /**
   * @param dispObj the DispatchObject to wrap
   * @return the wrapped object as a jsval JSObject
   */
  public static int wrapDispatch(DispatchObject dispObj) {
    int[] rval = new int[1];
    if (!_wrapDispatch(dispObj, rval)) {
      throw new RuntimeException("Failed to wrapDispatch.");
    }
    return rval[0];
  }

  /**
   * @param name method name.
   * @param dispMeth the DispatchMethod to wrap
   * @return the wrapped method as a jsval JSObject
   */
  public static int wrapFunction(String name, DispatchMethod dispMeth) {
    int[] rval = new int[1];
    if (!_wrapFunction(name, dispMeth, rval)) {
      throw new RuntimeException("Failed to wrapFunction.");
    }
    return rval[0];
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

  // CHECKSTYLE_NAMING_OFF
  private static native boolean _coerceToBoolean(int execState, int jsval,
      boolean[] rval);

  private static native boolean _coerceToDouble(int execState, int jsval,
      double[] rval);

  private static native boolean _coerceToString(int execState, int jsval,
      String[] rval);

  private static native boolean _convertBoolean(boolean v, int[] rval);

  private static native boolean _convertDouble(double v, int[] rval);

  private static native boolean _convertString(String v, int[] rval);

  private static native boolean _executeScript(int execState, String code);

  private static native boolean _executeScriptWithInfo(int execState,
      String newScript, String file, int line);

  private static native void _gcLock(int jsval);

  private static native void _gcUnlock(int jsval);

  private static native int _getArgc();

  private static native String _getArgv(int i);

  private static native boolean _getGlobalExecState(int scriptObject, int[] rval);

  private static native boolean _initNative(Class dispObjClass,
      Class dispMethClass);

  private static native boolean _invoke(int execState, int scriptObject,
      String methodName, int jsthis, int jsargCount, int[] jsargs, int[] rval);

  private static native boolean _isObject(int jsval);

  private static native boolean _isString(int jsval);

  private static native boolean _isWrappedDispatch(int jsval, boolean[] rval);

  private static native void _jsLock();

  private static native void _jsUnlock();

  private static native boolean _raiseJavaScriptException(int execState,
      int jsval);

  private static native boolean _unwrapDispatch(int jsval, DispatchObject[] rval);

  private static native boolean _wrapDispatch(DispatchObject dispObj, int[] rval);

  private static native boolean _wrapFunction(String name,
      DispatchMethod dispMeth, int[] rval);
  // CHECKSTYLE_NAMING_OFF
  
  /**
   * Not instantiable.
   */
  private LowLevelSaf() {
  }

}
