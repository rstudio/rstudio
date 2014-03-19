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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.rhino.EvaluatorException;
import com.google.gwt.dev.js.rhino.TokenStream;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * Tests class {@link TokenStream}.
 */
public class TokenStreamTest extends TestCase {
  private static class Token {
    final String text;
    final int type;

    public Token(int type, String text) {
      this.type = type;
      this.text = text;
    }
  }

  private static Token scanToken(String tokenString) throws EvaluatorException,
      IOException {
    TokenStream tokStream = new TokenStream(new StringReader(tokenString),
        "test input", 1);
    int type = tokStream.getToken();
    String text = tokStream.getString();
    return new Token(type, text);
  }

  public void testJsniRefs() throws IOException {
    // Field refs
    assertGoodJsni("@org.group.Foo::bar");

    // Method refs
    assertGoodJsni("@org.group.Foo::bar()");
    assertGoodJsni("@org.group.Foo::bar(I)");
    assertGoodJsni("@org.group.Foo::bar(IJ)");
    assertGoodJsni("@org.group.Foo::bar(Lorg/group/Foo;)");
    assertGoodJsni("@org.group.Foo::bar(Lorg/group/Foo;Lorg/group/Bar;)");
    assertGoodJsni("@org.group.Foo::bar([I)");
    // The following is currently tolerated
    // assertBadJsni("@org.group.Foo::bar(Lorg/group/Foo)");
    assertBadJsni("@org.group.Foo::bar(A)");
    assertBadJsni("@org.group.Foo::bar(L)");

    // Method refs with * as the parameter list
    assertGoodJsni("@org.group.Foo::bar(*)");
    assertBadJsni("@org.group.Foo::bar(*");

    // References to default or imported class
    assertGoodJsni("@Foo::bar");

    // Refs that span lines
    assertGoodJsni("@org.group.Foo::bar(\nLorg/group/Foo;)");
    assertGoodJsni("@org.group.Foo::bar(\nLorg/group/Foo;\n)");
    assertGoodJsni("@org.group.Foo::bar(\n[I\n)");
    assertGoodJsni("@org.group.Foo::bar(\nZ\nZ\n)");
    assertGoodJsni("@org.group.Foo::bar(\nLorg/group/Foo;\nZ)");
    assertGoodJsni("@org.group.Foo::\nbar()");


    assertBadJsni("@org.group.Foo::bar(\nLorg/group/Foo;,\nZ)");
    assertBadJsni("@org.group.Foo::bar(\nLorg/group/Foo,\nZ)");

    // bad references
    assertBadJsni("@");
    assertBadJsni("@org.group.Foo.bar");
    assertBadJsni("@org.group.Foo:");
    assertBadJsni("@org.group.Foo::");
    assertBadJsni("@org.group.Foo::(");
    assertBadJsni("@::bar()");
    assertBadJsni("@bar");
  }

  private void assertBadJsni(String token) throws IOException {
    try {
      scanToken(token);
      fail("Expected a token scanning error for " + token);
    } catch (EvaluatorException e) {
      // expected
    }
  }

  private void assertGoodJsni(String jsniRef) throws IOException {
    Token token = scanToken(jsniRef);
    assertEquals(TokenStream.NAME, token.type);
    // The token.text won't have any whitespace included.
    assertEquals(jsniRef.replaceAll("\\s", ""), token.text);
  }
}
