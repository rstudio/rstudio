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

import com.google.gwt.dev.shell.BrowserChannel.SessionHandler.ExceptionOrReturnValue;
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler.SpecialDispatchId;
import com.google.gwt.dev.shell.BrowserChannel.Value.ValueType;
import com.google.gwt.util.tools.Utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Set;

/**
 * A base class for communication between Dev-mode client and server.
 */
public abstract class BrowserChannel {

  /**
   * An error indicating that the remote side died and we should unroll the
   * call stack as painlessly as possible to allow cleanup.
   */
  public static class RemoteDeathError extends Error {

    public RemoteDeathError(Throwable cause) {
      super("Remote connection lost", cause);
    }
  }

  /**
   * Class representing a reference to a Java object.
   */
  public static class JavaObjectRef implements RemoteObjectRef {
    private int refId;

    public JavaObjectRef(int refId) {
      this.refId = refId;
    }

    @Override
    public int getRefid() {
      return Math.abs(refId);
    }

    @Override
    public int hashCode() {
      return refId;
    }

    public boolean isException() {
      return refId < 0;
    }

    @Override
    public String toString() {
      return "JavaObjectRef(ref=" + refId + ")";
    }
  }

  /**
   * Class representing a reference to a JS object.
   */
  public static class JsObjectRef implements RemoteObjectRef  {

    private int refId;

    public JsObjectRef(int refId) {
      this.refId = refId;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof JsObjectRef) && ((JsObjectRef) o).refId == refId;
    }

    @Override
    public int getRefid() {
      // exceptions are negative, so we get the absolute value
      return Math.abs(refId);
    }

    @Override
    public int hashCode() {
      return refId;
    }

    public boolean isException() {
      return refId < 0;
    }

