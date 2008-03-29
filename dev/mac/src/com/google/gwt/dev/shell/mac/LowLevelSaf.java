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
package com.google.gwt.dev.shell.mac;

import com.google.gwt.dev.shell.LowLevel;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Implements all native / low-level functions for Mac/Safari hosted mode.
 * 
 * TODO (knorton): Consider changing the APIs to not have to take a jsContext;
 * instead the context could always be pulled from top-of-stack in the wrapper
 * functions and passed into the native functions.
 */
public class LowLevelSaf {

  /**
   * Interface by which the native code interacts with a Java Method.
   */
  public interface DispatchMethod {
    int invoke(int jsContext, int jsthis, int[] jsargs, int[] exception);
  }

  /**
   * Interface by which the native code interacts with a Java Object.
   * 
   * TODO (knorton): Add additional argument for an exception array (like in
   * {@link DispatchMethod#invoke(int, int, int[], int[])}). An example of
   * where this would be immediately helpful is in {@link BrowserWidgetSaf}.
   */
  public interface DispatchObject {
    int getField(int jsContext, String name);

    Object getTarget();

    void setField(int jsContext, String name, int value);
  }

  /**
   * Stores a map from DispatchObject/DispatchMethod to the live underlying
   * jsval. This is used to both preserve identity for the same Java Object and
   * also prevent GC.
   */
  static Map<Object, Integer> sObjectToJsval = new IdentityHashMap<Object, Integer>();

  private static boolean initialized = false;

  private static final ThreadLocal<Stack<Integer>> jsContextStack = new ThreadLocal<Stack<Integer>>();

  private static boolean jsValueProtectionCheckingEnabled;

  public static int executeScript(int jsContext, String script) {
    final int[] rval = new int[1];
    if (!executeScriptWithInfoImpl(jsContext, script, null, 0, rval)) {
      throw new RuntimeException("Failed to execute script: " + script);
    }
    return rval[0];
  }

  public static int executeScriptWithInfo(int jsContext, String script,
      String url, int line) {
    final int[] rval = new int[1];
    if (!executeScriptWithInfoImpl(jsContext, script, url, line, rval)) {
      throw new RuntimeException(url + "(" + line
          + "): Failed to execute script: " + script);
    }
    return rval[0];
  }

  public static native void gcProtect(int jsContext, int jsValue);

  public static native void gcUnprotect(int jsContext, int jsValue);

  public static native int getArgc();

  public static native String getArgv(int ix);

  public static int getCurrentJsContext() {
    Stack<Integer> stack = jsContextStack.get();
    if (stack == null) {
      throw new RuntimeException("No JSContext stack on this thread.");
    }
    return stack.peek().intValue();
  }

  public static int getGlobalJsObject(int jsContext) {
    final int rval[] = new int[1];
    if (!getGlobalJsObjectImpl(jsContext, rval)) {
      throw new RuntimeException("Unable to get JavaScript global object.");
    }
    return rval[0];
  }

  public static native int getJsNull(int jsContext);

  public static native int getJsUndefined(int jsContext);

  public static String[] getProcessArgs() {
    int argc = getArgc();
    String[] result = new String[argc];
    for (int i = 0; i < argc; ++i) {
      result[i] = getArgv(i);
    }
    return result;
  }

  public static native String getTypeString(int jsContext, int jsValue);

  public static synchronized void init() {
    if (initialized) {
      return;
    }

    LowLevel.init();
    if (!initImpl(DispatchObject.class, DispatchMethod.class, LowLevelSaf.class)) {
      throw new RuntimeException("Unable to initialize LowLevelSaf");
    }

    jsValueProtectionCheckingEnabled = isJsValueProtectionCheckingEnabledImpl();

    initialized = true;
  }

  public static int invoke(int jsContext, int jsScriptObject,
      String methodName, int thisObj, int[] args) {

    final int[] rval = new int[1];
    if (!invokeImpl(jsContext, jsScriptObject, methodName, thisObj, args,
        args.length, rval)) {
      throw new RuntimeException("Failed to invoke native method: "
          + methodName + " with " + args.length + " arguments.");
    }
    return rval[0];
  }

