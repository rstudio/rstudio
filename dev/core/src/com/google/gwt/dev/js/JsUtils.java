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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.HasJsInfo.JsMemberType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.List;

import java.util.regex.Pattern;

/**
 * Utils for JS AST.
 */
public class JsUtils {
  /**
   * Given a JsInvocation, determine if it is invoking a JsFunction that is
   * specified to be executed only once during the program's lifetime.
   */
  public static JsFunction isExecuteOnce(JsInvocation invocation) {
    JsFunction f = isFunction(invocation.getQualifier());
    if (f != null && f.isClinit()) {
      return f;
    }
    return null;
  }

  /**
   * Given an expression, determine if it is a JsNameRef that refers to a
   * statically-defined JsFunction.
   */
  public static JsFunction isFunction(JsExpression e) {
    if (!(e instanceof JsNameRef)) {
      return null;
    }

    JsNameRef ref = (JsNameRef) e;

    // Unravel foo.call(...).
    if (!ref.getName().isObfuscatable() && CALL_STRING.equals(ref.getIdent())) {
      if (ref.getQualifier() instanceof JsNameRef) {
        ref = (JsNameRef) ref.getQualifier();
      }
    }

    JsNode staticRef = ref.getName().getStaticRef();
    if (staticRef instanceof JsFunction) {
      return (JsFunction) staticRef;
    }
    return null;
  }

  public static JsExpression createAssignment(JsExpression lhs, JsExpression rhs) {
    return createAssignment(lhs.getSourceInfo(), lhs, rhs);
  }

  public static JsExpression createAssignment(SourceInfo info, JsExpression lhs, JsExpression rhs) {
    return new JsBinaryOperation(info, JsBinaryOperator.ASG, lhs, rhs);
  }

  public static JsFunction createBridge(JMethod method, JsName polyName, JsScope scope) {
    SourceInfo sourceInfo = method.getSourceInfo();
    JsFunction bridge = new JsFunction(sourceInfo, scope);
    for (JParameter p : method.getParams()) {
      JsName name = bridge.getScope().declareName(p.getName());
      bridge.getParameters().add(new JsParameter(sourceInfo, name));
    }
    JsNameRef ref = polyName.makeRef(sourceInfo);
    ref.setQualifier(new JsThisRef(sourceInfo));
    List<JsExpression> args = Lists.newArrayList();
    for (JsParameter p : bridge.getParameters()) {
      args.add(p.getName().makeRef(sourceInfo));
    }

    JsExpression invocation =
        createInvocationOrPropertyAccess(sourceInfo, method.getJsMemberType(), ref, args);

    JsBlock block = new JsBlock(sourceInfo);
    if (method.getType() == JPrimitiveType.VOID) {
      block.getStatements().add(invocation.makeStmt());
    } else {
      block.getStatements().add(new JsReturn(sourceInfo, invocation));
    }
    bridge.setBody(block);
    return bridge;
  }

  public static JsExpression createCommaExpression(JsExpression... expressions) {
    return createCommaExpressionHelper(expressions, 0);
  }

  private static JsExpression createCommaExpressionHelper(JsExpression[] expressions, int index) {
    int remainingExpressions = expressions.length - index;
    assert remainingExpressions >= 2;

    JsExpression lhs = expressions[index];
    JsExpression rhs = expressions[index + 1];
    if (remainingExpressions > 2) {
      rhs = createCommaExpressionHelper(expressions, index + 1);
    }

    // Construct the binary expression
    if (rhs == null) {
      return lhs;
    } else if (lhs == null) {
      return rhs;
    }
    return new JsBinaryOperation(lhs.getSourceInfo(), JsBinaryOperator.COMMA, lhs, rhs);
  }

  public static JsFunction createEmptyFunctionLiteral(SourceInfo info, JsScope scope, JsName name) {
    JsFunction func = new JsFunction(info, scope, name);
    func.setBody(new JsBlock(info));
    return func;
  }

  public static JsNameRef createQualifiedNameRef(SourceInfo info,  JsName... names) {
    JsNameRef result = null;
    for (JsName name : names) {
      if (result == null) {
        result = name.makeRef(info);
        continue;
      }
      result = name.makeQualifiedRef(info, result);
    }
    return result;
  }

