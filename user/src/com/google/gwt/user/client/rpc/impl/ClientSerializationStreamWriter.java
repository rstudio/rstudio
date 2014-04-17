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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.lang.LongLib;
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
  @SuppressWarnings("unused")  // referenced by quoteString
  private static JavaScriptObject regex = getQuotingRegex();

  /**
   * Quote characters in a user-supplied string to make sure they are safe to
   * send to the server.
   * 
   * @param str string to quote
   * @return quoted string
   */
  public static native String quoteString(String str) /*-{
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

  private static void append(StringBuilder sb, String token) {
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
    if (ua.indexOf("android") != -1) {
      // initial version of Android WebKit has a double-encoding bug for UTF8,
      // so we have to encode every non-ASCII character.
      // TODO(jat): revisit when this bug is fixed in Android
      return /[\u0000\|\\\u0080-\uFFFF]/g;
    } else if (ua.indexOf("chrome/11") != -1) {
      // Chrome 11 mangles many more characters, so quote most of them
      // TOOD(jat): remove this when/if fixed
      return /[\u0000\|\\\u0300-\uFFFF]/g;
    } else if (ua.indexOf("webkit") != -1) {
      // other WebKit-based browsers need some additional quoting due to combining
      // forms and normalization (one codepoint being replaced with another).
      // Verified with Safari 4.0.1 (5530.18)
      return /[\u0000\|\\\u0300-\u03ff\u0590-\u05FF\u0600-\u06ff\u0730-\u074A\u07eb-\u07f3\u0940-\u0963\u0980-\u09ff\u0a00-\u0a7f\u0b00-\u0b7f\u0e00-\u0e7f\u0f00-\u0fff\u1900-\u194f\u1a00-\u1a1f\u1b00-\u1b7f\u1cda-\u1cdc\u1dc0-\u1dff\u1f00-\u1fff\u2000-\u206f\u20d0-\u20ff\u2100-\u214f\u2300-\u23ff\u2a00-\u2aff\u3000-\u303f\uaab2-\uaab4\uD800-\uFFFF]/g;
    } else {
      return /[\u0000\|\\\uD800-\uFFFF]/g;
    }
  }-*/;

  private StringBuilder encodeBuffer;

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
    encodeBuffer = new StringBuilder();

    // Write serialization policy info
    writeString(moduleBaseURL);
    writeString(serializationPolicyStrongName);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    writeHeader(buffer);
    writeStringTable(buffer);
    writePayload(buffer);
    return buffer.toString();
  }

  @Override
  public void writeLong(long value) {
    append(LongLib.toBase64(value));
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

    if (o instanceof Enum<?>) {
      Enum<?> e = (Enum<?>) o;
      clazz = e.getDeclaringClass();
    }

    return serializer.getSerializationSignature(clazz);
  }

  @Override
  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    serializer.serialize(this, instance, typeSignature);
  }

  private void writeHeader(StringBuilder buffer) {
    append(buffer, String.valueOf(getVersion()));
    append(buffer, String.valueOf(getFlags()));
  }

  private void writePayload(StringBuilder buffer) {
    buffer.append(encodeBuffer.toString());
  }

  private StringBuilder writeStringTable(StringBuilder buffer) {
    List<String> stringTable = getStringTable();
    append(buffer, String.valueOf(stringTable.size()));
    for (String s : stringTable) {
      append(buffer, quoteString(s));
    }
    return buffer;
  }

}
