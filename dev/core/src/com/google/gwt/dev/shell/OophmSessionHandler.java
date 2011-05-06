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
import com.google.gwt.dev.ModuleHandle;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.BrowserChannelServer.SessionHandlerServer;
import com.google.gwt.dev.shell.JsValue.DispatchMethod;
import com.google.gwt.dev.shell.JsValue.DispatchObject;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class OophmSessionHandler extends SessionHandlerServer {

  private BrowserWidgetHost host;

  private Map<BrowserChannelServer, ModuleSpace> moduleMap = Collections.synchronizedMap(new HashMap<BrowserChannelServer, ModuleSpace>());

  private Map<BrowserChannelServer, ModuleHandle> moduleHandleMap = Collections.synchronizedMap(new HashMap<BrowserChannelServer, ModuleHandle>());

  private final TreeLogger topLogger;

  /**
   * Listens for new connections from browsers.
   * 
   * @param topLogger logger to use for non-module-related messages
   * @param host BrowserWidgetHost instance
   */
  public OophmSessionHandler(TreeLogger topLogger, BrowserWidgetHost host) {
    this.host = host;
    this.topLogger = topLogger;
  }

  @Override
  public void freeValue(BrowserChannelServer channel, int[] ids) {
    ServerObjectsTable localObjects = channel.getJavaObjectsExposedInBrowser();
    for (int id : ids) {
      localObjects.free(id);
    }
  }

  @Override
  public ExceptionOrReturnValue getProperty(BrowserChannelServer channel,
      int refId, int dispId) {
    ModuleSpace moduleSpace = moduleMap.get(channel);
    ModuleHandle moduleHandle = moduleHandleMap.get(channel);
    assert moduleSpace != null && moduleHandle != null;
    TreeLogger logger = moduleHandle.getLogger();
    ServerObjectsTable localObjects = channel.getJavaObjectsExposedInBrowser();
    try {
      JsValueOOPHM obj = new JsValueOOPHM();
      DispatchObject dispObj;
      CompilingClassLoader ccl = moduleSpace.getIsolatedClassLoader();
      obj.setWrappedJavaObject(ccl, localObjects.get(refId));
      dispObj = obj.getJavaObjectWrapper();
      TreeLogger branch = logger.branch(TreeLogger.SPAM,
          "Client special invoke of getProperty(" + dispId + " ["
              + ccl.getClassInfoByDispId(dispId).getMember(dispId) + "]) on "
              + obj.toString(), null);
      JsValueOOPHM jsval = (JsValueOOPHM) dispObj.getField(dispId);
      Value retVal = channel.convertFromJsValue(localObjects, jsval);
      if (logger.isLoggable(TreeLogger.SPAM)) {
        branch.log(TreeLogger.SPAM, "result is " + retVal, null);
      }
      return new ExceptionOrReturnValue(false, retVal);
    } catch (Throwable t) {
      JsValueOOPHM jsval = new JsValueOOPHM();
      JsValueGlue.set(jsval, moduleSpace.getIsolatedClassLoader(),
          t.getClass(), t);
      Value retVal = channel.convertFromJsValue(localObjects, jsval);
      return new ExceptionOrReturnValue(true, retVal);
    }
  }

  /**
   * Invoke a method on a server object in from client code.
   */
  @Override
  public ExceptionOrReturnValue invoke(BrowserChannelServer channel,
      Value thisVal, int methodDispatchId, Value[] args) {
    Event jsToJavaCallEvent =
        SpeedTracerLogger.start(channel.getDevModeSession(), DevModeEventType.JS_TO_JAVA_CALL);
    ServerObjectsTable localObjects = channel.getJavaObjectsExposedInBrowser();
    ModuleSpace moduleSpace = moduleMap.get(channel);
    ModuleHandle moduleHandle = moduleHandleMap.get(channel);
    assert moduleSpace != null && moduleHandle != null;
    TreeLogger logger = moduleHandle.getLogger();
    CompilingClassLoader cl = moduleSpace.getIsolatedClassLoader();

    // Treat dispatch id 0 as toString()
    if (methodDispatchId == 0) {
      methodDispatchId = cl.getDispId("java.lang.Object::toString()");
    }

    JsValueOOPHM jsThis = new JsValueOOPHM();
    channel.convertToJsValue(cl, localObjects, thisVal, jsThis);

    if (SpeedTracerLogger.jsniCallLoggingEnabled()) {
      DispatchClassInfo clsInfo = cl.getClassInfoByDispId(methodDispatchId);
      if (clsInfo != null) {
        Member member = clsInfo.getMember(methodDispatchId);
        if (member != null) {
          jsToJavaCallEvent.addData("name", member.toString());
        }
      }
    }
    
    TreeLogger branch = TreeLogger.NULL;
    if (logger.isLoggable(TreeLogger.SPAM)) {
      StringBuffer logMsg = new StringBuffer();
      logMsg.append("Client invoke of ");
      logMsg.append(methodDispatchId);
      DispatchClassInfo classInfo = cl.getClassInfoByDispId(methodDispatchId);
      if (classInfo != null) {
        Member member = classInfo.getMember(methodDispatchId);
        if (member != null) {
          logMsg.append(" (");
          logMsg.append(member.getName());
          logMsg.append(")");
        }
      }
      logMsg.append(" on ");
      logMsg.append(jsThis.toString());
      branch = logger.branch(TreeLogger.SPAM, logMsg.toString(), null);
    }
    JsValueOOPHM[] jsArgs = new JsValueOOPHM[args.length];
    for (int i = 0; i < args.length; ++i) {
      jsArgs[i] = new JsValueOOPHM();
      channel.convertToJsValue(cl, localObjects, args[i], jsArgs[i]);
      if (logger.isLoggable(TreeLogger.SPAM)) {
        branch.log(TreeLogger.SPAM, " arg " + i + " = " + jsArgs[i].toString(),
            null);
      }
    }
    JsValueOOPHM jsRetVal = new JsValueOOPHM();
    JsValueOOPHM jsMethod;
    DispatchObject dispObj;
    if (jsThis.isWrappedJavaObject()) {
      // If this is a wrapped object, get get the method off it.
      dispObj = jsThis.getJavaObjectWrapper();
    } else {
      // Look it up on the static dispatcher.
      dispObj = (DispatchObject) moduleSpace.getStaticDispatcher();
    }
    jsMethod = (JsValueOOPHM) dispObj.getField(methodDispatchId);
    DispatchMethod dispMethod = jsMethod.getWrappedJavaFunction();
    boolean exception;
    try {
      exception = dispMethod.invoke(jsThis, jsArgs, jsRetVal);
    } catch (Throwable t) {
      exception = true;
      JsValueGlue.set(jsRetVal, moduleSpace.getIsolatedClassLoader(),
          t.getClass(), t);
    }
    Value retVal = channel.convertFromJsValue(localObjects, jsRetVal);
    jsToJavaCallEvent.end();
    return new ExceptionOrReturnValue(exception, retVal);
  }

  @Override
  public synchronized TreeLogger loadModule(BrowserChannelServer channel,
      String moduleName, String userAgent, String url, String tabKey,
      String sessionKey, byte[] userAgentIcon) {
    Event moduleInit =
        SpeedTracerLogger.start(channel.getDevModeSession(), DevModeEventType.MODULE_INIT,
            "Module Name", moduleName);
    ModuleHandle moduleHandle = host.createModuleLogger(moduleName, userAgent,
        url, tabKey, sessionKey, channel, userAgentIcon);
    TreeLogger logger = moduleHandle.getLogger();
    moduleHandleMap.put(channel, moduleHandle);
    ModuleSpace moduleSpace = null;
    try {
      // Attach a new ModuleSpace to make it programmable.
      ModuleSpaceHost msh = host.createModuleSpaceHost(moduleHandle, moduleName);
      moduleSpace = new ModuleSpaceOOPHM(msh, moduleName, channel);
      moduleMap.put(channel, moduleSpace);
      moduleSpace.onLoad(logger);
      if (logger.isLoggable(TreeLogger.INFO)) {
        moduleHandle.getLogger().log(TreeLogger.INFO,
            "Module " + moduleName + " has been loaded");
      }
    } catch (Throwable e) {
      // We do catch Throwable intentionally because there are a ton of things
      // that can go wrong trying to load a module, including Error-derived
      // things like NoClassDefFoundError.
      // 
      moduleHandle.getLogger().log(
          TreeLogger.ERROR,
          "Failed to load module '" + moduleName + "' from user agent '"
              + userAgent + "' at " + channel.getRemoteEndpoint(), e);
      if (moduleSpace != null) {
        moduleSpace.dispose();
      }
      moduleHandle.unload();
      moduleMap.remove(channel);
      moduleHandleMap.remove(channel);
      return null;
    } finally {
      moduleInit.end();
    }
    return moduleHandle.getLogger();
  }

  @Override
  public ExceptionOrReturnValue setProperty(BrowserChannelServer channel,
      int refId, int dispId, Value newValue) {
    ModuleSpace moduleSpace = moduleMap.get(channel);
    ModuleHandle moduleHandle = moduleHandleMap.get(channel);
    assert moduleSpace != null && moduleHandle != null;
    TreeLogger logger = moduleHandle.getLogger();
    ServerObjectsTable localObjects = channel.getJavaObjectsExposedInBrowser();
    try {
      JsValueOOPHM obj = new JsValueOOPHM();
      DispatchObject dispObj;
      obj.setWrappedJavaObject(moduleSpace.getIsolatedClassLoader(),
          localObjects.get(refId));
      dispObj = obj.getJavaObjectWrapper();
      if (logger.isLoggable(TreeLogger.SPAM)) {
        logger.log(TreeLogger.SPAM, "Client special invoke of setProperty(id="
            + dispId + ", newValue=" + newValue + ") on " + obj.toString(), null);
      }
      JsValueOOPHM jsval = new JsValueOOPHM();
      channel.convertToJsValue(moduleSpace.getIsolatedClassLoader(),
          localObjects, newValue, jsval);
      dispObj.setField(dispId, jsval);
      return new ExceptionOrReturnValue(false, newValue);
    } catch (Throwable t) {
      JsValueOOPHM jsval = new JsValueOOPHM();
      JsValueGlue.set(jsval, moduleSpace.getIsolatedClassLoader(),
          t.getClass(), t);
      Value retVal = channel.convertFromJsValue(localObjects, jsval);
      return new ExceptionOrReturnValue(true, retVal);
    }
  }

  @Override
  public void unloadModule(BrowserChannelServer channel, String moduleName) {
    ModuleHandle moduleHandle = moduleHandleMap.get(channel);
    ModuleSpace moduleSpace = moduleMap.get(channel);
    if (moduleSpace == null || moduleHandle == null) {
      topLogger.log(TreeLogger.ERROR, "Unload request without a module loaded",
          null);
      return;
    }
    moduleHandle.getLogger().log(
        TreeLogger.INFO,
        "Unloading module " + moduleSpace.getModuleName() + " (" + moduleName
            + ")", null);
    moduleSpace.dispose();
    moduleHandle.unload();
    moduleMap.remove(channel);
    moduleHandleMap.remove(channel);
  }
}
