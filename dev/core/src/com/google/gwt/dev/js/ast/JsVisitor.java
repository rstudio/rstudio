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
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.util.Iterator;
import java.util.List;

/**
 * Implemented by nodes that will visit child nodes.
 */
@SuppressWarnings("unused")
public class JsVisitor {

  protected static final JsContext<JsExpression> LVALUE_CONTEXT = new JsContext<JsExpression>() {

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JsExpression node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JsExpression node) {
      throw new UnsupportedOperationException();
    }

    public boolean isLvalue() {
      return true;
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JsExpression node) {
      throw new UnsupportedOperationException();
    }
  };

  protected static final JsContext UNMODIFIABLE_CONTEXT = new JsContext() {

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    public boolean isLvalue() {
      return false;
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JsVisitable node) {
      throw new UnsupportedOperationException();
    }
  };

  @SuppressWarnings("cast")
  public final <T extends JsVisitable> T accept(T node) {
    // The following cast to T is needed for javac 1.5.0_13
    // as shipped on OS X
    return (T) doAccept(node);
  }

  public final <T extends JsVisitable<T>> void acceptList(List<T> collection) {
    doAcceptList(collection);
  }

  public JsExpression acceptLvalue(JsExpression expr) {
    return doAcceptLvalue(expr);
  }

  public final <T extends JsVisitable<T>> void acceptWithInsertRemove(
      List<T> collection) {
    doAcceptWithInsertRemove(collection);
  }

  public boolean didChange() {
    throw new UnsupportedOperationException();
  }

  public void endVisit(JsArrayAccess x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsArrayLiteral x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsBlock x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsBooleanLiteral x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsBreak x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsCase x, JsContext<JsSwitchMember> ctx) {
  }

  public void endVisit(JsCatch x, JsContext<JsCatch> ctx) {
  }

  public void endVisit(JsConditional x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsContinue x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsDebugger x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsDefault x, JsContext<JsSwitchMember> ctx) {
  }

  public void endVisit(JsDoWhile x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsEmpty x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsExprStmt x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsFor x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsForIn x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsIf x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsLabel x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsNameOf x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsNew x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsNullLiteral x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsNumberLiteral x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsObjectLiteral x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsParameter x, JsContext<JsParameter> ctx) {
  }

  public void endVisit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsProgram x, JsContext<JsProgram> ctx) {
  }

  public void endVisit(JsProgramFragment x, JsContext<JsProgramFragment> ctx) {
  }

  public void endVisit(JsPropertyInitializer x,
      JsContext<JsPropertyInitializer> ctx) {
  }

  public void endVisit(JsRegExp x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsReturn x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsStringLiteral x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsSwitch x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsThisRef x, JsContext<JsExpression> ctx) {
  }

  public void endVisit(JsThrow x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsTry x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsVar x, JsContext<JsVar> ctx) {
  }

  public void endVisit(JsVars x, JsContext<JsStatement> ctx) {
  }

  public void endVisit(JsWhile x, JsContext<JsStatement> ctx) {
  }

  public boolean visit(JsArrayAccess x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsArrayLiteral x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsBlock x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsBooleanLiteral x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsBreak x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsCase x, JsContext<JsSwitchMember> ctx) {
    return true;
  }

  public boolean visit(JsCatch x, JsContext<JsCatch> ctx) {
    return true;
  }

  public boolean visit(JsConditional x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsContinue x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsDebugger x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsDefault x, JsContext<JsSwitchMember> ctx) {
    return true;
  }

  public boolean visit(JsDoWhile x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsEmpty x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsExprStmt x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsFor x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsForIn x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsIf x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsLabel x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsNameOf x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsNameRef x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsNew x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsNullLiteral x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsNumberLiteral x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsObjectLiteral x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsParameter x, JsContext<JsParameter> ctx) {
    return true;
  }

  public boolean visit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsProgram x, JsContext<JsProgram> ctx) {
    return true;
  }

  public boolean visit(JsProgramFragment x, JsContext<JsProgramFragment> ctx) {
    return true;
  }

  public boolean visit(JsPropertyInitializer x,
      JsContext<JsPropertyInitializer> ctx) {
    return true;
  }

  public boolean visit(JsRegExp x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsReturn x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsStringLiteral x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsSwitch x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsThisRef x, JsContext<JsExpression> ctx) {
    return true;
  }

  public boolean visit(JsThrow x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsTry x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsVar x, JsContext<JsVar> ctx) {
    return true;
  }

  public boolean visit(JsVars x, JsContext<JsStatement> ctx) {
    return true;
  }

  public boolean visit(JsWhile x, JsContext<JsStatement> ctx) {
    return true;
  }

  protected <T extends JsVisitable<T>> T doAccept(T node) {
    doTraverse(node, (JsContext<T>) UNMODIFIABLE_CONTEXT);
    return node;
  }

  protected <T extends JsVisitable<T>> void doAcceptList(List<T> collection) {
    for (Iterator<T> it = collection.iterator(); it.hasNext();) {
      doTraverse(it.next(), (JsContext<T>) UNMODIFIABLE_CONTEXT);
    }
  }

  protected JsExpression doAcceptLvalue(JsExpression expr) {
    doTraverse(expr, LVALUE_CONTEXT);
    return expr;
  }

  protected <T extends JsVisitable<T>> void doAcceptWithInsertRemove(
      List<T> collection) {
    for (Iterator<T> it = collection.iterator(); it.hasNext();) {
      doTraverse(it.next(), (JsContext<T>) UNMODIFIABLE_CONTEXT);
    }
  }

  protected final <T extends JsVisitable<T>> void doTraverse(T node,
      JsContext<T> ctx) {
    try {
      node.traverse(this, ctx);
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  private <T extends JsVisitable<T>> InternalCompilerException translateException(
      T node, Throwable e) {
    if (e instanceof OutOfMemoryError) {
      // Always rethrow OOMs (might have no memory to load ICE class anyway).
      throw (OutOfMemoryError) e;
    }
    InternalCompilerException ice;
    if (e instanceof InternalCompilerException) {
      ice = (InternalCompilerException) e;
    } else {
      ice = new InternalCompilerException("Unexpected error during visit.", e);
    }
    ice.addNode((HasSourceInfo) node);
    return ice;
  }
}
