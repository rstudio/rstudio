/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchMethod;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Mozilla JavaScript value.
 * 
 * TODO(jat): 64-bit compatibility - currently underlying pointers are passed
 * around in a Java int, which only works on standard 32-bit platforms where
 * sizeof(void*)=4
 */
public class JsValueMoz extends JsValue {

  /**
   * Records debug information, only used when {@link JsValueMoz#debugFlag} is
   * <code>true</code>.
   */
  private static class DebugLogging {
    private Map<Integer, Throwable> alreadyCleanedJsRootedValues = Collections.synchronizedMap(new HashMap<Integer, Throwable>());
    private int maxActive = 0;
    private int numActive = 0;
    private Map<Integer, Throwable> seenJsRootedValues = Collections.synchronizedMap(new HashMap<Integer, Throwable>());
    private int totAlloc = 0;

    /**
     * Count a JsValueMoz instance being created.
     * 
     * Verify that the underlying JsRootedValue is not currently active, and
     * mark that it is active.
     * 
     * This is debug code that is only executed if debugFlag is true. Since this
     * is a private static final field, the compiler should optimize out all
     * this code. It is useful to have for tracking down problems, so it is
     * being left in but disabled.
     */
    public void createInstance(int jsRootedValue) {
      Integer jsrv = new Integer(jsRootedValue);
      if (seenJsRootedValues.containsKey(jsrv)) {
        Throwable t = seenJsRootedValues.get(jsrv);
        String msg = hexString(jsRootedValue);
        System.err.println(msg + ", original caller stacktrace:");
        t.printStackTrace();
        throw new RuntimeException(msg);
      }
      Throwable t = new Throwable();
      seenJsRootedValues.put(jsrv, t);
      if (alreadyCleanedJsRootedValues.containsKey(jsrv)) {
        alreadyCleanedJsRootedValues.remove(jsrv);
      }
      if (++numActive > maxActive) {
        maxActive = numActive;
      }
      ++totAlloc;
    }

    /**
     * Count a JsValueMoz instance being destroyed.
     * 
     * Verify that this instance hasn't already been destroyed, that it has
     * previously been created, and that the underlying JsRootedValue is only
     * being cleaned once.
     * 
     * This is debug code that is only executed if debugFlag is true. Since this
     * is a private static final field, the compiler should optimize out all
     * this code. It is useful to have for tracking down problems, so it is
     * being left in but disabled.
     */
    public void destroyInstance(int jsRootedValue) {
      if (jsRootedValue == 0) {
        throw new RuntimeException("Cleaning already-cleaned JsValueMoz");
      }
      Integer jsrv = new Integer(jsRootedValue);
      if (!seenJsRootedValues.containsKey(jsrv)) {
        throw new RuntimeException("cleaning up 0x" + hexString(jsRootedValue)
            + ", not active");
      }
      if (alreadyCleanedJsRootedValues.containsKey(jsrv)) {
        Throwable t = seenJsRootedValues.get(jsrv);
        String msg = "Already cleaned 0x" + hexString(jsRootedValue);
        System.err.println(msg + ", original allocator stacktrace:");
        t.printStackTrace();
        throw new RuntimeException(msg);
      }
      Throwable t = new Throwable();
      alreadyCleanedJsRootedValues.put(jsrv, t);
      seenJsRootedValues.remove(jsrv);
      --numActive;
    }

    /**
     * Print collected statistics on JsValueMoz usage.
     */
    public void dumpStatistics() {
      System.gc();
      System.out.println("JsValueMoz usage:");
      System.out.println(" " + totAlloc + " total instances created");
      System.out.println(" " + maxActive + " at any one time");
      System.out.println(" " + seenJsRootedValues.size() + " uncleaned entries");
    }
  }

  private static class JsCleanupMoz implements JsCleanup {
    private final int jsRootedValue;

    public JsCleanupMoz(int jsRootedValue) {
      this.jsRootedValue = jsRootedValue;
    }

    public void doCleanup() {
      _destroyJsRootedValue(jsRootedValue);
    }
  }

  /**
   * Flag to enable debug checks on underlying JsRootedValues.
   */
  private static final boolean debugFlag = false;

