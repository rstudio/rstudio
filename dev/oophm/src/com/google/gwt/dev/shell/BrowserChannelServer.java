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
import com.google.gwt.dev.shell.JsValue.DispatchObject;

import java.io.IOException;
import java.net.Socket;

/**
 * 
 */
public class BrowserChannelServer extends BrowserChannel implements Runnable {

  public static final String JSO_CLASS = "com.google.gwt.core.client.JavaScriptObject";

  private SessionHandler handler;

  private final ObjectsTable javaObjectsInBrowser = new ObjectsTable();

  private TreeLogger logger;

  private String moduleName;

  private String userAgent;

  public BrowserChannelServer(TreeLogger initialLogger, Socket socket,
      SessionHandler handler) throws IOException {
    super(socket);
    this.handler = handler;
    this.logger = initialLogger;
    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.setName("Hosted mode worker");
    thread.start();
  }

  public void freeJsValue(int[] ids) {
    try {
      new FreeMessage(this, ids).send();
    } catch (IOException e) {
      // TODO(jat): error handling?
      e.printStackTrace();
      throw new HostedModeException("I/O error communicating with client");
    }
  }

  public ObjectsTable getJavaObjectsExposedInBrowser() {
    return javaObjectsInBrowser;
  }

  /**
   * @param ccl
   * @param jsthis
   * @param methodName
   * @param args
   * @param returnJsValue
   * @throws Throwable
   */
  public void invokeJavascript(CompilingClassLoader ccl, JsValueOOPHM jsthis,
      String methodName, JsValueOOPHM[] args, JsValueOOPHM returnJsValue)
      throws Throwable {
    final ObjectsTable remoteObjects = getJavaObjectsExposedInBrowser();
    Value vthis = convertFromJsValue(remoteObjects, jsthis);
    Value[] vargs = new Value[args.length];
    for (int i = 0; i < args.length; ++i) {
      vargs[i] = convertFromJsValue(remoteObjects, args[i]);
    }
    try {
      InvokeMessage invokeMessage = new InvokeMessage(this, methodName, vthis,
          vargs);
      invokeMessage.send();
      final ReturnMessage msg = reactToMessagesWhileWaitingForReturn(handler);
      Value returnValue = msg.getReturnValue();
      convertToJsValue(ccl, remoteObjects, returnValue, returnJsValue);
      if (msg.isException()) {
        if (returnValue.isNull() || returnValue.isUndefined()) {
          throw ModuleSpaceOOPHM.createJavaScriptException(ccl, null);

        } else if (returnValue.isString()) {
          throw ModuleSpaceOOPHM.createJavaScriptException(ccl,
              returnValue.getString());

        } else if (returnValue.isJsObject()) {
          Object jso = JsValueGlue.createJavaScriptObject(returnJsValue, ccl);
          throw ModuleSpaceOOPHM.createJavaScriptException(ccl, jso);

        } else if (returnValue.isJavaObject()) {
          Object object = remoteObjects.get(returnValue.getJavaObject().getRefid());
          Object target = ((JsValueOOPHM.DispatchObjectOOPHM) object).getTarget();
          if (target instanceof Throwable) {
            throw (Throwable) (target);
          } else {
            // JS throwing random Java Objects, which we'll wrap is JSException
            throw ModuleSpaceOOPHM.createJavaScriptException(ccl, target);
          }
        }
        // JS throwing random primitives, which we'll wrap is JSException
        throw ModuleSpaceOOPHM.createJavaScriptException(ccl,
            returnValue.getValue().toString());
      }
    } catch (IOException e) {
      // TODO(jat): error handling?
      e.printStackTrace();
      throw new HostedModeException("I/O error communicating with client");
    } catch (BrowserChannelException e) {
      // TODO(jat): error handling?
      e.printStackTrace();
      throw new HostedModeException("I/O error communicating with client");
    }
  }

  public void loadJsni(String jsni) {
    try {
      LoadJsniMessage jsniMessage = new LoadJsniMessage(this, jsni);
      jsniMessage.send();
      // we do not wait for a return value
    } catch (IOException e) {
      // TODO(jat): error handling?
      e.printStackTrace();
      throw new HostedModeException("I/O error communicating with client");
    }
  }

