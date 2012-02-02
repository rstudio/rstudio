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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsCastMap;
import com.google.gwt.dev.jjs.ast.js.JsCastMap.JsQueryType;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;

import java.util.List;

/**
 * A visitor for iterating through an AST.
 */
@SuppressWarnings("unused")
public class JVisitor {

  protected static final Context LVALUE_CONTEXT = new Context() {

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JNode node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JNode node) {
      throw new UnsupportedOperationException();
    }

    public boolean isLvalue() {
      return true;
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JNode node) {
      throw new UnsupportedOperationException();
    }
  };

  protected static final Context UNMODIFIABLE_CONTEXT = new Context() {

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JNode node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JNode node) {
      throw new UnsupportedOperationException();
    }

    public boolean isLvalue() {
      return false;
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JNode node) {
      throw new UnsupportedOperationException();
    }

  };

  protected static InternalCompilerException translateException(JNode node, Throwable e) {
    if (e instanceof VirtualMachineError) {
      // Always rethrow VM errors (an attempt to wrap may fail).
      throw (VirtualMachineError) e;
    }
    InternalCompilerException ice;
    if (e instanceof InternalCompilerException) {
      ice = (InternalCompilerException) e;
    } else {
      ice = new InternalCompilerException("Unexpected error during visit.", e);
    }
    ice.addNode(node);
    return ice;
  }

  public final JExpression accept(JExpression node) {
    return (JExpression) accept((JNode) node);
  }

  public JNode accept(JNode node) {
    return accept(node, false);
  }

  public JNode accept(JNode node, boolean allowRemove) {
    try {
      node.traverse(this, UNMODIFIABLE_CONTEXT);
      return node;
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  public final JStatement accept(JStatement node) {
    return accept(node, false);
  }

  public final JStatement accept(JStatement node, boolean allowRemove) {
    return (JStatement) accept((JNode) node, allowRemove);
  }

  public <T extends JNode> void accept(List<T> list) {
    int i = 0;
    try {
      for (int c = list.size(); i < c; ++i) {
        list.get(i).traverse(this, UNMODIFIABLE_CONTEXT);
      }
    } catch (Throwable e) {
      throw translateException(list.get(i), e);
    }
  }

  public <T extends JNode> List<T> acceptImmutable(List<T> list) {
    accept(list);
    return list;
  }

  public JExpression acceptLvalue(JExpression expr) {
    try {
      expr.traverse(this, LVALUE_CONTEXT);
      return expr;
    } catch (Throwable e) {
      throw translateException(expr, e);
    }
  }

  public <T extends JNode> void acceptWithInsertRemove(List<T> list) {
    accept(list);
  }

  public <T extends JNode> List<T> acceptWithInsertRemoveImmutable(List<T> list) {
    accept(list);
    return list;
  }

  public boolean didChange() {
    throw new UnsupportedOperationException();
  }

  public void endVisit(JAbsentArrayDimension x, Context ctx) {
    endVisit((JLiteral) x, ctx);
  }

  public void endVisit(JAbstractMethodBody x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JArrayLength x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JArrayRef x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JArrayType x, Context ctx) {
    endVisit((JReferenceType) x, ctx);
  }

  public void endVisit(JAssertStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JBinaryOperation x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JBlock x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JBooleanLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JBreakStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JCaseStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JCastOperation x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JCharLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JClassLiteral x, Context ctx) {
    endVisit((JLiteral) x, ctx);
  }

  public void endVisit(JClassType x, Context ctx) {
    endVisit((JDeclaredType) x, ctx);
  }

  public void endVisit(JConditional x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JConstructor x, Context ctx) {
    endVisit((JMethod) x, ctx);
  }

  public void endVisit(JContinueStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JDeclarationStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JDeclaredType x, Context ctx) {
    endVisit((JReferenceType) x, ctx);
  }

  public void endVisit(JDoStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JDoubleLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JExpression x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JExpressionStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JField x, Context ctx) {
    endVisit((JVariable) x, ctx);
  }

  /**
   * NOTE: not called from JsniFieldRef.
   */
  public void endVisit(JFieldRef x, Context ctx) {
    endVisit((JVariableRef) x, ctx);
  }

  public void endVisit(JFloatLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JForStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JGwtCreate x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JIfStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JInstanceOf x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JInterfaceType x, Context ctx) {
    endVisit((JDeclaredType) x, ctx);
  }

  public void endVisit(JIntLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JLabel x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JLabeledStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JLiteral x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JLocal x, Context ctx) {
    endVisit((JVariable) x, ctx);
  }

  public void endVisit(JLocalRef x, Context ctx) {
    endVisit((JVariableRef) x, ctx);
  }

  public void endVisit(JLongLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JMethod x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JMethodBody x, Context ctx) {
    endVisit((JAbstractMethodBody) x, ctx);
  }

  /**
   * NOTE: not called from JsniMethodRef.
   */
  public void endVisit(JMethodCall x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JMultiExpression x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JNameOf x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JNewArray x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JNewInstance x, Context ctx) {
    endVisit((JMethodCall) x, ctx);
  }

  public void endVisit(JNode x, Context ctx) {
    // empty block
  }

  public void endVisit(JNullLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JNullType x, Context ctx) {
    endVisit((JReferenceType) x, ctx);
  }
  
  public void endVisit(JNumericEntry x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JParameter x, Context ctx) {
    endVisit((JVariable) x, ctx);
  }

  public void endVisit(JParameterRef x, Context ctx) {
    endVisit((JVariableRef) x, ctx);
  }

  public void endVisit(JPostfixOperation x, Context ctx) {
    endVisit((JUnaryOperation) x, ctx);
  }

  public void endVisit(JPrefixOperation x, Context ctx) {
    endVisit((JUnaryOperation) x, ctx);
  }

  public void endVisit(JPrimitiveType x, Context ctx) {
    endVisit((JType) x, ctx);
  }

  public void endVisit(JProgram x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JReboundEntryPoint x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JReferenceType x, Context ctx) {
    endVisit((JType) x, ctx);
  }

  public void endVisit(JReturnStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JRunAsync x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JSeedIdOf x, Context ctx) {
    endVisit((JNameOf) x, ctx);
  }

  public void endVisit(JsCastMap x, Context ctx) {
    endVisit((JsonArray) x, ctx);
  }

  public void endVisit(JsniFieldRef x, Context ctx) {
    /* NOTE: Skip JFieldRef */
    endVisit((JVariableRef) x, ctx);
  }

  public void endVisit(JsniMethodBody x, Context ctx) {
    endVisit((JAbstractMethodBody) x, ctx);
  }

  public void endVisit(JsniMethodRef x, Context ctx) {
    /* NOTE: Skip JMethodCall */
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JsonArray x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JsonObject x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JsonPropInit x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JsQueryType x, Context ctx) {
    endVisit((JIntLiteral) x, ctx);
  }

  public void endVisit(JStatement x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JStringLiteral x, Context ctx) {
    endVisit((JValueLiteral) x, ctx);
  }

  public void endVisit(JSwitchStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JThisRef x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JThrowStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JTryStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public void endVisit(JType x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JUnaryOperation x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JValueLiteral x, Context ctx) {
    endVisit((JLiteral) x, ctx);
  }

  public void endVisit(JVariable x, Context ctx) {
    endVisit((JNode) x, ctx);
  }

  public void endVisit(JVariableRef x, Context ctx) {
    endVisit((JExpression) x, ctx);
  }

  public void endVisit(JWhileStatement x, Context ctx) {
    endVisit((JStatement) x, ctx);
  }

  public boolean visit(JAbsentArrayDimension x, Context ctx) {
    return visit((JLiteral) x, ctx);
  }

  public boolean visit(JAbstractMethodBody x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JArrayLength x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JArrayRef x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JArrayType x, Context ctx) {
    return visit((JReferenceType) x, ctx);
  }

  public boolean visit(JAssertStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JBinaryOperation x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JBlock x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JBooleanLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JBreakStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JCaseStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JCastOperation x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JCharLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JClassLiteral x, Context ctx) {
    return visit((JLiteral) x, ctx);
  }

  public boolean visit(JClassType x, Context ctx) {
    return visit((JDeclaredType) x, ctx);
  }

  public boolean visit(JConditional x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JConstructor x, Context ctx) {
    return visit((JMethod) x, ctx);
  }

  public boolean visit(JContinueStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JDeclarationStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JDeclaredType x, Context ctx) {
    return visit((JReferenceType) x, ctx);
  }

  public boolean visit(JDoStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JDoubleLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JExpression x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JExpressionStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JField x, Context ctx) {
    return visit((JVariable) x, ctx);
  }

  /**
   * NOTE: not called from JsniFieldRef.
   */
  public boolean visit(JFieldRef x, Context ctx) {
    return visit((JVariableRef) x, ctx);
  }

  public boolean visit(JFloatLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JForStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JGwtCreate x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JIfStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JInstanceOf x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JInterfaceType x, Context ctx) {
    return visit((JDeclaredType) x, ctx);
  }

  public boolean visit(JIntLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JLabel x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JLabeledStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JLiteral x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JLocal x, Context ctx) {
    return visit((JVariable) x, ctx);
  }

  public boolean visit(JLocalRef x, Context ctx) {
    return visit((JVariableRef) x, ctx);
  }

  public boolean visit(JLongLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JMethod x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JMethodBody x, Context ctx) {
    return visit((JAbstractMethodBody) x, ctx);
  }

  /**
   * NOTE: not called from JsniMethodRef.
   */
  public boolean visit(JMethodCall x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JMultiExpression x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JNameOf x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JNewArray x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JNewInstance x, Context ctx) {
    return visit((JMethodCall) x, ctx);
  }

  public boolean visit(JNode x, Context ctx) {
    return true;
  }

  public boolean visit(JNullLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JNullType x, Context ctx) {
    return visit((JReferenceType) x, ctx);
  }
  
  public boolean visit(JNumericEntry x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JParameter x, Context ctx) {
    return visit((JVariable) x, ctx);
  }

  public boolean visit(JParameterRef x, Context ctx) {
    return visit((JVariableRef) x, ctx);
  }

  public boolean visit(JPostfixOperation x, Context ctx) {
    return visit((JUnaryOperation) x, ctx);
  }

  public boolean visit(JPrefixOperation x, Context ctx) {
    return visit((JUnaryOperation) x, ctx);
  }

  public boolean visit(JPrimitiveType x, Context ctx) {
    return visit((JType) x, ctx);
  }

  public boolean visit(JProgram x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JReboundEntryPoint x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JReferenceType x, Context ctx) {
    return visit((JType) x, ctx);
  }

  public boolean visit(JReturnStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JRunAsync x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JSeedIdOf x, Context ctx) {
    return visit((JNameOf) x, ctx);
  }

  public boolean visit(JsCastMap x, Context ctx) {
    return visit((JsonArray) x, ctx);
  }

  public boolean visit(JsniFieldRef x, Context ctx) {
    /* NOTE: Skip JFieldRef */
    return visit((JVariableRef) x, ctx);
  }

  public boolean visit(JsniMethodBody x, Context ctx) {
    return visit((JAbstractMethodBody) x, ctx);
  }

  public boolean visit(JsniMethodRef x, Context ctx) {
    /* NOTE: Skip JMethodCall */
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JsonArray x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JsonObject x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JsonPropInit x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JsQueryType x, Context ctx) {
    return visit((JIntLiteral) x, ctx);
  }

  public boolean visit(JStatement x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JStringLiteral x, Context ctx) {
    return visit((JValueLiteral) x, ctx);
  }

  public boolean visit(JSwitchStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JThisRef x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JThrowStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JTryStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

  public boolean visit(JType x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JUnaryOperation x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JValueLiteral x, Context ctx) {
    return visit((JLiteral) x, ctx);
  }

  public boolean visit(JVariable x, Context ctx) {
    return visit((JNode) x, ctx);
  }

  public boolean visit(JVariableRef x, Context ctx) {
    return visit((JExpression) x, ctx);
  }

  public boolean visit(JWhileStatement x, Context ctx) {
    return visit((JStatement) x, ctx);
  }

}
