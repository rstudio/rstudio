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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Test case for testing Jjs optimizers. Adds a convenient Result class.
 */
public abstract class OptimizerTestBase extends JJSTestBase {
  protected boolean runDeadCodeElimination = false;

  /**
   * Holds the result of optimization to compare against expected results.
   */
  protected final class Result {
    private final String returnType;
    private final String originalCode;
    private final boolean madeChanges;
    private final JProgram optimizedProgram;
    private final String methodName;

    public Result(JProgram optimizedProgram, String returnType,
        String methodName, String originalCode, boolean madeChanges) {
      this.optimizedProgram = optimizedProgram;
      this.returnType = returnType;
      this.methodName = methodName;
      this.originalCode = originalCode;
      this.madeChanges = madeChanges;
    }

    public void classHasMethods(String className, List<String> expectedMethodSnippets) {
      final JDeclaredType targetClass = findClass(className);

      Set<String> actualMethodSignatures = Sets.newHashSet();
      for (JMethod method : targetClass.getMethods()) {
        actualMethodSignatures.add(method.toString());
      }

      ImmutableSet<String> expectedMethodSignatures = FluentIterable.from(expectedMethodSnippets)
          .transform(new Function<String, String>() {
            @Nullable
            @Override
            public String apply(String unqualifiedMethodSignature) {
              return targetClass.getName() + "." + unqualifiedMethodSignature;
            }
          }).toSet();
      assertContainsAll(
          expectedMethodSignatures, actualMethodSignatures);
    }

    /**
     * Check whether the resulting is equivalent to {@code expected}.<p>
     *
     * Caveat: {@code expected} needs to be syntactically and type correct (as it will be compiled).
     * Normalizer passes for the most part are not expected to produce type correct transformations
     * as at the end the will be translated into an untyped language.
     * In some cases the test might be able to use this function even if it is testing a pass
     * that does not produce type correct program by replacing some standard mocked resources
     * (tweaking method parameter and return types).
     */
    public void into(String... expected) throws UnableToCompleteException {
      // We can't compile expected code into non-main method.
      Preconditions.checkState(methodName.equals(MAIN_METHOD_NAME));
      JProgram program = compileSnippet(returnType, Joiner.on("\n").join(expected), true);
      String expectedSource =
        OptimizerTestBase.findMethod(program, methodName).getBody().toSource();
      String actualSource =
        OptimizerTestBase.findMethod(optimizedProgram, methodName)
            .getBody().toSource();
      assertEquals(originalCode, expectedSource, actualSource);
    }

    public void intoString(String... expected) {
      String expectedSource = Joiner.on("\n").join(expected);
      String actualSource =
        OptimizerTestBase.findMethod(optimizedProgram, methodName)
            .getBody().toSource();

      // Trim surrounding {} and unindent body once
      assertTrue(actualSource.startsWith("{"));
      assertTrue(actualSource.endsWith("}"));
      actualSource = actualSource.substring(1, actualSource.length() - 2).trim();
      actualSource = Pattern.compile("^  ", Pattern.MULTILINE)
          .matcher(actualSource).replaceAll("");

      assertEquals(originalCode, expectedSource, actualSource);
    }

    public void noChange() {
      assertFalse(madeChanges);
    }

    public JMethod findMethod(String methodName) {
      return OptimizerTestBase.findMethod(optimizedProgram, methodName);
    }

    public JField findField(String fieldName) {
      return OptimizerTestBase.findField(optimizedProgram,
          "EntryPoint." + fieldName);
    }

    public JDeclaredType findClass(String className) {
      return OptimizerTestBase.findDeclaredType(optimizedProgram, className);
    }

    public JProgram getOptimizedProgram() {
      return optimizedProgram;
    }
  }

  /**
   * Asserts that there {@code method} calls all and only {@code expectedTargets}.
   */
  protected static void assertCallsAndOnlyCalls(JMethod method, JMethod... expectedTargets) {
    final Set<JMethod> actualTargets = Sets.newHashSet();
    new JVisitor() {
      @Override
      public void endVisit(JMethodCall x, Context ctx) {
        actualTargets.add(x.getTarget());
      }
    }.accept(method);
    assertEquals(ImmutableSet.copyOf(expectedTargets), actualTargets);
  }

