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
import com.google.gwt.dev.shell.JavaDispatch;
import com.google.gwt.dev.shell.JavaDispatchImpl;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.MethodAdaptor;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchMethod;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

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

  public int getField(int jsContext, String name) {
    LowLevelSaf.pushJsContext(jsContext);
    try {
      int dispId = classLoader.getDispId(name);
      if (dispId < 0) {
        return LowLevelSaf.getJsUndefined(jsContext);
      }
      if (javaDispatch.isField(dispId)) {
        Field field = javaDispatch.getField(dispId);
        JsValueSaf jsValue = new JsValueSaf();
        JsValueGlue.set(jsValue, classLoader, field.getType(),
            javaDispatch.getFieldValue(dispId));
        int jsval = jsValue.getJsValue();
        // Native code will eat an extra ref.
        LowLevelSaf.gcProtect(jsContext, jsval);
        return jsval;
      } else {
        MethodAdaptor method = javaDispatch.getMethod(dispId);
        AccessibleObject obj = method.getUnderlyingObject();
        DispatchMethod dispMethod = (DispatchMethod) classLoader.getWrapperForObject(obj);
        if (dispMethod == null) {
          dispMethod = new MethodDispatch(classLoader, method);
          classLoader.putWrapperForObject(obj, dispMethod);
        }
        // Native code eats the same ref it gave us.
        return LowLevelSaf.wrapDispatchMethod(jsContext, method.toString(),
            dispMethod);
      }
    } finally {
      LowLevelSaf.popJsContext(jsContext);
    }
  }

  public Object getTarget() {
    return javaDispatch.getTarget();
  }

  public void setField(int jsContext, String name, int value) {
    LowLevelSaf.pushJsContext(jsContext);
    try {
      JsValue jsValue = new JsValueSaf(value);
      int dispId = classLoader.getDispId(name);
      if (dispId < 0) {
        // TODO (knorton): We could allow expandos, but should we?
        throw new RuntimeException("No such field " + name);
      }
      if (javaDispatch.isMethod(dispId)) {
        throw new RuntimeException("Cannot reassign method " + name);
      }
      Field field = javaDispatch.getField(dispId);
      Object val = JsValueGlue.get(jsValue, classLoader, field.getType(),
          "setField");
      javaDispatch.setFieldValue(dispId, val);
    } finally {
      LowLevelSaf.popJsContext(jsContext);
    }
  }

  @Override
  public String toString() {
    return getTarget().toString();
  }

}
