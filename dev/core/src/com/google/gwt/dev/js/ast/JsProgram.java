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

import com.google.gwt.dev.jjs.CorrelationFactory;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.jjs.Correlation.Literal;

import java.util.HashMap;
import java.util.Map;

/**
 * A JavaScript program.
 */
public final class JsProgram extends JsNode<JsProgram> {

  private final CorrelationFactory correlator;

  private final JsStatement debuggerStmt;

  private final JsEmpty emptyStmt;

  private final JsBooleanLiteral falseLiteral;

  private JsProgramFragment[] fragments;

  /**
   * The root intrinsic source info.
   */
  private final SourceInfo intrinsic;

  private final Map<String, JsFunction> indexedFunctions = new HashMap<String, JsFunction>();

  private final JsNullLiteral nullLiteral;

  private final Map<Double, JsNumberLiteral> numberLiteralMap = new HashMap<Double, JsNumberLiteral>();

  private final JsScope objectScope;

  private final JsRootScope rootScope;

  private final Map<String, JsStringLiteral> stringLiteralMap = new HashMap<String, JsStringLiteral>();

  private final SourceInfo stringPoolSourceInfo;

  private final JsScope topScope;

  private final JsBooleanLiteral trueLiteral;

  public JsProgram() {
    this(new CorrelationFactory.DummyCorrelationFactory());
  }

  /**
   * Constructs a JavaScript program object.
   * 
   * @param soycEnabled Controls whether or not SourceInfo nodes created via the
   *          JsProgram will record descendant information. Enabling this
   *          feature will collect extra data during the compilation cycle, but
   *          at a cost of memory and object allocations.
   */
  public JsProgram(CorrelationFactory correlator) {
    super(correlator.makeSourceInfo(SourceOrigin.create(0,
        JsProgram.class.getName())));

    this.correlator = correlator;
    intrinsic = createSourceInfo(0, getClass().getName());

    rootScope = new JsRootScope(this);
    topScope = new JsScope(rootScope, "Global");
    objectScope = new JsScope(rootScope, "Object");
    setFragmentCount(1);

    debuggerStmt = new JsDebugger(createLiteralSourceInfo("debugger statement"));
    emptyStmt = new JsEmpty(createLiteralSourceInfo("Empty statement"));
    falseLiteral = new JsBooleanLiteral(createLiteralSourceInfo(
        "false literal", Literal.JS_BOOLEAN), false);
    nullLiteral = new JsNullLiteral(createLiteralSourceInfo("null literal",
        Literal.JS_NULL));
    trueLiteral = new JsBooleanLiteral(createLiteralSourceInfo("true literal",
        Literal.JS_BOOLEAN), true);

    trueLiteral.getSourceInfo().addCorrelation(
        correlator.by(Literal.JS_BOOLEAN));
    stringPoolSourceInfo = createLiteralSourceInfo("String pool",
        Literal.JS_STRING);
  }

  public SourceInfo createSourceInfo(int lineNumber, String location) {
    return correlator.makeSourceInfo(SourceOrigin.create(lineNumber, location));
  }

  public SourceInfo createSourceInfoSynthetic(Class<?> caller,
      String description) {
    return createSourceInfo(0, caller.getName()).makeChild(caller, description);
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

  public JsBlock getFragmentBlock(int fragment) {
    if (fragment < 0 || fragment >= fragments.length) {
      throw new IllegalArgumentException("Invalid fragment: " + fragment);
    }
    return fragments[fragment].getGlobalBlock();
  }

  public int getFragmentCount() {
    return this.fragments.length;
  }

  /**
   * Gets the one and only global block.
   */
  public JsBlock getGlobalBlock() {
    return getFragmentBlock(0);
  }

  public JsFunction getIndexedFunction(String name) {
    return indexedFunctions.get(name);
  }

  public JsNullLiteral getNullLiteral() {
    return nullLiteral;
  }

  public JsNumberLiteral getNumberLiteral(double value) {
    return getNumberLiteral(null, value);
  }

  public JsNumberLiteral getNumberLiteral(SourceInfo info, double value) {
    /*
     * This method only canonicalizes number literals when we don't have an
     * incoming SourceInfo so that we can distinguish int-0 from double-0 in the
     * analysis.
     */
    if (info == null) {
      JsNumberLiteral lit = numberLiteralMap.get(value);
      if (lit == null) {
        info = createSourceInfoSynthetic(JsProgram.class, "Number literal "
            + value);
        info.addCorrelation(correlator.by(Literal.JS_NUMBER));
        lit = new JsNumberLiteral(info, value);
        numberLiteralMap.put(value, lit);
      }

      return lit;
    } else {
      // Only add a JS_NUMBER if no literal correlation present: e.g. Java int
      if (info.getPrimaryCorrelation(Axis.LITERAL) == null) {
        // Don't mutate incoming SourceInfo
        info = info.makeChild(JsProgram.class, "Number literal " + value);
        info.addCorrelation(correlator.by(Literal.JS_NUMBER));
      }
      return new JsNumberLiteral(info, value);
    }
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

  /**
   * Creates or retrieves a JsStringLiteral from an interned object pool.
   */
  public JsStringLiteral getStringLiteral(SourceInfo sourceInfo, String value) {
    JsStringLiteral lit = stringLiteralMap.get(value);
    if (lit == null) {
      lit = new JsStringLiteral(stringPoolSourceInfo.makeChild(JsProgram.class,
          "String literal: " + value), value);
      stringLiteralMap.put(value, lit);
    }
    lit.getSourceInfo().merge(sourceInfo);
    return lit;
  }

  public JsBooleanLiteral getTrueLiteral() {
    return trueLiteral;
  }

  public JsNameRef getUndefinedLiteral() {
    SourceInfo info = createSourceInfoSynthetic(JsProgram.class,
        "undefined reference");
    info.addCorrelation(correlator.by(Literal.JS_UNDEFINED));
    return rootScope.findExistingName("undefined").makeRef(info);
  }

  public void setFragmentCount(int fragments) {
    this.fragments = new JsProgramFragment[fragments];
    for (int i = 0; i < fragments; i++) {
      this.fragments[i] = new JsProgramFragment(createSourceInfoSynthetic(
          JsProgram.class, "fragment " + i));
    }
  }

  public void setIndexedFunctions(Map<String, JsFunction> indexedFunctions) {
    this.indexedFunctions.clear();
    this.indexedFunctions.putAll(indexedFunctions);
  }

  public void traverse(JsVisitor v, JsContext<JsProgram> ctx) {
    if (v.visit(this, ctx)) {
      for (JsProgramFragment fragment : fragments) {
        v.accept(fragment);
      }
    }
    v.endVisit(this, ctx);
  }

  private SourceInfo createLiteralSourceInfo(String description) {
    return intrinsic.makeChild(getClass(), description);
  }

  private SourceInfo createLiteralSourceInfo(String description, Literal literal) {
    SourceInfo child = createLiteralSourceInfo(description);
    child.addCorrelation(correlator.by(literal));
    return child;
  }
}
