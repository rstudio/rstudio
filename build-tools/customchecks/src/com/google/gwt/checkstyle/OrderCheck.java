// CHECKSTYLE_OFF:Must use GNU license for code based on checkstyle
// /////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2005 Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// //////////////////////////////////////////////////////////////////////////////
// CHECKSTYLE_ON

// This class is based upon the
// com.puppycrawl.tools.checkstyle.checks.coding.DeclarationOrderCheck

package com.google.gwt.checkstyle;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.ScopeUtils;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.Stack;

/**
 * Checks that the parts of a class or interface declaration appear in the order
 * specified by the 'Making GWT better' style guide.
 */

public class OrderCheck extends Check {
  /**
   * Encapsulate the state in each class scope in order to handle inner classes.
   */
  private static class ScopeState {
    /**
     * Current state.
     */
    private int state = State.TYPE;

    /**
     * Current access modifier for state.
     */
    private Scope visibility = Scope.PUBLIC;
  }

  /**
   * Ordered category states for code elements.
   */
  private static class State {
    private static final int TYPE = 0;
    private static final int STATIC_FIELDS = 1;
    private static final int STATIC_INITS = 2;
    private static final int STATIC_METHODS = 3;
    private static final int INSTANCE_FIELDS = 4;
    private static final int INSTANCE_INITS = 5;
    private static final int CONSTRUCTORS = 6;
    private static final int INSTANCE_METHODS = 7;
  }

  /**
   * List of Declaration States. This is necessary due to inner classes that
   * have their own state.
   */
  private final Stack<ScopeState> classScopes = new Stack<ScopeState>();

  /**
   * Previous method name, used for alphabetical ordering.
   */
  private String previousMethodName;

  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.CTOR_DEF, TokenTypes.METHOD_DEF, TokenTypes.MODIFIERS,
        TokenTypes.STATIC_INIT, TokenTypes.INSTANCE_INIT, TokenTypes.OBJBLOCK};
  }

  public void leaveToken(DetailAST aAST) {
    switch (aAST.getType()) {
      case TokenTypes.OBJBLOCK:
        classScopes.pop();
        previousMethodName = null;
        break;
      case TokenTypes.METHOD_DEF:
        // If the previous method was in the same class, with the same
        // modifiers, check that it is alphabetically before the current
        // method.
        String methodName = aAST.findFirstToken(TokenTypes.IDENT).getText();
        if (previousMethodName != null
            && (previousMethodName.compareToIgnoreCase(methodName)) > 0) {
          log(aAST, methodName + " is not alphabetical.");
        }
        previousMethodName = methodName;
        break;
      default:
    }
  }

  public void visitToken(DetailAST aAST) {
    try {
      int parentType = 0;
      if (aAST.getParent() != null) {
        parentType = aAST.getParent().getType();
      }
      switch (aAST.getType()) {
        case TokenTypes.OBJBLOCK:
          classScopes.push(new ScopeState());
          previousMethodName = null;
          break;

        case TokenTypes.CTOR_DEF:
          if (parentType != TokenTypes.OBJBLOCK) {
            return;
          }
          checkState(aAST, State.CONSTRUCTORS, "Constructor");
          break;

        case TokenTypes.MODIFIERS:
          if (parentType == TokenTypes.VARIABLE_DEF) {
            checkVariable(aAST);
          }
          if (parentType == TokenTypes.METHOD_DEF) {
            checkMethod(aAST);
          }
          break;
        case TokenTypes.STATIC_INIT: {
          checkState(aAST, State.STATIC_INITS, "Static initializer");
          break;
        }
        case TokenTypes.INSTANCE_INIT: {
          checkState(aAST, State.INSTANCE_INITS, "Instance initializer");
        }
          break;
        default:
      }
    } catch (Throwable t) {
      // CheckStyle swallows errors in general, we want OrderCheck errors to be
      // visible.
      t.printStackTrace();
      throw new RuntimeException("Exception/Error in OrderCheck", t);
    }
  }

  /**
   * Check the modifiers of a method for order conflicts.
   */
  private void checkMethod(DetailAST aAST) {
    if (aAST.getParent().getParent().getType() != TokenTypes.OBJBLOCK) {
      return;
    }
    if (aAST.findFirstToken(TokenTypes.LITERAL_STATIC) != null) {
      if (checkState(aAST, State.STATIC_METHODS, "Static method")) {
        previousMethodName = null;
      }
    } else {
      if (checkState(aAST, State.INSTANCE_METHODS, "Instance method")) {
        previousMethodName = null;
      }
    }
  }

  /**
   * Checks the category and visibility of declarations.
   * 
   * @return whether the state or visibility modifiers have changed
   */
  private boolean checkState(DetailAST aAST, int curState, String type) {
    ScopeState scope = classScopes.peek();
    if (scope.state > curState) {
      log(aAST, type + " in wrong order.");
      // Wrong type implies at least a temporary state switch.
      return true;
    } else if (scope.state == curState) {
      final Scope curVisibility = ScopeUtils.getScopeFromMods(aAST);
      if (scope.visibility.compareTo(curVisibility) > 0) {
        log(aAST, curVisibility.getName() + " " + type
            + " should not occur after " + scope.visibility.getName() + " "
            + type);
        return false;
      } else if (scope.visibility != curVisibility) {
        scope.visibility = curVisibility;
        return true;
      } else {
        return false;
      }
    } else {
      scope.state = curState;
      scope.visibility = Scope.PUBLIC;
      return true;
    }
  }

  /**
   * Check the modifiers of a variable for order conflicts.
   */
  private void checkVariable(DetailAST aAST) {
    if (aAST.getParent().getParent().getType() != TokenTypes.OBJBLOCK) {
      return;
    }
    if (aAST.findFirstToken(TokenTypes.LITERAL_STATIC) != null) {
      checkState(aAST, State.STATIC_FIELDS, "Static field");
    } else {
      checkState(aAST, State.INSTANCE_FIELDS, "Instance field");
    }
  }
}