  /**
   * Flag to enable debug checks on underlying JsRootedValues.
   */
  private static final DebugLogging debugInfo = debugFlag ? new DebugLogging()
      : null;

  /**
   * This must match the value from jsapi.h.
   */
  private static final int JSVAL_VOID = 0x80000001;

  // CHECKSTYLE_NAMING_OFF -- native methods start with '_'
  protected static native boolean _getBoolean(int jsRootedValue);

  protected static native int _getInt(int jsRootedValue);

  protected static native double _getNumber(int jsRootedValue);

  protected static native String _getString(int jsRootedValue);

  protected static native String _getTypeString(int jsRootedValue);

  protected static native DispatchObject _getWrappedJavaObject(int jsRootedValue);

  protected static native boolean _isBoolean(int jsRootedValue);

  protected static native boolean _isInt(int jsRootedValue);

  protected static native boolean _isJavaScriptObject(int jsRootedValue);

  protected static native boolean _isJavaScriptString(int jsRootedValue);

  protected static native boolean _isNull(int jsRootedValue);

  protected static native boolean _isNumber(int jsRootedValue);

  protected static native boolean _isString(int jsRootedValue);

  protected static native boolean _isUndefined(int jsRootedValue);

  protected static native boolean _isWrappedJavaObject(int jsRootedValue);

  protected static native void _setBoolean(int jsRootedValue, boolean val);

  protected static native void _setDouble(int jsRootedValue, double val);

  protected static native void _setInt(int jsRootedValue, int val);

  protected static native void _setJsRootedValue(int jsRootedValue,
      int jsOtherRootedValue);

  protected static native void _setNull(int jsRootedValue);

  protected static native void _setString(int jsRootedValue, String val);

  protected static native void _setUndefined(int jsRootedValue);

  protected static native void _setWrappedFunction(int jsRootedValue,
      String methodName, DispatchMethod dispatchMethod);

  protected static native void _setWrappedJavaObject(int jsRootedValue,
      DispatchObject val);

  private static native int _copyJsRootedValue(int jsRootedValue);

  /**
   * Create a JsRootedValue and return a pointer to it as a Java int.
   * 
   * @param jsval JavaScript jsval for initial value
   * @return pointer to JsRootedValue object as an integer
   */
  private static native int _createJsRootedValue(int jsval);

  /**
   * Destroy a JsRootedValue.
   * 
   * @param jsRootedValue pointer to underlying JsRootedValue as an integer.
   */
  private static native void _destroyJsRootedValue(int jsRootedValue);

  // CHECKSTYLE_NAMING_ON

  /**
   * Convert an address to a hex string.
   * 
   * @param jsRootedValue underlying JavaScript value as an opaque integer
   * @return a string with the JavaScript value represented as hex
   */
  private static String hexString(int jsRootedValue) {
    long l = jsRootedValue;
    l = l & 0xffffffffL;
    return Long.toHexString(l);
  }

  // pointer to underlying JsRootedValue object as an integer
  private int jsRootedValue;

  /**
   * Create a JsValueMoz object representing the undefined value.
   */
  public JsValueMoz() {
    this.jsRootedValue = _createJsRootedValue(JSVAL_VOID);
    if (debugFlag) {
      debugInfo.createInstance(jsRootedValue);
    }
  }

  /**
   * Create a JsValueMoz object wrapping a JsRootedValue object given the
   * pointer to it as an integer.
   * 
   * @param jsRootedValue pointer to underlying JsRootedValue as an integer.
   */
  public JsValueMoz(int jsRootedValue) {
    this.jsRootedValue = jsRootedValue;
    if (debugFlag) {
      debugInfo.createInstance(jsRootedValue);
    }
  }

