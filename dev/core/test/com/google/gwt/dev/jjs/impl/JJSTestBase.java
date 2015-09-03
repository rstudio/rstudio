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
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.PrecompileTaskOptionsImpl;
import com.google.gwt.dev.cfg.MockModuleDef;
import com.google.gwt.dev.javac.CheckerTestCase;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.JdtCompiler.AdditionalTypeProviderDelegate;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.jjs.JavaAstConstructor;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.arg.OptionJsInteropMode;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A useful base class for tests that build JJS ASTs.
 */
public abstract class JJSTestBase extends CheckerTestCase {

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
    JDeclaredType type = findDeclaredType(program, typeName);
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

  public static JMethod findMethod(JDeclaredType type, String methodNameOrSignature) {
    // Signatures and names never collide (names never have parens but signatures always do).
    for (JMethod method : type.getMethods()) {
      if (method.getSignature().equals(methodNameOrSignature) ||
          method.getName().equals(methodNameOrSignature)) {
        return method;
      }
    }

    return null;
  }

  public static JMethod findMethod(JProgram program, String methodName) {
    int lastDot = methodName.lastIndexOf(".");
    if (lastDot != -1) {
      String className = methodName.substring(0, lastDot);
      JDeclaredType clazz = program.getFromTypeMap(className);
      assertNotNull("Did not find class " + className, clazz);
      return clazz.findMethod(methodName.substring(lastDot + 1), true);
    }
    return findMethod(program.getFromTypeMap("test.EntryPoint"), methodName);
  }

  public static JMethod findQualifiedMethod(JProgram program, String methodName) {
    int pos = methodName.lastIndexOf('.');
    assertTrue(pos > 0);
    String typeName = methodName.substring(0, pos);
    String unqualMethodName = methodName.substring(pos + 1);
    JDeclaredType type = findDeclaredType(program, typeName);
    return findMethod(type, unqualMethodName);
  }

  private static Pattern typeNamePattern = Pattern.compile("([^\\[\\]]*)((?:\\[\\])*)");

  /**
   * Finds a type by name including arrays and primitives.
   */
  public static JType findType(JProgram program, String typeName) {
    Matcher matcher = typeNamePattern.matcher(typeName);
    if (!matcher.matches()) {
      return null;
    }
    String bareName = matcher.group(1);
    int dimensions = matcher.group(2).length() / 2;

    JType type = JPrimitiveType.getType(bareName);
    if (type == null) {
      type = findDeclaredType(program, bareName);
    }

    if (type != null && dimensions > 0) {
      type = program.getOrCreateArrayType(type, dimensions);
    }
    return type;
  }

