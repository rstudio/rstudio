/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.emultest.java.security;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * Tests the message digest implementations.
 */
public class MessageDigestTest extends EmulTestBase {

  /**
   * Pairs of strings: test data, then MD5 hash.
   */
  private static String[] md5TestData = new String[] {
      "",
      "d41d8cd98f00b204e9800998ecf8427e",

      "a",
      "0cc175b9c0f1b6a831c399e269772661",

      "abc",
      "900150983cd24fb0d6963f7d28e17f72",

      "message digest",
      "f96b697d7cb7938d525a2f31aaf161d0",

      "abcdefghijklmnopqrstuvwxyz",
      "c3fcd3d76192e4007dfb496cca67e13b",

      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
      "d174ab98d277d9f5a5611c2c9f419d9f",

      "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
      "57edf4a22be3c955ac49da2e2107b67a",
  };

  private static void assertDigest(String expected, MessageDigest md,
      String data) throws UnsupportedEncodingException {
    byte[] bytes = data.getBytes("UTF-8");
    byte[] digest = md.digest(bytes);
    assertEquals(expected, toHexString(digest));
  }

  private static void assertDigestByByte(String expected, MessageDigest md,
      String data) throws UnsupportedEncodingException {
    byte[] bytes = data.getBytes("UTF-8");
    for (int i = 0; i < bytes.length; ++i) {
      md.update(bytes[i]);
    }
    byte[] digest = md.digest();
    assertEquals(expected, toHexString(digest));
  }

  private static String toHexString(byte[] bytes) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < bytes.length; ++i) {
      String hex = Integer.toHexString(bytes[i] & 255);
      buf.append("00".substring(hex.length())).append(hex);
    }
    return buf.toString();
  }

  public void testMd5() throws Exception {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    for (int i = 0; i < md5TestData.length; i += 2) {
      assertDigest(md5TestData[i + 1], md5, md5TestData[i]);
    }
  }

  public void testMd5ByBytes() throws Exception {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    for (int i = 0; i < md5TestData.length; i += 2) {
      assertDigestByByte(md5TestData[i + 1], md5, md5TestData[i]);
    }
  }
}