    @Override
    public String toString() {
      return "JsObjectRef(" + refId + ")";
    }
  }

  /**
   * Enumeration of message type ids.
   *
   * <p>Ids are used instead of relying on the ordinal to avoid sychronization
   * problems with the client.
   */
  public enum MessageType {
    /**
     * A message to invoke a method on the other side of the wire.  Note that
     * the messages are asymmetric -- see {@link InvokeOnClientMessage} and
     * {@link InvokeOnServerMessage}.
     */
    INVOKE(0),

    /**
     * Returns the result of an INVOKE, INVOKE_SPECIAL, or LOAD_MODULE message.
     */
    RETURN(1),

    /**
     * v1 LOAD_MODULE message.
     */
    OLD_LOAD_MODULE(2),

    /**
     * Normal closure of the connection.
     */
    QUIT(3),

    /**
     * A request by the server to load JSNI source into the client's JS engine.
     */
    LOAD_JSNI(4),

    INVOKE_SPECIAL(5),

    FREE_VALUE(6),

    /**
     * Abnormal termination of the connection.
     */
    FATAL_ERROR(7),

    CHECK_VERSIONS(8),

    PROTOCOL_VERSION(9),

    CHOOSE_TRANSPORT(10),

    SWITCH_TRANSPORT(11),

    LOAD_MODULE(12),

    REQUEST_ICON(13),

    USER_AGENT_ICON(14),

    REQUEST_PLUGIN(15);

    private final int id;

    private MessageType(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }

  /**
   * Represents an object on the other side of the channel, known to this side
   * by an reference ID.
   */
  public interface RemoteObjectRef {

    /**
      * @return the reference ID for this object.
      */
    int getRefid();
  }

  /**
   * Hook interface for responding to messages.
   */
  public abstract static class SessionHandler<T extends BrowserChannel> {

    /**
     * Wrapper to return both a return value/exception and a flag as to whether
     * an exception was thrown or not.
     */
    public static class ExceptionOrReturnValue {
      private final boolean isException;
      private final Value returnValue;

      public ExceptionOrReturnValue(boolean isException, Value returnValue) {
        this.isException = isException;
        this.returnValue = returnValue;
      }

      public Value getReturnValue() {
        return returnValue;
      }

      public boolean isException() {
        return isException;
      }
    }

    /**
     * Enumeration of dispatch IDs on object 0 (the ServerMethods object).
     *
     * <p>Ids are set specifically rather than relying on the ordinal to avoid
     * synchronization problems with the client.
     *
     * TODO: hasMethod/hasProperty no longer used, remove them!
     */
    public enum SpecialDispatchId {
      HasMethod(0), HasProperty(1), GetProperty(2), SetProperty(3);

      private final int id;

      private SpecialDispatchId(int id) {
        this.id = id;
      }

      public int getId() {
        return id;
      }
    }

    public abstract void freeValue(T channel, int[] ids);
  }

  /**
   * Represents a value for BrowserChannel.
   */
  public static class Value {
    /**
     * Enum of type tags sent across the wire.
     */
    public enum ValueType {
      /**
       * Primitive values.
       */
      NULL(0), BOOLEAN(1), BYTE(2), CHAR(3), SHORT(4), INT(5), LONG_UNUSED(6),
      FLOAT_UNUSED(7), DOUBLE(8), STRING(9),

      /**
       * Representations of Java or JS objects, sent as an index into a table
       * kept on the side holding the actual object.
       */
      JAVA_OBJECT(10), JS_OBJECT(11),

      /**
       * A Javascript undef value, also used for void returns.
       */
      UNDEFINED(12);

      private final int id;

      private ValueType(int id) {
        this.id = id;
      }

      byte getTag() {
        return (byte) id;
      }
    }

    /**
     * Type tag value.
     */
    private ValueType type = ValueType.UNDEFINED;

    /**
     * Represents a value sent/received across the wire.
     */
    private Object value = null;

    public Value() {
    }

    public Value(Object obj) {
      convertFromJavaValue(obj);
    }

    /**
     * Convert a Java object to a value. Objects must be primitive wrappers,
     * Strings, or JsObjectRef/JavaObjectRef instances.
     *
     * @param obj value to convert.
     */
    public void convertFromJavaValue(Object obj) {
      if (obj == null) {
        type = ValueType.NULL;
      } else if (obj instanceof Boolean) {
        type = ValueType.BOOLEAN;
      } else if (obj instanceof Byte) {
        type = ValueType.BYTE;
      } else if (obj instanceof Character) {
        type = ValueType.CHAR;
      } else if (obj instanceof Double) {
        type = ValueType.DOUBLE;
      } else if (obj instanceof Integer) {
        type = ValueType.INT;
      } else if (obj instanceof Short) {
        type = ValueType.SHORT;
      } else if (obj instanceof String) {
        type = ValueType.STRING;
      } else if (obj instanceof JsObjectRef) {
        // TODO: exception handling?
        type = ValueType.JS_OBJECT;
      } else if (obj instanceof JavaObjectRef) {
        // TODO: exception handling?
        type = ValueType.JAVA_OBJECT;
      } else {
        throw new IllegalArgumentException("Unexpected type: " + obj.getClass());
      }
      value = obj;
    }

    /**
     * Convert a value to the requested Java type.
     *
     * @param reqType type to convert to
     * @return value as that type.
     */
    public Object convertToJavaType(Class<?> reqType) {
      if (reqType.isArray()) {
        // TODO(jat): handle arrays?
      }
      if (reqType.equals(Boolean.class)) {
        assert type == ValueType.BOOLEAN;
        return value;
      } else if (reqType.equals(Byte.class) || reqType.equals(byte.class)) {
        assert isNumber();
        return Byte.valueOf(((Number) value).byteValue());
      } else if (reqType.equals(Character.class) || reqType.equals(char.class)) {
        if (type == ValueType.CHAR) {
          return value;
        } else {
          assert isNumber();
          return Character.valueOf((char) ((Number) value).shortValue());
        }
      } else if (reqType.equals(Double.class) || reqType.equals(double.class)) {
        assert isNumber();
        return Double.valueOf(((Number) value).doubleValue());
      } else if (reqType.equals(Float.class) || reqType.equals(float.class)) {
        assert isNumber();
        return Float.valueOf(((Number) value).floatValue());
      } else if (reqType.equals(Integer.class) || reqType.equals(int.class)) {
        assert isNumber();
        return Integer.valueOf(((Number) value).intValue());
      } else if (reqType.equals(Long.class) || reqType.equals(long.class)) {
        assert isNumber();
        return Long.valueOf(((Number) value).longValue());
      } else if (reqType.equals(Short.class) || reqType.equals(short.class)) {
        assert isNumber();
        return Short.valueOf(((Number) value).shortValue());
      } else if (reqType.equals(String.class)) {
        assert type == ValueType.STRING;
        return value;
      } else {
        // Wants an object, caller must deal with object references.
        return value;
      }
    }

    public boolean getBoolean() {
      assert type == ValueType.BOOLEAN;
      return ((Boolean) value).booleanValue();
    }

    public byte getByte() {
      assert type == ValueType.BYTE;
      return ((Byte) value).byteValue();
    }

    public char getChar() {
      assert type == ValueType.CHAR;
      return ((Character) value).charValue();
    }

    public double getDouble() {
      assert type == ValueType.DOUBLE;
      return ((Double) value).doubleValue();
    }

    public int getInt() {
      assert type == ValueType.INT;
      return ((Integer) value).intValue();
    }

    public JavaObjectRef getJavaObject() {
      assert type == ValueType.JAVA_OBJECT;
      return (JavaObjectRef) value;
    }

    public JsObjectRef getJsObject() {
      assert type == ValueType.JS_OBJECT;
      return (JsObjectRef) value;
    }

    public short getShort() {
      assert type == ValueType.SHORT;
      return ((Short) value).shortValue();
    }

    public String getString() {
      assert type == ValueType.STRING;
      return (String) value;
    }

    public ValueType getType() {
      return type;
    }

    public Object getValue() {
      return value;
    }

    public boolean isBoolean() {
      return type == ValueType.BOOLEAN;
    }

    public boolean isByte() {
      return type == ValueType.BYTE;
    }

    public boolean isChar() {
      return type == ValueType.CHAR;
    }

    public boolean isDouble() {
      return type == ValueType.DOUBLE;
    }

    public boolean isInt() {
      return type == ValueType.INT;
    }

    public boolean isJavaObject() {
      return type == ValueType.JAVA_OBJECT;
    }

    public boolean isJsObject() {
      return type == ValueType.JS_OBJECT;
    }

    public boolean isNull() {
      return type == ValueType.NULL;
    }

    public boolean isNumber() {
      switch (type) {
        case BYTE:
        case CHAR:
        case DOUBLE:
        case INT:
        case SHORT:
          return true;
        default:
          return false;
      }
    }

    public boolean isPrimitive() {
      switch (type) {
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case DOUBLE:
        case INT:
        case SHORT:
          return true;
        default:
          return false;
      }
    }

    public boolean isShort() {
      return type == ValueType.SHORT;
    }

    public boolean isString() {
      return type == ValueType.STRING;
    }

    public boolean isUndefined() {
      return type == ValueType.UNDEFINED;
    }

    public void setBoolean(boolean val) {
      type = ValueType.BOOLEAN;
      value = Boolean.valueOf(val);
    }

    public void setByte(byte val) {
      type = ValueType.BYTE;
      value = Byte.valueOf(val);
    }

    public void setChar(char val) {
      type = ValueType.CHAR;
      value = Character.valueOf(val);
    }

    public void setDouble(double val) {
      type = ValueType.DOUBLE;
      value = Double.valueOf(val);
    }

    public void setInt(int val) {
      type = ValueType.INT;
      value = Integer.valueOf(val);
    }

    public void setJavaObject(JavaObjectRef val) {
      type = ValueType.JAVA_OBJECT;
      value = val;
    }

    public void setJsObject(JsObjectRef val) {
      type = ValueType.JS_OBJECT;
      value = val;
    }

    public void setLong(long val) {
      type = ValueType.BOOLEAN;
      value = Long.valueOf(val);
    }

    public void setNull() {
      type = ValueType.NULL;
      value = null;
    }

    public void setShort(short val) {
      type = ValueType.SHORT;
      value = Short.valueOf(val);
    }

    public void setString(String val) {
      type = ValueType.STRING;
      value = val;
    }

    public void setUndefined() {
      type = ValueType.UNDEFINED;
      value = null;
    }

    @Override
    public String toString() {
      return type + ": " + value;
    }
  }

  /**
   * The initial request from the client, supplies a range of supported versions
   * and the version from hosted.html (so stale copies on an external server
   * can be detected).
   */
  protected static class CheckVersionsMessage extends Message {

    public static CheckVersionsMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      int minVersion = stream.readInt();
      int maxVersion = stream.readInt();
      String hostedHtmlVersion = readUtf8String(stream);
      return new CheckVersionsMessage(channel, minVersion, maxVersion,
          hostedHtmlVersion);
    }

    private final String hostedHtmlVersion;

    private final int maxVersion;

    private final int minVersion;

    public CheckVersionsMessage(BrowserChannel channel, int minVersion,
        int maxVersion, String hostedHtmlVersion) {
      super(channel);
      this.minVersion = minVersion;
      this.maxVersion = maxVersion;
      this.hostedHtmlVersion = hostedHtmlVersion;
    }

    public String getHostedHtmlVersion() {
      return hostedHtmlVersion;
    }

    public int getMaxVersion() {
      return maxVersion;
    }

    public int getMinVersion() {
      return minVersion;
    }

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.CHECK_VERSIONS.getId());
      stream.writeInt(minVersion);
      stream.writeInt(maxVersion);
      writeUtf8String(stream, hostedHtmlVersion);
      stream.flush();
    }
  }

  /**
   * A message from the client giving a list of supported connection methods
   * and requesting the server choose one of them to switch protocol traffic to.
   */
  protected static class ChooseTransportMessage extends Message {

    public static ChooseTransportMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      int n = stream.readInt();
      String[] transports = new String[n];
      for (int i = 0; i < n; ++i) {
        transports[i] = readUtf8String(stream);
      }
      return new ChooseTransportMessage(channel, transports);
    }

    private final String[] transports;

    public ChooseTransportMessage(BrowserChannel channel,
        String[] transports) {
      super(channel);
      this.transports = transports;
    }

    public String[] getTransports() {
      return transports;
    }

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.CHOOSE_TRANSPORT.getId());
      stream.writeInt(transports.length);
      for (String transport : transports) {
        writeUtf8String(stream, transport);
      }
    }
  }

  /**
   * A message reporting a connection error to the client.
   */
  protected static class FatalErrorMessage extends Message {

    public static FatalErrorMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      // NOTE: Tag has already been read.
      String error = readUtf8String(stream);
      return new FatalErrorMessage(channel, error);
    }

    private final String error;

    public FatalErrorMessage(BrowserChannel channel, String error) {
      super(channel);
      this.error = error;
    }

    public String getError() {
      return error;
    }

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.FATAL_ERROR.getId());
      writeUtf8String(stream, error);
    }
  }

  /**
   * A message asking the other side to free object references. Note that there
   * is no response to this message, and this must only be sent immediately
   * before an Invoke or Return message.
   */
  protected static class FreeMessage extends Message {
    public static FreeMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      int numIds = stream.readInt();
      // TODO: sanity check id count
      int ids[] = new int[numIds];
      for (int i = 0; i < numIds; ++i) {
        ids[i] = stream.readInt();
      }
      return new FreeMessage(channel, ids);
    }

    public static void send(BrowserChannel channel, int[] ids)
        throws IOException {
      DataOutputStream stream = channel.getStreamToOtherSide();
      stream.writeByte(MessageType.FREE_VALUE.getId());
      stream.writeInt(ids.length);
      for (int id : ids) {
        stream.writeInt(id);
      }
      stream.flush();
    }

    private final int ids[];

    public FreeMessage(BrowserChannel channel, int[] ids) {
      super(channel);
      this.ids = ids;
    }

    public int[] getIds() {
      return ids;
    }

    @Override
    public boolean isAsynchronous() {
      return true;
    }

    @Override
    public void send() throws IOException {
      send(getBrowserChannel(), ids);
    }
  }

  /**
   * A request from the server to invoke a function on the client.
   *
   * Note that MessageType.INVOKE can refer to either this class
   * or {@link InvokeOnServerMessage} depending on the direction, as the
   * protocol is asymmetric (Java needs a dispatch ID, Javascript needs a
   * name).
   */
  protected static class InvokeOnClientMessage extends Message {
    public static InvokeOnClientMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      // NOTE: Tag has already been read.
      String methodName = readUtf8String(stream);
      Value thisRef = channel.readValue(stream);
      int argLen = stream.readInt();
      Value[] args = new Value[argLen];
      for (int i = 0; i < argLen; i++) {
        args[i] = channel.readValue(stream);
      }
      return new InvokeOnClientMessage(channel, methodName, thisRef, args);
    }

    private final Value[] args;
    private final String methodName;
    private final Value thisRef;

    public InvokeOnClientMessage(BrowserChannel channel, String methodName,
        Value thisRef, Value[] args) {
      super(channel);
      this.thisRef = thisRef;
      this.methodName = methodName;
      this.args = args;
    }

    public Value[] getArgs() {
      return args;
    }

    public String getMethodName() {
      return methodName;
    }

    public Value getThis() {
      return thisRef;
    }

    @Override
    public void send() throws IOException {
      final DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();

      stream.writeByte(MessageType.INVOKE.getId());
      writeUtf8String(stream, methodName);
      getBrowserChannel().writeValue(stream, thisRef);
      stream.writeInt(args.length);
      for (int i = 0; i < args.length; i++) {
        getBrowserChannel().writeValue(stream, args[i]);
      }
      stream.flush();
    }
  }

  /**
   * A request from the client to invoke a function on the server.
   *
   * Note that MessageType.INVOKE can refer to either this class
   * or {@link InvokeOnClientMessage} depending on the direction, as the
   * protocol is asymmetric (Java needs a dispatch ID, Javascript needs a
   * name).
   */
  protected static class InvokeOnServerMessage extends Message {
    public static InvokeOnServerMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      // NOTE: Tag has already been read.
      int methodDispatchId = stream.readInt();
      Value thisRef = channel.readValue(stream);
      int argLen = stream.readInt();
      Value[] args = new Value[argLen];
      for (int i = 0; i < argLen; i++) {
        args[i] = channel.readValue(stream);
      }
      return new InvokeOnServerMessage(channel, methodDispatchId, thisRef,
          args);
    }

    private final Value[] args;
    private final int methodDispatchId;
    private final Value thisRef;

    public InvokeOnServerMessage(BrowserChannel channel, int methodDispatchId,
        Value thisRef, Value[] args) {
      super(channel);
      this.thisRef = thisRef;
      this.methodDispatchId = methodDispatchId;
      this.args = args;
    }

    public Value[] getArgs() {
      return args;
    }

    public int getMethodDispatchId() {
      return methodDispatchId;
    }

    public Value getThis() {
      return thisRef;
    }

    @Override
    public void send() throws IOException {
      final DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();

      stream.writeByte(MessageType.INVOKE.getId());
      stream.writeInt(methodDispatchId);
      getBrowserChannel().writeValue(stream, thisRef);
      stream.writeInt(args.length);
      for (int i = 0; i < args.length; i++) {
        getBrowserChannel().writeValue(stream, args[i]);
      }
      stream.flush();
    }
  }

  /**
   * A request from the to invoke a function on the other side.
   */
  protected static class InvokeSpecialMessage extends Message {
    public static InvokeSpecialMessage receive(BrowserChannel channel)
        throws IOException, BrowserChannelException {
      final DataInputStream stream = channel.getStreamFromOtherSide();
      // NOTE: Tag has already been read.
      final int specialMethodInt = stream.readByte();
      SpecialDispatchId[] ids = SpecialDispatchId.values();
      if (specialMethodInt < 0 || specialMethodInt >= ids.length) {
        throw new BrowserChannelException("Invalid dispatch id "
            + specialMethodInt);
      }
      final SpecialDispatchId dispatchId = ids[specialMethodInt];
      final int argLen = stream.readInt();
      final Value[] args = new Value[argLen];
      for (int i = 0; i < argLen; i++) {
        args[i] = channel.readValue(stream);
      }
      return new InvokeSpecialMessage(channel, dispatchId, args);
    }

    private final Value[] args;
    private final SpecialDispatchId dispatchId;

    public InvokeSpecialMessage(BrowserChannel channel,
        SpecialDispatchId dispatchId, Value[] args) {
      super(channel);
      this.dispatchId = dispatchId;
      this.args = args;
    }

    public Value[] getArgs() {
      return args;
    }

    public SpecialDispatchId getDispatchId() {
      return dispatchId;
    }

    @Override
    public void send() throws IOException {
      final DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();

      stream.writeByte(MessageType.INVOKE_SPECIAL.getId());
      stream.writeByte(dispatchId.getId());
      stream.writeInt(args.length);
      for (int i = 0; i < args.length; i++) {
        getBrowserChannel().writeValue(stream, args[i]);
      }
      stream.flush();
    }
  }

  /**
   * A message sending JSNI code to be evaluated. Note that there is no response
   * to this message, and this must only be sent immediately before an Invoke or
   * Return message.
   */
  protected static class LoadJsniMessage extends Message {
    public static LoadJsniMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      String js = readUtf8String(stream);
      return new LoadJsniMessage(channel, js);
    }

    public static void send(BrowserChannel channel, String js)
        throws IOException {
      DataOutputStream stream = channel.getStreamToOtherSide();
      stream.write(MessageType.LOAD_JSNI.getId());
      writeUtf8String(stream, js);
      stream.flush();
    }

    private final String js;

    public LoadJsniMessage(BrowserChannel channel, String js) {
      super(channel);
      this.js = js;
    }

    public String getJsni() {
      return js;
    }

    @Override
    public boolean isAsynchronous() {
      return true;
    }

    @Override
    public void send() throws IOException {
      send(getBrowserChannel(), js);
    }
  }

  /**
   * A request from the client that the server load and initialize a given
   * module.
   */
  protected static class LoadModuleMessage extends Message {
    public static LoadModuleMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      String url = readUtf8String(stream);
      String tabKey = readUtf8String(stream);
      String sessionKey = readUtf8String(stream);
      String moduleName = readUtf8String(stream);
      String userAgent = readUtf8String(stream);
      return new LoadModuleMessage(channel, url, tabKey, sessionKey, moduleName,
          userAgent);
    }

    private final String moduleName;

    private final String sessionKey;

    private final String tabKey;

    private final String url;

    private final String userAgent;

    /**
     * Creates a LoadModule message to be sent to the server.
     *
     * @param channel BrowserChannel instance
     * @param url URL of main top-level window - may not be null
     * @param tabKey opaque key identifying the tab in the browser, or an
     *     empty string if it cannot be determined - may not be null
     * @param sessionKey opaque key identifying a particular session (ie,
     *     group of modules) - may not be null
     * @param moduleName name of GWT module to load - may not be null
     * @param userAgent user agent identifier of the browser - may not be null
     */
    public LoadModuleMessage(BrowserChannel channel, String url,
        String tabKey, String sessionKey, String moduleName, String userAgent) {
      super(channel);
      assert url != null;
      assert tabKey != null;
      assert sessionKey != null;
      assert moduleName != null;
      assert userAgent != null;
      this.url = url;
      this.tabKey = tabKey;
      this.sessionKey = sessionKey;
      this.moduleName = moduleName;
      this.userAgent = userAgent;
    }

    public String getModuleName() {
      return moduleName;
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

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.LOAD_MODULE.getId());
      writeUtf8String(stream, url);
      writeUtf8String(stream, tabKey);
      writeUtf8String(stream, sessionKey);
      writeUtf8String(stream, moduleName);
      writeUtf8String(stream, userAgent);
      stream.flush();
    }
  }

  /**
   * Abstract base class of OOPHM messages.
   */
  protected abstract static class Message {
    public static MessageType readMessageType(DataInputStream stream)
        throws IOException, BrowserChannelException {
      stream.mark(1);
      int type = stream.readByte();
      MessageType[] types = MessageType.values();
      if (type < 0 || type >= types.length) {
        stream.reset();
        throw new BrowserChannelException("Invalid message type " + type);
      }
      return types[type];
    }

    private final BrowserChannel channel;

    public Message(BrowserChannel channel) {
      this.channel = channel;
    }

    public final BrowserChannel getBrowserChannel() {
      return channel;
    }

    /**
     * @return true if this message type is asynchronous and does not expect a
     *         return message.
     */
    public boolean isAsynchronous() {
      return false;
    }

    /**
     * @throws IOException if a subclass encounters an I/O error
     */
    public void send() throws IOException {
      throw new UnsupportedOperationException(getClass().getName()
          + " is a message format that can only be received.");
    }
  }

  /**
   * Provides a way of allocating JS and Java object ids without knowing
   * which one is the remote type, so code can be shared between client and
   * server.
   */
  protected interface ObjectRefFactory {

    JavaObjectRef getJavaObjectRef(int refId);

    JsObjectRef getJsObjectRef(int refId);

    Set<Integer> getRefIdsForCleanup();
  }

  /**
   * A request from the client that the server load and initialize a given
   * module (original v1 version).
   */
  protected static class OldLoadModuleMessage extends Message {
    public static OldLoadModuleMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      int protoVersion = stream.readInt();
      String moduleName = readUtf8String(stream);
      String userAgent = readUtf8String(stream);
      return new OldLoadModuleMessage(channel, protoVersion, moduleName,
          userAgent);
    }

    private final String moduleName;

    private final int protoVersion;

    private final String userAgent;

    public OldLoadModuleMessage(BrowserChannel channel, int protoVersion,
        String moduleName, String userAgent) {
      super(channel);
      this.protoVersion = protoVersion;
      this.moduleName = moduleName;
      this.userAgent = userAgent;
    }

    public String getModuleName() {
      return moduleName;
    }

    public int getProtoVersion() {
      return protoVersion;
    }

    public String getUserAgent() {
      return userAgent;
    }

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.OLD_LOAD_MODULE.getId());
      stream.writeInt(protoVersion);
      writeUtf8String(stream, moduleName);
      writeUtf8String(stream, userAgent);
      stream.flush();
    }
  }

  /**
   * Reports the selected protocol version.
   */
  protected static class ProtocolVersionMessage extends Message {

    public static ProtocolVersionMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      int protocolVersion = stream.readInt();
      return new ProtocolVersionMessage(channel, protocolVersion);
    }

    private final int protocolVersion;

    public ProtocolVersionMessage(BrowserChannel channel, int protocolVersion) {
      super(channel);
      this.protocolVersion = protocolVersion;
    }

    public int getProtocolVersion() {
      return protocolVersion;
    }

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.PROTOCOL_VERSION.getId());
      stream.writeInt(protocolVersion);
      stream.flush();
    }
  }

  /**
   * A message signifying a soft close of the communications channel.
   */
  protected static class QuitMessage extends Message {
    public static QuitMessage receive(BrowserChannel channel) {
      return new QuitMessage(channel);
    }

    public static void send(BrowserChannel channel) throws IOException {
      final DataOutputStream stream = channel.getStreamToOtherSide();
      stream.writeByte(MessageType.QUIT.getId());
      stream.flush();
    }

    public QuitMessage(BrowserChannel channel) {
      super(channel);
    }

    @Override
    public void send() throws IOException {
      send(getBrowserChannel());
    }
  }

  /**
   * A message asking the client to send an icon suitable for use in the UI.
   * <p>See {@link UserAgentIconMessage}.
   */
  protected static class RequestIconMessage extends Message {

    /**
     * Receive a RequestIconMessage, assuming the message tag has already been
     * read.
     *
     * @throws IOException
     */
    public static RequestIconMessage receive(BrowserChannel channel)
        throws IOException {
      return new RequestIconMessage(channel);
    }

    public static void send(BrowserChannel channel)
        throws IOException {
      DataOutputStream stream = channel.getStreamToOtherSide();
      stream.writeByte(MessageType.REQUEST_ICON.getId());
      stream.flush();
    }

    public RequestIconMessage(BrowserChannel channel) {
      super(channel);
    }

    @Override
    public void send() throws IOException {
      send(getBrowserChannel());
    }
  }

  /**
   * Signifies a return from a previous invoke.
   */
  protected static class ReturnMessage extends Message {
    public static ReturnMessage receive(BrowserChannel channel)
        throws IOException {
      final DataInputStream stream = channel.getStreamFromOtherSide();
      final boolean isException = stream.readBoolean();
      final Value returnValue = channel.readValue(stream);
      return new ReturnMessage(channel, isException, returnValue);
    }

    public static void send(BrowserChannel channel, boolean isException,
        Value returnValue) throws IOException {
      final DataOutputStream stream = channel.getStreamToOtherSide();
      stream.writeByte(MessageType.RETURN.getId());
      stream.writeBoolean(isException);
      channel.writeValue(stream, returnValue);
      stream.flush();
    }

    public static void send(BrowserChannel channel,
        ExceptionOrReturnValue returnOrException) throws IOException {
      send(channel, returnOrException.isException(),
          returnOrException.getReturnValue());
    }

    private final boolean isException;
    private final Value returnValue;

    public ReturnMessage(BrowserChannel channel, boolean isException,
        Value returnValue) {
      super(channel);
      this.returnValue = returnValue;
      this.isException = isException;
    }

    public Value getReturnValue() {
      return returnValue;
    }

    public boolean isException() {
      return isException;
    }

    @Override
    public void send() throws IOException {
      send(getBrowserChannel(), isException, returnValue);
    }
  }

  /**
   * A response to ChooseTransport telling the client which transport should
   * be used for the remainder of the protocol.
   */
  protected static class SwitchTransportMessage extends Message {

    public static SwitchTransportMessage receive(BrowserChannel channel)
        throws IOException {
      DataInputStream stream = channel.getStreamFromOtherSide();
      String transport = readUtf8String(stream);
      String transportArgs = readUtf8String(stream);
      return new SwitchTransportMessage(channel, transport, transportArgs);
    }

    private final String transport;

    private final String transportArgs;

    public SwitchTransportMessage(BrowserChannel channel,
        String transport, String transportArgs) {
      super(channel);
      // Change nulls to empty strings
      if (transport == null) {
        transport = "";
      }
      if (transportArgs == null) {
        transportArgs = "";
      }
      this.transport = transport;
      this.transportArgs = transportArgs;
    }

    public String getTransport() {
      return transport;
    }

    public String getTransportArgs() {
      return transportArgs;
    }

    @Override
    public void send() throws IOException {
      DataOutputStream stream = getBrowserChannel().getStreamToOtherSide();
      stream.writeByte(MessageType.SWITCH_TRANSPORT.getId());
      writeUtf8String(stream, transport);
      writeUtf8String(stream, transportArgs);
      stream.flush();
    }
  }

  /**
   * A message supplying an icon, which fits in 24x24 and in a standard image
   * format such as PNG or GIF, suitable for use in the UI.
   * <p>See {@link RequestIconMessage}.
   */
  protected static class UserAgentIconMessage extends Message {
    public static UserAgentIconMessage receive(BrowserChannel channel)
        throws IOException {
      byte[] iconBytes = null;
      DataInputStream stream = channel.getStreamFromOtherSide();
      int len = stream.readInt();
      if (len > 0) {
        iconBytes = new byte[len];
        for (int i = 0; i < len; ++i) {
          iconBytes[i] = stream.readByte();
        }
      }
      return new UserAgentIconMessage(channel, iconBytes);
    }

    public static void send(BrowserChannel channel, byte[] iconBytes)
        throws IOException {
      DataOutputStream stream = channel.getStreamToOtherSide();
      stream.writeByte(MessageType.USER_AGENT_ICON.getId());
      if (iconBytes == null) {
        stream.writeInt(0);
      } else {
        stream.writeInt(iconBytes.length);
        for (byte b : iconBytes) {
          stream.writeByte(b);
        }
      }
      stream.flush();
    }

    private byte[] iconBytes;

    public UserAgentIconMessage(BrowserChannel channel, byte[] iconBytes) {
      super(channel);
      this.iconBytes = iconBytes;
    }

    public byte[] getIconBytes() {
      return iconBytes;
    }

    @Override
    public void send() throws IOException {
      send(getBrowserChannel(), iconBytes);
    }
  }

  /**
   * The current version of the protocol.
   */
  public static final int PROTOCOL_VERSION_CURRENT = 3;

  /**
   * The oldest protocol version supported by this code.
   */
  public static final int PROTOCOL_VERSION_OLDEST = 2;

  /**
   * The protocol version that added the GetIcon message.
   */
  public static final int PROTOCOL_VERSION_GET_ICON = 3;

  public static final int SPECIAL_CLIENTMETHODS_OBJECT = 0;

  public static final int SPECIAL_SERVERMETHODS_OBJECT = 0;

  protected static JavaObjectRef getJavaObjectRef(int refId) {
    return new JavaObjectRef(refId);
  }

  protected static String readUtf8String(DataInputStream stream)
      throws IOException {
    final int len = stream.readInt();
    final byte[] data = new byte[len];
    stream.readFully(data);
    return new String(data, "UTF8");
  }

  protected static ValueType readValueType(DataInputStream stream)
      throws IOException, BrowserChannelException {
    int type = stream.readByte();
    ValueType[] types = ValueType.values();
    if (type < 0 || type >= types.length) {
      throw new BrowserChannelException("Invalid value type " + type);
    }
    return types[type];
  }

  protected static void writeJavaObject(DataOutputStream stream,
      JavaObjectRef value) throws IOException {
    stream.writeByte(ValueType.JAVA_OBJECT.getTag());
    stream.writeInt(value.getRefid());
  }

  protected static void writeJsObject(DataOutputStream stream,
      JsObjectRef value) throws IOException {
    stream.writeByte(ValueType.JS_OBJECT.getTag());
    stream.writeInt(value.getRefid());
  }

  protected static void writeNull(DataOutputStream stream) throws IOException {
    stream.writeByte(ValueType.NULL.getTag());
  }

  protected static void writeTaggedBoolean(DataOutputStream stream,
      boolean value) throws IOException {
    stream.writeByte(ValueType.BOOLEAN.getTag());
    stream.writeBoolean(value);
  }

  protected static void writeTaggedByte(DataOutputStream stream, byte value)
      throws IOException {
    stream.writeByte(ValueType.BYTE.getTag());
    stream.writeByte(value);
  }

  protected static void writeTaggedChar(DataOutputStream stream, char value)
      throws IOException {
    stream.writeByte(ValueType.CHAR.getTag());
    stream.writeChar(value);
  }

  protected static void writeTaggedDouble(DataOutputStream stream, double value)
      throws IOException {
    stream.writeByte(ValueType.DOUBLE.getTag());
    stream.writeDouble(value);
  }

  protected static void writeTaggedInt(DataOutputStream stream, int value)
      throws IOException {
    stream.writeByte(ValueType.INT.getTag());
    stream.writeInt(value);
  }

  protected static void writeTaggedShort(DataOutputStream stream, short value)
      throws IOException {
    stream.writeByte(ValueType.SHORT.getTag());
    stream.writeShort(value);
  }

  protected static void writeTaggedString(DataOutputStream stream, String data)
      throws IOException {
    stream.writeByte(ValueType.STRING.getTag());
    writeUtf8String(stream, data);
  }

  protected static void writeUtf8String(DataOutputStream stream, String data)
      throws IOException {
    try {
      final byte[] bytes = data.getBytes("UTF8");
      stream.writeInt(bytes.length);
      stream.write(bytes);
    } catch (UnsupportedEncodingException e) {
      // TODO: Add description.
      throw new RuntimeException();
    }
  }

  private static void writeUndefined(DataOutputStream stream)
      throws IOException {
    stream.writeByte(ValueType.UNDEFINED.getTag());
  }

  private final ObjectRefFactory objectRefFactory;

  private Socket socket;

  private final DataInputStream streamFromOtherSide;

  private final DataOutputStream streamToOtherSide;

  public BrowserChannel(Socket socket, ObjectRefFactory objectRefFactory)
      throws IOException {
    this(new BufferedInputStream(socket.getInputStream()),
        new BufferedOutputStream(socket.getOutputStream()),
        objectRefFactory);
    this.socket = socket;
  }

  protected BrowserChannel(InputStream inputStream, OutputStream outputStream,
      ObjectRefFactory objectRefFactory) {
    streamFromOtherSide = new DataInputStream(inputStream);
    streamToOtherSide = new DataOutputStream(outputStream);
    socket = null;
    this.objectRefFactory = objectRefFactory;
  }

  public void endSession() {
    Utility.close(streamFromOtherSide);
    Utility.close(streamToOtherSide);
    Utility.close(socket);
  }

  /**
   * @return a set of remote object reference IDs to be freed.
   */
  public Set<Integer> getRefIdsForCleanup() {
    return objectRefFactory.getRefIdsForCleanup();
  }

  public String getRemoteEndpoint() {
    if (socket == null) {
      return "";
    }
    return socket.getInetAddress().getCanonicalHostName() + ":"
        + socket.getPort();
  }

  protected DataInputStream getStreamFromOtherSide() {
    return streamFromOtherSide;
  }

  protected DataOutputStream getStreamToOtherSide() {
    return streamToOtherSide;
  }

  protected Value readValue(DataInputStream stream) throws IOException {
    ValueType tag;
    try {
      tag = readValueType(stream);
    } catch (BrowserChannelException e) {
      IOException ee = new IOException();
      ee.initCause(e);
      throw ee;
    }
    Value value = new Value();
    switch (tag) {
      case NULL:
        value.setNull();
        break;
      case UNDEFINED:
        value.setUndefined();
        break;
      case BOOLEAN:
        value.setBoolean(stream.readByte() != 0);
        break;
      case BYTE:
        value.setByte(stream.readByte());
        break;
      case CHAR:
        value.setChar(stream.readChar());
        break;
      case INT:
        value.setInt(stream.readInt());
        break;
      case DOUBLE:
        value.setDouble(stream.readDouble());
        break;
      case SHORT:
        value.setShort(stream.readShort());
        break;
      case STRING:
        value.setString(readUtf8String(stream));
        break;
      case JS_OBJECT:
        value.setJsObject(objectRefFactory.getJsObjectRef(stream.readInt()));
        break;
      case JAVA_OBJECT:
        value.setJavaObject(objectRefFactory.getJavaObjectRef(
            stream.readInt()));
        break;
      default:
        throw new IllegalArgumentException("Unexpected type: " + tag);
    }
    return value;
  }

  protected void sendFreedValues() throws IOException {
    Set<Integer> freed = objectRefFactory.getRefIdsForCleanup();
    int n = freed.size();
    if (n > 0) {
      int[] ids = new int[n];
      int i = 0;
      for (Integer id : freed) {
        ids[i++] = id;
      }
      FreeMessage.send(this, ids);
    }
  }

  protected void writeValue(DataOutputStream stream, Value value)
      throws IOException {
    if (value.isNull()) {
      writeNull(stream);
    } else if (value.isUndefined()) {
      writeUndefined(stream);
    } else if (value.isJsObject()) {
      writeJsObject(stream, value.getJsObject());
    } else if (value.isJavaObject()) {
      writeJavaObject(stream, value.getJavaObject());
    } else if (value.isBoolean()) {
      writeTaggedBoolean(stream, value.getBoolean());
    } else if (value.isByte()) {
      writeTaggedByte(stream, value.getByte());
    } else if (value.isChar()) {
      writeTaggedChar(stream, value.getChar());
    } else if (value.isShort()) {
      writeTaggedShort(stream, value.getShort());
    } else if (value.isDouble()) {
      writeTaggedDouble(stream, value.getDouble());
    } else if (value.isInt()) {
      writeTaggedInt(stream, value.getInt());
    } else if (value.isString()) {
      writeTaggedString(stream, value.getString());
    } else {
      throw new IllegalArgumentException("Unexpected type: " + value.getType());
    }
  }
}