  public void run() {
    try {
      MessageType type = Message.readMessageType(getStreamFromOtherSide());
      assert type == MessageType.LoadModule;
      LoadModuleMessage message = LoadModuleMessage.receive(this);
      moduleName = message.getModuleName();
      userAgent = message.getUserAgent();
      Thread.currentThread().setName(
          "Hosting " + moduleName + " for" + userAgent);
      logger = handler.loadModule(logger, this, moduleName, userAgent);
      try {
        // send LoadModule response
        ReturnMessage.send(this, false, new Value());
        reactToMessages(handler);
      } finally {
        handler.unloadModule(this, moduleName);
      }
    } catch (IOException e) {
      logger.log(TreeLogger.WARN, "Client connection lost", e);
    } catch (BrowserChannelException e) {
      logger.log(TreeLogger.ERROR,
          "Unrecognized command for client; closing connection", e);
    } finally {
      try {
        shutdown();
      } catch (IOException ignored) {
      }
      endSession();
    }
  }

  public void shutdown() throws IOException {
    QuitMessage.send(this);
  }

  /**
   * Convert a JsValue into a BrowserChannel Value.
   * 
   * @param localObjects lookup table for local objects -- may be null if jsval
   *          is known to be a primitive (including String).
   * @param jsval value to convert
   * @return jsval as a Value object.
   */
  Value convertFromJsValue(ObjectsTable localObjects, JsValueOOPHM jsval) {
    Value value = new Value();
    if (jsval.isNull()) {
      value.setNull();
    } else if (jsval.isUndefined()) {
      value.setUndefined();
    } else if (jsval.isBoolean()) {
      value.setBoolean(jsval.getBoolean());
    } else if (jsval.isInt()) {
      value.setInt(jsval.getInt());
    } else if (jsval.isNumber()) {
      value.setDouble(jsval.getNumber());
    } else if (jsval.isString()) {
      value.setString(jsval.getString());
    } else if (jsval.isJavaScriptObject()) {
      value.setJsObject(jsval.getJavascriptObject());
    } else if (jsval.isWrappedJavaObject()) {
      assert localObjects != null;
      DispatchObject javaObj = jsval.getJavaObjectWrapper();
      value.setJavaObject(new JavaObjectRef(localObjects.add(javaObj)));
    } else if (jsval.isWrappedJavaFunction()) {
      assert localObjects != null;
      value.setJavaObject(new JavaObjectRef(
          localObjects.add(jsval.getWrappedJavaFunction())));
    } else {
      throw new RuntimeException("Unknown JsValue type " + jsval);
    }
    return value;
  }

  /**
   * Convert a BrowserChannel Value into a JsValue.
   * 
   * @param ccl Compiling class loader, may be null if val is known to not be a
   *          Java object or exception.
   * @param localObjects table of Java objects, may be null as above.
   * @param val Value to convert
   * @param jsval JsValue object to receive converted value.
   */
  void convertToJsValue(CompilingClassLoader ccl, ObjectsTable localObjects,
      Value val, JsValueOOPHM jsval) {
    switch (val.getType()) {
      case NULL:
        jsval.setNull();
        break;
      case BOOLEAN:
        jsval.setBoolean(val.getBoolean());
        break;
      case BYTE:
        jsval.setByte(val.getByte());
        break;
      case CHAR:
        jsval.setChar(val.getChar());
        break;
      case DOUBLE:
        jsval.setDouble(val.getDouble());
        break;
      case FLOAT:
        jsval.setDouble(val.getFloat());
        break;
      case INT:
        jsval.setInt(val.getInt());
        break;
      case LONG:
        jsval.setDouble(val.getLong());
        break;
      case SHORT:
        jsval.setShort(val.getShort());
        break;
      case STRING:
        jsval.setString(val.getString());
        break;
      case UNDEFINED:
        jsval.setUndefined();
        break;
      case JS_OBJECT:
        jsval.setJavascriptObject(val.getJsObject());
        break;
      case JAVA_OBJECT:
        assert ccl != null && localObjects != null;
        jsval.setWrappedJavaObject(ccl,
            localObjects.get(val.getJavaObject().getRefid()));
        break;
    }
  }
}
