/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
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
@SuppressWarnings({"unused", "unchecked"})
public class JVisitor {

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

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JNode node) {
      throw new UnsupportedOperationException();
    }

  };

  public final JExpression accept(JExpression node) {
    return (JExpression) doAccept(node);
  }

  public final JNode accept(JNode node) {
    return doAccept(node);
  }

  public final JStatement accept(JStatement node) {
    return (JStatement) doAccept(node);
  }

  public final void accept(List list) {
    doAccept(list);
  }

  public final void acceptWithInsertRemove(List list) {
    doAcceptWithInsertRemove(list);
  }

  public boolean didChange() {
    throw new UnsupportedOperationException();
  }

  public void endVisit(JAbsentArrayDimension x, Context ctx) {
  }

  public void endVisit(JArrayRef x, Context ctx) {
  }

  public void endVisit(JArrayType x, Context ctx) {
  }

  public void endVisit(JAssertStatement x, Context ctx) {
  }

  public void endVisit(JBinaryOperation x, Context ctx) {
  }

  public void endVisit(JBlock x, Context ctx) {
  }

  public void endVisit(JBooleanLiteral x, Context ctx) {
  }

  public void endVisit(JBreakStatement x, Context ctx) {
  }

  public void endVisit(JCaseStatement x, Context ctx) {
  }

  public void endVisit(JCastOperation x, Context ctx) {
  }

  public void endVisit(JCharLiteral x, Context ctx) {
  }

  public void endVisit(JClassLiteral x, Context ctx) {
  }

  public void endVisit(JClassSeed x, Context ctx) {
  }

  public void endVisit(JClassType x, Context ctx) {
  }

  public void endVisit(JConditional x, Context ctx) {
  }

  public void endVisit(JContinueStatement x, Context ctx) {
  }

  public void endVisit(JDeclarationStatement x, Context ctx) {
  }

  public void endVisit(JDoStatement x, Context ctx) {
  }

  public void endVisit(JDoubleLiteral x, Context ctx) {
  }

  public void endVisit(JExpressionStatement x, Context ctx) {
  }

  public void endVisit(JField x, Context ctx) {
  }

  public void endVisit(JFieldRef x, Context ctx) {
  }

  public void endVisit(JFloatLiteral x, Context ctx) {
  }

  public void endVisit(JForStatement x, Context ctx) {
  }

  public void endVisit(JIfStatement x, Context ctx) {
  }

  public void endVisit(JInstanceOf x, Context ctx) {
  }

  public void endVisit(JInterfaceType x, Context ctx) {
  }

  public void endVisit(JIntLiteral x, Context ctx) {
  }

  public void endVisit(JLabel x, Context ctx) {
  }

  public void endVisit(JLabeledStatement x, Context ctx) {
  }

  public void endVisit(JLocal x, Context ctx) {
  }

  public void endVisit(JLocalRef x, Context ctx) {
  }

  public void endVisit(JLongLiteral x, Context ctx) {
  }

  public void endVisit(JMethod x, Context ctx) {
  }

  public void endVisit(JMethodBody x, Context ctx) {
  }

  public void endVisit(JMethodCall x, Context ctx) {
  }

  public void endVisit(JMultiExpression x, Context ctx) {
  }

  public void endVisit(JNewArray x, Context ctx) {
  }

  public void endVisit(JNewInstance x, Context ctx) {
  }

  public void endVisit(JNullLiteral x, Context ctx) {
  }

  public void endVisit(JNullType x, Context ctx) {
  }

  public void endVisit(JParameter x, Context ctx) {
  }

  public void endVisit(JParameterRef x, Context ctx) {
  }

  public void endVisit(JPostfixOperation x, Context ctx) {
  }

  public void endVisit(JPrefixOperation x, Context ctx) {
  }

  public void endVisit(JPrimitiveType x, Context ctx) {
  }

  public void endVisit(JProgram x, Context ctx) {
  }

  public void endVisit(JReturnStatement x, Context ctx) {
  }

  public void endVisit(JsniFieldRef x, Context ctx) {
  }

  public void endVisit(JsniMethodBody x, Context ctx) {
  }

  public void endVisit(JsniMethodRef x, Context ctx) {
  }

  public void endVisit(JsonArray x, Context ctx) {
  }

  public void endVisit(JsonObject x, Context ctx) {
  }

  public void endVisit(JsonPropInit x, Context ctx) {
  }

  public void endVisit(JStringLiteral x, Context ctx) {
  }

  public void endVisit(JSwitchStatement x, Context ctx) {
  }

  public void endVisit(JThisRef x, Context ctx) {
  }

  public void endVisit(JThrowStatement x, Context ctx) {
  }

  public void endVisit(JTryStatement x, Context ctx) {
  }

  public void endVisit(JWhileStatement x, Context ctx) {
  }

  public boolean visit(JAbsentArrayDimension x, Context ctx) {
    return true;
  }

  public boolean visit(JArrayRef x, Context ctx) {
    return true;
  }

  public boolean visit(JArrayType x, Context ctx) {
    return true;
  }

  public boolean visit(JAssertStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JBinaryOperation x, Context ctx) {
    return true;
  }

  public boolean visit(JBlock x, Context ctx) {
    return true;
  }

  public boolean visit(JBooleanLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JBreakStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JCaseStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JCastOperation x, Context ctx) {
    return true;
  }

  public boolean visit(JCharLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JClassLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JClassSeed x, Context ctx) {
    return true;
  }

  public boolean visit(JClassType x, Context ctx) {
    return true;
  }

  public boolean visit(JConditional x, Context ctx) {
    return true;
  }

  public boolean visit(JContinueStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JDeclarationStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JDoStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JDoubleLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JExpressionStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JField x, Context ctx) {
    return true;
  }

  public boolean visit(JFieldRef x, Context ctx) {
    return true;
  }

  public boolean visit(JFloatLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JForStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JIfStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JInstanceOf x, Context ctx) {
    return true;
  }

  public boolean visit(JInterfaceType x, Context ctx) {
    return true;
  }

  public boolean visit(JIntLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JLabel x, Context ctx) {
    return true;
  }

  public boolean visit(JLabeledStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JLocal x, Context ctx) {
    return true;
  }

  public boolean visit(JLocalRef x, Context ctx) {
    return true;
  }

  public boolean visit(JLongLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JMethod x, Context ctx) {
    return true;
  }

  public boolean visit(JMethodBody x, Context ctx) {
    return true;
  }

  public boolean visit(JMethodCall x, Context ctx) {
    return true;
  }

  public boolean visit(JMultiExpression x, Context ctx) {
    return true;
  }

  public boolean visit(JNewArray x, Context ctx) {
    return true;
  }

  public boolean visit(JNewInstance x, Context ctx) {
    return true;
  }

  public boolean visit(JNullLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JNullType x, Context ctx) {
    return true;
  }

  public boolean visit(JParameter x, Context ctx) {
    return true;
  }

  public boolean visit(JParameterRef x, Context ctx) {
    return true;
  }

  public boolean visit(JPostfixOperation x, Context ctx) {
    return true;
  }

  public boolean visit(JPrefixOperation x, Context ctx) {
    return true;
  }

  public boolean visit(JPrimitiveType x, Context ctx) {
    return true;
  }

  public boolean visit(JProgram x, Context ctx) {
    return true;
  }

  public boolean visit(JReturnStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JsniFieldRef x, Context ctx) {
    return true;
  }

  public boolean visit(JsniMethodBody x, Context ctx) {
    return true;
  }

  public boolean visit(JsniMethodRef x, Context ctx) {
    return true;
  }

  public boolean visit(JsonArray x, Context ctx) {
    return true;
  }

  public boolean visit(JsonObject x, Context ctx) {
    return true;
  }

  public boolean visit(JsonPropInit x, Context ctx) {
    return true;
  }

  public boolean visit(JStringLiteral x, Context ctx) {
    return true;
  }

  public boolean visit(JSwitchStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JThisRef x, Context ctx) {
    return true;
  }

  public boolean visit(JThrowStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JTryStatement x, Context ctx) {
    return true;
  }

  public boolean visit(JWhileStatement x, Context ctx) {
    return true;
  }

  protected JNode doAccept(JNode node) {
    doTraverse(node, UNMODIFIABLE_CONTEXT);
    return node;
  }

  protected void doAccept(List list) {
    for (Object node : list) {
      doTraverse((JNode) node, UNMODIFIABLE_CONTEXT);
    }
  }

  protected void doAcceptWithInsertRemove(List list) {
    for (Object node : list) {
      doTraverse((JNode) node, UNMODIFIABLE_CONTEXT);
    }
  }

  protected final void doTraverse(JNode node, Context ctx) {
    try {
      node.traverse(this, ctx);
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  private InternalCompilerException translateException(JNode node, Throwable e) {
    InternalCompilerException ice;
    if (e instanceof InternalCompilerException) {
      ice = (InternalCompilerException) e;
    } else {
      ice = new InternalCompilerException("Unexpected error during visit.", e);
    }
    ice.addNode(node);
    return ice;
  }
}
