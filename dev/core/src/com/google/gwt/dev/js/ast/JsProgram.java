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

import com.google.gwt.dev.jjs.SourceInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * A JavaScript program.
 */
public final class JsProgram extends JsNode<JsProgram> {

  private final JsStatement debuggerStmt = new JsDebugger(
      createSourceInfoSynthetic("debugger statement"));

  private final JsEmpty emptyStmt = new JsEmpty(
      createSourceInfoSynthetic("Empty statement"));

  private final JsBooleanLiteral falseLiteral = new JsBooleanLiteral(
      createSourceInfoSynthetic("false literal"), false);

  private final JsGlobalBlock globalBlock;

  private final JsNullLiteral nullLiteral = new JsNullLiteral(
      createSourceInfoSynthetic("null literal"));

  private final Map<Double, JsNumberLiteral> numberLiteralMap = new HashMap<Double, JsNumberLiteral>();

  private final JsScope objectScope;

  private final JsRootScope rootScope;

  private final Map<String, JsStringLiteral> stringLiteralMap = new HashMap<String, JsStringLiteral>();

  private final JsScope topScope;

  private final JsBooleanLiteral trueLiteral = new JsBooleanLiteral(
      createSourceInfoSynthetic("true literal"), true);

  private boolean enableSourceInfoDescendants;

  /**
   * Constructs a JavaScript program object.
   */
  public JsProgram() {
    super(SourceInfoJs.INTRINSIC.makeChild("JavaScript program"));
    rootScope = new JsRootScope(this);
    globalBlock = new JsGlobalBlock(createSourceInfoSynthetic("global block"));
    topScope = new JsScope(rootScope, "Global");
    objectScope = new JsScope(rootScope, "Object");
  }

  public SourceInfo createSourceInfo(int lineNumber, String location) {
    return new SourceInfoJs(-1, -1, lineNumber, location,
        enableSourceInfoDescendants);
  }

  public SourceInfo createSourceInfoSynthetic(String description) {
    String caller = enableSourceInfoDescendants ? SourceInfoJs.findCaller()
        : "Unknown caller";
    return createSourceInfo(0, caller).makeChild(description);
  }

  public JsBooleanLiteral getBooleanLiteral(boolean truth) {
    if (truth) {
      return getTrueLiteral();
    }
    return getFalseLiteral();
  }

  /**
   * Gets the {@link JsStatement} to use whenever parsed source include a
   * <code>debugger</code> statement.
   * 
   * @see #setDebuggerStmt(JsStatement)
   */
  public JsStatement getDebuggerStmt() {
    return debuggerStmt;
  }

  public JsEmpty getEmptyStmt() {
    return emptyStmt;
  }

  public JsBooleanLiteral getFalseLiteral() {
    return falseLiteral;
  }

  /**
   * Gets the one and only global block.
   */
  public JsBlock getGlobalBlock() {
    return globalBlock;
  }

  public JsNullLiteral getNullLiteral() {
    return nullLiteral;
  }

  public JsNumberLiteral getNumberLiteral(double value) {
    JsNumberLiteral lit = numberLiteralMap.get(value);
    if (lit == null) {
      lit = new JsNumberLiteral(createSourceInfoSynthetic("Number literal "
          + value), value);
      numberLiteralMap.put(value, lit);
    }
    return lit;
  }

  public JsScope getObjectScope() {
    return objectScope;
  }

  /**
   * Gets the quasi-mythical root scope. This is not the same as the top scope;
   * all unresolvable identifiers wind up here, because they are considered
   * external to the program.
   */
  public JsRootScope getRootScope() {
    return rootScope;
  }

  /**
   * Gets the top level scope. This is the scope of all the statements in the
   * main program.
   */
  public JsScope getScope() {
    return topScope;
  }

  public JsStringLiteral getStringLiteral(SourceInfo sourceInfo, String value) {
    JsStringLiteral lit = stringLiteralMap.get(value);
    if (lit == null) {
      lit = new JsStringLiteral(sourceInfo, value);
      stringLiteralMap.put(value, lit);
    } else {
      lit.getSourceInfo().addAdditonalAncestors(sourceInfo);
    }
    return lit;
  }

  public JsBooleanLiteral getTrueLiteral() {
    return trueLiteral;
  }

  public JsNameRef getUndefinedLiteral() {
    return rootScope.findExistingName("undefined").makeRef(
        createSourceInfoSynthetic("undefined reference"));
  }

  /**
   * Controls whether or not SourceInfo nodes created via the JsProgram will
   * record descendant information. Enabling this feature will collect extra
   * data during the compilation cycle, but at a cost of memory and object
   * allocations.
   */
  public void setEnableSourceInfoDescendants(boolean enable) {
    enableSourceInfoDescendants = enable;
  }

  public void traverse(JsVisitor v, JsContext<JsProgram> ctx) {
    if (v.visit(this, ctx)) {
      v.accept(globalBlock);
    }
    v.endVisit(this, ctx);
  }
}
