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
package com.google.gwt.user.server;

import junit.framework.TestCase;

/**
 * Tests for the {@link com.google.gwt.user.server.Base64Utils Base64Utils} class.
 */
public class Base64Test extends TestCase {

  /**
   * Tests that base 64 encoding/decoding round trips are lossless. 
   */
  public static void testBase64Utils() {
    base64RoundTrip((byte[]) null);
    base64RoundTrip(new byte[0]);

    java.util.Random r = new java.util.Random(100);
    for (int i = 0; i < 10000; i++) {
      base64RoundTrip(r);
    }
  }

  private static void base64RoundTrip(java.util.Random r) {
    int len = r.nextInt(10);
    byte[] b1 = new byte[len];
    r.nextBytes(b1);

    base64RoundTrip(b1);
  }

  private static void base64RoundTrip(byte[] b1) {
    String s = Base64Utils.toBase64(b1);
    if (b1 == null) {
      assert s == null;
    } else {
      assert s != null;
      if (b1.length == 0) {
        assert s.length() == 0;
      } else {
        assert s.length() != 0;
      }
    }

    byte[] b2 = Base64Utils.fromBase64(s);
    if (b1 == null) {
      assert b2 == null;
      return;
    }
    assert b2 != null;
    assert (b1.length == b2.length);

    for (int i = 0; i < b1.length; i++) {
      assert b1[i] == b2[i];
    }
  }
}
