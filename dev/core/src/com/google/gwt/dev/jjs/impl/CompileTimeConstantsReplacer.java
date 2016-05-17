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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Map;

/**
 * Replaces compile time constants by their values.
 * <p>
 * This pass is necessary, even in unoptimized compiles, to allow forward references to compile
 * time values in bootstrap code.
 **/
public class CompileTimeConstantsReplacer {
  private static class CompileTimeConstantsReplacingVisitor extends JModVisitor {

    private final Map<JField, JExpression> resolveValuesByField = Maps.newIdentityHashMap();
    private final JProgram program;

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JField field = x.getField();
      if (field.isCompileTimeConstant() && !ctx.isLvalue()) {
        ctx.replaceMe(resolveFieldValue(field));
      }
    }

    private JExpression resolveFieldValue(JField field) {
      JExpression value = resolveValuesByField.get(field);
      if (value != null) {
        return new CloneExpressionVisitor().cloneExpression(value);
      }
      // TODO(rluble): Simplify the expression to a literal here after refactoring Simplifier.
      // The initializer is guaranteed to be constant but it may be a non literal expression, so
      // clone it and recursively remove field references, and finally cache the results.
      value = accept(new CloneExpressionVisitor().cloneExpression(field.getInitializer()));
      JType fieldType = field.getType().getUnderlyingType();
      assert fieldType.isPrimitiveType() || fieldType == program.getTypeJavaLangString()
          : fieldType.getName() + " is not a primitive nor String";
      if (fieldType != value.getType()) {
        value = new JCastOperation(value.getSourceInfo(), fieldType, value);
      }
      resolveValuesByField.put(field, value);
      return value;
    }

    private CompileTimeConstantsReplacingVisitor(JProgram program) {
      this.program = program;
    }
  }
  public static void exec(JProgram program) {
    new CompileTimeConstantsReplacingVisitor(program).accept(program);
  }
}
