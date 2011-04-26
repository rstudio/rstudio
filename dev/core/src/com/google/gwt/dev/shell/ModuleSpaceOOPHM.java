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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.JsniMethod;
import com.google.gwt.dev.shell.JsValue.DispatchObject;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.List;
import java.util.Set;

/**
 */
public class ModuleSpaceOOPHM extends ModuleSpace {

  private BrowserChannelServer channel;

  public ModuleSpaceOOPHM(ModuleSpaceHost msh, String moduleName,
      BrowserChannelServer channel) {
    super(msh.getLogger(), msh, moduleName);
    this.channel = channel;
    msh.getLogger().log(TreeLogger.DEBUG,
        "Created ModuleSpaceOOPHM for " + moduleName, null);
  }

  public void createNativeMethods(TreeLogger logger,
      List<JsniMethod> jsniMethods, DispatchIdOracle dispatchIdOracle) {
    if (jsniMethods.isEmpty()) {
      return;
    }
    StringBuilder jsni = new StringBuilder();
    for (JsniMethod jsniMethod : jsniMethods) {
      if (jsniMethod.isScriptOnly()) {
        continue;
      }
      String body = Jsni.getJavaScriptForHostedMode(dispatchIdOracle,
          jsniMethod);
      if (body == null) {
        // The error has been logged; just ignore it for now.
        continue;
      }
      jsni.append("// " + jsniMethod.location() + ":" + jsniMethod.line()
          + "\n");
      jsni.append("this[\"" + jsniMethod.name() + "\"] = function(");
      String[] paramNames = jsniMethod.paramNames();
      for (int i = 0; i < paramNames.length; ++i) {
        if (i > 0) {
          jsni.append(", ");
        }
        jsni.append(paramNames[i]);
      }
      jsni.append(") ");
      jsni.append(body);
      jsni.append(";\n\n");
    }
    channel.loadJsni(jsni.toString());
  }

  // @Override
  protected void cleanupJsValues() {
    Set<Integer> refIdsForCleanup = channel.getRefIdsForCleanup();
    if (refIdsForCleanup.isEmpty()) {
      // nothing to do
      return;
    }
    int[] ids = new int[refIdsForCleanup.size()];
    int i = 0;
    for (Integer id : refIdsForCleanup) {
      ids[i++] = id;
    }
    channel.freeJsValue(ids);
  }

  /**
   * 
   */
  @Override
  protected void createStaticDispatcher(TreeLogger logger) {
    channel.loadJsni("function __defineStatic(__arg0) { window.__static = __arg0; }");
  }

  /**
   * Invoke a JS method and return its value.
   * 
   * @param name method name to invoke
   * @param jthis object to invoke method on, null if static method
   * @param types argument types
   * @param args argument values
   */
  @Override
  protected JsValue doInvoke(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable {
    TreeLogger branch = host.getLogger().branch(TreeLogger.SPAM,
        "Invoke native method " + name, null);
    Event javaToJsCallEvent =
        SpeedTracerLogger.start(DevModeEventType.JAVA_TO_JS_CALL);
    if (SpeedTracerLogger.jsniCallLoggingEnabled()) {
      javaToJsCallEvent.addData("name", name);
    }

    CompilingClassLoader isolatedClassLoader = getIsolatedClassLoader();
    JsValueOOPHM jsthis = new JsValueOOPHM();
    Class<?> jthisType = (jthis == null) ? Object.class : jthis.getClass();
    JsValueGlue.set(jsthis, isolatedClassLoader, jthisType, jthis);
    if (branch.isLoggable(TreeLogger.SPAM)) {
      branch.log(TreeLogger.SPAM, "  this=" + jsthis);
    }

    int argc = args.length;
    JsValueOOPHM argv[] = new JsValueOOPHM[argc];
    for (int i = 0; i < argc; ++i) {
      argv[i] = new JsValueOOPHM();
      JsValueGlue.set(argv[i], isolatedClassLoader, types[i], args[i]);
      if (branch.isLoggable(TreeLogger.SPAM)) {
        branch.log(TreeLogger.SPAM, "  arg[" + i + "]=" + argv[i]);
      }
    }
    JsValueOOPHM returnVal = new JsValueOOPHM();
    try {
      channel.invokeJavascript(isolatedClassLoader, jsthis, name, argv,
          returnVal);
      if (branch.isLoggable(TreeLogger.SPAM)) {
        branch.log(TreeLogger.SPAM, "  returned " + returnVal);
      }
      return returnVal;
    } catch (Throwable t) {
      branch.log(TreeLogger.SPAM, "exception thrown", t);
      throw t;
    } finally {
      javaToJsCallEvent.end();
    }
  }

  @Override
  protected DispatchObject getStaticDispatcher() {
    return new JsValueOOPHM.DispatchObjectOOPHM(getIsolatedClassLoader());
  }
}
