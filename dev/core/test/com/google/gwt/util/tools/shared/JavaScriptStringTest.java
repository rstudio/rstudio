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
package com.google.gwt.util.tools.shared;

import com.google.gwt.dev.js.rhino.TokenStream;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * Tests {@link StringUtils#javaScriptString(String)}.
 */
public class JavaScriptStringTest extends TestCase {
  private void test(String original) throws IOException {
    String escaped = StringUtils.javaScriptString(original);

    // Parse it back
    TokenStream tokenStream = new TokenStream(new StringReader(escaped),
        "virtual file", 1);
    assertEquals(TokenStream.STRING, tokenStream.getToken());
    assertEquals(original, tokenStream.getString());

    // It should be the only token
    assertEquals(TokenStream.EOF, tokenStream.getToken());
  }

  public void testBasic() throws IOException {
    test("abc");
    test("");
    test("abc\0def");
    test("abc\\def");
    test("\u00CC\u1234\5678\uabcd");
    test("'''");
    test("\"\"\"");
    test("\b\f\n\r\t");
  }
}
