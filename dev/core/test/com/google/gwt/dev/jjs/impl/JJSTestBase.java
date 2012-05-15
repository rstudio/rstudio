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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.JdtCompiler.AdditionalTypeProviderDelegate;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.jjs.JavaAstConstructor;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.Strings;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.Set;
import java.util.TreeSet;

/**
 * A useful base class for tests that build JJS ASTs.
 */
public abstract class JJSTestBase extends TestCase {

  public static final String MAIN_METHOD_NAME = "onModuleLoad";

  /**
   * Finds a field with a type.
   */
  public static JField findField(JDeclaredType type, String fieldName) {
    for (JField field : type.getFields()) {
      if (field.getName().equals(fieldName)) {
        return field;
      }
    }
    return null;
  }

  /**
   * Finds a field by name, e.g. <code>Foo.field</code>.
   */
  public static JField findField(JProgram program, String qualifiedFieldName) {
    int pos = qualifiedFieldName.lastIndexOf('.');
    assertTrue(pos > 0);
    String typeName = qualifiedFieldName.substring(0, pos);
    String fieldName = qualifiedFieldName.substring(pos + 1);
    JDeclaredType type = findType(program, typeName);
    JField field = findField(type, fieldName);
    return field;
  }

  /**
   * Find a local variable declared within a JMethod.
   */
  public static JLocal findLocal(JMethod method, final String localName) {
    class LocalVisitor extends JVisitor {
      JLocal found;

      @Override
      public void endVisit(JLocal x, Context ctx) {
        if (x.getName().equals(localName)) {
          found = x;
        }
      }
    }
    LocalVisitor v = new LocalVisitor();
    v.accept(method);
    return v.found;
  }

  public static JMethod findMainMethod(JProgram program) {
    return findMethod(program, MAIN_METHOD_NAME);
  }

  public static JMethod findMethod(JDeclaredType type, String methodName) {
    for (JMethod method : type.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }

    return null;
  }

  public static JMethod findMethod(JProgram program, String methodName) {
    JDeclaredType mainType = program.getFromTypeMap("test.EntryPoint");
    return findMethod(mainType, methodName);
  }

  public static JMethod findQualifiedMethod(JProgram program, String methodName) {
    int pos = methodName.lastIndexOf('.');
    assertTrue(pos > 0);
    String typeName = methodName.substring(0, pos);
    String unqualMethodName = methodName.substring(pos + 1);
    JDeclaredType type = findType(program, typeName);
    return findMethod(type, unqualMethodName);
  }

  /**
   * Finds a type by name. The type name may be short, e.g. <code>"Foo"</code>,
   * or fully-qualified, e.g. <code>"com.google.example.Foo"</code>. If a short
   * name is used, it must be unambiguous.
   */
  public static JDeclaredType findType(JProgram program, String typeName) {
    JDeclaredType type = program.getFromTypeMap(typeName);
    if (type == null && typeName.indexOf('.') < 0) {
      // Do a slow lookup by short name.
      for (JDeclaredType checkType : program.getDeclaredTypes()) {
        if (checkType.getShortName().equals(typeName)) {
          if (type == null) {
            type = checkType;
          } else {
            fail("Ambiguous type reference '" + typeName + "' might be '"
                + type.getName() + "' or '" + checkType.getName()
                + "' (possibly more matches)");
          }
        }
      }
    }
    return type;
  }

  public static String getMainMethodSource(JProgram program) {
    JMethod mainMethod = findMainMethod(program);
    return mainMethod.getBody().toSource();
  }

  /**
   * Tweak this if you want to see the log output.
   */
  private static TreeLogger createTreeLogger() {
    boolean reallyLog = true;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.WARN);
      return logger;
    }
    return TreeLogger.NULL;
  }

  protected TreeLogger logger = createTreeLogger();

  protected final MockResourceOracle sourceOracle = new MockResourceOracle();

  private final Set<String> snippetClassDecls = new TreeSet<String>();

  private final Set<String> snippetImports = new TreeSet<String>();

  public JJSTestBase() {
    sourceOracle.add(JavaAstConstructor.getCompilerTypes());
  }

  /**
   * Adds a snippet of code, for example a field declaration, to the class that
   * encloses the snippet subsequently passed to
   * {@link #compileSnippet(String, String)}.
   */
  protected void addSnippetClassDecl(String...decl) {
    snippetClassDecls.add(Strings.join(decl, "\n"));
  }

  /**
   * Adds an import statement for any code subsequently passed to
   * {@link #compileSnippet(String, String)}.
   */
  protected void addSnippetImport(String typeName) {
    snippetImports.add(typeName);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   * 
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param codeSnippet the body of the entry method
   */
  protected JProgram compileSnippet(final String returnType,
      final String codeSnippet) throws UnableToCompleteException {
    return compileSnippet(returnType, "", codeSnippet);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   * 
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param params the parameter list of the method to compile
   * @param codeSnippet the body of the entry method
   */
  protected JProgram compileSnippet(final String returnType,
      final String params, final String codeSnippet)
      throws UnableToCompleteException {
    sourceOracle.addOrReplace(new MockJavaResource("test.EntryPoint") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        for (String snippetImport : snippetImports) {
          code.append("import " + snippetImport + ";\n");
        }
        code.append("public class EntryPoint {\n");
        for (String snippetClassDecl : snippetClassDecls) {
          code.append(snippetClassDecl + ";\n");
        }
        code.append("  public static " + returnType + " onModuleLoad(" + params
            + ") {\n");
        code.append(codeSnippet);
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    });
    addBuiltinClasses(sourceOracle);
    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, sourceOracle.getResources(),
            getAdditionalTypeProviderDelegate());
    JProgram program =
        JavaAstConstructor.construct(logger, state, "test.EntryPoint",
            "com.google.gwt.lang.Exceptions");
    return program;
  }

  protected void addBuiltinClasses(MockResourceOracle sourceOracle) {
    sourceOracle.addOrReplace(new MockJavaResource("java.lang.RuntimeException") {
      @Override
      public CharSequence getContent() {
        return "package java.lang;" +
          "public class RuntimeException extends Exception { }";
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("com.google.gwt.lang.Exceptions") {
      @Override
      public CharSequence getContent() {
        return "package com.google.gwt.lang;" +
          "public class Exceptions { static boolean throwAssertionError() { throw new RuntimeException(); } }";
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("java.lang.String") {
      @Override
      public CharSequence getContent() {
        return "package java.lang;" +
          "public class String {" +
          "  public int length() { return 0; }" +
          "  public char charAt(int pos) { return 0; }" +
          "  public String toString() { return this; }" +
          "  public int hashCode() { return 0; }" +
          "}";
      }
    });
  }
  
  /**
   * Return an AdditionalTypeProviderDelegate that will be able to provide
   * new sources for unknown classnames. 
   */
  protected AdditionalTypeProviderDelegate getAdditionalTypeProviderDelegate() {
    return null;
  }
}
