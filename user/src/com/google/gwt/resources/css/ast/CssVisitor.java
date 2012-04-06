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
package com.google.gwt.resources.css.ast;

import java.util.List;

/**
 * The base class for visiting a CSS tree. Traversal is initiated with a call to
 * one of the <code>accept</code> methods. The default behavior of the
 * <code>visit</code> methods is to return <code>true</code> to indicate that
 * the calling node should traverse its descendant nodes.
 */
public class CssVisitor {
  protected static final Context UNMODIFIABLE_CONTEXT = new Context() {

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(CssNode node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(CssNode node) {
      throw new UnsupportedOperationException();
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(CssNode node) {
      throw new UnsupportedOperationException();
    }
  };

  public final void accept(List<? extends CssNode> nodes) {
    doAccept(nodes);
  }

  public final <T extends CssNode> T accept(T node) {
    return doAccept(node);
  }

  public final void acceptWithInsertRemove(List<? extends CssNode> nodes) {
    doAcceptWithInsertRemove(nodes);
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssDef x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssEval x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssExternalSelectors x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssFontFace x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssIf x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssMediaRule x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssNoFlip x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssPageRule x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssProperty x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssRule x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssSelector x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssSprite x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssStylesheet x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssUrl x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssUnknownAtRule x, Context ctx) {
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssDef x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssEval x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssExternalSelectors x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssFontFace x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssIf x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssMediaRule x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssNoFlip x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssPageRule x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssProperty x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssRule x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssSelector x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssSprite x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssStylesheet x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssUrl x, Context ctx) {
    return true;
  }

  /**
   * @param x the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssUnknownAtRule x, Context ctx) {
    return true;
  }

  protected void doAccept(List<? extends CssNode> list) {
    for (CssNode node : list) {
      doTraverse(node, UNMODIFIABLE_CONTEXT);
    }
  }

  protected <T extends CssNode> T doAccept(T node) {
    doTraverse(node, UNMODIFIABLE_CONTEXT);
    return node;
  }

  protected void doAcceptWithInsertRemove(List<? extends CssNode> list) {
    for (CssNode node : list) {
      doTraverse(node, UNMODIFIABLE_CONTEXT);
    }
  }

  protected final void doTraverse(CssNode node, Context ctx) {
    try {
      node.traverse(this, ctx);
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  private CssCompilerException translateException(CssNode node, Throwable e) {
    CssCompilerException ex;
    if (e instanceof CssCompilerException) {
      ex = (CssCompilerException) e;
    } else {
      ex = new CssCompilerException("Unexpected error during visit.", e);
    }
    ex.addNode(node);
    return ex;
  }
}
