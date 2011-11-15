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

/**
 * Tests the JsStaticEval optimizer.
 */
public class JsStaticEvalTest extends OptimizerTestBase {

  public void testAddLiterals() throws Exception {
    assertEquals("alert(42);", optimize("alert(21+21);"));
    assertEquals("alert('Hello World');", optimize("alert('Hello '+'World');"));
    assertEquals("alert('Hello 42');", optimize("alert('Hello ' + 42);"));
    assertEquals("alert('42 Hello');", optimize("alert(42 + ' Hello');"));
    assertEquals("alert('42 Hello');", optimize("alert(42.0 + ' Hello');"));
    assertEquals("alert('42.2 Hello');", optimize("alert(42.2 + ' Hello');"));
    assertEquals("alert('Hello 42.2');", optimize("alert('Hello ' + 42.2);"));
  }

  public void testAssociativity() throws Exception {
    // Simple test
    assertEquals("alert(a||b||c||d);", optimize("alert((a||b)||(c||d));"));
    assertEquals("alert(a&&b&&c&&d);", optimize("alert((a&&b)&&(c&&d));"));

    // Preserve precedence
    assertEquals("alert((a||b)&&(c||d));",
        optimize("alert((a || b) && (c || d));"));
    assertEquals("alert(a&&b||c&&d);",
        optimize("alert((a && b) || ( c && d));"));
    assertEquals("a(),b&&c();", optimize("a(), b && c()"));
    assertEquals("a()&&b,c();", optimize("a() && b, c()"));

    // Don't damage math expressions
    assertEquals("alert(seconds/(60*60));",
        optimize("alert(seconds / (60 * 60))"));
    assertEquals("alert(1-(1-foo));", optimize("alert(1 - (1 - foo))"));

    // Don't damage assignments
    assertEquals("alert((a=0,b=foo));",
        optimize("alert((a = 0, b = (bar, foo)))"));
    assertEquals("alert(1+(a='2')+3+4);",
        optimize("alert(1 + (a = '2') + 3 + 4);"));
    assertEquals("alert(1+(a='2')+7);",
        optimize("alert(1 + (a = '2') + (3 + 4));"));

    // Break comma expressions up
    assertEquals("alert((a(),b(),c(),d));",
        optimize("alert(((a(),b()),(c(),d)));"));
    // and remove expressions without side effects
    assertEquals("alert(d);", optimize("alert(((a,b),(c,d)));"));

    // Pattern of coercing a numeric add operation to a string
    assertEquals("alert(''+(a+b));", optimize("alert('' + (a + b))"));
    assertEquals("alert('foo'+(a+b));",
        optimize("alert('foo' + ('' + (a + b)))"));

    // Tests involving numeric and string literals and identifiers
    assertEquals("alert(21+(1+$foo));",
        optimize("alert((20 + 1) + (1 + $foo));"));
    assertEquals("alert('211'+$foo);",
        optimize("alert((20 + 1) + ('1' + $foo));"));
    assertEquals("alert('2011'+$foo);",
        optimize("alert((20 + '1') + ('1' + $foo));"));
    assertEquals("alert('2011'+$foo);",
        optimize("alert(('20' + 1) + ('1' + $foo));"));
    assertEquals("alert('2011'+$foo);",
        optimize("alert(('20' + '1') + ('1' + $foo));"));

    // These are also tricky, because $foo could be non-numeric
    assertEquals("alert($foo+1+21);", optimize("alert(($foo + 1) + (20 + 1));"));
    assertEquals("alert($bar+13+7+(2+$foo));",
        optimize("alert((($bar + (10 + 3)) + (2 + 5)) + (2 + $foo));"));

    // Without type info, there's nothing that can be done for this expr
    assertEquals("alert($foo+($bar+($baz+$quux)));",
        optimize("alert($foo + ($bar + ($baz + $quux)));"));
  }

  public void testIfWithEmptyThen() throws Exception {
    assertEquals("a();", optimize("if (a()) { }"));
  }

  public void testIfWithEmptyThenAndElseExpression() throws Exception {
    assertEquals("a()||b();", optimize("if (a()) { } else { b(); }"));
  }

  public void testIfWithEmptyThenAndElse() throws Exception {
    assertEquals("if(!a()){throw 1}",
        optimize("if (a()) { } else { throw 1; }"));
  }

  public void testIfWithEmptyThenAndEmptyElse() throws Exception {
    assertEquals("a();", optimize("if (a()) { } else { }"));
  }

  public void testIfWithThenAndEmptyElse() throws Exception {
    assertEquals("if(a()){throw 1}", optimize("if (a()) { throw 1; } else { }"));
  }

  public void testIfWithThenExpressionAndEmptyElse() throws Exception {
    assertEquals("a()&&b();", optimize("if (a()) { b() } else { }"));
  }

  public void testIfWithThenExpressionAndElseExpression() throws Exception {
    assertEquals("a()?b():c();", optimize("if (a()) { b() } else { c(); }"));
  }

  public void testIfWithThenExpressionAndElseStatement() throws Exception {
    // This can't be optimized further
    assertEquals("if(a()){b()}else{throw 1}",
        optimize("if (a()) { b() } else { throw 1; }"));
  }

  public void testLiteralCompares() throws Exception {
    assertEquals("alert(false);", optimize("alert(2 != 2)"));
    assertEquals("alert(false);", optimize("alert(2 == 3)"));
    assertEquals("alert(true);", optimize("alert(2 == 2)"));
    assertEquals("alert(true);", optimize("alert(2 != 3)"));
    assertEquals("alert(true);", optimize("alert(2 < 3)"));
    assertEquals("alert(true);", optimize("alert(3 <= 3)"));
    assertEquals("alert(true);", optimize("alert(3 > 2)"));
    assertEquals("alert(true);", optimize("alert(3 >= 3)"));
    assertEquals("alert(false);", optimize("alert(2 > 3)"));
    assertEquals("alert(false);", optimize("alert(2 >= 3)"));
    assertEquals("alert(false);", optimize("alert(3 < 2)"));
    assertEquals("alert(false);", optimize("alert(3 <= 2)"));
    assertEquals("alert(false);", optimize("alert(1.8E+10308 < 1.9E+10308)"));
    assertEquals("alert(false);", optimize("alert(1.8E+10308 > 1.9E+10308)"));
  }

  public void testLiteralEqNull() throws Exception {
    assertEquals("alert(false);", optimize("alert('test' == null)"));
  }

  public void testLiteralNeNull() throws Exception {
    assertEquals("alert(true);", optimize("alert('test' != null)"));
  }

  public void testNullEqNull() throws Exception {
    assertEquals("alert(true);", optimize("alert(null == null)"));
  }

  public void testNullNeNull() throws Exception {
    assertEquals("alert(false);", optimize("alert(null != null)"));
  }

  private String optimize(String js) throws Exception {
    return optimize(js, JsStaticEval.class);
  }
}