  public static boolean isDispatchObject(int jsContext, int jsValue) {
    final boolean[] rval = new boolean[1];
    if (!isDispatchObjectImpl(jsContext, jsValue, rval)) {
      throw new RuntimeException("Failed isDispatchObject.");
    }
    return rval[0];
  }

  public static native boolean isJsBoolean(int jsContext, int jsValue);

  public static native boolean isJsNull(int jsContext, int jsValue);

  public static native boolean isJsNumber(int jsContext, int jsValue);

  public static native boolean isJsObject(int jsContext, int jsValue);

  public static boolean isJsString(int jsContext, int jsValue) {
    final boolean rval[] = new boolean[1];
    if (!isJsStringImpl(jsContext, jsValue, rval)) {
      throw new RuntimeException("Failed isJsString.");
    }
    return rval[0];
  }

  public static native boolean isJsUndefined(int jsContext, int jsValue);

  public static void popJsContext(int expectedJsContext) {
    final Stack<Integer> stack = jsContextStack.get();
    if (stack == null) {
      throw new RuntimeException("No JSContext stack on this thread.");
    }
    if (stack.pop().intValue() != expectedJsContext) {
      throw new RuntimeException(
          "Popping JSContext returned an unxpected value.");
    }
  }

  public static void pushJsContext(int jsContext) {
    Stack<Integer> stack = jsContextStack.get();
    if (stack == null) {
      stack = new Stack<Integer>();
      jsContextStack.set(stack);
    }
    stack.push(Integer.valueOf(jsContext));
  }

  public static native void releaseJsGlobalContext(int jsContext);

  public static native void retainJsGlobalContext(int jsContext);

  public static boolean toBoolean(int jsContext, int jsValue) {
    boolean[] rval = new boolean[1];
    if (!toBooleanImpl(jsContext, jsValue, rval)) {
      throw new RuntimeException("Failed to coerce to boolean value.");
    }
    return rval[0];
  }

  public static byte toByte(int jsContext, int jsValue) {
    return (byte) toNumber(jsContext, jsValue, "byte");
  }

  public static char toChar(int jsContext, int jsValue) {
    return (char) toNumber(jsContext, jsValue, "char");
  }

  public static double toDouble(int jsContext, int jsValue) {
    return toNumber(jsContext, jsValue, "double");
  }

  public static float toFloat(int jsContext, int jsValue) {
    return (float) toNumber(jsContext, jsValue, "float");
  }

  public static int toInt(int jsContext, int jsValue) {
    return (int) toNumber(jsContext, jsValue, "int");
  }

  public static int toJsBoolean(int jsContext, boolean value) {
    final int[] rval = new int[1];
    if (!toJsBooleanImpl(jsContext, value, rval)) {
      throw new RuntimeException("Failed to convert Boolean value: "
          + String.valueOf(value));
    }
    return rval[0];
  }

  public static int toJsNumber(int jsContext, double value) {
    final int[] rval = new int[1];
    if (!toJsNumberImpl(jsContext, value, rval)) {
      throw new RuntimeException("Failed to convert Double value: "
          + String.valueOf(value));
    }
    return rval[0];
  }

  public static int toJsString(int jsContext, String value) {
    final int[] rval = new int[1];
    if (!toJsStringImpl(jsContext, value, rval)) {
      throw new RuntimeException("Failed to convert String value: "
          + String.valueOf(value));
    }
    return rval[0];
  }

  public static long toLong(int jsContext, int jsValue) {
    return (long) toNumber(jsContext, jsValue, "long");
  }

  public static short toShort(int jsContext, int jsValue) {
    return (short) toNumber(jsContext, jsValue, "short");
  }

  public static String toString(int jsContext, int jsValue) {
    final String[] rval = new String[1];
    if (!toStringImpl(jsContext, jsValue, rval)) {
      throw new RuntimeException("Failed to coerce to String value");
    }
    return rval[0];
  }

  public static DispatchObject unwrapDispatchObject(int jsContext, int jsValue) {
    final DispatchObject[] rval = new DispatchObject[1];
    if (!unwrapDispatchObjectImpl(jsContext, jsValue, rval)) {
      throw new RuntimeException("Failed to unwrap DispatchObject.");
    }
    return rval[0];
  }

