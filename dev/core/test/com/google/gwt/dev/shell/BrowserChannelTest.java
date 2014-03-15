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

import com.google.gwt.dev.shell.BrowserChannel.CheckVersionsMessage;
import com.google.gwt.dev.shell.BrowserChannel.ChooseTransportMessage;
import com.google.gwt.dev.shell.BrowserChannel.FatalErrorMessage;
import com.google.gwt.dev.shell.BrowserChannel.FreeMessage;
import com.google.gwt.dev.shell.BrowserChannel.InvokeOnClientMessage;
import com.google.gwt.dev.shell.BrowserChannel.InvokeOnServerMessage;
import com.google.gwt.dev.shell.BrowserChannel.InvokeSpecialMessage;
import com.google.gwt.dev.shell.BrowserChannel.JavaObjectRef;
import com.google.gwt.dev.shell.BrowserChannel.LoadJsniMessage;
import com.google.gwt.dev.shell.BrowserChannel.LoadModuleMessage;
import com.google.gwt.dev.shell.BrowserChannel.MessageType;
import com.google.gwt.dev.shell.BrowserChannel.OldLoadModuleMessage;
import com.google.gwt.dev.shell.BrowserChannel.ProtocolVersionMessage;
import com.google.gwt.dev.shell.BrowserChannel.QuitMessage;
import com.google.gwt.dev.shell.BrowserChannel.RequestIconMessage;
import com.google.gwt.dev.shell.BrowserChannel.ReturnMessage;
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler.SpecialDispatchId;
import com.google.gwt.dev.shell.BrowserChannel.SwitchTransportMessage;
import com.google.gwt.dev.shell.BrowserChannel.UserAgentIconMessage;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.BrowserChannel.Value.ValueType;
import com.google.gwt.dev.util.TemporaryBufferStream;

import junit.framework.TestCase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Test for {@link BrowserChannel}.
 */
public class BrowserChannelTest extends TestCase {
  // TODO(jat): add more tests for Value types

  private TemporaryBufferStream bufferStream = new TemporaryBufferStream();

  private DataInputStream iStr = new DataInputStream(
      bufferStream.getInputStream());
  private DataOutputStream oStr = new DataOutputStream(
      bufferStream.getOutputStream());
  private TestBrowserChannel channel;

  public void testBooleanValue() throws IOException {
    Value val = new Value();
    val.setBoolean(true);
    channel.writeValue(oStr, val);
    val = channel.readValue(iStr);
    assertEquals(ValueType.BOOLEAN, val.getType());
    assertEquals(true, val.getBoolean());
    val.setBoolean(false);
    channel.writeValue(oStr, val);
    val = channel.readValue(iStr);
    assertEquals(ValueType.BOOLEAN, val.getType());
    assertEquals(false, val.getBoolean());
  }

