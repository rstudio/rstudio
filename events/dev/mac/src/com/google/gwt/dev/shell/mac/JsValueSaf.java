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

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

/**
 * Represents a Safari JavaScript value.
 * 
 * The basic rule is that any JSValue passed to Java code from native code will
 * always be GC-protected in the native code and Java will always unprotect it
 * when the value is finalized. It should always be stored in a JsValue object
 * immediately to make sure it is cleaned up properly when it is no longer
 * needed. This approach is required to avoid a race condition where the value
 * is allocated in JNI code but could be garbage collected before Java takes
 * ownership of the value. Java values passed into JavaScript store a GlobalRef
 * of a WebKitDispatchAdapter or MethodDispatch objects, which are freed when
 * the JS value is finalized.
 */
public class JsValueSaf extends JsValue {

  private static class JsCleanupSaf implements JsCleanup {
    private final int jsval;

    /**
     * Create a cleanup object which takes care of cleaning up the underlying JS
     * object.
     * 
     * @param jsval JSValue pointer as an integer
     */
    public JsCleanupSaf(int jsval) {
      this.jsval = jsval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.google.gwt.dev.shell.JsValue.JsCleanup#doCleanup()
     */
    public void doCleanup() {
      LowLevelSaf.gcUnprotect(LowLevelSaf.getCurrentJsContext(), jsval);
    }
  }

  /*
   * Underlying JSValue* as an integer.
   */
  private int jsval;

  /**
   * Create a Java wrapper around an undefined JSValue.
   */
  public JsValueSaf() {
    init(LowLevelSaf.getJsUndefined(LowLevelSaf.getCurrentJsContext()));
  }

  /**
   * Create a Java wrapper around the underlying JSValue.
   * 
   * @param jsval a pointer to the underlying JSValue object as an integer
   */
  public JsValueSaf(int jsval) {
    init(jsval);
  }

  @Override
  public boolean getBoolean() {
    int curJsContext = LowLevelSaf.getCurrentJsContext();
    return LowLevelSaf.toBoolean(curJsContext, jsval);
  }

  @Override
  public int getInt() {
    int currentJsContext = LowLevelSaf.getCurrentJsContext();
    return LowLevelSaf.toInt(currentJsContext, jsval);
  }

  @Override
  public int getJavaScriptObjectPointer() {
    assert isJavaScriptObject();
    return jsval;
  }

  public int getJsValue() {
    return jsval;
  }

  @Override
  public double getNumber() {
    int currentJsContext = LowLevelSaf.getCurrentJsContext();
    return LowLevelSaf.toDouble(currentJsContext, jsval);
  }

  @Override
  public String getString() {
    final int currentJsContext = LowLevelSaf.getCurrentJsContext();
    return LowLevelSaf.toString(currentJsContext, jsval);
  }

  @Override
  public String getTypeString() {
    return LowLevelSaf.getTypeString(LowLevelSaf.getCurrentJsContext(), jsval);
  }

  @Override
  public Object getWrappedJavaObject() {
    DispatchObject obj = LowLevelSaf.unwrapDispatchObject(
        LowLevelSaf.getCurrentJsContext(), jsval);
    return obj.getTarget();
  }

  @Override
  public boolean isBoolean() {
    return LowLevelSaf.isJsBoolean(LowLevelSaf.getCurrentJsContext(), jsval);
  }

  @Override
  public boolean isInt() {
    // Safari doesn't have integers, so this is always false
    return false;
  }

  @Override
  public boolean isJavaScriptObject() {
    final int currentJsContext = LowLevelSaf.getCurrentJsContext();
    return LowLevelSaf.isJsObject(currentJsContext, jsval)
        && !LowLevelSaf.isDispatchObject(currentJsContext, jsval);
  }

  @Override
  public boolean isNull() {
    return LowLevelSaf.isJsNull(LowLevelSaf.getCurrentJsContext(), jsval);
  }

  @Override
  public boolean isNumber() {
    return LowLevelSaf.isJsNumber(LowLevelSaf.getCurrentJsContext(), jsval);
  }

  @Override
  public boolean isString() {
    return LowLevelSaf.isJsString(LowLevelSaf.getCurrentJsContext(), jsval);
  }

  @Override
  public boolean isUndefined() {
    return LowLevelSaf.isJsUndefined(LowLevelSaf.getCurrentJsContext(), jsval);
  }

  @Override
  public boolean isWrappedJavaObject() {
    return LowLevelSaf.isDispatchObject(LowLevelSaf.getCurrentJsContext(),
        jsval);
  }

  @Override
  public void setBoolean(boolean val) {
    setJsVal(LowLevelSaf.toJsBoolean(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setByte(byte val) {
    setJsVal(LowLevelSaf.toJsNumber(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setChar(char val) {
    setJsVal(LowLevelSaf.toJsNumber(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setDouble(double val) {
    setJsVal(LowLevelSaf.toJsNumber(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setInt(int val) {
    setJsVal(LowLevelSaf.toJsNumber(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setNull() {
    setJsVal(LowLevelSaf.getJsNull(LowLevelSaf.getCurrentJsContext()));
  }

  @Override
  public void setShort(short val) {
    setJsVal(LowLevelSaf.toJsNumber(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setString(String val) {
    setJsVal(LowLevelSaf.toJsString(LowLevelSaf.getCurrentJsContext(), val));
  }

  @Override
  public void setUndefined() {
    setJsVal(LowLevelSaf.getJsUndefined(LowLevelSaf.getCurrentJsContext()));
  }

  @Override
  public void setValue(JsValue other) {
    int jsvalOther = ((JsValueSaf) other).jsval;
    /*
     * Add another lock to this jsval, since both the other object and this one
     * will eventually release it.
     */
    LowLevelSaf.gcProtect(LowLevelSaf.getCurrentJsContext(), jsvalOther);
    setJsVal(jsvalOther);
  }

  @Override
  public <T> void setWrappedJavaObject(CompilingClassLoader cl, T val) {
    DispatchObject dispObj;
    if (val == null) {
      setNull();
      return;
    } else if (val instanceof DispatchObject) {
      dispObj = (DispatchObject) val;
    } else {
      dispObj = (DispatchObject) cl.getWrapperForObject(val);
      if (dispObj == null) {
        dispObj = new WebKitDispatchAdapter(cl, val);
        cl.putWrapperForObject(val, dispObj);
      }
    }
    setJsVal(LowLevelSaf.wrapDispatchObject(LowLevelSaf.getCurrentJsContext(),
        dispObj));
  }

  @Override
  protected JsCleanup createCleanupObject() {
    return new JsCleanupSaf(jsval);
  }

  /**
   * Initialization helper method.
   * 
   * @param jsval underlying JSValue*
   */
  private void init(int jsval) {
    this.jsval = jsval;

    // If protection checking is enabled, we check to see if the value we are
    // accepting is protected as it should be.
    if (LowLevelSaf.isJsValueProtectionCheckingEnabled()
        && !LowLevelSaf.isGcProtected(jsval)) {
      throw new RuntimeException("Cannot accepted unprotected JSValue ("
          + Integer.toHexString(jsval) + ", "
          + LowLevelSaf.getTypeString(LowLevelSaf.getCurrentJsContext(), jsval)
          + ")");
    }
  }

  /**
   * Set a new value. Unlock the previous value, but do *not* lock the new value
   * (see class comment).
   * 
   * @param jsval the new value to set
   */
  private void setJsVal(int jsval) {
    LowLevelSaf.gcUnprotect(LowLevelSaf.getCurrentJsContext(), this.jsval);
    init(jsval);
  }

}
