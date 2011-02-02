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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsDebugger;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;

/**
 * Determines if a statement at the end of a block requires a semicolon.
 * 
 * For example, the following statements require semicolons:<br>
 * <ul>
 * <li>if (cond);</li>
 * <li>while (cond);</li>
 * </ul>
 * 
 * The following do not require semicolons:<br>
 * <ul>
 * <li>return 1</li>
 * <li>do {} while(true)</li>
 * </ul>
 */
public class JsRequiresSemiVisitor extends JsVisitor {

  public static boolean exec(JsStatement lastStatement) {
    JsRequiresSemiVisitor visitor = new JsRequiresSemiVisitor();
    visitor.accept(lastStatement);
    return visitor.needsSemicolon;
  }

  private boolean needsSemicolon = false;

  private JsRequiresSemiVisitor() {
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsBreak x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsDebugger x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsDoWhile x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsEmpty x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsFor x, JsContext ctx) {
    if (x.getBody() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

  @Override
  public boolean visit(JsForIn x, JsContext ctx) {
    if (x.getBody() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

  @Override
  public boolean visit(JsIf x, JsContext ctx) {
    JsStatement thenStmt = x.getThenStmt();
    JsStatement elseStmt = x.getElseStmt();
    JsStatement toCheck = thenStmt;
    if (elseStmt != null) {
      toCheck = elseStmt;
    }
    if (toCheck instanceof JsEmpty) {
      needsSemicolon = true;
    } else {
      // Must recurse to determine last statement (possible if-else chain).
      accept(toCheck);
    }
    return false;
  }

  @Override
  public boolean visit(JsLabel x, JsContext ctx) {
    if (x.getStmt() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

  @Override
  public boolean visit(JsReturn x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsSwitch x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsThrow x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsTry x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsVars x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsWhile x, JsContext ctx) {
    if (x.getBody() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

}
