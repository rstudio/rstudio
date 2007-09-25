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
package com.google.gwt.dev.shell.mac;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JavaDispatch;
import com.google.gwt.dev.shell.JavaDispatchImpl;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchMethod;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wraps an arbitrary Java Object as a Dispatch component. The class was
 * motivated by the need to expose Java objects into JavaScript.
 * 
 * An instance of this class with no target is used to globally access all
 * static methods or fields.
 */
class WebKitDispatchAdapter implements DispatchObject {

  private final CompilingClassLoader classLoader;

  private final JavaDispatch javaDispatch;

  /**
   * This constructor initializes as the static dispatcher, which handles only
   * static method calls and field references.
   * 
   * @param cl this class's classLoader
   */
  WebKitDispatchAdapter(CompilingClassLoader cl) {
    javaDispatch = new JavaDispatchImpl(cl);
    this.classLoader = cl;
  }

  /**
   * This constructor initializes a dispatcher, around a particular instance.
   * 
   * @param cl this class's classLoader
   * @param target the object being wrapped as an IDispatch
   */
  WebKitDispatchAdapter(CompilingClassLoader cl, Object target) {
    javaDispatch = new JavaDispatchImpl(cl, target);
    this.classLoader = cl;
  }

  /* (non-Javadoc)
   * @see com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject#getField(java.lang.String)
   */
  public int getField(String name) {
    int dispId = classLoader.getDispId(name);
    if (dispId < 0) {
      return LowLevelSaf.jsUndefined();
    }
    if (javaDispatch.isField(dispId)) {
      Field field = javaDispatch.getField(dispId);
      JsValueSaf jsValue = new JsValueSaf();
      JsValueGlue.set(jsValue, classLoader, field.getType(),
          javaDispatch.getFieldValue(dispId));
      int jsval = jsValue.getJsValue();
      return jsval;
    } else {
      Method method = javaDispatch.getMethod(dispId);
      DispatchMethod dispMethod;
      dispMethod = (DispatchMethod) classLoader.getMethodDispatch(method);
      if (dispMethod == null) {
        dispMethod = new MethodDispatch(classLoader, method);
        classLoader.putMethodDispatch(method, dispMethod);
      }
      return LowLevelSaf.wrapFunction(method.toString(), dispMethod);
    }
  }

  /* (non-Javadoc)
   * @see com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject#getTarget()
   */
  public Object getTarget() {
    return javaDispatch.getTarget();
  }

  /* (non-Javadoc)
   * @see com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject#setField(java.lang.String, int)
   */
  public void setField(String name, int value) {
    JsValue jsValue = new JsValueSaf(value);
    int dispId = classLoader.getDispId(name);
    if (dispId < 0) {
      // TODO: expandos?
      throw new RuntimeException("No such field " + name);
    }
    if (javaDispatch.isMethod(dispId)) {
      throw new RuntimeException("Cannot reassign method " + name);
    }
    Field field = javaDispatch.getField(dispId);
    Object val = JsValueGlue.get(jsValue, field.getType(), "setField");
    javaDispatch.setFieldValue(dispId, val);
  }

  @Override
  public String toString() {
    return getTarget().toString();
  }

}
