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

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

/**
 * Represents a Safari JavaScript value.
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
      // TODO(jat): perform cleanup operation
    }
  }

  // pointer to underlying JSValue object as an integer
  private int jsval;

  /**
   * Create a Java wrapper around the underlying JSValue.
   * 
   * @param jsval a pointer to the underlying JSValue object as an integer
   */
  public JsValueSaf(int jsval) {
    this.jsval = jsval;
  }

  public JsValueSaf() {
    this.jsval = LowLevelSaf.jsUndefined();
  }

  public boolean getBoolean() {
    int curExecState = LowLevelSaf.getExecState();
    return LowLevelSaf.coerceToBoolean(curExecState, jsval);
  }

  public int getInt() {
    int curExecState = LowLevelSaf.getExecState();
    return LowLevelSaf.coerceToInt(curExecState, jsval);
 }

  public int getJsValue() {
    return jsval;
  }

  public double getNumber() {
    int curExecState = LowLevelSaf.getExecState();
    return LowLevelSaf.coerceToDouble(curExecState, jsval);
  }

  public String getString() {
    int curExecState = LowLevelSaf.getExecState();
    return LowLevelSaf.coerceToString(curExecState, jsval);
  }

  public String getTypeString() {
    return LowLevelSaf.getTypeString(jsval);
  }

  public Object getWrappedJavaObject() {
    DispatchObject obj = LowLevelSaf.unwrapDispatch(jsval);
    return obj.getTarget();
  }

  public boolean isBoolean() {
    return LowLevelSaf.isBoolean(jsval);
  }

  public boolean isInt() {
    // Safari doesn't have integers, so this is always false
    return false;
  }

  public boolean isJavaScriptObject() {
    return LowLevelSaf.isObject(jsval) && !LowLevelSaf.isWrappedDispatch(jsval);
  }

  public boolean isNull() {
    return LowLevelSaf.isNull(jsval);
  }

  public boolean isNumber() {
    return LowLevelSaf.isNumber(jsval);
  }

  public boolean isObject() {
    return LowLevelSaf.isObject(jsval);
  }

  public boolean isString() {
    return LowLevelSaf.isString(jsval);
  }

  public boolean isUndefined() {
    return LowLevelSaf.isUndefined(jsval);
  }

  public boolean isWrappedJavaObject() {
    return LowLevelSaf.isWrappedDispatch(jsval);
  }

  public void setBoolean(boolean val) {
    jsval = LowLevelSaf.convertBoolean(val);
  }

  public void setByte(byte val) {
    jsval = LowLevelSaf.convertDouble(val);
  }

  public void setChar(char val) {
    jsval = LowLevelSaf.convertDouble(val);
  }

  public void setDouble(double val) {
    jsval = LowLevelSaf.convertDouble(val);
  }

  public void setInt(int val) {
    jsval = LowLevelSaf.convertDouble(val);
  }
  
  public void setJsVal(int jsval) {
    this.jsval = jsval;
  }

  public void setNull() {
    jsval = LowLevelSaf.jsNull();
  }

  public void setShort(short val) {
    jsval = LowLevelSaf.convertDouble(val);
  }

  public void setString(String val) {
    jsval = LowLevelSaf.convertString(val);
  }

  public void setUndefined() {
    jsval = LowLevelSaf.jsUndefined();
  }

  public void setValue(JsValue other) {
    jsval = ((JsValueSaf)other).jsval;
  }

  public void setWrappedJavaObject(CompilingClassLoader cl, Object val) {
    DispatchObject dispObj;
    if (val == null) {
      setNull();
      return;
    } else if (val instanceof DispatchObject) {
      dispObj = (DispatchObject)val;
    } else {
      dispObj = new WebKitDispatchAdapter(cl, val);
    }
    jsval = LowLevelSaf.wrapDispatch(dispObj);
  }

  protected JsCleanup createCleanupObject() {
    return new JsCleanupSaf(jsval);
  }

}
