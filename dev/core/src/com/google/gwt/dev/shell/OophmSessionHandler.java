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
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.JsValue.DispatchMethod;
import com.google.gwt.dev.shell.JsValue.DispatchObject;
import com.google.gwt.dev.util.PerfLogger;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class OophmSessionHandler extends SessionHandler {

  private BrowserWidgetHost host;

  private TreeLogger logger;

  private Map<BrowserChannelServer, ModuleSpace> moduleMap = Collections.synchronizedMap(new HashMap<BrowserChannelServer, ModuleSpace>());

  /**
   * Listens for new connections from browsers.
   */
  public OophmSessionHandler(BrowserWidgetHost host) {
    this.host = host;
    logger = host.getLogger();
  }

  @Override
  public void freeValue(BrowserChannel channel, int[] ids) {
    BrowserChannelServer serverChannel = (BrowserChannelServer) channel;
    ServerObjectsTable localObjects = serverChannel.getJavaObjectsExposedInBrowser();
    for (int id : ids) {
      localObjects.free(id);
    }
  }

  @Override
  public ExceptionOrReturnValue getProperty(BrowserChannel channel, int refId,
      int dispId) {
    BrowserChannelServer serverChannel = (BrowserChannelServer) channel;
    ModuleSpace moduleSpace = moduleMap.get(serverChannel);
    assert moduleSpace != null;
    ServerObjectsTable localObjects = serverChannel.getJavaObjectsExposedInBrowser();
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
      Value retVal = serverChannel.convertFromJsValue(localObjects, jsval);
      branch.log(TreeLogger.SPAM, "result is " + retVal, null);
      return new ExceptionOrReturnValue(false, retVal);
    } catch (Throwable t) {
      JsValueOOPHM jsval = new JsValueOOPHM();
      JsValueGlue.set(jsval, moduleSpace.getIsolatedClassLoader(),
          t.getClass(), t);
      Value retVal = serverChannel.convertFromJsValue(localObjects, jsval);
      return new ExceptionOrReturnValue(true, retVal);
    }
  }

  /**
   * Invoke a method on a server object in from client code.
   */
  @Override
  public ExceptionOrReturnValue invoke(BrowserChannel channel, Value thisVal,
      int methodDispatchId, Value[] args) {
    BrowserChannelServer serverChannel = (BrowserChannelServer) channel;
    ServerObjectsTable localObjects = serverChannel.getJavaObjectsExposedInBrowser();
    ModuleSpace moduleSpace = moduleMap.get(serverChannel);
    assert moduleSpace != null;
    CompilingClassLoader cl = moduleSpace.getIsolatedClassLoader();

    // Treat dispatch id 0 as toString()
    if (methodDispatchId == 0) {
      methodDispatchId = cl.getDispId("java.lang.Object::toString()");
    }

    JsValueOOPHM jsThis = new JsValueOOPHM();
    serverChannel.convertToJsValue(cl, localObjects, thisVal, jsThis);

    TreeLogger branch = TreeLogger.NULL;
    if (logger.isLoggable(TreeLogger.DEBUG)) {
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
      branch = logger.branch(TreeLogger.DEBUG, logMsg.toString(), null);
    }
    JsValueOOPHM[] jsArgs = new JsValueOOPHM[args.length];
    for (int i = 0; i < args.length; ++i) {
      jsArgs[i] = new JsValueOOPHM();
      serverChannel.convertToJsValue(cl, localObjects, args[i], jsArgs[i]);
      branch.log(TreeLogger.DEBUG, " arg " + i + " = " + jsArgs[i].toString(),
          null);
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
    Value retVal = serverChannel.convertFromJsValue(localObjects, jsRetVal);
    return new ExceptionOrReturnValue(exception, retVal);
  }

  @Override
  public TreeLogger loadModule(TreeLogger loadModuleLogger,
      BrowserChannel channel, String moduleName, String userAgent, String url,
      String tabKey, String sessionKey, byte[] userAgentIcon) {
    logger = loadModuleLogger;
    try {
      PerfLogger.start("OophmSessionHandler.loadModule " + moduleName);
      // Attach a new ModuleSpace to make it programmable.
      //
      BrowserChannelServer serverChannel = (BrowserChannelServer) channel;
      ModuleSpaceHost msh = host.createModuleSpaceHost(loadModuleLogger,
          moduleName, userAgent, url, tabKey, sessionKey, serverChannel,
          userAgentIcon);
      logger = msh.getLogger();
      ModuleSpace moduleSpace = new ModuleSpaceOOPHM(msh, moduleName,
          serverChannel);
      moduleMap.put(serverChannel, moduleSpace);
      PerfLogger.start("ModuleSpace.onLoad");
      moduleSpace.onLoad(logger);
    } catch (Throwable e) {
      // We do catch Throwable intentionally because there are a ton of things
      // that can go wrong trying to load a module, including Error-derived
      // things like NoClassDefFoundError.
      // 
      this.logger.log(TreeLogger.ERROR, "Failed to load module '" + moduleName
          + "' from user agent '" + userAgent + "' at "
          + channel.getRemoteEndpoint(), e);
    } finally {
      PerfLogger.end();
      PerfLogger.end();
    }
    return this.logger;
  }

  @Override
  public ExceptionOrReturnValue setProperty(BrowserChannel channel, int refId,
      int dispId, Value newValue) {
    BrowserChannelServer serverChannel = (BrowserChannelServer) channel;
    ModuleSpace moduleSpace = moduleMap.get(serverChannel);
    assert moduleSpace != null;
    ServerObjectsTable localObjects = serverChannel.getJavaObjectsExposedInBrowser();
    try {
      JsValueOOPHM obj = new JsValueOOPHM();
      DispatchObject dispObj;
      obj.setWrappedJavaObject(moduleSpace.getIsolatedClassLoader(),
          localObjects.get(refId));
      dispObj = obj.getJavaObjectWrapper();
      logger.log(TreeLogger.SPAM, "Client special invoke of setProperty(id="
          + dispId + ", newValue=" + newValue + ") on " + obj.toString(), null);
      JsValueOOPHM jsval = new JsValueOOPHM();
      serverChannel.convertToJsValue(moduleSpace.getIsolatedClassLoader(),
          localObjects, newValue, jsval);
      dispObj.setField(dispId, jsval);
      return new ExceptionOrReturnValue(false, newValue);
    } catch (Throwable t) {
      JsValueOOPHM jsval = new JsValueOOPHM();
      JsValueGlue.set(jsval, moduleSpace.getIsolatedClassLoader(),
          t.getClass(), t);
      Value retVal = serverChannel.convertFromJsValue(localObjects, jsval);
      return new ExceptionOrReturnValue(true, retVal);
    }
  }

  @Override
  public void unloadModule(BrowserChannel channel, String moduleName) {
    BrowserChannelServer serverChannel = (BrowserChannelServer) channel;
    ModuleSpace moduleSpace = moduleMap.get(serverChannel);
    if (moduleSpace == null) {
      logger.log(TreeLogger.ERROR, "Unload request without a module loaded",
          null);
      return;
    }
    logger.log(TreeLogger.INFO, "Unloading module "
        + moduleSpace.getModuleName() + " (" + moduleName + ")", null);
    host.unloadModule(moduleSpace.host);
    moduleSpace.dispose();
    moduleMap.remove(serverChannel);
  }
}
