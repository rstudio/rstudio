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
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler.ExceptionOrReturnValue;
import com.google.gwt.dev.shell.JsValue.DispatchObject;
import com.google.gwt.dev.util.log.dashboard.DashboardNotifier;
import com.google.gwt.dev.util.log.dashboard.DashboardNotifierFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Server-side of the browser channel protocol.
 */
public class BrowserChannelServer extends BrowserChannel
    implements Runnable {

  /**
   * Hook interface for responding to messages from the client.
   */
  public abstract static class SessionHandlerServer extends SessionHandler<BrowserChannelServer> {
    public abstract ExceptionOrReturnValue getProperty(BrowserChannelServer channel,
        int refId, int dispId);

    public abstract ExceptionOrReturnValue invoke(BrowserChannelServer channel,
        Value thisObj, int dispId, Value[] args);

    /**
     * Load a new instance of a module.
     *
     * @param channel
     * @param moduleName
     * @param userAgent
     * @param url top-level URL of the main page, null if using an old plugin
     * @param tabKey opaque key of the tab, may be empty if the plugin can't
     *     distinguish tabs or null if using an old plugin
     * @param sessionKey opaque key for this session, null if using an old plugin
     * @param userAgentIcon byte array containing an icon (which fits within
     *     24x24) representing the user agent or null if unavailable
     * @return a TreeLogger to use for the module's logs, or null if the module
     *     load failed
     */
    public abstract TreeLogger loadModule(BrowserChannelServer channel,
        String moduleName, String userAgent, String url, String tabKey,
        String sessionKey, byte[] userAgentIcon);

    public abstract ExceptionOrReturnValue setProperty(BrowserChannelServer channel,
        int refId, int dispId, Value newValue);

    public abstract void unloadModule(BrowserChannelServer channel, String moduleName);
  }

  private static class ServerObjectRefFactory implements ObjectRefFactory {

    private final RemoteObjectTable<JsObjectRef> remoteObjectTable;

    public ServerObjectRefFactory() {
      remoteObjectTable = new RemoteObjectTable<JsObjectRef>();
    }

    @Override
    public JavaObjectRef getJavaObjectRef(int refId) {
      return new JavaObjectRef(refId);
    }

    @Override
    public JsObjectRef getJsObjectRef(int refId) {
      JsObjectRef objectRef = remoteObjectTable.getRemoteObjectRef(refId);
      if (objectRef == null) {
        objectRef = new JsObjectRef(refId);
        remoteObjectTable.putRemoteObjectRef(refId, objectRef);
      }
      return objectRef;
    }

    @Override
    public Set<Integer> getRefIdsForCleanup() {
      return remoteObjectTable.getRefIdsForCleanup();
    }
  }

  /**
   * Full qualified class name of JavaScriptObject.  This must be a string
   * because this class is in a different class loader.
   */
  public static final String JSO_CLASS = "com.google.gwt.core.client.JavaScriptObject";

  private static Map<String, byte[]> iconCache = new HashMap<String, byte[]>();

  private static final Object cacheLock = new Object();

  private DevModeSession devModeSession;

  private final SessionHandlerServer handler;

  private final boolean ignoreRemoteDeath;

  private final ServerObjectsTable javaObjectsInBrowser = new ServerObjectsTable();

  private TreeLogger logger;

  private String moduleName;

  private String userAgent;

  private int protocolVersion = -1;

  /**
   * Create a code server for the supplied socket.
   *
   * @param initialLogger
   * @param socket
   * @param handler
   * @param ignoreRemoteDeath
   * @throws IOException
   */
  public BrowserChannelServer(TreeLogger initialLogger, Socket socket,
      SessionHandlerServer handler, boolean ignoreRemoteDeath) throws IOException {
    super(socket, new ServerObjectRefFactory());
    this.handler = handler;
    this.ignoreRemoteDeath = ignoreRemoteDeath;
    init(initialLogger);
  }

  // @VisibleForTesting
  BrowserChannelServer(TreeLogger initialLogger, InputStream inputStream,
      OutputStream outputStream, SessionHandlerServer handler,
      boolean ignoreRemoteDeath) {
    super(inputStream, outputStream, new ServerObjectRefFactory());
    this.handler = handler;
    this.ignoreRemoteDeath = ignoreRemoteDeath;
    init(initialLogger);
  }

  /**
   * Indicate that Java no longer has references to the supplied JS objects.
   *
   * @param ids array of JS object IDs that have been freeded
   */
  public void freeJsValue(int[] ids) {
    try {
      new FreeMessage(this, ids).send();
    } catch (IOException e) {
      // TODO(jat): error handling?
      e.printStackTrace();
      throw new HostedModeException("I/O error communicating with client");
    }
  }

  /**
   * Returns the {@code DevModeSession} representing this browser connection.
   */
  public DevModeSession getDevModeSession() {
    return devModeSession;
  }

  /**
   * @return the table of Java objects which have been sent to the browser.
   */
  public ServerObjectsTable getJavaObjectsExposedInBrowser() {
    return javaObjectsInBrowser;
  }

  /**
   * @return the negotiated protocol version, or -1 if not yet negotiated.
   */
  public int getProtocolVersion() {
    return protocolVersion;
  }

  public ReturnMessage invoke(String methodName, Value vthis, Value[] vargs,
      SessionHandlerServer handler) throws IOException, BrowserChannelException {
    new InvokeOnClientMessage(this, methodName, vthis, vargs).send();
    return reactToMessagesWhileWaitingForReturn(handler);
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
    final ServerObjectsTable remoteObjects = getJavaObjectsExposedInBrowser();
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
        Object exceptionValue;
        if (returnValue.isNull() || returnValue.isUndefined()) {
          exceptionValue = null;
        } else if (returnValue.isString()) {
          exceptionValue = returnValue.getString();
        } else if (returnValue.isJsObject()) {
          exceptionValue = JsValueGlue.createJavaScriptObject(returnJsValue,
              ccl);
        } else if (returnValue.isJavaObject()) {
          Object object = remoteObjects.get(returnValue.getJavaObject().getRefid());
          Object target = ((JsValueOOPHM.DispatchObjectOOPHM) object).getTarget();
          if (target instanceof Throwable) {
            throw (Throwable) (target);
          } else {
            // JS throwing random Java Objects, which we'll wrap in JSException
            exceptionValue = target;
          }
        } else {
          // JS throwing random primitives, which we'll wrap as a string in
          // JSException
          exceptionValue = returnValue.getValue().toString();
        }
        RuntimeException exception = ModuleSpace.createJavaScriptException(ccl,
            exceptionValue, methodName + "(" + Arrays.toString(args) + ")");
        // reset the stack trace to here to minimize GWT infrastructure in
        // the stack trace
        exception.fillInStackTrace();
        throw exception;
      }
    } catch (IOException e) {
      throw new RemoteDeathError(e);
    } catch (BrowserChannelException e) {
      throw new RemoteDeathError(e);
    }
  }

  /**
   * Load the supplied JSNI code into the browser.
   *
   * @param jsni JSNI source to load into the browser
   */
  public void loadJsni(String jsni) {
    try {
      LoadJsniMessage jsniMessage = new LoadJsniMessage(this, jsni);
      jsniMessage.send();
      // we do not wait for a return value
    } catch (IOException e) {
      throw new RemoteDeathError(e);
    }
  }

  /**
   * React to messages from the other side, where no return value is expected.
   *
   * @param handler
   * @throws RemoteDeathError
   */
  public void reactToMessages(SessionHandlerServer handler) {
    do {
      try {
        getStreamToOtherSide().flush();
        MessageType messageType = Message.readMessageType(
            getStreamFromOtherSide());
        switch (messageType) {
          case FREE_VALUE:
            final FreeMessage freeMsg = FreeMessage.receive(this);
            handler.freeValue(this, freeMsg.getIds());
            break;
          case INVOKE:
            InvokeOnServerMessage imsg = InvokeOnServerMessage.receive(this);
            ExceptionOrReturnValue result = handler.invoke(this, imsg.getThis(),
                imsg.getMethodDispatchId(), imsg.getArgs());
            sendFreedValues();
            ReturnMessage.send(this, result);
            break;
          case INVOKE_SPECIAL:
            handleInvokeSpecial(handler);
            break;
          case QUIT:
            return;
          default:
            throw new RemoteDeathError(new BrowserChannelException(
                "Invalid message type " + messageType));
        }
      } catch (IOException e) {
        throw new RemoteDeathError(e);
      } catch (BrowserChannelException e) {
        throw new RemoteDeathError(e);
      }
    } while (true);
  }

  /**
   * React to messages from the other side, where a return value is expected.
   *
   * @param handler
   * @throws BrowserChannelException
   * @throws RemoteDeathError
   */
  public ReturnMessage reactToMessagesWhileWaitingForReturn(
      SessionHandlerServer handler) throws BrowserChannelException, RemoteDeathError {
    do {
      try {
        getStreamToOtherSide().flush();
        MessageType messageType = Message.readMessageType(
            getStreamFromOtherSide());
        switch (messageType) {
          case FREE_VALUE:
            final FreeMessage freeMsg = FreeMessage.receive(this);
            handler.freeValue(this, freeMsg.getIds());
            break;
          case RETURN:
            return ReturnMessage.receive(this);
          case INVOKE:
            InvokeOnServerMessage imsg = InvokeOnServerMessage.receive(this);
            ExceptionOrReturnValue result = handler.invoke(this, imsg.getThis(),
                imsg.getMethodDispatchId(), imsg.getArgs());
            sendFreedValues();
            ReturnMessage.send(this, result);
            break;
          case INVOKE_SPECIAL:
            handleInvokeSpecial(handler);
            break;
          case QUIT:
            // if we got an unexpected QUIT here, the remote plugin probably
            // realized it was dying and had time to close the socket properly.
            throw new RemoteDeathError(null);
          default:
            throw new BrowserChannelException("Invalid message type "
                + messageType + " received waiting for return.");
        }
      } catch (IOException e) {
        throw new RemoteDeathError(e);
      } catch (BrowserChannelException e) {
        throw new RemoteDeathError(e);
      }
    } while (true);
  }

  @Override
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

  /**
   * Close the connection to the browser.
   *
   * @throws IOException
   */
  public void shutdown() throws IOException {
    getDashboardNotifier().devModeSessionEnd(devModeSession);
    QuitMessage.send(this);
  }

  // @VisibleForTesting
  protected void processConnection() throws IOException, BrowserChannelException {
    MessageType type = Message.readMessageType(getStreamFromOtherSide());
    // TODO(jat): add support for getting the a shim plugin downloading the
    //    real plugin via a GetRealPlugin message before CheckVersions
    String url = null;
    String tabKey = null;
    String sessionKey = null;
    byte[] iconBytes = null;
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
        protocolVersion = 1;
        HelpInfo helpInfo = new HelpInfo() {
          @Override
          public String getAnchorText() {
            return "UsingOOPHM wiki page";
          }

          @Override
          public URL getURL() {
            try {
              // TODO(jat): better landing page for more info
              return new URL(
                  "http://code.google.com/p/google-web-toolkit/wiki/UsingOOPHM");
            } catch (MalformedURLException e) {
              // can't happen
              return null;
            }
          }
        };
        logger.log(TreeLogger.WARN, "Connection from old browser plugin -- "
            + "please upgrade to a later version for full functionality", null,
            helpInfo);
        break;
      case CHECK_VERSIONS:
        String connectError = null;
        CheckVersionsMessage hello = CheckVersionsMessage.receive(this);
        int minVersion = hello.getMinVersion();
        int maxVersion = hello.getMaxVersion();
        String hostedHtmlVersion = hello.getHostedHtmlVersion();
        if (minVersion > PROTOCOL_VERSION_CURRENT
            || maxVersion < PROTOCOL_VERSION_OLDEST) {
          connectError = "Client supported protocol version range "
              + minVersion + " - " + maxVersion + "; server "
              + PROTOCOL_VERSION_OLDEST + " - " + PROTOCOL_VERSION_CURRENT;
        } else {
          if (!HostedHtmlVersion.validHostedHtmlVersion(logger,
              hostedHtmlVersion)) {
            new FatalErrorMessage(this,
                "Invalid hosted.html version - check log window").send();
            return;
          }
        }
        if (connectError != null) {
          logger.log(TreeLogger.ERROR, "Connection error: " + connectError,
              null);
          new FatalErrorMessage(this, connectError).send();
          return;
        }
        protocolVersion = Math.min(PROTOCOL_VERSION_CURRENT, maxVersion);
        new ProtocolVersionMessage(this, protocolVersion).send();
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
        tabKey = loadModule.getTabKey();
        sessionKey = loadModule.getSessionKey();
        moduleName = loadModule.getModuleName();
        userAgent = loadModule.getUserAgent();
        break;
      case REQUEST_PLUGIN:
        logger.log(TreeLogger.ERROR, "Plugin download not supported yet");
        // We can't clear the socket since we don't know how to interpret this
        // message yet -- it is only here now so we can give a better error
        // message with mixed versions once it is supported.
        new FatalErrorMessage(this, "Plugin download not supported").send();
        return;
      default:
        logger.log(TreeLogger.ERROR, "Unexpected message type " + type
            + "; expecting CheckVersions");
        return;
    }
    if (protocolVersion >= PROTOCOL_VERSION_GET_ICON) {
      synchronized (cacheLock) {
        if (iconCache.containsKey(userAgent)) {
          iconBytes = iconCache.get(userAgent);
        } else {
          RequestIconMessage.send(this);
          type = Message.readMessageType(getStreamFromOtherSide());
          if (type != MessageType.USER_AGENT_ICON) {
            logger.log(TreeLogger.ERROR, "Unexpected message type " + type
                + "; expecting UserAgentIcon");
            return;
          }
          UserAgentIconMessage uaIconMessage = UserAgentIconMessage.receive(
              this);
          iconBytes = uaIconMessage.getIconBytes();
          iconCache.put(userAgent, iconBytes);
        }
      }
    }
    Thread.currentThread().setName(
        "Code server for " + moduleName + " from " + userAgent + " on " + url
        + " @ " + sessionKey);

    createDevModeSession();

    logger = handler.loadModule(this, moduleName, userAgent, url,
        tabKey, sessionKey, iconBytes);
    if (logger == null) {
      // got an error
      try {
        Value errMsg = new Value();
        errMsg.setString("An error occurred loading the GWT module "
            + moduleName);
        ReturnMessage.send(this, true, errMsg);
        return;
      } catch (IOException e) {
        throw new RemoteDeathError(e);
      }
    }
    try {
      // send LoadModule response
      try {
        ReturnMessage.send(this, false, new Value());
      } catch (IOException e) {
        throw new RemoteDeathError(e);
      }
      reactToMessages(handler);
    } catch (RemoteDeathError e) {
      if (!ignoreRemoteDeath) {
        logger.log(TreeLogger.ERROR, e.getMessage(), e);
      }
    } finally {
      handler.unloadModule(this, moduleName);
    }
  }

  /**
   * Convert a JsValue into a BrowserChannel Value.
   *
   * @param localObjects lookup table for local objects -- may be null if jsval
   *          is known to be a primitive (including String).
   * @param jsval value to convert
   * @return jsval as a Value object.
   */
  Value convertFromJsValue(ServerObjectsTable localObjects, JsValueOOPHM jsval) {
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
  void convertToJsValue(CompilingClassLoader ccl, ServerObjectsTable localObjects,
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
      case INT:
        jsval.setInt(val.getInt());
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
   * Returns the {@code DashboardNotifier} used to send notices to a dashboard
   * service.
   */
  // @VisibleForTesting
  DashboardNotifier getDashboardNotifier() {
    return DashboardNotifierFactory.getNotifier();
  }

  /**
   * Creates the {@code DevModeSession} that represents the current browser
   * connection, sets it as the "default" session for the current thread, and
   * notifies a GWT Dashboard.
   */
  private void createDevModeSession() {
    devModeSession = new DevModeSession(moduleName, userAgent);
    DevModeSession.setSessionForCurrentThread(devModeSession);
    getDashboardNotifier().devModeSessionBegin(devModeSession);
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

  private void handleInvokeSpecial(SessionHandlerServer handler) throws IOException,
      BrowserChannelException {
    final InvokeSpecialMessage ismsg = InvokeSpecialMessage.receive(this);
    Value[] args = ismsg.getArgs();
    ExceptionOrReturnValue retExc = null;
    switch (ismsg.getDispatchId()) {
      case GetProperty:
        assert args.length == 2;
        retExc = handler.getProperty(this, args[0].getInt(), args[1].getInt());
        break;
      case SetProperty:
        assert args.length == 3;
        retExc = handler.setProperty(this, args[0].getInt(), args[1].getInt(),
            args[2]);
        break;
      default:
        throw new HostedModeException("Unexpected InvokeSpecial method "
            + ismsg.getDispatchId());
    }
    ReturnMessage.send(this, retExc);
  }

  private void init(TreeLogger initialLogger) {
    this.logger = initialLogger;
    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.setName("Code server (initializing)");
    thread.start();
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
