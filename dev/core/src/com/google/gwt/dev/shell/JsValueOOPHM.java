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
package com.google.gwt.dev.shell;

import com.google.gwt.dev.shell.BrowserChannel.JsObjectRef;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Represents a JavaScript value in OOPHM.
 */
public class JsValueOOPHM extends JsValue {
  /**
   * OOPHM implementation of the DispatchObject interface.
   */
  static class DispatchObjectOOPHM implements DispatchObject {

    private final CompilingClassLoader classLoader;

    private final JavaDispatchImpl javaDispatch;

    public DispatchObjectOOPHM(CompilingClassLoader ccl) {
      javaDispatch = new JavaDispatchImpl(ccl);
      classLoader = ccl;
    }

    public DispatchObjectOOPHM(CompilingClassLoader ccl, Object val) {
      javaDispatch = new JavaDispatchImpl(ccl, val);
      classLoader = ccl;
    }

    public JsValue getField(int dispId) {
      JsValueOOPHM jsValue = new JsValueOOPHM();
      if (javaDispatch.isField(dispId)) {
        Field field = javaDispatch.getField(dispId);
        JsValueGlue.set(jsValue, classLoader, field.getType(),
            javaDispatch.getFieldValue(dispId));
      } else {
        MethodAdaptor method = javaDispatch.getMethod(dispId);
        AccessibleObject obj = method.getUnderlyingObject();
        DispatchMethod dispMethod = (DispatchMethod) classLoader.getWrapperForObject(obj);
        if (dispMethod == null) {
          dispMethod = new MethodDispatch(classLoader, method);
          classLoader.putWrapperForObject(obj, dispMethod);
        }
        jsValue.setWrappedFunction(method.toString(), dispMethod);
      }
      return jsValue;
    }

    public JsValue getField(String name) {
      int dispId = getFieldId(name);
      if (dispId < 0) {
        // no field by that name, return undefined
        return new JsValueOOPHM();
      }
      return getField(dispId);
    }

    public int getFieldId(String name) {
      return classLoader.getDispId(name);
    }

    public Object getTarget() {
      return javaDispatch.getTarget();
    }

    public void setField(int dispId, JsValue jsValue) {
      if (javaDispatch.isMethod(dispId)) {
        throw new RuntimeException("Cannot reassign method "
            + javaDispatch.getMethod(dispId).getName());
      }
      Field field = javaDispatch.getField(dispId);
      Object val = JsValueGlue.get(jsValue, classLoader, field.getType(),
          "setField");
      javaDispatch.setFieldValue(dispId, val);
    }

    public void setField(String name, JsValue jsValue) {
      int dispId = getFieldId(name);
      if (dispId < 0) {
        // no field by that name, and we do not support expands on Java objects
        throw new RuntimeException("No such field " + name);
      }
      setField(dispId, jsValue);
    }
  }

  /**
   * Class used to identify a JavaScript undefined value.
   */
  private static class UndefinedValue {
  }

  public static final String JSE_CLASS = "com.google.gwt.core.client.JavaScriptException";

  private static final ThreadLocal<Map<Object, DispatchObject>> dispatchObjectCache = new ThreadLocal<Map<Object, DispatchObject>>();

  private static final UndefinedValue undefValue = new UndefinedValue();

  /**
   * Underlying value.
   * 
   * This may be one of:
   * 
   * <pre>
   *   - Boolean instance
   *   - Integer instance
   *   - Double instance
   *   - String instance
   *   - null
   *   - undefValue
   * </pre>
   */
  private Object value;

  /**
   * Create a JsValueMoz object representing the undefined value.
   */
  public JsValueOOPHM() {
    this.value = undefValue;
  }

  /**
   * Create a JsValueOOPHM object wrapping a JS object given the object
   * reference id.
   * 
   * @param jsRefId pointer to underlying JsRootedValue as an integer.
   */
  public JsValueOOPHM(int jsRefId) {
    this.value = new JsObjectRef(jsRefId);
  }

  /**
   * Copy constructor.
   * 
   * @param other JsValueMoz instance to copy
   */
  public JsValueOOPHM(JsValueOOPHM other) {
    value = other.value;
  }

  @Override
  public boolean getBoolean() {
    return (Boolean) value;
  }

  @Override
  public int getInt() {
    return (Integer) value;
  }

