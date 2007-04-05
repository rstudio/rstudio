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
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.util.Iterator;

/**
 * Implemented by nodes that will visit child nodes.
 */
public class JsVisitor {

  protected static final JsContext UNMODIFIABLE_CONTEXT = new JsContext() {

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JsNode node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JsNode node) {
      throw new UnsupportedOperationException();
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JsNode node) {
      throw new UnsupportedOperationException();
    }

  };

  public final JsExpression accept(JsExpression node) {
    return (JsExpression) doAccept(node);
  }

  public final JsNode accept(JsNode node) {
    return doAccept(node);
  }

  public final JsStatement accept(JsStatement node) {
    return (JsStatement) doAccept(node);
  }

  public final void accept(JsCollection collection) {
    doAccept(collection);
  }

  public final void acceptWithInsertRemove(JsCollection collection) {
    doAcceptWithInsertRemove(collection);
  }

  public boolean didChange() {
    throw new UnsupportedOperationException();
  }

  public void endVisit(JsArrayAccess x, JsContext ctx) {
  }

  public void endVisit(JsArrayLiteral x, JsContext ctx) {
  }

  public void endVisit(JsBinaryOperation x, JsContext ctx) {
  }

  public void endVisit(JsBlock x, JsContext ctx) {
  }

  public void endVisit(JsBooleanLiteral x, JsContext ctx) {
  }

  public void endVisit(JsBreak x, JsContext ctx) {
  }

  public void endVisit(JsCase x, JsContext ctx) {
  }

  public void endVisit(JsCatch x, JsContext ctx) {
  }

  public void endVisit(JsConditional x, JsContext ctx) {
  }

  public void endVisit(JsContinue x, JsContext ctx) {
  }

  public void endVisit(JsDecimalLiteral x, JsContext ctx) {
  }

  public void endVisit(JsDefault x, JsContext ctx) {
  }

  public void endVisit(JsDoWhile x, JsContext ctx) {
  }

  public void endVisit(JsEmpty x, JsContext ctx) {
  }

  public void endVisit(JsExprStmt x, JsContext ctx) {
  }

  public void endVisit(JsFor x, JsContext ctx) {
  }

  public void endVisit(JsForIn x, JsContext ctx) {
  }

  public void endVisit(JsFunction x, JsContext ctx) {
  }

  public void endVisit(JsIf x, JsContext ctx) {
  }

  public void endVisit(JsIntegralLiteral x, JsContext ctx) {
  }

  public void endVisit(JsInvocation x, JsContext ctx) {
  }

  public void endVisit(JsLabel x, JsContext ctx) {
  }

  public void endVisit(JsNameRef x, JsContext ctx) {
  }

  public void endVisit(JsNew x, JsContext ctx) {
  }

  public void endVisit(JsNullLiteral x, JsContext ctx) {
  }

  public void endVisit(JsObjectLiteral x, JsContext ctx) {
  }

  public void endVisit(JsParameter x, JsContext ctx) {
  }

  public void endVisit(JsPostfixOperation x, JsContext ctx) {
  }

  public void endVisit(JsPrefixOperation x, JsContext ctx) {
  }

  public void endVisit(JsProgram x, JsContext ctx) {
  }

  public void endVisit(JsPropertyInitializer x, JsContext ctx) {
  }

  public void endVisit(JsRegExp x, JsContext ctx) {
  }

  public void endVisit(JsReturn x, JsContext ctx) {
  }

  public void endVisit(JsStringLiteral x, JsContext ctx) {
  }

  public void endVisit(JsSwitch x, JsContext ctx) {
  }

  public void endVisit(JsThisRef x, JsContext ctx) {
  }

  public void endVisit(JsThrow x, JsContext ctx) {
  }

  public void endVisit(JsTry x, JsContext ctx) {
  }

  public void endVisit(JsVar x, JsContext ctx) {
  }

  public void endVisit(JsVars x, JsContext ctx) {
  }

  public void endVisit(JsWhile x, JsContext ctx) {
  }

  public boolean visit(JsArrayAccess x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBinaryOperation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBlock x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBooleanLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBreak x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsCase x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsCatch x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsConditional x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsContinue x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsDecimalLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsDefault x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsDoWhile x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsEmpty x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsExprStmt x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsFor x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsForIn x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsFunction x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsIf x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsIntegralLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsInvocation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsLabel x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNameRef x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNew x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNullLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsParameter x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsParameters x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsPostfixOperation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsPrefixOperation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsProgram x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsPropertyInitializer x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsRegExp x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsReturn x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsStringLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsSwitch x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsThisRef x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsThrow x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsTry x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsVar x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsVars x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsWhile x, JsContext ctx) {
    return true;
  }

  protected JsNode doAccept(JsNode node) {
    doTraverse(node, UNMODIFIABLE_CONTEXT);
    return node;
  }

  protected void doAccept(JsCollection collection) {
    for (Iterator it = collection.iterator(); it.hasNext();) {
      doTraverse((JsNode) it.next(), UNMODIFIABLE_CONTEXT);
    }
  }

  protected void doAcceptWithInsertRemove(JsCollection collection) {
    for (Iterator it = collection.iterator(); it.hasNext();) {
      doTraverse((JsNode) it.next(), UNMODIFIABLE_CONTEXT);
    }
  }

  protected final void doTraverse(JsNode node, JsContext ctx) {
    try {
      node.traverse(this, ctx);
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  private InternalCompilerException translateException(JsNode node, Throwable e) {
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