  /**
   * Asserts that the compile fails with {@code expectedErrors}.
   */
  public final void assertCompileFails(String code, String... expectedErrors) {
    assert expectedErrors != null : "Failed compiles must specify error messages.";
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    for (String expectedError : expectedErrors) {
      builder.expectError(expectedError, null);
    }
    UnitTestTreeLogger errorLogger = builder.createLogger();

    try {
      optimize(errorLogger, "void", code);
      fail("Compile should have failed but succeeded.");
    } catch (Exception e) {
      if (!(e.getCause() instanceof UnableToCompleteException)
          && !(e instanceof UnableToCompleteException)) {
        e.printStackTrace();
        fail();
      }
    }
    errorLogger.assertCorrectLogEntries();
  }

  /**
   * Asserts that the compile succeeds with {@code expectedWarnings}.
   */
  public final Result assertCompileSucceeds(String code,
      String... expectedWarnings) throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    if (expectedWarnings != null) {
      for (String expectedWarning : expectedWarnings) {
        builder.expectWarn(expectedWarning, null);
      }
    }
    UnitTestTreeLogger errorLogger = builder.createLogger();

    try {
      return optimize(errorLogger, "void", code);
    } catch (UnableToCompleteException e) {
      fail("Compile failed");
    } finally {
      errorLogger.assertCorrectLogEntries();
    }
    return null;
  }

  /**
   * Asserts that there {@code method} only calls {@code forwardsToMethod}.
   */
  protected static void assertForwardsTo(JMethod method, JMethod forwardsToMethod) {
    assertCallsAndOnlyCalls(method, forwardsToMethod);
  }

  protected static void assertOverrides(
      Result result, String fullMethodSignature, String... overriddenMethodSignatures) {
    assertEquals(ImmutableSet.copyOf(overriddenMethodSignatures),
        findOverrides(result, fullMethodSignature));
  }

  protected static void assertParameterTypes(
      final Result result, String methodName, String... parameterTypeNames) {
    JMethod method = findMethod(result, methodName);
    assertNotNull("Did not find method " + methodName, method);
    assertEquals(parameterTypeNames.length, method.getParams().size());
    JType[] parameterTypes = FluentIterable.from(Arrays.asList(parameterTypeNames))
        .transform(new Function<String, JType>() {
          @Nullable
          @Override
          public JType apply(String typeName) {
            return findType(result, typeName);
          }
        })
        .toArray(JType.class);
    assertParameterTypes(method, parameterTypes);
  }

  protected static <T extends JNode> Collection<T> getNodes(
      final Class<T> expressionClass, JNode inNode, final boolean exactClass) {
    final List<T> result = Lists.newArrayList();
    new JVisitor() {
      @Override
      public boolean visit(JNode x, Context ctx) {
        if (x.getClass() == expressionClass || expressionClass.isInstance(x) && !exactClass) {
          result.add(expressionClass.cast(x));
        }
        return true;
      }

    }.accept(inNode);
    return result;
  }

  protected static void assertReturnType(
      Result result, String methodName, String resultTypeName) {
    JMethod method = findMethod(result, methodName);
    assertNotNull("Did not find method " + methodName, method);
    JDeclaredType resultType = result.findClass(resultTypeName);
    assertNotNull("Did not find class " + resultTypeName, resultType);
    assertEquals(resultType, method.getType().getUnderlyingType());
  }

  protected static JMethod findMethod(Result result, String methodName) {
    int lastDot = methodName.lastIndexOf(".");
    JMethod method = null;
    if (lastDot != -1) {
      String className = methodName.substring(0, lastDot);
      JDeclaredType clazz = result.findClass(className);
      assertNotNull("Did not find class " + className, clazz);
      method = clazz.findMethod(methodName.substring(lastDot + 1), true);
    } else {
      method = result.findMethod(methodName);
    }
    return method;
  }

  protected static ImmutableSet<String> findOverrides(Result result, String fullMethodSignature) {
    final Function<JMethod, String> METHOD_TO_STRING =
        new Function<JMethod, String>() {
          @Nullable
          @Override
          public String apply(JMethod method) {
            return method.toString();
          }
        };
    JMethod method = findMethod(result, fullMethodSignature);
    assertNotNull("Method " + fullMethodSignature + " not found", method);
    assertEquals(fullMethodSignature, method.toString());
    return FluentIterable
        .from(method.getOverriddenMethods())
        .transform(METHOD_TO_STRING)
        .toSet();
  }

  private static JReferenceType findType(Result result, String parameterTypeName) {
    JReferenceType parameterType;
    if (parameterTypeName.equals("null")) {
      parameterType = JReferenceType.NULL_TYPE;
    } else {
      parameterType = result.findClass(parameterTypeName);
    }
    return parameterType;
  }

  /**
   * Makes implicit <code>$clinit()</code> calls explicit to mimic the effect of other
   * optimizations. Otherwise can not test optimizations that involve <code>$clinit</code> calls
   * as they don't appear when compiling small snippets.
   *
   * @param method method to transform to make <code>$clinit</code> calls explicit.
   */
  private void insertImplicitClinitCalls(final JMethod method) {
    // Mimic the method inliner which inserts clinits calls prior to method or field dereference.
    // The actual clinit() calls might be inserted as a result of optimizations: e,g,
    // DeadCodeElimination inserts clinit calls when it removes (some) field accesses or method
    // calls.
    final JMethodBody body = (JMethodBody) method.getBody();

    new JModVisitor() {

      private JMethodCall createClinitCall(SourceInfo sourceInfo, JDeclaredType targetType) {
        JMethod clinit = targetType.getClinitTarget().getClinitMethod();
        assert (JProgram.isClinit(clinit));
        return new JMethodCall(sourceInfo, null, clinit);
      }

      private JMultiExpression createMultiExpressionForInstanceAndClinit(JExpression x) {
        JMultiExpression multi = new JMultiExpression(x.getSourceInfo());

        JMethodCall clinit = null;
        if (x instanceof JMethodCall) {
          JExpression instance = ((JMethodCall) x).getInstance();

          // Any instance expression goes first (this can happen even with statics).
          if (instance != null) {

            multi.addExpressions(instance);
            JLocal var = JProgram.createLocal(instance.getSourceInfo(), "$t", instance.getType(),
                false, body);

            JLocalRef localRef = var.makeRef(var.getSourceInfo());
            instance = new JBinaryOperation(instance.getSourceInfo(), localRef.getType(),
                JBinaryOperator.ASG, localRef, instance);
          }
          clinit = createClinitCall(x.getSourceInfo(),
              ((JMethodCall) x).getTarget().getEnclosingType());
        } else if (x instanceof JFieldRef) {
          clinit = createClinitCall(x.getSourceInfo(), ((JFieldRef) x).getEnclosingType());
        }
        // If we need a clinit call, add it first
        if (clinit != null) {
          multi.addExpressions(clinit);
        }
        multi.addExpressions(x);
        return multi;
      }

      @Override
      public void endVisit(JMethodCall x, Context ctx) {
        ctx.replaceMe(createMultiExpressionForInstanceAndClinit(x));
      }

      @Override
      public void endVisit(JFieldRef x, Context ctx) {
        ctx.replaceMe(createMultiExpressionForInstanceAndClinit(x));
      }

    }.accept(method);
  }

  protected final Result optimize(TreeLogger logger, final String returnType,
      final String... codeSnippet) throws UnableToCompleteException {
    return optimizeMethod(logger, MAIN_METHOD_NAME, returnType, codeSnippet);
  }

  protected final Result optimize(final String returnType,
      final String... codeSnippet) {

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);

    try {
      return optimize(null, returnType, codeSnippet);
    } catch (UnableToCompleteException e) {
      fail();
      return null;
    }
  }

  /**
   * Test the effect of an optimization on a JMultiExpression.
   * JMultiExpression can not be constructed from source code at the moment as it is not a valid
   * java source construct.
   *
   * @param addClinitCalls whether to insert the implicit clinit calls. This is necessary because
   *                       clinit() methods are synthetic can not be inserted explicitly as source
   *                       code calls.
   * @param returnType the return type of the JMultiExpression. Must be <code>void</code> or
   *                   compatible with the last expression.
   * @param expressionSnippets source code of the expressions.
   * @return the optimization result.
   * @throws UnableToCompleteException
   */
  protected final Result optimizeExpressions(boolean addClinitCalls, final String returnType,
                                  final String... expressionSnippets)
      throws UnableToCompleteException {

    // TODO(rluble): Not very elegant to require that the snippets be statements instead of
    // expressions.

    assert expressionSnippets.length > 0;

    // Compile as statements
    if (!returnType.equals("void")) {
      expressionSnippets[expressionSnippets.length - 1] =
          "return " + expressionSnippets[expressionSnippets.length - 1];
    }
    String snippet = Joiner.on(";\n").join(expressionSnippets) + ";\n";
    final JProgram program = compileSnippet(returnType, snippet, true);
    JMethod method = findMethod(program, MAIN_METHOD_NAME);
    JMethodBody body = (JMethodBody) method.getBody();
    JMultiExpression multi = new JMultiExpression(body.getSourceInfo());

    // Transform statement sequence into a JMultiExpression
    for (JStatement stmt : body.getStatements()) {

      if (stmt instanceof JExpressionStatement) {
        JExpressionStatement exprStmt = (JExpressionStatement) stmt;
        JExpression expr = exprStmt.getExpr();
        multi.addExpressions(expr);
      } else if (stmt instanceof JReturnStatement) {
        JReturnStatement returnStatement = (JReturnStatement) stmt;
        JExpression expr = returnStatement.getExpr();
        if (expr != null) {
            multi.addExpressions(expr);
        }
      } else {
        assert false : "Not a valid multiexpression";
      }
    }

    // Take care of the return type
    JStatement multiStm =
        returnType.equals("void") ? multi.makeStatement() : multi.makeReturnStatement();

    // Replace the method body
    JMethodBody newBody = new JMethodBody(method.getBody().getSourceInfo());
    newBody.getBlock().addStmt(multiStm);
    method.setBody(newBody);
    newBody.setMethod(method);
    if (addClinitCalls) {
      insertImplicitClinitCalls(method);
    }

    // Finally optimize.
    boolean madeChanges = doOptimizeMethod(TreeLogger.NULL, program, method);
    if (madeChanges && runDeadCodeElimination) {
      DeadCodeElimination.exec(program);
    }

    return new Result(program, returnType, MAIN_METHOD_NAME, snippet, madeChanges);
  }

  protected final Result optimizeMethod(final String methodName,
      final String mainMethodReturnType, final String... mainMethodSnippet)
      throws UnableToCompleteException {
    return optimizeMethod(null, methodName, mainMethodReturnType, mainMethodSnippet);
  }

  protected final Result optimizeMethod(TreeLogger logger, final String methodName,
      final String mainMethodReturnType, final String... mainMethodSnippet)
      throws UnableToCompleteException {
    if (logger == null) {
      logger = this.logger;
    }
    String snippet = Joiner.on("\n").join(mainMethodSnippet);
    JProgram program = compileSnippet(logger, mainMethodReturnType, snippet, true);
    JMethod method = findMethod(program, methodName);
    boolean madeChanges = doOptimizeMethod(logger, program, method);
    if (madeChanges && runDeadCodeElimination) {
      DeadCodeElimination.exec(program);
    }
    return new Result(program, mainMethodReturnType, methodName, snippet, madeChanges);
  }

  protected abstract boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method)
      throws UnableToCompleteException;
}
