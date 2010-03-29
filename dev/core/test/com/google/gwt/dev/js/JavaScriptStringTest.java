// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.gwt.dev.js;

import com.google.gwt.dev.js.JsToStringGenerationVisitor;
import com.google.gwt.dev.js.rhino.TokenStream;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * Tests {@link JsToStringGenerationVisitor#javaScriptString(String)}.
 */
public class JavaScriptStringTest extends TestCase {
  private void test(String original) throws IOException {
    String escaped = JsToStringGenerationVisitor.javaScriptString(original);

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
