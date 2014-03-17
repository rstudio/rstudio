/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMapImpl;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * Verifies that {@link JsNamespaceChooser} can put globals into namespaces.
 */
public class JsNamespaceChooserTest extends TestCase {
  private JsProgram program;

  // components of the jjsmap
  private JClassType comExampleFooClass =
      new JClassType(SourceOrigin.UNKNOWN, "com.example.Foo", false, false);
  private JClassType defaultPackageBarClass =
      new JClassType(SourceOrigin.UNKNOWN, "Bar", false, false);

  private Map<HasName, JsName> javaToName = Maps.newHashMap();

  public void testMoveJavaField() throws Exception {
    program = parseJs("var x = 1; x = 2;");
    mapJavaField("x");
    checkResult("var ce={};ce.x=1;ce.x=2;");
    checkNamespaceEquals("ce", "x");
  }

  public void testMoveUninitializedJavaField() throws Exception {
    program = parseJs("var x; x = 1;");
    mapJavaField("x");
    checkResult("var ce={};ce.x=1;");
    checkNamespaceEquals("ce", "x");
  }

  public void testSkipDefaultPackageMember() throws Exception {
    program = parseJs("var x = 1; x = 2;");
    mapJavaField("x", defaultPackageBarClass);
    checkResult("var x=1;x=2;");
    checkNamespaceEquals(null, "x");
  }

  public void testSkipNonJavaGlobal() throws Exception {
    program = parseJs("var x = 1; x = 2;");
    checkResult("var x=1;x=2;");
    checkNamespaceEquals(null, "x");
  }

  public void testMoveJavaFunction() throws Exception {
    program = parseJs("function f() {} f();");
    mapJavaMethod("f");
    checkResult("var ce={};ce.f=function f(){};ce.f();");
    checkNamespaceEquals("ce", "f");
  }

  public void testSkipNonJavaFunction() throws Exception {
    program = parseJs("function f() {} f();");
    checkResult("function f(){}\nf();");
    checkNamespaceEquals(null, "f");
  }

  public void testSkipPolymorphicJavaMethod() throws Exception {
    program = parseJs("var _ = {}; _.f = function f() {};");
    mapJavaMethod("f");
    checkResult("var _={};_.f=function f(){};");
    checkNamespaceEquals(null, "_");
    checkNamespaceEquals(null, "f");
  }

  public void testSkipNamedFunctionExpression() throws Exception {
    // The name in a named function expression is not a global.
    // (The scope of the name is just the JavaScript function itself.)
    program = parseJs("var a; a = function f() {}; a();");
    mapJavaMethod("f");

    checkResult("var a;a=function f(){};a();");
  }

  /** Adds a mapping from a JavaScript global to a Java field. */
  private void mapJavaField(String name) {
    mapJavaField(name, comExampleFooClass);
  }

  /** Adds a mapping from a JavaScript global to a Java field. */
  private void mapJavaField(String name, JClassType clazz) {
    JField field = new JField(SourceOrigin.UNKNOWN, name, clazz, JPrimitiveType.INT, true,
        Disposition.NONE);
    clazz.addField(field);
    javaToName.put(field, program.getScope().findExistingName(name));
  }

  /** Adds a mapping from a JavaScript global to a Java method. */
  private void mapJavaMethod(String name) {
    JMethod method = new JMethod(SourceOrigin.UNKNOWN, name, comExampleFooClass, JPrimitiveType.VOID, false,
        true, false, AccessModifier.DEFAULT);
    comExampleFooClass.addMethod(method);
    javaToName.put(method, program.getScope().findExistingName(name));
  }

  private void checkResult(String expectedJs) {
    exec();
    String actual = serializeJs(program);
    assertEquals(expectedJs, actual);
  }

  private void checkNamespaceEquals(String expectedNamespace, String global) {
    JsName globalName = program.getScope().findExistingName(global);
    assertNotNull("name doesn't exist: " + global, globalName);
    JsName actual = globalName.getNamespace();
    assertEquals("namespace is different",
        expectedNamespace, actual == null ? null : actual.getIdent());
  }

  private void exec() {
    // Prerequisite: resolve name references.
    JsSymbolResolver.exec(program);

    // Build the jjsmap.
    List<JDeclaredType> types = ImmutableList.<JDeclaredType>of(comExampleFooClass);
    Map<JsStatement, JClassType> typeForStatement = ImmutableMap.of();
    Map<JsStatement, JMethod> vtableInitForMethod = ImmutableMap.of();
    JavaToJavaScriptMapImpl jjsmap = new JavaToJavaScriptMapImpl(types, javaToName,
        typeForStatement, vtableInitForMethod);

    // Run it.
    JsNamespaceChooser.exec(program, jjsmap);
  }

  private static JsProgram parseJs(String js) throws IOException, JsParserException {
    JsProgram program = new JsProgram();
    List<JsStatement> statements = JsParser.parse(SourceOrigin.UNKNOWN, program.getScope(),
        new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(statements);
    return program;
  }

  private static String serializeJs(JsProgram program1) {
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.accept(program1);
    return text.toString();
  }
}
