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
import com.google.gwt.dev.shell.JavaDispatch;
import com.google.gwt.dev.shell.JavaDispatchImpl;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchMethod;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wraps an arbitrary Java Object as a Dispatch component. The class was
 * motivated by the need to expose Java objects into JavaScript.
 * 
 * An instance of this class with no target is used to globally access all
 * static methods or fields.
 */
class GeckoDispatchAdapter implements DispatchObject {

  private final CompilingClassLoader classLoader;

  private final JavaDispatch javaDispatch;

  /**
   * This constructor initializes as the static dispatcher, which handles only
   * static method calls and field references.
   * 
   * @param cl this class's classLoader
   */
  GeckoDispatchAdapter(CompilingClassLoader cl) {
    javaDispatch = new JavaDispatchImpl(cl);
    classLoader = cl;
  }

  /**
   * This constructor initializes a dispatcher, around a particular instance.
   * 
   * @param cl this class's classLoader
   * @param target the object being wrapped as an IDispatch
   */
  GeckoDispatchAdapter(CompilingClassLoader cl, Object target) {
    javaDispatch = new JavaDispatchImpl(cl, target);
    classLoader = cl;
  }

  /**
   * Retrieve a field and store in the passed JsValue. This function is called
   * exclusively from native code.
   * 
   * @param name name of the field to retrieve
   * @param jsValue a reference to the JsValue object to receive the value of
   *          the field
   */
  public void getField(String name, int jsRootedValue) {
    JsValueMoz jsValue = new JsValueMoz(jsRootedValue);
    // TODO(jat): factor out remaining code into platform-independent code
    int dispId = classLoader.getDispId(name);
    if (dispId < 0) {
      // no field by that name, return undefined
      jsValue.setUndefined();
      return;
    }
    if (javaDispatch.isField(dispId)) {
      Field field = javaDispatch.getField(dispId);
      JsValueGlue.set(jsValue, classLoader, field.getType(),
          javaDispatch.getFieldValue(dispId));
      return;
    } else {
      Method method = javaDispatch.getMethod(dispId);
      DispatchMethod dispMethod = (DispatchMethod) classLoader.getMethodDispatch(method);
      if (dispMethod == null) {
        dispMethod = new MethodDispatch(classLoader, method);
        classLoader.putMethodDispatch(method, dispMethod);
      }
      jsValue.setWrappedFunction(method.toString(), dispMethod);
    }
  }

  public Object getTarget() {
    return javaDispatch.getTarget();
  }

  public void setField(String name, int jsRootedValue) {
    JsValue jsValue = new JsValueMoz(jsRootedValue);
    int dispId = classLoader.getDispId(name);
    if (dispId < 0) {
      // no field by that name
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
}
