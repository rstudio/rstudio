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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.JsniMethod;
import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.DispatchIdOracle;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.shell.Jsni;
import com.google.gwt.dev.shell.ModuleSpace;
import com.google.gwt.dev.shell.ModuleSpaceHost;

import java.util.List;

/**
 * An implementation of {@link com.google.gwt.dev.shell.ModuleSpace} for Safari.
 */
public class ModuleSpaceSaf extends ModuleSpace {

  private final int globalObject;

  private final int globalContext;

  /**
   * Constructs a browser interface for use with a global window object.
   * 
   * @param moduleName name of the module
   * @param key unique key for this instance of the module
   */
  public ModuleSpaceSaf(TreeLogger logger, ModuleSpaceHost host,
      int scriptGlobalObject, int scriptGlobalContext, String moduleName,
      Object key) {
    super(logger, host, moduleName, key);

    // Hang on to the global execution state.
    //
    this.globalObject = scriptGlobalObject;
    this.globalContext = scriptGlobalContext;
    LowLevelSaf.gcProtect(LowLevelSaf.getCurrentJsContext(), scriptGlobalObject);
    LowLevelSaf.retainJsGlobalContext(scriptGlobalContext);
  }

  public void createNativeMethods(TreeLogger logger,
      List<JsniMethod> jsniMethods, DispatchIdOracle dispatchIdOracle) {
    for (JsniMethod jsniMethod : jsniMethods) {
      String body = Jsni.getJavaScriptForHostedMode(logger, dispatchIdOracle,
          jsniMethod);
      if (body == null) {
        // The error has been logged; just ignore it for now.
        continue;
      }
      createNative(jsniMethod.location(), jsniMethod.line(), jsniMethod.name(),
          jsniMethod.paramNames(), body);
    }
  }

  @Override
  public void dispose() {
    LowLevelSaf.gcUnprotect(LowLevelSaf.getCurrentJsContext(), globalObject);
    LowLevelSaf.releaseJsGlobalContext(globalContext);
    super.dispose();
  }

  @Override
  protected void createStaticDispatcher(TreeLogger logger) {
    createNative("initializeStaticDispatcher", 0, "__defineStatic",
        new String[] {"__arg0"}, "window.__static = __arg0;");
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
  @Override
  protected JsValue doInvoke(String name, Object jthis, Class<?>[] types,
      Object[] args) {
    CompilingClassLoader isolatedClassLoader = getIsolatedClassLoader();

    JsValueSaf jsValueThis = new JsValueSaf();
    Class<?> jthisType = (jthis == null) ? Object.class : jthis.getClass();
    JsValueGlue.set(jsValueThis, isolatedClassLoader, jthisType, jthis);
    int jsthis = jsValueThis.getJsValue();

    int argc = args.length;
    int[] argv = new int[argc];
    // GC protect passed arguments on the Java stack for call duration.
    JsValueSaf[] jsValueArgs = new JsValueSaf[argc];
    for (int i = 0; i < argc; ++i) {
      JsValueSaf jsValue = jsValueArgs[i] = new JsValueSaf();
      JsValueGlue.set(jsValue, isolatedClassLoader, types[i], args[i]);
      argv[i] = jsValue.getJsValue();
    }

    final int curJsContext = LowLevelSaf.getCurrentJsContext();

    int result = LowLevelSaf.invoke(curJsContext, globalObject, name, jsthis,
        argv);
    return new JsValueSaf(result);
  }

  @Override
  protected Object getStaticDispatcher() {
    return new WebKitDispatchAdapter(getIsolatedClassLoader());
  }

  private void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    // Execute the function definition within the browser, which will define
    // a new top-level function.
    //
    String newScript = createNativeMethodInjector(jsniSignature, paramNames, js);
    LowLevelSaf.executeScriptWithInfo(globalContext, newScript, file, line);
  }
}
