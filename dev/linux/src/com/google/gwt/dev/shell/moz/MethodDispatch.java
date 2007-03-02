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
package com.google.gwt.dev.shell.moz;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.moz.LowLevelMoz.DispatchMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Wraps an arbitrary Java Method as a Dispatchable component. The class was
 * motivated by the need to expose Java objects into JavaScript.
 */
class MethodDispatch implements DispatchMethod {

  private final CompilingClassLoader classLoader;

  private final Method method;

  public MethodDispatch(CompilingClassLoader classLoader, Method method) {
    this.classLoader = classLoader;
    this.method = method;
  }

  /**
   * Invoke a Java method from JavaScript.
   * This is called solely from native code.
   * 
   * @param jscontext JSContext* passed as an integer
   * @param jsthis JavaScript reference to Java object
   * @param jsargs array of JavaScript values for parameters
   * @param returnValue JavaScript value to return result in
   * @throws RuntimeException if improper arguments are supplied
   * 
   * TODO(jat): lift most of this interface to platform-independent code (only
   *     exceptions still need to be made platform-independent)
   */
  public void invoke(int jscontext, int jsthisInt, int[] jsargsInt,
      int returnValueInt) {
    JsValue jsthis = new JsValueMoz(jsthisInt);
    JsValue jsargs[] = new JsValue[jsargsInt.length];
    for (int i = 0; i < jsargsInt.length; ++i) {
      jsargs[i] = new JsValueMoz(jsargsInt[i]);
    }
    JsValue returnValue = new JsValueMoz(returnValueInt);
    Class[] paramTypes = method.getParameterTypes();
    int argc = paramTypes.length;
    Object args[] = new Object[argc];
    // too many arguments are ok: the extra will be silently ignored
    if (jsargs.length < argc) {
      throw new RuntimeException("Not enough arguments to " + method);
    }
    Object jthis = null;
    if ((method.getModifiers() & Modifier.STATIC) == 0) {
      jthis = JsValueGlue.get(jsthis, method.getDeclaringClass(), "invoke this");
    }
    for (int i = 0; i < argc; ++i) {
      args[i] = JsValueGlue.get(jsargs[i], paramTypes[i], "invoke arguments");
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
    } catch (InvocationTargetException e) {
      // If we get here, it means an exception is being thrown from
      // Java back into JavaScript
      Throwable t = e.getTargetException();
      RuntimeException re;
      if (t instanceof RuntimeException) {
        re = (RuntimeException) t;
      } else {
        re = new RuntimeException("Checked exception thrown into JavaScript"
            + " (Web Mode behavior may differ)", t);
      }
      // TODO(jat): if this was originally JavaScript exception, re-throw the
      // original exception rather than just a null. 
      ModuleSpaceMoz.setThrownJavaException(re);
      LowLevelMoz.raiseJavaScriptException(jscontext);
    } catch (IllegalArgumentException e) {
      // TODO(jat): log to treelogger instead?  If so, how do I get to it?
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
}