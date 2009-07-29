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
 * An implementation of {@link com.google.gwt.dev.shell.ModuleSpace} for
 * Mozilla.
 */
public class ModuleSpaceMoz extends ModuleSpace {

  private final int window;

  /**
   * Constructs a browser interface for use with a Mozilla global window object.
   */
  public ModuleSpaceMoz(TreeLogger logger, ModuleSpaceHost host,
      int scriptGlobalObject, String moduleName, Object key) {
    super(logger, host, moduleName, key);

    // Hang on to the parent window.
    //
    window = scriptGlobalObject;
    SwtGeckoGlue.addRefInt(window);
  }

  /**
   * Define one or more JSNI methods.
   * 
   * @param logger
   * @param jsniMethods
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.dev.shell.ModuleSpace#dispose()
   */
  @Override
  public void dispose() {
    SwtGeckoGlue.releaseInt(window);
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

    JsValueMoz jsthis = new JsValueMoz();
    Class<?> jthisType = (jthis == null) ? Object.class : jthis.getClass();
    JsValueGlue.set(jsthis, isolatedClassLoader, jthisType, jthis);

    int argc = args.length;
    JsValueMoz argv[] = new JsValueMoz[argc];
    int[] jsArgsInt = new int[argc];
    for (int i = 0; i < argc; ++i) {
      argv[i] = new JsValueMoz();
      JsValueGlue.set(argv[i], isolatedClassLoader, types[i], args[i]);
      jsArgsInt[i] = argv[i].getJsRootedValue();
    }
    JsValueMoz returnVal = new JsValueMoz();
    LowLevelMoz.invoke(window, name, jsthis.getJsRootedValue(), jsArgsInt,
        returnVal.getJsRootedValue());
    return returnVal;
  }

  @Override
  protected Object getStaticDispatcher() {
    return new GeckoDispatchAdapter(getIsolatedClassLoader());
  }

  /**
   * Defines a new native JavaScript function.
   * 
   * @param name the function's name, usually a JSNI signature
   * @param paramNames parameter names
   * @param js the script body
   */
  private void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    // Execute the function definition within the browser, which will define
    // a new top-level function.
    //
    String newScript = createNativeMethodInjector(jsniSignature, paramNames, js);
    LowLevelMoz.executeScriptWithInfo(window, newScript, file, line);
  }
}
