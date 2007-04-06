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

import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.mac.LowLevelSaf.DispatchObject;

/**
 * An implementation of {@link com.google.gwt.dev.shell.ModuleSpace} for Safari.
 */
public class ModuleSpaceSaf extends ModuleSpace {

  private DispatchObject staticDispatch;

  private final int window;

  /**
   * Constructs a browser interface for use with a global window object.
   * 
   * @param moduleName name of the module 
   * @param key unique key for this instance of the module
   */
  public ModuleSpaceSaf(ModuleSpaceHost host, int scriptGlobalObject,
      String moduleName, Object key) {
    super(host, moduleName, key);

    // Hang on to the global execution state.
    //
    this.window = scriptGlobalObject;
    LowLevelSaf.gcLock(scriptGlobalObject);
  }

  public void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    // Execute the function definition within the browser, which will define
    // a new top-level function.
    //
    String newScript = createNativeMethodInjector(jsniSignature, paramNames, js);
    LowLevelSaf.executeScriptWithInfo(LowLevelSaf.getGlobalExecState(window),
        newScript, file, line);
  }

  public void dispose() {
    LowLevelSaf.gcUnlock(window);
    super.dispose();
  }

  public void exceptionCaught(int number, String name, String message) {
    RuntimeException thrown = (RuntimeException) sThrownJavaExceptionObject.get();

    // See if the caught exception is null (thus thrown by us)
    if (thrown != null) {
      if (name == null && message == null) {
        sCaughtJavaExceptionObject.set(thrown);
        sThrownJavaExceptionObject.set(null);
        return;
      }
    }

    sCaughtJavaExceptionObject.set(createJavaScriptException(
        getIsolatedClassLoader(), name, message));
  }

  /**
   * Invokes a native JavaScript function.
   * 
   * @param name the name of the function to invoke
   * @param jthis the function's 'this' context
   * @param types the type of each argument
   * @param args the arguments to be passed
   * @return the return value as a Object.
   */
  protected JsValue doInvoke(String name, Object jthis, Class[] types,
      Object[] args) {
    int jsthis = wrapObjectAsJSObject(jthis);
    int curExecState = LowLevelSaf.getExecState();
    int argc = args.length;
    int argv[] = new int[argc];
    for (int i = 0; i < argc; ++i) {
      JsValueSaf jsValue = new JsValueSaf();
      JsValueGlue.set(jsValue, getIsolatedClassLoader(), types[i], args[i]);
      argv[i] = jsValue.getJsValue();
    }

    int result = LowLevelSaf.invoke(curExecState, window, name, jsthis, argv);
    if (!isExceptionActive()) {
      return new JsValueSaf(result);
    }

    /*
     * The stack trace on the stored exception will not be very useful due to
     * how it was created. Using fillInStackTrace() resets the stack trace to
     * this moment in time, which is usually far more useful.
     */
    RuntimeException thrown = takeJavaException();
    thrown.fillInStackTrace();
    throw thrown;
  }

  protected void initializeStaticDispatcher() {
    staticDispatch = new WebKitDispatchAdapter(getIsolatedClassLoader());

    // Define the static dispatcher for use by JavaScript.
    //
    createNative("initializeStaticDispatcher", 0, "__defineStatic",
        new String[] {"__arg0"}, "window.__static = __arg0;");
    invokeNativeVoid("__defineStatic", null, new Class[] {Object.class},
        new Object[] {staticDispatch});
  }

  protected int wrapObjectAsJSObject(Object o) {
    if (o == null) {
      return LowLevelSaf.jsNull();
    }
    
    DispatchObject dispObj;
    if (o instanceof DispatchObject) {
      dispObj = (DispatchObject) o;
    } else {
      dispObj = new WebKitDispatchAdapter(getIsolatedClassLoader(), o);
    }
    return LowLevelSaf.wrapDispatch(dispObj);
  }

}
