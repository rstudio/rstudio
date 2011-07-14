/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for the {@link JsoDevirtualizer} visitor.
 */
public class JsoDevirtualizerTest extends OptimizerTestBase {

  /**
   * JsoDevirtualizer should allow dual Java/JSO implementations of the same
   * interface, so long as there is only one of each. If there are multiple
   * methods with the same method name, it should distinguish between them.
   */
  public void testDualJsoImpl() throws UnableToCompleteException {

    sourceOracle.addOrReplace(new MockJavaResource("com.google.gwt.lang.Cast") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.lang;");
        code.append("public class Cast {");
        code.append("  public static boolean isJavaObject(Object o) { return true; };");
        code.append("  public static boolean isJavaScriptObject(Object o) { return true; };");
        code.append("}");
        return code;
      }
    });
    
    addSnippetImport("com.google.gwt.lang.Cast");
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    
    addSnippetClassDecl(
        "interface Iface1 { int a(); }",
        "static class J1 implements Iface1 {",
        "  public int a() { return 1; }",
        "}",
        "static class Jso1 extends JavaScriptObject implements Iface1 {",
        "  protected Jso1() { }",
        "  public final int a() { return 2; }",
        "  public static native Jso1 create() /*-{ return {} }-*/;",
        "}",
        "static interface Iface2 { int a(); }",
        "static class J2 implements Iface2 {",
        "  public int a() { return 3; }",
        "}",
        "static class Jso2 extends JavaScriptObject implements Iface2 {",
        "  protected Jso2() { }", "  public final int a() { return 4; }",
        "  public static native Jso2 create() /*-{ return {} }-*/;", 
        "}",
        "static Iface1 val1 = new J1();",
        "static Iface1 val2 = Jso1.create();",
        "static Iface2 val3 = new J2();",
        "static Iface2 val4 = Jso2.create();");        

    StringBuilder code = new StringBuilder();    
    code.append("int result = val1.a() + val2.a() + val3.a() + val4.a();");
            
    // The salient point in the results below is that the JSO method used for
    // val1 and val1 has a different name the method used for val2 and val3.
    StringBuffer expected = new StringBuffer();
    expected.append("int result = ");
    expected.append("JavaScriptObject.a__devirtual$(EntryPoint.val1) + ");
    expected.append("JavaScriptObject.a__devirtual$(EntryPoint.val2) + ");
    expected.append("JavaScriptObject.a0__devirtual$(EntryPoint.val3) + ");
    expected.append("JavaScriptObject.a0__devirtual$(EntryPoint.val4);");
    
    optimize("void", code.toString()).intoString(expected.toString());
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    JsoDevirtualizer.exec(program);
    return true;
  }
}
