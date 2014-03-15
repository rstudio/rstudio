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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsProgram;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * Tests {@link JsParser}.
 */
public class JsParserTest extends TestCase {
  /**
   * The result of parsing Js code.
   */
  public class Result {

    private String source;
    private JsParserException exception;

    public Result(JsBlock jsBlock) {
      this.source = jsBlock.toSource().replaceAll("\\s+", " ");
    }

    public Result(JsParserException e) {
      this.exception = e;
    }

    public void into(String expected) throws JsParserException {
      if (exception != null) {
        throw exception;
      }
      assertEquals(expected, source);
    }

    public void error(String expectedMsg) {
      assertNotNull("No JsParserException was thrown", exception);
      assertEquals(expectedMsg, exception.getMessage());
    }
  }

  public void testBasic() throws JsParserException {
    parse("foo").into("foo; ");
    parse(" foo ").into("foo; ");
    parse("foo()").into("foo(); ");
    parse("foo(); bar()").into("foo(); bar(); ");
    parse("window.alert('3' + 3)").into("window.alert('3' + 3); ");
    parse("{ foo() }").into("{ foo(); } ");
  }

  public void testParseErrors() {
    parse("1a2b").error(
        "test.js(1): missing ; before statement\n> 1a2b\n> ----^");
    parse("foo(").error("test.js(1): syntax error\n> \n> ^");
    parse("+").error("test.js(1): syntax error\n> \n> ^");
    parse(")").error("test.js(1): syntax error\n> )\n> -^");
    parse("}").error("test.js(1): syntax error\n> }\n> -^");
    parse("foo();\n}").error("test.js(2): syntax error\n> }\n> -^");
    parse("foo();\nbar;\n}").error("test.js(3): syntax error\n> }\n> -^");
  }

  private Result parse(String js) {
    try {
      JsProgram program = new JsProgram();
      SourceInfo rootSourceInfo = program.createSourceInfo(1, "test.js");
      JsBlock block = program.getGlobalBlock();
      JsParser.parseInto(rootSourceInfo, program.getScope(), block,
          new StringReader(js));
      return new Result(block);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected error reading in-memory stream", e);
    } catch (JsParserException e) {
      return new Result(e);
    }
  }
}
