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
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.MethodAdaptor;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchMethod;

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

  public int invoke(int jsContext, int jsthisInt, int[] jsargsInt,
      int[] exception) {
    LowLevelSaf.pushJsContext(jsContext);
    JsValue jsthis = new JsValueSaf(jsthisInt);
    JsValue jsargs[] = new JsValue[jsargsInt.length];
    for (int i = 0; i < jsargsInt.length; ++i) {
      jsargs[i] = new JsValueSaf(jsargsInt[i]);
    }
    JsValueSaf returnValue = new JsValueSaf();
    try {
      Class<?>[] paramTypes = method.getParameterTypes();
      int argc = paramTypes.length;
      Object args[] = new Object[argc];
      // too many arguments are ok: the extra will be silently ignored
      if (jsargs.length < argc) {
        throw new RuntimeException("Not enough arguments to " + method);
      }
      Object jthis = null;
      if (method.needsThis()) {
        jthis = JsValueGlue.get(jsthis, classLoader,
            method.getDeclaringClass(), "invoke this");
      }
      for (int i = 0; i < argc; ++i) {
        args[i] = JsValueGlue.get(jsargs[i], classLoader, paramTypes[i],
            "invoke args");
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
        JsValueGlue.set(returnValue, classLoader, method.getReturnType(),
            result);
        int jsResult = returnValue.getJsValue();
        // Native code will eat an extra ref.
        LowLevelSaf.gcProtect(jsContext, jsResult);
        return jsResult;
      } catch (InstantiationException e) {
        // If we get here, it means an exception is being thrown from
        // Java back into JavaScript
        ModuleSpace.setThrownJavaException(e.getCause());
        // Native code eats the same ref it gave us.
        exception[0] = LowLevelSaf.getJsNull(jsContext);
        // Native code eats the same ref it gave us.
        return LowLevelSaf.getJsUndefined(jsContext);
      } catch (InvocationTargetException e) {
        // If we get here, it means an exception is being thrown from
        // Java back into JavaScript
        Throwable t = e.getTargetException();
        ModuleSpace.setThrownJavaException(t);
        // Native code eats the same ref it gave us.
        exception[0] = LowLevelSaf.getJsNull(jsContext);
        // Native code eats the same ref it gave us.
        return LowLevelSaf.getJsUndefined(jsContext);
      }
    } finally {
      LowLevelSaf.popJsContext(jsContext);
    }
  }
}
