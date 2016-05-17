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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;

import java.util.List;

/**
 * This class will identify instances of an implicit upcast between
 * non-primitive types, and call the overridable processImplicitUpcast method.
 *
 * TODO(jbrosenberg): Consider extending to handle implicit upcasts between
 * primitive types. This is not as straightforward as for reference types,
 * because primitives can be boxed and unboxed implicitly as well.
 */
public class ImplicitUpcastAnalyzer extends JVisitor {

  protected JMethod currentMethod;
  private final JType javaScriptObjectType;
  private final JType throwableType;

  public ImplicitUpcastAnalyzer(JProgram program) {
    this.throwableType = program.getIndexedType("Throwable");
    this.javaScriptObjectType = program.getJavaScriptObject();
  }

  @Override
  public void endVisit(JBinaryOperation x, Context ctx) {
    if (x.isAssignment()) {
      processIfTypesNotEqual(x.getRhs().getType(), x.getLhs().getType(), x.getSourceInfo());
    } else if (x.getRhs().getType().isNullType()) {
      processIfTypesNotEqual(JReferenceType.NULL_TYPE, x.getLhs().getType(), x.getSourceInfo());
    } else if (x.getLhs().getType().isNullType()) {
      processIfTypesNotEqual(JReferenceType.NULL_TYPE, x.getRhs().getType(), x.getSourceInfo());
    } else if (x.getOp() == JBinaryOperator.CONCAT || x.getOp() == JBinaryOperator.EQ
        || x.getOp() == JBinaryOperator.NEQ) {
      /*
       * Since we are not attempting to handle detection of upcasts between
       * primitive types, we limit handling here to CONCAT, EQ and NEQ. Need to
       * do both directions.
       */
      processIfTypesNotEqual(x.getLhs().getType(), x.getRhs().getType(), x.getSourceInfo());
      processIfTypesNotEqual(x.getRhs().getType(), x.getLhs().getType(), x.getSourceInfo());
    }
  }

  @Override
  public void endVisit(JConditional x, Context ctx) {
    processIfTypesNotEqual(x.getThenExpr().getType(), x.getType(), x.getSourceInfo());
    processIfTypesNotEqual(x.getElseExpr().getType(), x.getType(), x.getSourceInfo());
  }

  @Override
  public void endVisit(JDeclarationStatement x, Context ctx) {
    if (x.getInitializer() != null) {
      processIfTypesNotEqual(x.getInitializer().getType(), x.getVariableRef().getType(), x
          .getSourceInfo());
    }
  }

  @Override
  public void endVisit(JField x, Context ctx) {
    if (x.getInitializer() == null
        && !x.isFinal()
        && !x.getType().isPrimitiveType()) {
      // if it is declared without an initial value, it defaults to null
      processIfTypesNotEqual(JReferenceType.NULL_TYPE, x.getType(), x.getSourceInfo());
    }
  }

  @Override
  public void endVisit(JMethod x, Context ctx) {
    // check for upcast in return type as compared to an overridden method
    for (JMethod overridden : x.getOverriddenMethods()) {
      processIfTypesNotEqual(x.getType(), overridden.getType(), x.getSourceInfo());
    }

    if (x.getBody() != null && x.getBody().isJsniMethodBody()) {
      /*
       * Check if this method has a native (jsni) method body, in which case all
       * arguments passed in are implicitly cast to JavaScriptObject
       */
      List<JParameter> params = x.getParams();
      for (int i = 0; i < params.size(); i++) {
        processIfTypesNotEqual(params.get(i).getType(), javaScriptObjectType, x.getSourceInfo());
      }

      /*
       * Check if this method has a non-void return type, in which case it will
       * be implicitly cast from JavaScriptObject
       */
      if (x.getType() != JPrimitiveType.VOID) {
        processIfTypesNotEqual(javaScriptObjectType, x.getType(), x.getSourceInfo());
      }
    }
  }

  @Override
  public void endVisit(JMethodCall x, Context ctx) {
    // check for upcast in argument passing
    List<JExpression> args = x.getArgs();
    List<JParameter> params = x.getTarget().getParams();

    for (int i = 0; i < args.size(); i++) {
      // make sure the param wasn't pruned
      if (i < params.size()) {
        processIfTypesNotEqual(args.get(i).getType(), params.get(i).getType(), x.getSourceInfo());
      }
    }
  }

  @Override
  public void endVisit(JNewArray x, Context ctx) {
    JType elementType = x.getArrayType().getElementType();
    if (x.getInitializers() != null) {
      for (JExpression init : x.getInitializers()) {
        processIfTypesNotEqual(init.getType(), elementType, x.getSourceInfo());
      }
    }
  }

  @Override
  public void endVisit(JReturnStatement x, Context ctx) {
    if (x.getExpr() != null) {
      // check against the current method return type
      processIfTypesNotEqual(x.getExpr().getType(), currentMethod.getType(), x.getSourceInfo());
    }
  }

  @Override
  public void endVisit(JsniMethodRef x, Context ctx) {
    // the return type of this method ref will be cast to JavaScriptObject
    if (x.getTarget().getType() != JPrimitiveType.VOID) {
      processIfTypesNotEqual(x.getTarget().getType(), javaScriptObjectType, x.getSourceInfo());
    }

    // check referenced method's params, which are passed as JavaScriptObjects
    List<JParameter> params = x.getTarget().getParams();
    for (int i = 0; i < params.size(); i++) {
      processIfTypesNotEqual(javaScriptObjectType, params.get(i).getType(), x.getSourceInfo());
    }
  }

  @Override
  public void endVisit(JThrowStatement x, Context ctx) {
    // all things thrown are upcast to a Throwable
    JType type = x.getExpr().getType().getUnderlyingType();
    processIfTypesNotEqual(type, throwableType, x.getSourceInfo());
  }

  @Override
  public boolean visit(JMethod x, Context ctx) {
    // save this, so can use it later for checking JReturnStatement
    currentMethod = x;
    return true;
  }

  /**
   * An overriding method will be called for each detected implicit upcast.
   *
   * @param fromType
   * @param destType
   */
  protected void processImplicitUpcast(JType fromType, JType destType, SourceInfo info) {
    // override
  }

  private void processIfTypesNotEqual(JType fromType, JType destType, SourceInfo info) {
    // Ignore nullability when determining type inequality.
    fromType = fromType.getUnderlyingType();
    destType = destType.getUnderlyingType();
    if (fromType != destType) {
      processImplicitUpcast(fromType, destType, info);
    }
  }
}
