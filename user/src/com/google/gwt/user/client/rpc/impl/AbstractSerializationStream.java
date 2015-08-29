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

/**
 * Base class for the client and server serialization streams. This class
 * handles the basic serialization and deserialization formatting for primitive
 * types since these are common between the client and the server.
 */
public abstract class AbstractSerializationStream {

  /**
   * The default flags to be used by serialization streams.
   */
  public static final int DEFAULT_FLAGS = 0;

  /**
   * The character used to separate fields in client->server RPC messages.
   * 
   * Note that this character is referenced in the following places not using
   * this constant, and they must be changed if this is:
   * <ul>
   * <li>{@link ServerSerializationStreamWriter}.deserializeStringTable
   * <li>{@link ClientSerializationStreamReader}.getQuotingRegex
   * </ul>
   */
  public static final char RPC_SEPARATOR_CHAR = '|';

  /**
   * The newest supported RPC protocol version.
   */
  public static final int SERIALIZATION_STREAM_MAX_VERSION = 8;

  /**
   * The current RPC protocol version.
   */
  public static final int SERIALIZATION_STREAM_VERSION = 7;
  
  /**
   * The oldest supported RPC protocol version.
   */
  public static final int SERIALIZATION_STREAM_MIN_VERSION = 5;

  /**
   * First version to support valid JSON formatted payload responses
   */
  public static final int SERIALIZATION_STREAM_JSON_VERSION = 8;

  /**
   * Indicates that obfuscated type names should be used in the RPC payload.
   */
  public static final int FLAG_ELIDE_TYPE_NAMES = 0x1;
  
  /**
   * Indicates that RPC token is included in the RPC payload.
   */
  public static final int FLAG_RPC_TOKEN_INCLUDED = 0x2;
  
  /**
   * Bit mask representing all valid flags.
   */
  public static final int VALID_FLAGS_MASK = 0x3;

  private int flags = DEFAULT_FLAGS;
  private int version = SERIALIZATION_STREAM_VERSION;

  public final void addFlags(int flags) {
    this.flags |= flags;
  }
  
  /**
   * Checks if flags are valid.
   * 
   * @return <code>true</code> if flags are valid and <code>false</code>
   *         otherwise.
   */
  public final boolean areFlagsValid() {
    return (((flags | VALID_FLAGS_MASK) ^ VALID_FLAGS_MASK) == 0);
  }

  public final int getFlags() {
    return flags;
  }

  public final int getVersion() {
    return version;
  }

  public final boolean hasFlags(int flags) {
    return (getFlags() & flags) == flags;
  }

  public final void setFlags(int flags) {
    this.flags = flags;
  }

  protected final void setVersion(int version) {
    this.version = version;
  }

  /**
   * Parse a string containing a base-64 encoded version of a long value.
   *
   * Keep this synchronized with the version in Base64Utils.
   */
  static long longFromBase64(String value) {
    int pos = 0;
    long longVal = base64Value(value.charAt(pos++));
    int len = value.length();
    while (pos < len) {
      longVal <<= 6;
      longVal |= base64Value(value.charAt(pos++));
    }
    return longVal;
  }

  /**
   * Return an optionally single-quoted string containing a base-64 encoded
   * version of the given long value.
   *
   * Keep this synchronized with the version in Base64Utils.
   */
  static String longToBase64(long value) {
    // Convert to ints early to avoid need for long ops
    int low = (int) (value & 0xffffffff);
    int high = (int) (value >> 32);

    StringBuilder sb = new StringBuilder();
    boolean haveNonZero = base64Append(sb, (high >> 28) & 0xf, false);
    haveNonZero = base64Append(sb, (high >> 22) & 0x3f, haveNonZero);
    haveNonZero = base64Append(sb, (high >> 16) & 0x3f, haveNonZero);
    haveNonZero = base64Append(sb, (high >> 10) & 0x3f, haveNonZero);
    haveNonZero = base64Append(sb, (high >> 4) & 0x3f, haveNonZero);
    int v = ((high & 0xf) << 2) | ((low >> 30) & 0x3);
    haveNonZero = base64Append(sb, v, haveNonZero);
    haveNonZero = base64Append(sb, (low >> 24) & 0x3f, haveNonZero);
    haveNonZero = base64Append(sb, (low >> 18) & 0x3f, haveNonZero);
    haveNonZero = base64Append(sb, (low >> 12) & 0x3f, haveNonZero);
    base64Append(sb, (low >> 6) & 0x3f, haveNonZero);
    base64Append(sb, low & 0x3f, true);

    return sb.toString();
  }

  private static boolean base64Append(StringBuilder sb, int digit, boolean haveNonZero) {
    if (digit > 0) {
      haveNonZero = true;
    }
    if (haveNonZero) {
      int c;
      if (digit < 26) {
        c = 'A' + digit;
      } else if (digit < 52) {
        c = 'a' + digit - 26;
      } else if (digit < 62) {
        c = '0' + digit - 52;
      } else if (digit == 62) {
        c = '$';
      } else {
        c = '_';
      }
      sb.append((char) c);
    }
    return haveNonZero;
  }

  // Assume digit is one of [A-Za-z0-9$_]
  private static int base64Value(char digit) {
    if (digit >= 'A' && digit <= 'Z') {
      return digit - 'A';
    }
    // No need to check digit <= 'z'
    if (digit >= 'a') {
      return digit - 'a' + 26;
    }
    if (digit >= '0' && digit <= '9') {
      return digit - '0' + 52;
    }
    if (digit == '$') {
      return 62;
    }
    // digit == '_'
    return 63;
  }

}
