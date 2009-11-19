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
import com.google.gwt.dev.shell.BrowserChannel.CheckVersionsMessage;
import com.google.gwt.dev.shell.BrowserChannel.LoadModuleMessage;
import com.google.gwt.dev.shell.BrowserChannel.MessageType;
import com.google.gwt.dev.shell.BrowserChannel.OldLoadModuleMessage;
import com.google.gwt.dev.shell.BrowserChannel.ProtocolVersionMessage;
import com.google.gwt.dev.shell.BrowserChannel.QuitMessage;
import com.google.gwt.dev.shell.BrowserChannel.RequestIconMessage;
import com.google.gwt.dev.shell.BrowserChannel.ReturnMessage;
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler;
import com.google.gwt.dev.shell.BrowserChannel.UserAgentIconMessage;
import com.google.gwt.dev.shell.BrowserChannel.Value;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Semaphore;

/**
 * Test that the protocol startup of BrowserChannelworks for all supported
 * protocol versions.
 */
public class BrowserChannelServerTest extends TestCase {

  /**
   * A BrowserChannelServer that can notify when the connection is closed.
   */
  private class TestBrowserChannelServer extends BrowserChannelServer {

    private final Semaphore finishNotify = new Semaphore(0);

    public TestBrowserChannelServer(TreeLogger logger,
        InputStream inputStream, OutputStream outputStream,
        SessionHandler handler) throws IOException {
      super(logger, inputStream, outputStream, handler, true);
    }

    @Override
    protected void processConnection() throws IOException,
        BrowserChannelException {
      super.processConnection();
      finishNotify.release();
    }
    
    public void waitForClose() throws InterruptedException {
      finishNotify.acquire();
    }
  }

  /**
   * Maintains a connected pair of piped streams.
   */
  private class PipedStreamPair {

    private final PipedOutputStream output;
    private final PipedInputStream input;
    
    public PipedStreamPair() {
      PipedOutputStream out = null;
      PipedInputStream in = null;
      try {
        out = new PipedOutputStream();
        in = new PipedInputStream(out);
      } catch (IOException e) {        
      }
      output = out;
      input = in;
    }
    
    public PipedInputStream getInputStream() {
      return input;
    }
    
    public PipedOutputStream getOutputStream() {
      return output;
    }
  }

  /**
   * A SessionHandler which keeps track of parameters from the LoadModule
   * message, but mocks out everything else.
   */
  private static class TestSessionHandler extends SessionHandler {

    private String loadedModule;
    private String userAgent;
    private String url;
    private String tabKey;
    private String sessionKey;
    private byte[] userAgentIcon;
    private String moduleName;

    @Override
    public void freeValue(BrowserChannel channel, int[] ids) {
    }

    public String getLoadedModule() {
      return loadedModule;
    }

    public String getModuleName() {
      return moduleName;
    }

    @Override
    public ExceptionOrReturnValue getProperty(BrowserChannel channel,
        int refId, int dispId) {
      return new ExceptionOrReturnValue(false, new Value());
    }

    public String getSessionKey() {
      return sessionKey;
    }

    public String getTabKey() {
      return tabKey;
    }

    public String getUrl() {
      return url;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public byte[] getUserAgentIcon() {
      return userAgentIcon;
    }

    @Override
    public ExceptionOrReturnValue invoke(BrowserChannel channel, Value thisObj,
        int dispId, Value[] args) {
      return new ExceptionOrReturnValue(false, new Value());
    }

    @Override
    public TreeLogger loadModule(BrowserChannel channel, String moduleName,
        String userAgent, String url, String tabKey, String sessionKey,
        byte[] userAgentIcon) {
      loadedModule = moduleName;
      this.moduleName = moduleName;
      this.userAgent = userAgent;
      this.url = url;
      this.tabKey = tabKey;
      this.sessionKey = sessionKey;
      this.userAgentIcon = userAgentIcon;
      return new FailErrorLogger();
    }

    @Override
    public ExceptionOrReturnValue setProperty(BrowserChannel channel,
        int refId, int dispId, Value newValue) {
      return new ExceptionOrReturnValue(false, new Value());
    }

    @Override
    public void unloadModule(BrowserChannel channel, String moduleName) {
      loadedModule = null;
    }   
  }

  private PipedStreamPair clientToServer = new PipedStreamPair();
  private PipedStreamPair serverToClient = new PipedStreamPair();

