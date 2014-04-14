/*
 * Copyright 2009 Google Inc.
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

import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.gargoylesoftware.htmlunit.javascript.host.WindowProxy;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import java.io.IOException;

/**
 * HTMLUnit object that represents the hosted-mode plugin.
 */
public class HostedModePluginObject extends ScriptableObject {

  /**
   * Function object which implements the connect method on the hosted-mode
   * plugin.
   */
  private class ConnectMethod extends ScriptableObject implements Function {

    private static final long serialVersionUID = -8799481412144205779L;
    private static final int EXPECTED_NUM_ARGS = 5;

    @Override
    public Object call(Context context, Scriptable scope, Scriptable thisObj,
        Object[] args) {
      // Allow extra arguments for forward compatibility
      if (args.length < EXPECTED_NUM_ARGS) {
        throw Context.reportRuntimeError("Bad number of parameters for function"
            + " connect: expected "
            + EXPECTED_NUM_ARGS
            + ", got "
            + args.length);
      }
      try {
        /*
         * connect arguments: url, sessionKey, ipAddress:port, moduleName,
         * hostedHtmlVersion
         */
        return connect((String) args[0], (String) args[1], (String) args[2],
            (String) args[3], (String) args[4]);
      } catch (ClassCastException e) {
        throw Context.reportRuntimeError("Incorrect parameter types for "
            + " connect: expected String/String/String/String/String");
      }
    }

    @Override
    public Scriptable construct(Context context, Scriptable scope, Object[] args) {
      throw Context.reportRuntimeError("Function connect can't be used as a "
          + "constructor");
    }

    @Override
    public String getClassName() {
      return "function HostedModePluginObject.connect";
    }
  }

  /**
   * Function object which implements the disconnect method on the hosted-mode
   * plugin.
   */
  private class DisconnectMethod extends ScriptableObject implements Function {

    private static final long serialVersionUID = -8799481412144519779L;

    @Override
    public Object call(Context context, Scriptable scope, Scriptable thisObj,
        Object[] args) {
      // Allow extra arguments for forward compatibility
      return disconnect();
    }

    @Override
    public Scriptable construct(Context context, Scriptable scope, Object[] args) {
      throw Context.reportRuntimeError("Function disconnect can't be used as a "
          + "constructor");
    }

    @Override
    public String getClassName() {
      return "function HostedModePluginObject.disconnect";
    }
  }

  /**
   * Function object which implements the init method on the hosted-mode plugin.
   */
  private class InitMethod extends ScriptableObject implements Function {

    private static final long serialVersionUID = -8799481412144205779L;
    private static final String VERSION = "2.0";

    @Override
    public Object call(Context context, Scriptable scope, Scriptable thisObj,
        Object[] args) {
      // Allow extra arguments for forward compatibility
      if (args.length < 1) {
        throw Context.reportRuntimeError("Bad number of parameters for function"
            + " init: expected 1, got " + args.length);
      }
      try {
        window = ((WindowProxy) args[0]).getDelegee();
        return init(VERSION);
      } catch (ClassCastException e) {
        throw Context.reportRuntimeError("Incorrect parameter types for "
            + " initt: expected String");
      }
    }

    @Override
    public Scriptable construct(Context context, Scriptable scope, Object[] args) {
      throw Context.reportRuntimeError("Function init can't be used as a "
          + "constructor");
    }

    @Override
    public String getClassName() {
      return "function HostedModePluginObject.init";
    }
  }

  private static final long serialVersionUID = -1815031145376726799L;

  private Scriptable connectMethod;
  private Scriptable disconnectMethod;
  private Scriptable initMethod;
  private Window window;
  private final JavaScriptEngine jsEngine;
  private final TreeLogger logger;

  private BrowserChannelClient browserChannelClient;

  /**
   * Creates a HostedModePluginObject with the passed-in JavaScriptEngine.
   *
   * @param jsEngine The JavaScriptEngine.
   */
  public HostedModePluginObject(JavaScriptEngine jsEngine, TreeLogger logger) {
    this.jsEngine = jsEngine;
    this.logger = logger;
  }

  /**
   * Initiate a hosted mode connection to the requested port and load the
   * requested module.
   *
   * @param url the complete url
   * @param sessionKey a length 16 string to identify a "session"
   * @param address "host:port" or "ipAddress:port" to use for the OOPHM server
   * @param module module name to load
   * @param version version string
   * @return true if the connection succeeds
   */
  public boolean connect(String url, String sessionKey, String address,
      String module, String version) {
    String addressParts[] = address.split(":");
    if (addressParts.length < 2) {
      logger.log(TreeLogger.ERROR, "connect failed because address " + address
          + " was not of the form foo.com:8080");
      return false;
    }
    // TODO: add whitelist and default-port support?

    try {
      HtmlUnitSessionHandler htmlUnitSessionHandler = new HtmlUnitSessionHandler(
          window, jsEngine);
      browserChannelClient = new BrowserChannelClient(addressParts, url,
          sessionKey, module, version, htmlUnitSessionHandler);
      htmlUnitSessionHandler.setSessionData(new SessionData(
          htmlUnitSessionHandler, browserChannelClient));
      return browserChannelClient.process();
    } catch (BrowserChannelException e) {
      logger.log(TreeLogger.ERROR,
          "BrowserChannelException returned from connect " + e.getMessage(), e);
      return false;
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "IOException returned from connect "
          + e.getMessage(), e);
      return false;
    }
  }

  public boolean disconnect() {
    try {
      return browserChannelClient.disconnectFromHost();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "IOException returned from disconnect "
          + e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Object get(String name, Scriptable start) {
    if ("connect".equals(name)) {
      if (connectMethod == null) {
        connectMethod = new ConnectMethod();
      }
      return connectMethod;
    } else if ("disconnect".equals(name)) {
      if (disconnectMethod == null) {
        disconnectMethod = new DisconnectMethod();
      }
      return disconnectMethod;
    } else if ("init".equals(name)) {
      if (initMethod == null) {
        initMethod = new InitMethod();
      }
      return initMethod;
    }
    return NOT_FOUND;
  }

  @Override
  public String getClassName() {
    return "HostedModePluginObject";
  }

  /**
   * Verify that the plugin can be initialized properly and supports the
   * requested version.
   *
   * @param version hosted mode protocol version
   * @return true if initialization succeeds, otherwise false
   */
  public boolean init(String version) {
    // TODO: what needs to be done here?
    return true;
  }
}
