package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsStatement;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

/**
 * Force all functions to be evaluated at the top of the lexical scope in
 * which they reside. This makes {@link StaticEvalVisitor} simpler in that we
 * no longer have to worry about function declarations within expressions.
 * After this runs, only statements can contain declarations. Moved functions
 * will end up just before the statement in which they presently reside.
 */
public class EvalFunctionsAtTopScope extends JsModVisitor {
  private final Set<JsFunction> dontMove = new HashSet<JsFunction>();
  private final Stack<ListIterator<JsStatement>> itrStack = new Stack<ListIterator<JsStatement>>();
  private final Stack<JsBlock> scopeStack = new Stack<JsBlock>();

  @Override
  public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
    scopeStack.pop();
  }

  @Override
  public void endVisit(JsProgram x, JsContext<JsProgram> ctx) {
    scopeStack.pop();
  }

  @Override
  public void endVisit(JsProgramFragment x, JsContext<JsProgramFragment> ctx) {
    scopeStack.pop();
  }

  @Override
  public boolean visit(JsBlock x, JsContext<JsStatement> ctx) {
    if (x == scopeStack.peek()) {
      ListIterator<JsStatement> itr = x.getStatements().listIterator();
      itrStack.push(itr);
      while (itr.hasNext()) {
        JsStatement stmt = itr.next();
        JsFunction func = JsStaticEval.isFunctionDecl(stmt);
        // Already at the top level.
        if (func != null) {
          dontMove.add(func);
        }
        accept(stmt);
        if (func != null) {
          dontMove.remove(func);
        }
      }
      itrStack.pop();
      // Already visited.
      return false;
    } else {
      // Just do normal visitation.
      return true;
    }
  }

  @Override
  public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
    /*
     * We do this during visit() to preserve first-to-last evaluation order.
     */
    if (x.getName() != null && !dontMove.contains(x)) {
      /*
       * Reinsert this function into the statement immediately before the
       * current statement. The current statement will have already been
       * returned from the current iterator's next(), so we have to
       * backshuffle one step to get in front of it.
       */
      ListIterator<JsStatement> itr = itrStack.peek();
      itr.previous();
      itr.add(x.makeStmt());
      itr.next();
      ctx.replaceMe(x.getName().makeRef(
          x.getSourceInfo().makeChild(EvalFunctionsAtTopScope.class,
              "Shuffled evaluation order")));
    }

    // Dive into the function itself.
    scopeStack.push(x.getBody());
    return true;
  }

  @Override
  public boolean visit(JsProgram x, JsContext<JsProgram> ctx) {
    scopeStack.push(x.getGlobalBlock());
    return true;
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext<JsProgramFragment> ctx) {
    scopeStack.push(x.getGlobalBlock());
    return true;
  }

  public static void exec(JsProgram jsProgram) {
    EvalFunctionsAtTopScope fev = new EvalFunctionsAtTopScope();
    fev.accept(jsProgram);
  }
}