  /**
   * Test a version 1 client interacting with the server.
   * 
   * @throws IOException 
   * @throws BrowserChannelException 
   * @throws InterruptedException 
   */
  public void testVersion1() throws IOException, BrowserChannelException,
      InterruptedException {
    TestSessionHandler handler = new TestSessionHandler();
    TestBrowserChannelServer server = new TestBrowserChannelServer(
        new FailErrorLogger(), clientToServer.getInputStream(),
        serverToClient.getOutputStream(), handler);
    TestBrowserChannel client = new TestBrowserChannel(
        serverToClient.getInputStream(), clientToServer.getOutputStream());
    new OldLoadModuleMessage(client, 1, "testModule", "userAgent").send();
    MessageType type = client.readMessageType();
    assertEquals("testModule", handler.getModuleName());
    assertEquals("userAgent", handler.getUserAgent());
    assertNull(handler.getUrl());
    assertNull(handler.getTabKey());
    assertNull(handler.getSessionKey());
    assertNull(handler.getUserAgentIcon());
    assertEquals(MessageType.RETURN, type);
    ReturnMessage.receive(client);
    QuitMessage.send(client);
    server.waitForClose();
    assertNull(handler.getLoadedModule());
  }

  /**
   * Test a version 2 client interacting with the server.
   * 
   * @throws IOException 
   * @throws BrowserChannelException 
   * @throws InterruptedException 
   */
  public void testVersion2() throws IOException, BrowserChannelException,
      InterruptedException {
    TestSessionHandler handler = new TestSessionHandler();
    TestBrowserChannelServer server = new TestBrowserChannelServer(
        new FailErrorLogger(), clientToServer.getInputStream(),
        serverToClient.getOutputStream(), handler);
    TestBrowserChannel client = new TestBrowserChannel(
        serverToClient.getInputStream(), clientToServer.getOutputStream());
    new CheckVersionsMessage(client, 2, 2,
        HostedHtmlVersion.EXPECTED_GWT_ONLOAD_VERSION).send();
    MessageType type = client.readMessageType();
    assertEquals(MessageType.PROTOCOL_VERSION, type);
    ProtocolVersionMessage protocolMessage = ProtocolVersionMessage.receive(
        client);
    assertEquals(2, protocolMessage.getProtocolVersion());
    new LoadModuleMessage(client, "url", "tabkey", "session", "testModule",
        "userAgent").send();
    type = client.readMessageType();
    assertEquals("testModule", handler.getModuleName());
    assertEquals("userAgent", handler.getUserAgent());
    assertEquals("url", handler.getUrl());
    assertEquals("tabkey", handler.getTabKey());
    assertEquals("session", handler.getSessionKey());
    assertNull(handler.getUserAgentIcon());
    assertEquals(MessageType.RETURN, type);
    ReturnMessage.receive(client);
    QuitMessage.send(client);
    server.waitForClose();
    assertNull(handler.getLoadedModule());
  }

  /**
   * Test a version 3 client interacting with the server.
   * 
   * @throws IOException 
   * @throws BrowserChannelException 
   * @throws InterruptedException 
   */
  public void testVersion3() throws IOException, BrowserChannelException,
      InterruptedException {
    TestSessionHandler handler = new TestSessionHandler();
    TestBrowserChannelServer server = new TestBrowserChannelServer(
        new FailErrorLogger(), clientToServer.getInputStream(),
        serverToClient.getOutputStream(), handler);
    TestBrowserChannel client = new TestBrowserChannel(
        serverToClient.getInputStream(), clientToServer.getOutputStream());
    new CheckVersionsMessage(client, 2, 3,
        HostedHtmlVersion.EXPECTED_GWT_ONLOAD_VERSION).send();
    MessageType type = client.readMessageType();
    assertEquals(MessageType.PROTOCOL_VERSION, type);
    ProtocolVersionMessage protocolMessage = ProtocolVersionMessage.receive(
        client);
    assertEquals(3, protocolMessage.getProtocolVersion());
    new LoadModuleMessage(client, "url", "tabkey", "session", "testModule",
        "userAgent").send();
    type = client.readMessageType();
    byte[] iconBytes = null;
    if (type == MessageType.REQUEST_ICON) {
      RequestIconMessage.receive(client);
      iconBytes = new byte[] { 0, 1, 2, 3, 4, 5 };
      UserAgentIconMessage.send(client, iconBytes);
      type = client.readMessageType();
    }
    assertEquals("testModule", handler.getModuleName());
    assertEquals("userAgent", handler.getUserAgent());
    assertEquals("url", handler.getUrl());
    assertEquals("tabkey", handler.getTabKey());
    assertEquals("session", handler.getSessionKey());
    byte[] receivedIcon = handler.getUserAgentIcon();
    assertNotNull(receivedIcon);
    assertEquals(iconBytes.length, receivedIcon.length);
    for (int i = 0; i < iconBytes.length; ++i) {
      assertEquals(iconBytes[i], receivedIcon[i]);
    }
    assertEquals(MessageType.RETURN, type);
    ReturnMessage.receive(client);
    QuitMessage.send(client);
    server.waitForClose();
    assertNull(handler.getLoadedModule());
  }
}