  public static int wrapDispatchMethod(int jsContext, String name,
      DispatchMethod dispatch) {
    Integer cached = LowLevelSaf.sObjectToJsval.get(dispatch);
    if (cached != null) {
      /*
       * Add another lock to the cached jsval, since it will not have any.
       */
      LowLevelSaf.gcProtect(LowLevelSaf.getCurrentJsContext(), cached);
      return cached;
    } else {
      final int[] rval = new int[1];
      if (!wrapDispatchMethodImpl(jsContext, name, dispatch, rval)) {
        throw new RuntimeException("Failed to wrap DispatchMethod.");
      }
      LowLevelSaf.sObjectToJsval.put(dispatch, rval[0]);
      return rval[0];
    }
  }

  public static int wrapDispatchObject(int jsContext, DispatchObject dispatcher) {
    Integer cached = LowLevelSaf.sObjectToJsval.get(dispatcher);
    if (cached != null) {
      /*
       * Add another lock to the cached jsval, since it will not have any.
       */
      LowLevelSaf.gcProtect(LowLevelSaf.getCurrentJsContext(), cached);
      return cached;
    } else {
      final int[] rval = new int[1];
      if (!wrapDispatchObjectImpl(jsContext, dispatcher, rval)) {
        throw new RuntimeException("Failed to wrap DispatchObject.");
      }
      LowLevelSaf.sObjectToJsval.put(dispatcher, rval[0]);
      return rval[0];
    }
  }

  static native boolean isGcProtected(int jsValue);

  /**
   * Enables checking of JSValueRef protect/unprotect calls to ensure calls are
   * properly matched. See ENABLE_JSVALUE_PROTECTION_CHECKING in trace.h to
   * enable this feature.
   * 
   * @return whether JSValue protection checking is enabled
   */
  static boolean isJsValueProtectionCheckingEnabled() {
    return jsValueProtectionCheckingEnabled;
  }

  /**
   * Native code accessor to remove the mapping upon GC.
   */
  static void releaseObject(Object o) {
    sObjectToJsval.remove(o);
  }

  private static native boolean executeScriptWithInfoImpl(int jsContext,
      String script, String url, int line, int[] rval);

  private static native boolean getGlobalJsObjectImpl(int jsContext, int[] rval);

  private static native boolean initImpl(
      Class<DispatchObject> dispatchObjectClass,
      Class<DispatchMethod> dispatchMethodClass,
      Class<LowLevelSaf> lowLevelSafClass);

  private static native boolean invokeImpl(int jsContext, int jsScriptObject,
      String methodName, int thisObj, int[] args, int argsLength, int[] rval);

  private static native boolean isDispatchObjectImpl(int jsContext,
      int jsValue, boolean[] rval);

  private static native boolean isJsStringImpl(int jsContext, int jsValue,
      boolean[] rval);

  private static native boolean isJsValueProtectionCheckingEnabledImpl();

  private static native boolean toBooleanImpl(int jsContext, int jsValue,
      boolean[] rval);

  private static native boolean toDoubleImpl(int jsContext, int jsValue,
      double[] rval);

  private static native boolean toJsBooleanImpl(int jsContext, boolean value,
      int[] rval);

  private static native boolean toJsNumberImpl(int jsContext, double value,
      int[] rval);

  private static native boolean toJsStringImpl(int jsContext, String value,
      int[] rval);

  private static double toNumber(int jsContext, int jsValue, String typeName) {
    double[] rval = new double[1];
    if (!toDoubleImpl(jsContext, jsValue, rval)) {
      throw new RuntimeException("Failed to coerce to " + typeName + " value");
    }
    return rval[0];
  }

  private static native boolean toStringImpl(int jsContext, int jsValue,
      String[] rval);

  private static native boolean unwrapDispatchObjectImpl(int jsContext,
      int jsValue, DispatchObject[] rval);

  private static native boolean wrapDispatchMethodImpl(int jsContext,
      String name, DispatchMethod dispatch, int[] rval);

  private static native boolean wrapDispatchObjectImpl(int jsContext,
      DispatchObject obj, int[] rval);
}