  @Override
  public DispatchObject getJavaObjectWrapper() {
    return (DispatchObject) value;
  }

  /**
   * @return the value as a JsObjectRef.
   * 
   * Fails if isJavascriptObject() is false.
   */
  public JsObjectRef getJavascriptObject() {
    return (JsObjectRef) value;
  }

  @Override
  public int getJavaScriptObjectPointer() {
    assert isJavaScriptObject();
    return ((JsObjectRef) value).getRefid();
  }

  @Override
  public double getNumber() {
    return ((Number) value).doubleValue();
  }

  @Override
  public String getString() {
    return (String) value;
  }

  @Override
  public String getTypeString() {
    if (isBoolean()) {
      return "boolean";
    } else if (isInt()) {
      return "int";
    } else if (isJavaScriptObject()) {
      JsObjectRef objRef = (JsObjectRef) value;
      return "JavaScript object(" + objRef.getRefid() + ")";
    } else if (isNull()) {
      return "null";
    } else if (isNumber()) {
      return "number";
    } else if (isString()) {
      return "string";
    } else if (isUndefined()) {
      return "undefined";
    } else if (isWrappedJavaFunction()) {
      return "Java Method";
    } else if (isWrappedJavaObject()) {
      return "Java Object " + value.getClass().getName();
    }
    return "unexpected value type";
  }

  @Override
  public DispatchMethod getWrappedJavaFunction() {
    return (DispatchMethod) value;
  }

  @Override
  public Object getWrappedJavaObject() {
    return ((DispatchObject) value).getTarget();
  }

  @Override
  public boolean isBoolean() {
    return value instanceof Boolean;
  }

  @Override
  public boolean isInt() {
    return value instanceof Integer;
  }

  @Override
  public boolean isJavaScriptObject() {
    return value instanceof JsObjectRef;
  }

  @Override
  public boolean isNull() {
    return value == null;
  }

  @Override
  public boolean isNumber() {
    return value instanceof Number;
  }

  @Override
  public boolean isString() {
    return value instanceof String;
  }

  @Override
  public boolean isUndefined() {
    return value == undefValue;
  }

  @Override
  public boolean isWrappedJavaFunction() {
    return value instanceof DispatchMethod;
  }

  @Override
  public boolean isWrappedJavaObject() {
    return value instanceof DispatchObject;
  }

  @Override
  public void setBoolean(boolean val) {
    value = Boolean.valueOf(val);
  }

  /*
   * TODO(jat): remove this method
   */
  @Override
  public void setByte(byte val) {
    value = Integer.valueOf(val);
  }

  /*
   * TODO(jat): remove this method
   */
  @Override
  public void setChar(char val) {
    value = Integer.valueOf(val);
  }

  @Override
  public void setDouble(double val) {
    value = Double.valueOf(val);
  }

  @Override
  public void setInt(int val) {
    value = Integer.valueOf(val);
  }

  public void setJavascriptObject(JsObjectRef jsObject) {
    value = jsObject;
  }

  @Override
  public void setNull() {
    value = null;
  }

  /*
   * TODO(jat): remove this method
   */
  @Override
  public void setShort(short val) {
    value = Integer.valueOf(val);
  }

  @Override
  public void setString(String val) {
    value = val;
  }

  @Override
  public void setUndefined() {
    value = undefValue;
  }

  @Override
  public void setValue(JsValue other) {
    value = ((JsValueOOPHM) other).value;
  }

  /**
   * Wrap a function call to a Java method in this JavaScript value.
   * 
   * @param methodName the name of the method to invoke
   * @param dispatchMethod the wrapper object
   */
  public void setWrappedFunction(String methodName,
      DispatchMethod dispatchMethod) {
    value = dispatchMethod;
  }

  @Override
  public <T> void setWrappedJavaObject(CompilingClassLoader cl, T val) {
    if (val == null) {
      setNull();
      return;
    }
    if (val instanceof DispatchObject) {
      value = val;
    } else {
      Map<Object, DispatchObject> cache = dispatchObjectCache.get();
      if (cache == null) {
        cache = new IdentityHashMap<Object, DispatchObject>();
        dispatchObjectCache.set(cache);
      }
      DispatchObject dispObj = cache.get(val);
      if (dispObj == null) {
        dispObj = new DispatchObjectOOPHM(cl, val);
        cache.put(val, dispObj);
      }
      value = dispObj;
    }
  }
}