  public void testCheckVersions() throws IOException, BrowserChannelException {
    int minVersion = BrowserChannel.PROTOCOL_VERSION_OLDEST;
    int maxVersion = BrowserChannel.PROTOCOL_VERSION_CURRENT;
    String hostedHtmlVersion = HostedHtmlVersion.EXPECTED_GWT_ONLOAD_VERSION;
    new CheckVersionsMessage(channel, minVersion, maxVersion,
        hostedHtmlVersion).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.CHECK_VERSIONS, type);
    CheckVersionsMessage message = CheckVersionsMessage.receive(channel);
    assertEquals(minVersion, message.getMinVersion());
    assertEquals(maxVersion, message.getMaxVersion());
    assertEquals(hostedHtmlVersion, message.getHostedHtmlVersion());
  }

  public void testChooseTransport() throws IOException,
      BrowserChannelException {
    String[] transports = new String[] { "shm" };
    new ChooseTransportMessage(channel, transports).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.CHOOSE_TRANSPORT, type);
    ChooseTransportMessage message = ChooseTransportMessage.receive(channel);
    String[] transportsRecv = message.getTransports();
    assertTrue(Arrays.equals(transports, transportsRecv));
  }

  public void testFatalErrorMessage() throws IOException,
      BrowserChannelException {
    String error = "Fatal error";
    new FatalErrorMessage(channel, error).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.FATAL_ERROR, type);
    FatalErrorMessage message = FatalErrorMessage.receive(channel);
    assertEquals(error, message.getError());
  }

  public void testFreeMessage() throws IOException, BrowserChannelException {
    int[] ids = new int[] { 42, 1024 };
    new FreeMessage(channel, ids).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.FREE_VALUE, type);
    FreeMessage message = FreeMessage.receive(channel);
    int[] idsRecv = message.getIds();
    assertTrue(Arrays.equals(ids, idsRecv));
  }

  public void testInvokeOnClientMessage() throws IOException,
      BrowserChannelException {
    String methodName = "fooMethod";
    Value thisRef = new Value();
    thisRef.setJavaObject(new JavaObjectRef(42));
    Value[] args = new Value[] {
      new Value(), new Value(),
    };
    args[0].setInt(0);
    args[1].setInt(1);
    new InvokeOnClientMessage(channel, methodName, thisRef, args).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.INVOKE, type);
    InvokeOnClientMessage message = InvokeOnClientMessage.receive(channel);
    assertEquals(methodName, message.getMethodName());
    Value thisRefRecv = message.getThis();
    assertEquals(ValueType.JAVA_OBJECT, thisRefRecv.getType());
    assertEquals(42, thisRefRecv.getJavaObject().getRefid());
    Value[] argsRecv = message.getArgs();
    assertEquals(2, argsRecv.length);
    for (int i = 0; i < 2; ++i) {
      assertEquals(ValueType.INT, argsRecv[i].getType());
      assertEquals(i, argsRecv[i].getInt());
    }
  }

  public void testInvokeOnServerMessage() throws IOException,
      BrowserChannelException {
    int methodId = -1;
    Value thisRef = new Value();
    thisRef.setJavaObject(new JavaObjectRef(42));
    Value[] args = new Value[] {
      new Value(), new Value(),
    };
    args[0].setInt(0);
    args[1].setInt(1);
    new InvokeOnServerMessage(channel, methodId, thisRef, args).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.INVOKE, type);
    InvokeOnServerMessage message = InvokeOnServerMessage.receive(channel);
    assertEquals(methodId, message.getMethodDispatchId());
    Value thisRefRecv = message.getThis();
    assertEquals(ValueType.JAVA_OBJECT, thisRefRecv.getType());
    assertEquals(42, thisRefRecv.getJavaObject().getRefid());
    Value[] argsRecv = message.getArgs();
    assertEquals(2, argsRecv.length);
    for (int i = 0; i < 2; ++i) {
      assertEquals(ValueType.INT, argsRecv[i].getType());
      assertEquals(i, argsRecv[i].getInt());
    }
  }

  public void testInvokeSpecialMessage() throws IOException,
      BrowserChannelException {
    Value[] args = new Value[] {
      new Value(), new Value(),
    };
    args[0].setInt(0);
    args[1].setInt(1);
    new InvokeSpecialMessage(channel, SpecialDispatchId.HasMethod, args).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.INVOKE_SPECIAL, type);
    InvokeSpecialMessage message = InvokeSpecialMessage.receive(channel);
    assertEquals(SpecialDispatchId.HasMethod, message.getDispatchId());
    Value[] argsRecv = message.getArgs();
    assertEquals(2, argsRecv.length);
    for (int i = 0; i < 2; ++i) {
      assertEquals(ValueType.INT, argsRecv[i].getType());
      assertEquals(i, argsRecv[i].getInt());
    }
  }

  public void testLoadJsniMessage() throws IOException,
      BrowserChannelException {
    String jsni = "function foo() { }";
    new LoadJsniMessage(channel, jsni).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.LOAD_JSNI, type);
    LoadJsniMessage message = LoadJsniMessage.receive(channel);
    assertEquals(jsni, message.getJsni());
  }

  public void testLoadModuleMessage() throws IOException,
      BrowserChannelException {
    String url = "http://www.google.com";
    String sessionKey = "asdkfjklAI*23ja";
    String tabKey = "372F4";
    String moduleName = "org.example.Hello";
    String userAgent = "Firefox";
    new LoadModuleMessage(channel, url, tabKey, sessionKey, moduleName,
        userAgent).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.LOAD_MODULE, type);
    LoadModuleMessage message = LoadModuleMessage.receive(channel);
    assertEquals(url, message.getUrl());
    assertEquals(tabKey, message.getTabKey());
    assertEquals(sessionKey, message.getSessionKey());
    assertEquals(moduleName, message.getModuleName());
    assertEquals(userAgent, message.getUserAgent());
    url = "https://www.google.com:8443/extra_stuff_that_is_really_long?foo";
    sessionKey = "asdfkasdjfkjaskldfjkajsfkjasdfjklaasdkfjklAI*23ja";
    tabKey = "";
    moduleName = "showcase";
    userAgent = "Safari";
    new LoadModuleMessage(channel, url, tabKey, sessionKey, moduleName,
        userAgent).send();
    type = channel.readMessageType();
    assertEquals(MessageType.LOAD_MODULE, type);
    message = LoadModuleMessage.receive(channel);
    assertEquals(url, message.getUrl());
    assertEquals(tabKey, message.getTabKey());
    assertEquals(sessionKey, message.getSessionKey());
    assertEquals(moduleName, message.getModuleName());
    assertEquals(userAgent, message.getUserAgent());

    // create a separate channel so we don't cause problems with partial
    // messages written to the stream
    TemporaryBufferStream tempBufferStream = new TemporaryBufferStream();
    TestBrowserChannel trashableChannel = new TestBrowserChannel(
        tempBufferStream.getInputStream(),
          tempBufferStream.getOutputStream());

    try {
      new LoadModuleMessage(trashableChannel, null, tabKey, sessionKey, moduleName,
          userAgent).send();
      fail("Expected exception with null url");
    } catch (AssertionError expected) {
      // If assertions are on.
    } catch (NullPointerException expected) {
      // If assertions are off.
    }

    try {
      new LoadModuleMessage(trashableChannel, url, null, sessionKey, moduleName,
          userAgent).send();
      fail("Expected exception with null tabKey");
    } catch (AssertionError expected) {
      // If assertions are on.
    } catch (NullPointerException expected) {
      // If assertions are off.
    }

    try {
      new LoadModuleMessage(trashableChannel, url, tabKey, null, moduleName,
          userAgent).send();
      fail("Expected exception with null sessionKey");
    } catch (AssertionError expected) {
      // If assertions are on.
    } catch (NullPointerException expected) {
      // If assertions are off.
    }

    try {
      new LoadModuleMessage(trashableChannel, url, tabKey, sessionKey, null,
          userAgent).send();
      fail("Expected exception with null moduleName");
    } catch (AssertionError expected) {
      // If assertions are on.
    } catch (NullPointerException expected) {
      // If assertions are off.
    }

    try {
      new LoadModuleMessage(trashableChannel, url, tabKey, sessionKey, moduleName,
          null).send();
      fail("Expected exception with null userAgent");
    } catch (AssertionError expected) {
      // If assertions are on.
    } catch (NullPointerException expected) {
      // If assertions are off.
    }
  }

  public void testOldLoadModuleMessage() throws IOException,
      BrowserChannelException {
    int protoVersion = 42;
    String moduleName = "org.example.Hello";
    String userAgent = "Firefox";
    new OldLoadModuleMessage(channel, protoVersion, moduleName,
        userAgent).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.OLD_LOAD_MODULE, type);
    OldLoadModuleMessage message = OldLoadModuleMessage.receive(channel);
    assertEquals(protoVersion, message.getProtoVersion());
    assertEquals(moduleName, message.getModuleName());
    assertEquals(userAgent, message.getUserAgent());
  }

  public void testProtocolVersionMessage() throws IOException,
      BrowserChannelException {
    int protoVersion = 42;
    new ProtocolVersionMessage(channel, protoVersion).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.PROTOCOL_VERSION, type);
    ProtocolVersionMessage message = ProtocolVersionMessage.receive(channel);
    assertEquals(protoVersion, message.getProtocolVersion());
  }

  public void testQuitMessage() throws IOException,
      BrowserChannelException {
    new QuitMessage(channel).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.QUIT, type);
    QuitMessage.receive(channel);
  }

  public void testRequestIconMessage() throws IOException,
      BrowserChannelException {
    RequestIconMessage.send(channel);
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.REQUEST_ICON, type);
    RequestIconMessage message = RequestIconMessage.receive(channel);
    assertFalse(message.isAsynchronous());
  }

  public void testReturnMessage() throws IOException,
      BrowserChannelException {
    Value val = new Value();
    val.setInt(42);
    new ReturnMessage(channel, false, val).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.RETURN, type);
    ReturnMessage message = ReturnMessage.receive(channel);
    assertFalse(message.isException());
    Value valRecv = message.getReturnValue();
    assertEquals(ValueType.INT, valRecv.getType());
    assertEquals(42, valRecv.getInt());
  }

  public void testSwitchTransportMessage() throws IOException,
      BrowserChannelException {
    String transport = "shm";
    String transportArgs = "17021";
    new SwitchTransportMessage(channel, transport, transportArgs).send();
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.SWITCH_TRANSPORT, type);
    SwitchTransportMessage message = SwitchTransportMessage.receive(channel);
    assertEquals(transport, message.getTransport());
    assertEquals(transportArgs, message.getTransportArgs());
  }

  public void testUserAgentIconMessage() throws IOException,
      BrowserChannelException {
    UserAgentIconMessage.send(channel, null);
    MessageType type = channel.readMessageType();
    assertEquals(MessageType.USER_AGENT_ICON, type);
    UserAgentIconMessage message = UserAgentIconMessage.receive(channel);
    assertFalse(message.isAsynchronous());
    assertNull(message.getIconBytes());

    UserAgentIconMessage.send(channel, new byte[0]);
    type = channel.readMessageType();
    assertEquals(MessageType.USER_AGENT_ICON, type);
    message = UserAgentIconMessage.receive(channel);
    assertFalse(message.isAsynchronous());
    assertNull(message.getIconBytes());

    byte[] bytes = new byte[] { 0, 1, 2, 3, 4 };
    UserAgentIconMessage.send(channel, bytes);
    type = channel.readMessageType();
    assertEquals(MessageType.USER_AGENT_ICON, type);
    message = UserAgentIconMessage.receive(channel);
    assertFalse(message.isAsynchronous());
    byte[] receivedBytes = message.getIconBytes();
    assertNotNull(receivedBytes);
    assertEquals(bytes.length, receivedBytes.length);
    for (int i = 0; i < bytes.length; ++i) {
      assertEquals(bytes[i], receivedBytes[i]);
    }
  }

  @Override
  protected void setUp() throws Exception {
    channel = new TestBrowserChannel(bufferStream.getInputStream(),
        bufferStream.getOutputStream());
  }
}
