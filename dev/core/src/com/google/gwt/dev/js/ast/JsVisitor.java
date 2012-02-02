/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
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

  protected static final JsContext LVALUE_CONTEXT = new JsContext() {

    @Override
    public boolean canInsert() {
      return false;
    }

    @Override
    public boolean canRemove() {
      return false;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLvalue() {
      return true;
    }

    @Override
    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceMe(JsVisitable node) {
      throw new UnsupportedOperationException();
    }
  };

  protected static final JsContext UNMODIFIABLE_CONTEXT = new JsContext() {

    @Override
    public boolean canInsert() {
      return false;
    }

    @Override
    public boolean canRemove() {
      return false;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLvalue() {
      return false;
    }

    @Override
    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    @Override
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

  public final <T extends JsVisitable> void acceptList(List<T> collection) {
    doAcceptList(collection);
  }

  public JsExpression acceptLvalue(JsExpression expr) {
    return doAcceptLvalue(expr);
  }

  public final <T extends JsVisitable> void acceptWithInsertRemove(List<T> collection) {
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

  public void endVisit(JsDebugger x, JsContext ctx) {
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

  public void endVisit(JsInvocation x, JsContext ctx) {
  }

  public void endVisit(JsLabel x, JsContext ctx) {
  }

  public void endVisit(JsNameOf x, JsContext ctx) {
  }

  public void endVisit(JsNameRef x, JsContext ctx) {
  }

  public void endVisit(JsNew x, JsContext ctx) {
  }

  public void endVisit(JsNullLiteral x, JsContext ctx) {
  }

  public void endVisit(JsNumberLiteral x, JsContext ctx) {
  }
  
  public void endVisit(JsNumericEntry x, JsContext ctx) {
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

  public void endVisit(JsProgramFragment x, JsContext ctx) {
  }

  public void endVisit(JsPropertyInitializer x, JsContext ctx) {
  }

  public void endVisit(JsRegExp x, JsContext ctx) {
  }

  public void endVisit(JsReturn x, JsContext ctx) {
  }

  public void endVisit(JsSeedIdOf x, JsContext ctx) {
    endVisit((JsNameOf) x, ctx);
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

  public boolean visit(JsDebugger x, JsContext ctx) {
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

  public boolean visit(JsInvocation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsLabel x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNameOf x, JsContext ctx) {
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

  public boolean visit(JsNumberLiteral x, JsContext ctx) {
    return true;
  }
  
  public boolean visit(JsNumericEntry x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsParameter x, JsContext ctx) {
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

  public boolean visit(JsProgramFragment x, JsContext ctx) {
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

  public boolean visit(JsSeedIdOf x, JsContext ctx) {
    return visit((JsNameOf) x, ctx);
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

  protected <T extends JsVisitable> T doAccept(T node) {
    doTraverse(node, UNMODIFIABLE_CONTEXT);
    return node;
  }

  protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
    for (Iterator<T> it = collection.iterator(); it.hasNext();) {
      doTraverse(it.next(), UNMODIFIABLE_CONTEXT);
    }
  }

  protected JsExpression doAcceptLvalue(JsExpression expr) {
    doTraverse(expr, LVALUE_CONTEXT);
    return expr;
  }

  protected <T extends JsVisitable> void doAcceptWithInsertRemove(List<T> collection) {
    for (Iterator<T> it = collection.iterator(); it.hasNext();) {
      doTraverse(it.next(), UNMODIFIABLE_CONTEXT);
    }
  }

  protected final <T extends JsVisitable> void doTraverse(T node, JsContext ctx) {
    try {
      node.traverse(this, ctx);
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  private InternalCompilerException translateException(JsVisitable node, Throwable e) {
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
    ice.addNode((HasSourceInfo) node);
    return ice;
  }
}
