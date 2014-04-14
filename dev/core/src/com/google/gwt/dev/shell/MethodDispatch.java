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

import com.google.gwt.dev.shell.JsValue.DispatchMethod;

import java.lang.reflect.InvocationTargetException;

/**
 * Wraps an arbitrary Java Method as a Dispatchable component. The class was
 * motivated by the need to expose Java objects into JavaScript.
 */
class MethodDispatch implements DispatchMethod {

  private final CompilingClassLoader classLoader;

  private final MethodAdaptor method;

  public MethodDispatch(CompilingClassLoader classLoader, MethodAdaptor method) {
    this.classLoader = classLoader;
    this.method = method;
  }

  /**
   * Invoke a Java method from JavaScript. This is called solely from native
   * code.
   *
   * @param jsthis JavaScript reference to Java object
   * @param jsargs array of JavaScript values for parameters
   * @param returnValue JavaScript value to return result in
   * @return <code>true</code> if an exception was thrown
   * @throws RuntimeException if improper arguments are supplied
   */
  @Override
  public boolean invoke(JsValue jsthis, JsValue[] jsargs, JsValue returnValue) {
    Class<?>[] paramTypes = method.getParameterTypes();
    int argc = paramTypes.length;
    Object args[] = new Object[argc];
    // too many arguments are ok: the extra will be silently ignored
    if (jsargs.length < argc) {
      throw new RuntimeException("Not enough arguments to " + method);
    }
    Object jthis = null;
    if (method.needsThis()) {
      jthis = JsValueGlue.get(jsthis, classLoader, method.getDeclaringClass(),
          "invoke this");
      if (jthis == null) {
        throw ModuleSpace.createJavaScriptException(classLoader,
            "Invoking an instance method on a null instance");
      }
    }
    for (int i = 0; i < argc; ++i) {
      args[i] = JsValueGlue.get(jsargs[i], classLoader, paramTypes[i],
          "invoke arguments");
    }
    try {
      Object result;
      try {
        result = method.invoke(jthis, args);
      } catch (IllegalAccessException e) {
        // should never, ever happen
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      JsValueGlue.set(returnValue, classLoader, method.getReturnType(), result);
      return false;
    } catch (InstantiationException e) {
      // If we get here, it means an exception is being thrown from
      // Java back into JavaScript
      wrapException(returnValue, e.getCause());
      return true;

    } catch (InvocationTargetException e) {
      // If we get here, it means an exception is being thrown from
      // Java back into JavaScript
      wrapException(returnValue, e.getTargetException());
      return true;

    } catch (IllegalArgumentException e) {
      // TODO(jat): log to treelogger instead? If so, how do I get to it?
      System.err.println("MethodDispatch.invoke, method=" + method.toString()
          + ": argument mismatch");
      for (int i = 0; i < argc; ++i) {
        System.err.println(" param " + i + " type is "
            + paramTypes[i].toString() + " value is type "
            + jsargs[i].getTypeString() + " = " + args[i].toString());
      }
      throw e;
    }
  }

  @Override
  public String toString() {
    return method.toString();
  }

  /**
   * Send an exception back to the client. This will either wrap a Java
   * Throwable as a Java Object to be sent over the wire, or if the exception is
   * a JavaScriptException unwinding through the stack, send the original thrown
   * object instead.
   */
  private void wrapException(JsValue returnValue, Throwable t) {
    ModuleSpace.setThrownJavaException(t);

    Object thrown = ModuleSpace.getThrownObject(classLoader, t);
    Class<?> type = thrown == null ? Object.class : thrown.getClass();
    JsValueGlue.set(returnValue, classLoader, type, thrown);
  }
}
