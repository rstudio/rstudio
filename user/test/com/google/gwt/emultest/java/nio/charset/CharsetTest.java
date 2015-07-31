/*
 * Copyright 2015 Google Inc.
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

package com.google.gwt.emultest.java.nio.charset;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Unit test for the {@link java.nio.charset.Charset} emulated class.
 */
public class CharsetTest extends EmulTestBase {

  public void testIso88591() {
    assertEquals("ISO-8859-1", Charset.forName("ISO-8859-1").name());
    assertEquals("ISO-8859-1", Charset.forName("iso-8859-1").name());
  }

  public void testUtf8() {
    assertEquals("UTF-8", Charset.forName("UTF-8").name());
    assertEquals("UTF-8", Charset.forName("utf-8").name());
  }

  public void testForName_null() {
    try {
      Charset.forName(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testForName_illegal() {
    try {
      Charset.forName("");
      fail();
    } catch (IllegalCharsetNameException expected) {
    }
    try {
      Charset.forName("!@#$");
      fail();
    } catch (IllegalCharsetNameException expected) {
    }
    try {
      Charset.forName("_UTF_8");
      fail();
    } catch (IllegalCharsetNameException expected) {
    }
    try {
      Charset.forName("UTF_8#");
      fail();
    } catch (IllegalCharsetNameException expected) {
    }
  }

  public void testForName_unsupported() {
    try {
      Charset.forName("qwer");
      fail();
    } catch (UnsupportedCharsetException expected) {
    }
    try {
      Charset.forName("A:.+-:_Aa0");
      fail();
    } catch (UnsupportedCharsetException expected) {
    }
  }
}