  /**
   * Copy constructor.
   * 
   * @param other JsValueMoz instance to copy
   */
  public JsValueMoz(JsValueMoz other) {
    jsRootedValue = _copyJsRootedValue(other.jsRootedValue);
    if (debugFlag) {
      debugInfo.createInstance(jsRootedValue);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getBoolean()
   */
  @Override
  public boolean getBoolean() {
    return _getBoolean(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getInt()
   */
  @Override
  public int getInt() {
    return _getInt(jsRootedValue);
  }

  /**
   * Returns the underlying JavaScript object pointer as an integer.
   */
  public int getJsRootedValue() {
    return jsRootedValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getNumber()
   */
  @Override
  public double getNumber() {
    return _getNumber(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getString()
   */
  @Override
  public String getString() {
    return _getString(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getTypeString()
   */
  @Override
  public String getTypeString() {
    return _getTypeString(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#getWrappedJavaObject()
   */
  @Override
  public Object getWrappedJavaObject() {
    DispatchObject obj = _getWrappedJavaObject(jsRootedValue);
    return obj.getTarget();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isBoolean()
   */
  @Override
  public boolean isBoolean() {
    return _isBoolean(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isInt()
   */
  @Override
  public boolean isInt() {
    return _isInt(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isJavaScriptObject()
   */
  @Override
  public boolean isJavaScriptObject() {
    return _isJavaScriptObject(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isNull()
   */
  @Override
  public boolean isNull() {
    return _isNull(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isNumber()
   */
  @Override
  public boolean isNumber() {
    return _isNumber(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isString()
   */
  @Override
  public boolean isString() {
    // String objects are acceptable for String value returns
    return _isString(jsRootedValue) || _isJavaScriptString(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isUndefined()
   */
  @Override
  public boolean isUndefined() {
    return _isUndefined(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#isWrappedJavaObject()
   */
  @Override
  public boolean isWrappedJavaObject() {
    return _isWrappedJavaObject(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setBoolean(boolean)
   */
  @Override
  public void setBoolean(boolean val) {
    _setBoolean(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setByte(byte)
   * 
   * TODO(jat): remove this method
   */
  @Override
  public void setByte(byte val) {
    _setInt(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setChar(char)
   * 
   * TODO(jat): remove this method
   */
  @Override
  public void setChar(char val) {
    _setInt(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setDouble(double)
   */
  @Override
  public void setDouble(double val) {
    _setDouble(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setInt(int)
   */
  @Override
  public void setInt(int val) {
    _setInt(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setNull()
   */
  @Override
  public void setNull() {
    _setNull(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setShort(short)
   * 
   * TODO(jat): remove this method
   */
  @Override
  public void setShort(short val) {
    _setInt(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setString(java.lang.String)
   */
  @Override
  public void setString(String val) {
    _setString(jsRootedValue, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setUndefined()
   */
  @Override
  public void setUndefined() {
    _setUndefined(jsRootedValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setValue(com.google.gwt.dev.shell.JsValue)
   */
  @Override
  public void setValue(JsValue other) {
    _setJsRootedValue(jsRootedValue, ((JsValueMoz) other).jsRootedValue);
  }

  /**
   * Wrap a function call to a Java method in this JavaScript value.
   * 
   * @param methodName the name of the method to invoke
   * @param dispatchMethod the wrapper object
   */
  public void setWrappedFunction(String methodName,
      DispatchMethod dispatchMethod) {
    _setWrappedFunction(jsRootedValue, methodName, dispatchMethod);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.JsValue#setWrappedJavaObject(com.google.gwt.dev.shell.CompilingClassLoader,
   *      java.lang.Object)
   */
  @Override
  public void setWrappedJavaObject(CompilingClassLoader cl, Object val) {
    if (val == null) {
      setNull();
      return;
    }
    DispatchObject dispObj;
    if (val instanceof DispatchObject) {
      dispObj = (DispatchObject) val;
    } else {
      dispObj = new GeckoDispatchAdapter(cl, val);
    }
    _setWrappedJavaObject(jsRootedValue, dispObj);
  }

  /**
   * Create a cleanup object that will free the underlying JsRootedValue object.
   */
  @Override
  protected JsCleanup createCleanupObject() {
    JsCleanup cleanup = new JsCleanupMoz(jsRootedValue);
    if (debugFlag) {
      debugInfo.destroyInstance(jsRootedValue);
      jsRootedValue = 0;
    }
    return cleanup;
  }

}