  public static JsExpression createInvocationOrPropertyAccess(SourceInfo sourceInfo,
      JsMemberType memberType, JsNameRef reference, List<JsExpression> args) {
    switch (memberType) {
      case SETTER:
        assert args.size() == 1;
        return createAssignment(reference, args.get(0));
      case GETTER:
        assert args.size() == 0;
        return reference;
      default:
        return new JsInvocation(sourceInfo, reference, args);
    }
  }

  /**
   * Given a string qualifier such as 'foo.bar.Baz', returns a chain of JsNameRef's representing
   * this qualifier.
   */
  public static JsNameRef createQualifiedNameRef(String namespace, SourceInfo sourceInfo) {
    assert !namespace.isEmpty();
    JsNameRef ref = null;
    for (String part : namespace.split("\\.")) {
      JsNameRef newRef = new JsNameRef(sourceInfo, part);
      if (ref != null) {
        newRef.setQualifier(ref);
      }
      ref = newRef;
    }
    return ref;
  }

  /**
   * Attempts to extract a single expression from a given statement and returns
   * it. If no such expression exists, returns <code>null</code>.
   */
  public static JsExpression extractExpression(JsStatement stmt) {
    if (stmt == null) {
      return null;
    }

    if (stmt instanceof JsExprStmt) {
      return ((JsExprStmt) stmt).getExpression();
    }

    if (stmt instanceof JsBlock && ((JsBlock) stmt).getStatements().size() == 1) {
      return extractExpression(((JsBlock) stmt).getStatements().get(0));
    }

    return null;
  }

  public static JsName getJsNameForMethod(JavaToJavaScriptMap jjsmap, JProgram jprogram,
      String indexedMethodName) {
    return jjsmap.nameForMethod(jprogram.getIndexedMethod(indexedMethodName));
  }

  public static JsName getJsNameForField(JavaToJavaScriptMap jjsmap, JProgram jprogram,
      String indexedMethodName) {
    return jjsmap.nameForField(jprogram.getIndexedField(indexedMethodName));
  }

  public static boolean isEmpty(JsStatement stmt) {
    if (stmt == null) {
      return true;
    }
    return (stmt instanceof JsBlock && ((JsBlock) stmt).getStatements().isEmpty());
  }

  /**
   * If the statement is a JsExprStmt that declares a function with no other
   * side effects, returns that function; otherwise <code>null</code>.
   */
  public static JsFunction isFunctionDeclaration(JsStatement stmt) {
    if (stmt instanceof JsExprStmt) {
      JsExprStmt exprStmt = (JsExprStmt) stmt;
      JsExpression expr = exprStmt.getExpression();
      if (expr instanceof JsFunction) {
        JsFunction func = (JsFunction) expr;
        if (func.getName() != null) {
          return func;
        }
      }
    }
    return null;
  }

  /**
   * A JavaScript identifier contains only letters, numbers, _, $ and does not begin with a number.
   * There are actually other valid identifiers, such as ones that contain escaped Unicode
   * characters but we disallow those for the time being.
   */
  public static boolean isValidJsIdentifier(String name) {
    return JAVASCRIPT_VALID_IDENTIFIER_PATTERN.matcher(name).matches();
  }

  public static boolean isValidJsQualifiedName(String name) {
    return JAVASCRIPT_VALID_QUALIFIED_NAME_PATTERN.matcher(name).matches();
  }

  private static final String VALID_JS_NAME_REGEX = "[a-zA-Z_$][\\w_$]*";
  private static final Pattern JAVASCRIPT_VALID_QUALIFIED_NAME_PATTERN =
      Pattern.compile(VALID_JS_NAME_REGEX + "(\\." + VALID_JS_NAME_REGEX + ")*");
  private static final Pattern JAVASCRIPT_VALID_IDENTIFIER_PATTERN =
      Pattern.compile(VALID_JS_NAME_REGEX);
  private static final String CALL_STRING = StringInterner.get().intern("call");

  private JsUtils() {
  }
}
