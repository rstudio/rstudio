// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JavaDispatch;
import com.google.gwt.dev.shell.JavaDispatchImpl;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchMethod;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wraps an arbitrary Java Object as a Dispatchable component. The class was
 * motivated by the need to expose Java objects into JavaScript.
 * 
 * An instance of this class with no target is used to globally access all
 * static methods or fields.
 */
class GeckoDispatchAdapter implements DispatchObject {

  private final CompilingClassLoader classLoader;

  private final JavaDispatch javaDispatch;

  private final int scriptObject;

  /**
   * This constructor initializes as the static dispatcher, which handles only
   * static method calls and field references.
   * 
   * @param cl this class's classLoader
   * @param aScriptObject the execution iframe's window
   */
  GeckoDispatchAdapter(CompilingClassLoader cl, int aScriptObject) {
    javaDispatch = new JavaDispatchImpl(cl);
    classLoader = cl;
    scriptObject = aScriptObject;
  }

  /**
   * This constructor initializes a dispatcher, around a particular instance.
   * 
   * @param cl this class's classLoader
   * @param aScriptObject the execution iframe's window
   * @param target the object being wrapped as an IDispatch
   */
  GeckoDispatchAdapter(CompilingClassLoader cl, int aScriptObject, Object target) {
    javaDispatch = new JavaDispatchImpl(cl, target);
    classLoader = cl;
    scriptObject = aScriptObject;
  }

  public int getField(String name) {
    int dispId = classLoader.getDispId(name);
    if (dispId < 0) {
      return LowLevelMoz.JSVAL_VOID;
    }
    if (javaDispatch.isField(dispId)) {
      Field field = javaDispatch.getField(dispId);
      return SwtGeckoGlue.convertObjectToJSVal(scriptObject, classLoader,
          field.getType(), javaDispatch.getFieldValue(dispId));
    } else {
      Method method = javaDispatch.getMethod(dispId);
      DispatchMethod dispMethod;
      dispMethod = (DispatchMethod) classLoader.getMethodDispatch(method);
      if (dispMethod == null) {
        dispMethod = new MethodDispatch(classLoader, method, scriptObject);
        classLoader.putMethodDispatch(method, dispMethod);
      }
      return LowLevelMoz.wrapFunction(scriptObject, method.toString(),
          dispMethod);
    }
  }

  public Object getTarget() {
    return javaDispatch.getTarget();
  }

  public void setField(String name, int value) {
    int dispId = classLoader.getDispId(name);
    if (dispId < 0) {
      // TODO: expandos?
      throw new RuntimeException("No such field " + name);
    }
    if (javaDispatch.isMethod(dispId)) {
      throw new RuntimeException("Cannot reassign method " + name);
    }
    Field field = javaDispatch.getField(dispId);
    Object val = SwtGeckoGlue.convertJSValToObject(scriptObject,
        field.getType(), value);
    javaDispatch.setFieldValue(dispId, val);
  }
}
