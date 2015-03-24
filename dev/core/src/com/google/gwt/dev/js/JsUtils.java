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
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.util.StringInterner;

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
    if (f != null && f.getExecuteOnce()) {
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

  /**
   * Similar to {@link #isFunction(com.google.gwt.dev.js.ast.JsExpression)} but
   * retrieves the JsName of an invocation if the qualifier is a name ref.
   */
  public static JsName maybeGetFunctionName(JsExpression expression) {
    if (expression instanceof JsInvocation) {
      JsInvocation jsInvoke = (JsInvocation) expression;
      if (jsInvoke.getQualifier() instanceof JsNameRef) {
        JsNameRef nameRef = (JsNameRef) jsInvoke.getQualifier();
        return nameRef.getName();
      }
    }
    return null;
  }

  public static JsExpression createAssignment(JsExpression lhs, JsExpression rhs) {
    return new JsBinaryOperation(lhs.getSourceInfo(), JsBinaryOperator.ASG, lhs, rhs);
  }

  public static JsFunction createEmptyFunctionLiteral(SourceInfo info, JsScope scope) {
    JsFunction func = new JsFunction(info, scope);
    func.setBody(new JsBlock(info));
    return func;
  }

  /**
   * Given a string qualifier such as 'foo.bar.Baz', returns a chain of JsNameRef's representing
   * this qualifier.
   */
  public static JsNameRef createQualifier(String namespace, SourceInfo sourceInfo) {
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

  private static final String CALL_STRING = StringInterner.get().intern("call");

  private JsUtils() {
  }
}
