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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.user.client.rpc.SerializationException;

import java.util.List;

/**
 * For internal use only. Used for server call serialization.
 */
public final class ClientSerializationStreamWriter extends
    AbstractSerializationStreamWriter {

  /**
   * Used by JSNI, see {@link #quoteString(String)}.
   */
  @SuppressWarnings("unused")
  private static JavaScriptObject regex = getQuotingRegex();

  private static void append(StringBuffer sb, String token) {
    assert (token != null);
    sb.append(token);
    sb.append(RPC_SEPARATOR_CHAR);
  }

  /**
   * Create the RegExp instance used for quoting dangerous characters in user
   * payload strings.
   * 
   * Note that {@link AbstractSerializationStream#RPC_SEPARATOR_CHAR} is used in
   * this expression, which must be updated if the separator character is
   * changed.
   * 
   * For Android WebKit, we quote many more characters to keep them from being
   * mangled.
   * 
   * @return RegExp object
   */
  private static native JavaScriptObject getQuotingRegex() /*-{
    // "|" = AbstractSerializationStream.RPC_SEPARATOR_CHAR
    var ua = navigator.userAgent.toLowerCase();
    var webkitregex = /webkit\/([\d]+)/;
    var webkit = 0;
    var result = webkitregex.exec(ua);
    if (result) {
      webkit = parseInt(result[1]);
    }
    if (ua.indexOf("android") != -1) {
      // initial version of Android WebKit has a double-encoding bug for UTF8,
      // so we have to encode every non-ASCII character.
      // TODO(jat): revisit when this bug is fixed in Android
      return /[\u0000\|\\\u0080-\uFFFF]/g;
    } else if (webkit < 522) {
      // Safari 2 doesn't handle \\uXXXX in regexes
      // TODO(jat): should iPhone be treated specially?
      return /[\x00\|\\]/g;
    } else if (webkit > 0) {
      // other WebKit-based browsers need some additional quoting
      return /[\u0000\|\\\u0300-\u036F\u0590-\u05FF\uD800-\uFFFF]/g;
    } else {
      return /[\u0000\|\\\uD800-\uFFFF]/g;
    }
  }-*/;

  @UnsafeNativeLong
  // Keep synchronized with LongLib
  private static native double[] makeLongComponents0(long value) /*-{
    return value;
  }-*/;

  /**
   * Quote characters in a user-supplied string to make sure they are safe to
   * send to the server.
   * 
   * See {@link ServerSerializationStreamReader#deserializeStringTable} for the
   * corresponding dequoting.
   * 
   * @param str string to quote
   * @return quoted string
   */
  private static native String quoteString(String str) /*-{
    var regex = @com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter::regex;
    var idx = 0;
    var out = "";
    var result;
    while ((result = regex.exec(str)) != null) {
       out += str.substring(idx, result.index);
       idx = result.index + 1;
       var ch = result[0].charCodeAt(0);
       if (ch == 0) {
         out += "\\0";
       } else if (ch == 92) { // backslash
         out += "\\\\";
       } else if (ch == 124) { // vertical bar
         // 124 = "|" = AbstractSerializationStream.RPC_SEPARATOR_CHAR
         out += "\\!";
       } else {
         var hex = ch.toString(16);
         out += "\\u0000".substring(0, 6 - hex.length) + hex;
       }
    }
    return out + str.substring(idx);
  }-*/;

  private StringBuffer encodeBuffer;

  private final String moduleBaseURL;

  private final String serializationPolicyStrongName;

  private final Serializer serializer;

  /**
   * Constructs a <code>ClientSerializationStreamWriter</code> using the
   * specified module base URL and the serialization policy.
   * 
   * @param serializer the {@link Serializer} to use
   * @param moduleBaseURL the location of the module
   * @param serializationPolicyStrongName the strong name of serialization
   *          policy
   */
  public ClientSerializationStreamWriter(Serializer serializer,
      String moduleBaseURL, String serializationPolicyStrongName) {
    this.serializer = serializer;
    this.moduleBaseURL = moduleBaseURL;
    this.serializationPolicyStrongName = serializationPolicyStrongName;
  }

  /**
   * Call this method before attempting to append any tokens. This method
   * implementation <b>must</b> be called by any overridden version.
   */
  @Override
  public void prepareToWrite() {
    super.prepareToWrite();
    encodeBuffer = new StringBuffer();

    // Write serialization policy info
    writeString(moduleBaseURL);
    writeString(serializationPolicyStrongName);
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    writeHeader(buffer);
    writeStringTable(buffer);
    writePayload(buffer);
    return buffer.toString();
  }

  @Override
  public void writeLong(long fieldValue) {
    /*
     * Client code represents longs internally as an array of two Numbers. In
     * order to make serialization of longs faster, we'll send the component
     * parts so that the value can be directly reconstituted on the server.
     */
    double[] parts;
    if (GWT.isScript()) {
      parts = makeLongComponents0(fieldValue);
    } else {
      parts = makeLongComponents((int) (fieldValue >> 32), (int) fieldValue);
    }
    assert parts.length == 2;
    writeDouble(parts[0]);
    writeDouble(parts[1]);
  }

  /**
   * Appends a token to the end of the buffer.
   */
  @Override
  protected void append(String token) {
    append(encodeBuffer, token);
  }

  @Override
  protected String getObjectTypeSignature(Object o) {
    Class<?> clazz = o.getClass();

    if (o instanceof Enum) {
      Enum<?> e = (Enum<?>) o;
      clazz = e.getDeclaringClass();
    }

    String typeName = clazz.getName();

    String serializationSignature = serializer.getSerializationSignature(typeName);
    if (serializationSignature != null) {
      typeName += "/" + serializationSignature;
    }
    return typeName;
  }

  @Override
  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    serializer.serialize(this, instance, typeSignature);
  }

  private void writeHeader(StringBuffer buffer) {
    append(buffer, String.valueOf(getVersion()));
    append(buffer, String.valueOf(getFlags()));
  }

  private void writePayload(StringBuffer buffer) {
    buffer.append(encodeBuffer.toString());
  }

  private StringBuffer writeStringTable(StringBuffer buffer) {
    List<String> stringTable = getStringTable();
    append(buffer, String.valueOf(stringTable.size()));
    for (String s : stringTable) {
      append(buffer, quoteString(s));
    }
    return buffer;
  }

}