  /**
   * Finds a declared type by name. The type name may be short, e.g. <code>"Foo"</code>,
   * or fully-qualified, e.g. <code>"com.google.example.Foo"</code>. If a short
   * name is used, it must be unambiguous.
   */
  public static JDeclaredType findDeclaredType(JProgram program, String typeName) {
    JDeclaredType type = program.getFromTypeMap(typeName);
    if (type != null || typeName.indexOf('.') != -1) {
      return type;
    }
    // Do a slow lookup by short name.
    for (JDeclaredType checkType : program.getDeclaredTypes()) {
      if (!checkType.getShortName().equals(typeName)) {
        continue;
      }
      if (type != null) {
        fail("Ambiguous type reference '" + typeName + "' might be '"
            + type.getName() + "' or '" + checkType.getName()
            + "' (possibly more matches)");
      }
      type = checkType;
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

  private final List<String> snippetClassDecls = Lists.newArrayList();

  private final Set<String> snippetImports = Sets.newTreeSet();

  public JJSTestBase() {
    sourceOracle.add(JavaAstConstructor.getCompilerTypes());
  }

  /**
   * Adds a snippet of code, for example a field declaration, to the class that
   * encloses the snippet subsequently passed to
   * {@link #compileSnippet(String, String, boolean)}.
   */
  protected void addSnippetClassDecl(String...decl) {
    snippetClassDecls.add(Joiner.on("\n").join(decl));
  }

  /**
   * Adds an import statement for any code subsequently passed to
   * {@link #compileSnippet(String, String, boolean)}.
   */
  protected void addSnippetImport(String typeName) {
    snippetImports.add(typeName);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   * @param logger a logger where to log, the default logger will be used if null.
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param codeSnippet the body of the entry method
   */
  protected JProgram compileSnippet(TreeLogger logger,  final String returnType,
      final String codeSnippet) throws UnableToCompleteException {
    return compileSnippet(logger, returnType, "", codeSnippet, false);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   * @param logger a logger where to log, the default logger will be used if null.
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param codeSnippet the body of the entry method
   * @param staticMethod whether to make the method static
   */
  protected JProgram compileSnippet(TreeLogger logger,  final String returnType,
      final String codeSnippet, boolean staticMethod) throws UnableToCompleteException {
    return compileSnippet(logger, returnType, "", codeSnippet, staticMethod);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param codeSnippet the body of the entry method
   * @param staticMethod whether to make the method static
   */
  protected JProgram compileSnippet(final String returnType,
      final String codeSnippet, boolean staticMethod) throws UnableToCompleteException {
    return compileSnippet(logger, returnType, "", codeSnippet, staticMethod);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param codeSnippet the body of the entry method
   */
  protected JProgram compileSnippet(final String returnType,
      final String codeSnippet) throws UnableToCompleteException {
    return compileSnippet(logger, returnType, "", codeSnippet, false);
  }

  /**
   * Returns the program that results from compiling the specified code snippet
   * as the body of an entry point method.
   *
   * @param logger a logger where to log, the default logger will be used if null.
   * @param returnType the return type of the method to compile; use "void" if
   *          the code snippet has no return statement
   * @param params the parameter list of the method to compile
   * @param codeSnippet the body of the entry method
   * @param staticMethod whether the entryPoint should be static
   */
  protected JProgram compileSnippet(TreeLogger logger, final String returnType,
      final String params, final String codeSnippet, final boolean staticMethod)
      throws UnableToCompleteException {
    sourceOracle.addOrReplace(new MockJavaResource("test.EntryPoint") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package test;\n");
        for (String snippetImport : snippetImports) {
          code.append("import " + snippetImport + ";\n");
        }
        code.append("public class EntryPoint {\n");
        for (String snippetClassDecl : snippetClassDecls) {
          code.append(snippetClassDecl + ";\n");
        }
        code.append("  public " + (staticMethod ? "static " : "") + returnType + " onModuleLoad(" +
            params + ") {\n");
        code.append(codeSnippet);
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    });
    CompilerContext compilerContext = provideCompilerContext();

    if (logger == null)  {
      logger = this.logger;
    }
    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, compilerContext,
            sourceOracle.getResources(), getAdditionalTypeProviderDelegate());
    JProgram program =
        JavaAstConstructor.construct(logger, state, compilerContext,
            null, "test.EntryPoint", "com.google.gwt.lang.Exceptions");
    return program;
  }

  /**
   * Returns a compiler context to be used for compiling code within the test.
   */
  protected CompilerContext provideCompilerContext() {
    CompilerContext compilerContext = new CompilerContext.Builder().module(new MockModuleDef())
        .options(new PrecompileTaskOptionsImpl() {
                   @Override
                   public boolean shouldJDTInlineCompileTimeConstants() {
                     return false;
                   }
                 }
        ).build();

    compilerContext.getOptions().setSourceLevel(sourceLevel);
    compilerContext.getOptions().setStrict(true);
    compilerContext.getOptions().setJsInteropMode(OptionJsInteropMode.Mode.JS);
    return compilerContext;
  }

  /**
   * Return an AdditionalTypeProviderDelegate that will be able to provide
   * new sources for unknown classnames.
   */
  protected AdditionalTypeProviderDelegate getAdditionalTypeProviderDelegate() {
    return null;
  }

  /**
   * Java source level compatibility option.
   */
  protected SourceLevel sourceLevel = SourceLevel.DEFAULT_SOURCE_LEVEL;

  protected static <T> void assertContainsAll(Iterable<T> expectedMethodSnippets,
      Set<T> actualMethodSnippets) {
    List<T> missing = FluentIterable.from(expectedMethodSnippets)
        .filter(Predicates.not(Predicates.in(actualMethodSnippets)))
        .toList();
    assertTrue(missing + " not contained in " + actualMethodSnippets, missing.size() == 0);
  }

  protected void assertEqualBlock(String expected, String input)
      throws UnableToCompleteException {
    JBlock testExpression = getStatement(input);
    assertEquals(formatSource("{ " + expected + "}"),
        formatSource(testExpression.toSource()));
  }

  protected static void assertParameterTypes(JMethod method, JType... parameterTypes) {
    for (int i = 0; i < parameterTypes.length; i++) {
      JType parameterType = parameterTypes[i];
      assertEquals(parameterType, method.getParams().get(i).getType().getUnderlyingType());
    }
  }

  protected static void assertParameterTypes(
      final JProgram program, String methodName, String... parameterTypeNames) {
    JMethod method = findMethod(program, methodName);
    assertNotNull("Did not find method " + methodName, method);
    assertEquals(parameterTypeNames.length, method.getParams().size());
    JType[] parameterTypes = FluentIterable.from(Arrays.asList(parameterTypeNames))
        .transform(new Function<String, JType>() {
          @Override
          public JType apply(String typeName) {
            return findType(program, typeName);
          }
        })
        .toArray(JType.class);
    assertParameterTypes(method, parameterTypes);
  }

  protected static void assertReturnType(
      JProgram program, String methodName, String resultTypeName) {
    JMethod method = findMethod(program, methodName);
    assertNotNull("Did not find method " + methodName, method);
    JDeclaredType resultType = program.getFromTypeMap(resultTypeName);
    assertNotNull("Did not find class " + resultTypeName, resultType);
    assertEquals(resultType, method.getType().getUnderlyingType());
  }

  public Result assertTransform(String codeSnippet, JVisitor visitor)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", codeSnippet, true);
    JMethod mainMethod = findMainMethod(program);
    visitor.accept(mainMethod);
    return new Result("void", codeSnippet, mainMethod.getBody().toSource());
  }

  protected JMethod getMethod(JProgram program, String name) {
    return findMethod(program, name);
  }

  protected JReferenceType getType(JProgram program, String name) {
    return program.getFromTypeMap(name);
  }

  protected JBlock getStatement(String statement)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", statement, false);
    JMethod mainMethod = findMainMethod(program);
    JMethodBody body = (JMethodBody) mainMethod.getBody();
    return body.getBlock();
  }

  /**
   * Removes most whitespace while still leaving one space separating words.
   *
   * Used to make the assertEquals ignore whitespace (mostly) while still retaining meaningful
   * output when the test fails.
   */
  protected String formatSource(String source) {
    return source.replaceAll("\\s+", " ") // substitutes multiple whitespaces into one.
      .replaceAll("\\s([\\p{Punct}&&[^$]])", "$1")  // removes whitespace preceding symbols
                                                    // (except $ which can be part of an identifier)
      .replaceAll("([\\p{Punct}&&[^$]])\\s", "$1"); // removes whitespace succeeding symbols.
  }

  protected void addAll(Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      if (sourceFile != null) {
        sourceOracle.addOrReplace(sourceFile);
      }
    }
  }

  protected JExpression getExpression(String type, String expression)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(type, "return " + expression + ";", false);
    JMethod mainMethod = findMainMethod(program);
    JMethodBody body = (JMethodBody) mainMethod.getBody();
    JReturnStatement returnStmt = (JReturnStatement) body.getStatements().get(0);
    return returnStmt.getExpr();
  }

  /**
   * Holds the result of a optimizations to compare with expected results.
   */
  protected final class Result {
    private final String optimized;
    private final String returnType;
    private final String userCode;

    public Result(String returnType, String userCode, String optimized) {
      this.returnType = returnType;
      this.userCode = userCode;
      this.optimized = optimized;
    }

    public void into(String expected) throws UnableToCompleteException {
      JProgram program = compileSnippet(returnType, expected, true);
      expected = getMainMethodSource(program);
      assertEquals(userCode, expected, optimized);
    }
  }
}
