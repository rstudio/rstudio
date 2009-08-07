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
public final class BrowserChannelServer extends BrowserChannel
    implements Runnable {

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
      InvokeOnClientMessage invokeMessage = new InvokeOnClientMessage(this,
          methodName, vthis, vargs);
      invokeMessage.send();
      final ReturnMessage msg = reactToMessagesWhileWaitingForReturn(handler);
      Value returnValue = msg.getReturnValue();
      convertToJsValue(ccl, remoteObjects, returnValue, returnJsValue);
      if (msg.isException()) {
        if (returnValue.isNull() || returnValue.isUndefined()) {
          throw ModuleSpace.createJavaScriptException(ccl, null);

        } else if (returnValue.isString()) {
          throw ModuleSpace.createJavaScriptException(ccl,
              returnValue.getString());

        } else if (returnValue.isJsObject()) {
          Object jso = JsValueGlue.createJavaScriptObject(returnJsValue, ccl);
          throw ModuleSpace.createJavaScriptException(ccl, jso);

        } else if (returnValue.isJavaObject()) {
          Object object = remoteObjects.get(returnValue.getJavaObject().getRefid());
          Object target = ((JsValueOOPHM.DispatchObjectOOPHM) object).getTarget();
          if (target instanceof Throwable) {
            throw (Throwable) (target);
          } else {
            // JS throwing random Java Objects, which we'll wrap is JSException
            throw ModuleSpace.createJavaScriptException(ccl, target);
          }
        }
        // JS throwing random primitives, which we'll wrap is JSException
        throw ModuleSpace.createJavaScriptException(ccl,
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
      processConnection();
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

  /**
   * Create the requested transport and return the appropriate information so
   * the client can connect to the same transport.
   * 
   * @param transport transport name to create
   * @return transport-specific arguments for the client to use in attaching
   *     to this transport
   */
  private String createTransport(String transport) {
    // TODO(jat): implement support for additional transports
    throw new UnsupportedOperationException(
        "No alternate transports supported");
  }

  private void processConnection() throws IOException, BrowserChannelException {
    MessageType type = Message.readMessageType(getStreamFromOtherSide());
    // TODO(jat): add support for getting the a shim plugin downloading the
    //    real plugin via a GetRealPlugin message before CheckVersions
    String url = null;
    String sessionKey = null;
    switch (type) {
      case OLD_LOAD_MODULE:
        // v1 client
        OldLoadModuleMessage oldLoadModule = OldLoadModuleMessage.receive(this);
        if (oldLoadModule.getProtoVersion() != 1) {
          // This message type was only used in v1, so something is really
          // broken here.
          throw new BrowserChannelException(
              "Old LoadModule message used, but not v1 protocol");
        }
        moduleName = oldLoadModule.getModuleName();
        userAgent = oldLoadModule.getUserAgent();
        break;
      case CHECK_VERSIONS:
        String connectError = null;
        CheckVersionsMessage hello = CheckVersionsMessage.receive(this);
        int minVersion = hello.getMinVersion();
        int maxVersion = hello.getMaxVersion();
        String hostedHtmlVersion = hello.getHostedHtmlVersion();
        if (minVersion > BROWSERCHANNEL_PROTOCOL_VERSION
            || maxVersion < BROWSERCHANNEL_PROTOCOL_VERSION) {
          connectError = "No supported protocol version in range " + minVersion
          + " - " + maxVersion;
        }
        // TODO(jat): verify hosted.html version
        if (connectError != null) {
          logger.log(TreeLogger.ERROR, "Connection error " + connectError, null);
          new FatalErrorMessage(this, connectError).send();
          return;
        }
        new ProtocolVersionMessage(this, BROWSERCHANNEL_PROTOCOL_VERSION).send();
        type = Message.readMessageType(getStreamFromOtherSide());
        
        // Optionally allow client to request switch of transports.  Inband is
        // always supported, so a return of an empty transport string requires
        // the client to stay in this channel.
        if (type == MessageType.CHOOSE_TRANSPORT) {
          ChooseTransportMessage chooseTransport = ChooseTransportMessage.receive(this);
          String transport = selectTransport(chooseTransport.getTransports());
          String transportArgs = null;
          if (transport != null) {
            transportArgs = createTransport(transport);
          }
          new SwitchTransportMessage(this, transport, transportArgs).send();
          type = Message.readMessageType(getStreamFromOtherSide());
        }
        
        // Now we expect a LoadModule message to load a GWT module.
        if (type != MessageType.LOAD_MODULE) {
          logger.log(TreeLogger.ERROR, "Unexpected message type " + type
              + "; expecting LoadModule");
          return;
        }
        LoadModuleMessage loadModule = LoadModuleMessage.receive(this);
        url = loadModule.getUrl();
        sessionKey = loadModule.getSessionKey();
        moduleName = loadModule.getModuleName();
        userAgent = loadModule.getUserAgent();
        break;
      default:
        logger.log(TreeLogger.ERROR, "Unexpected message type " + type
            + "; expecting CheckVersions");
        return;
    }
    Thread.currentThread().setName(
        "Hosting " + moduleName + " for " + userAgent + " on " + url + " @ "
        + sessionKey);
    logger = handler.loadModule(logger, this, moduleName, userAgent, url,
        sessionKey);
    try {
      // send LoadModule response
      ReturnMessage.send(this, false, new Value());
      reactToMessages(handler);
    } finally {
      handler.unloadModule(this, moduleName);
    }
  }

  /**
   * Select a transport from those provided by the client.
   * 
   * @param transports array of supported transports
   * @return null to continue in-band, or a transport type
   */
  private String selectTransport(String[] transports) {
    // TODO(jat): add support for shared memory, others
    return null;
  }
}
