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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.List;

public class JsToStringGenerationVisitorConcisenessTest extends TestCase {

  public void testComplexDecrement() throws Exception {
    String output = parse("var x = -(-(-(--y)))");
    assertEquals("var x=- - - --y", output);
  }

  public void testComplexIncrement() throws Exception {
    String output = parse("var x = (y++) + (-(--z))");
    assertEquals("var x=y+++- --z", output);
  }

  public void testConstruction() throws Exception {
    String output = parse("new a.b(c,d)");
    assertEquals("new a.b(c,d)", output);
  }

  public void testIncrement() throws Exception {
    // y++-x is valid
    assertEquals("var x=y++-z", parse("var x = (y++) - z"));
  }

  public void testObjectLiteralAssignment() throws Exception {
    assertEquals("var x={a:b=2,c:d}", parse("var x = {a : (b = 2), c : d}"));
  }

  public void testObjectLiteralConditional() throws Exception {
    // the parentheses are not required around the conditional
    assertEquals("var x={a:b?c:d,e:f}",
        parse("var x = {a : (b ? c : d), e : f}"));
  }

  public void testObjectLiteralDeclarationConcise() throws Exception {
    // quotes are not necessary around many property variables in object
    // literals
    assertEquals("var x={1:'b'}", parse("var x = {1 : 'b'}"));
    assertEquals("var x={$a_:'b'}", parse("var x = {'$a_' : 'b'}"));
    assertEquals("var x={1.2:'b'}", parse("var x = {1.2 : 'b'}"));
  }

  public void testLastStatement() throws Exception {
    assertEquals("function(){x++;i++}", parse("function() {x++;i++;}"));
    assertEquals("function(){do{}while(x)}",
        parse("function() {do{} while(x);}"));
    assertEquals("function(){if(b){}}", parse("function() {if(b){}}"));
  }

  private String parse(String js) throws Exception {
    List<JsStatement> statements = JsParser.parse(SourceOrigin.UNKNOWN,
        new JsProgram().getScope(), new StringReader(js));
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsToStringGenerationVisitor(text);
    generator.acceptList(statements);
    return text.toString();
  }
}
