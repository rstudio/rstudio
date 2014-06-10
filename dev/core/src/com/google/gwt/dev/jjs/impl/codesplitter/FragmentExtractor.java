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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.JavaAndJavaScript;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.JsHoister.Cloner;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts multiple JS statements (called a fragment) out of the complete JS program based on
 * supplied type/method/field/string liveness conditions.
 *
 * <p>
 * <b>Liveness as defined here is not an intuitive concept.</b> A type or method (note that
 * constructors are methods) is considered live for the current fragment when that type can only be
 * instantiated or method executed when the current fragment has already been loaded. That does not
 * always mean that it was caused by direct execution of the current fragment. It may instead mean
 * that direction execution of some other fragment has been affected by the loading of the current
 * fragment in a way that results in the instantiation of the type or execution of the method. It is
 * this second case that can lead to seemingly contradictory but valid situations like having a type
 * which is not currently live but which has a currently live constructor. For example it might be
 * possible to instantiate type Foo even with fragment Bar being loaded (i.e. Foo is not live for
 * Bar) but the loading of fragment Bar might be required to reach a particular one of Bar's
 * multiple constructor (i.e. that constructor is live for Bar).
 * </p>
 */
public class FragmentExtractor {

    /**
   * A logger for statements that the fragment extractor encounters. Install one using
   * {@link FragmentExtractor#setStatementLogger(StatementLogger)} .
   */
  public static interface StatementLogger {
    void log(JsStatement statement, boolean include);
  }

  /**
   * Mutates the provided defineClass statement to remove references to constructors which have not
   * been made live by the current fragment. It also counts the constructor references that
   * were not removed.
   */
  private class DefineClassMinimizerVisitor extends JsModVisitor {

    private final LivenessPredicate alreadyLoadedPredicate;
    private final LivenessPredicate livenessPredicate;
    private int liveConstructorCount;

    private DefineClassMinimizerVisitor(
        LivenessPredicate alreadyLoadedPredicate, LivenessPredicate livenessPredicate) {
      this.alreadyLoadedPredicate = alreadyLoadedPredicate;
      this.livenessPredicate = livenessPredicate;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      JMethod method = map.nameToMethod(x.getName());
      if (!(method instanceof JConstructor)) {
        return;
      }
      // Only examines references to constructor methods.

      JConstructor constructor = (JConstructor) method;
      boolean fragmentExpandsConstructorLiveness =
          !alreadyLoadedPredicate.isLive(constructor) && livenessPredicate.isLive(constructor);
      if (fragmentExpandsConstructorLiveness) {
        // Counts kept references to live constructors.
        liveConstructorCount++;
      } else {
        // Removes references to dead constructors.
        ctx.removeMe();
      }
    }

    /**
     * Enables varargs mutation.
     */
    @Override
    protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
      doAcceptWithInsertRemove(collection);
    }
  }

  private static class MinimalDefineClassResult {

    private int liveConstructorCount;
    private JsExprStmt statement;

    public MinimalDefineClassResult(JsExprStmt statement, int liveConstructorCount) {
      this.statement = statement;
      this.liveConstructorCount = liveConstructorCount;
    }
  }

  private static class NullStatementLogger implements StatementLogger {
    @Override
    public void log(JsStatement statement, boolean include) {
    }
  }

  /**
   * Return the Java method corresponding to <code>stat</code>, or
   * <code>null</code> if there isn't one. It recognizes JavaScript of the form
   * <code>function foo(...) { ...}</code>, where <code>foo</code> is the name
   * of the JavaScript translation of a Java method.
   */
  public static JMethod methodFor(JsStatement stat, JavaToJavaScriptMap map) {
    if (stat instanceof JsExprStmt) {
      JsExpression exp = ((JsExprStmt) stat).getExpression();
      if (exp instanceof JsFunction) {
        JsFunction func = (JsFunction) exp;
        if (func.getName() != null) {
          JMethod method = map.nameToMethod(func.getName());
          if (method != null) {
            return method;
          }
        }
      }
    }

    return map.vtableInitToMethod(stat);
  }

  private static JsExprStmt createDefineClassClone(JsExprStmt defineClassStatement) {
    Cloner cloner = new Cloner();
    cloner.accept(defineClassStatement.getExpression());
    JsExprStmt minimalDefineClassStatement = cloner.getExpression().makeStmt();
    return minimalDefineClassStatement;
  }

  private final JProgram jprogram;

  private final JsProgram jsprogram;

  private final JavaToJavaScriptMap map;

  private StatementLogger statementLogger = new NullStatementLogger();

  public FragmentExtractor(JavaAndJavaScript javaAndJavaScript) {
    this(javaAndJavaScript.jprogram, javaAndJavaScript.jsprogram, javaAndJavaScript.map);
  }

  public FragmentExtractor(JProgram jprogram, JsProgram jsprogram, JavaToJavaScriptMap map) {
    this.jprogram = jprogram;
    this.jsprogram = jsprogram;
    this.map = map;
  }

  /**
   * Create a call to {@link AsyncFragmentLoader#onLoad}.
   */
  public List<JsStatement> createOnLoadedCall(int fragmentId) {
    JMethod loadMethod = jprogram.getIndexedMethod("AsyncFragmentLoader.onLoad");
    JsName loadMethodName = map.nameForMethod(loadMethod);
    SourceInfo sourceInfo = jsprogram.getSourceInfo();
    JsInvocation call = new JsInvocation(sourceInfo);
    call.setQualifier(wrapWithEntry(loadMethodName.makeRef(sourceInfo)));
    call.getArguments().add(new JsNumberLiteral(sourceInfo, fragmentId));
    List<JsStatement> newStats = Collections.<JsStatement> singletonList(call.makeStmt());
    return newStats;
  }

  /**
   * Assume that all code described by <code>alreadyLoadedPredicate</code> has
   * been downloaded. Extract enough JavaScript statements that the code
   * described by <code>livenessPredicate</code> can also run. The caller should
   * ensure that <code>livenessPredicate</code> includes strictly more live code
   * than <code>alreadyLoadedPredicate</code>.
   */
  public List<JsStatement> extractStatements(
      LivenessPredicate livenessPredicate, LivenessPredicate alreadyLoadedPredicate) {
    List<JsStatement> extractedStats = new ArrayList<JsStatement>();

    /**
     * The type whose vtables can currently be installed.
     */
    JClassType currentVtableType = null;
    JClassType pendingVtableType = null;
    JsExprStmt pendingDefineClass = null;

    List<JsStatement> statements = jsprogram.getGlobalBlock().getStatements();
    for (JsStatement statement : statements) {

      boolean keep;
      JClassType vtableTypeAssigned = vtableTypeAssigned(statement);
      if (vtableTypeAssigned != null) {
        // Keeps defineClass statements of live types or types with a live constructor.
        MinimalDefineClassResult minimalDefineClassResult = createMinimalDefineClass(
            livenessPredicate, alreadyLoadedPredicate, (JsExprStmt) statement);
        boolean liveType = !alreadyLoadedPredicate.isLive(vtableTypeAssigned)
            && livenessPredicate.isLive(vtableTypeAssigned);
        boolean liveConstructors = minimalDefineClassResult.liveConstructorCount > 0;

        if (liveConstructors || liveType) {
          statement = minimalDefineClassResult.statement;
          keep = true;
        } else {
          pendingDefineClass = minimalDefineClassResult.statement;
          pendingVtableType = vtableTypeAssigned;
          keep = false;
        }
      } else if (containsRemovableVars(statement)) {
        statement = removeSomeVars((JsVars) statement, livenessPredicate, alreadyLoadedPredicate);
        keep = !(statement instanceof JsEmpty);
      } else {
        keep = isLive(statement, livenessPredicate) && !isLive(statement, alreadyLoadedPredicate);
      }

      statementLogger.log(statement, keep);

      if (keep) {
        if (vtableTypeAssigned != null) {
          currentVtableType = vtableTypeAssigned;
        }
        JClassType vtableType = vtableTypeNeeded(statement);
        if (vtableType != null && vtableType != currentVtableType) {
          assert pendingVtableType == vtableType;
          extractedStats.add(pendingDefineClass);
          currentVtableType = pendingVtableType;
          pendingDefineClass = null;
          pendingVtableType = null;
        }
        extractedStats.add(statement);
      }
    }

    return extractedStats;
  }

  /**
   * Find all Java methods that still exist in the resulting JavaScript, even
   * after JavaScript inlining and pruning.
   */
  public Set<JMethod> findAllMethodsInJavaScript() {
    Set<JMethod> methodsInJs = new HashSet<JMethod>();
    for (int frag = 0; frag < jsprogram.getFragmentCount(); frag++) {
      List<JsStatement> stats = jsprogram.getFragmentBlock(frag).getStatements();
      for (JsStatement stat : stats) {
        JMethod method = methodFor(stat);
        if (method != null) {
          methodsInJs.add(method);
        }
      }
    }
    return methodsInJs;
  }

  public void setStatementLogger(StatementLogger logger) {
    statementLogger = logger;
  }

  /**
   * Check whether this statement is a {@link JsVars} that contains individual vars that could be
   * removed. If it does, then {@link #removeSomeVars(JsVars, LivenessPredicate, LivenessPredicate)}
   * is sensible for this statement and should be used instead of
   * {@link #isLive(JsStatement, LivenessPredicate)} .
   */
  private boolean containsRemovableVars(JsStatement stat) {
    if (stat instanceof JsVars) {
      for (JsVar var : (JsVars) stat) {

        JField field = map.nameToField(var.getName());
        if (field != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * DefineClass calls mark the existence of a class and associate a castMaps with the class's
   * various constructors. These multiple constructors are provided as JsNameRef varargs to the
   * defineClass call but only the constructors that are live in the current fragment should be
   * included.
   *
   * <p>
   * This function strips out the dead constructors and returns the modified defineClass call. The
   * stripped constructors will be kept by other defineClass calls in other fragments at other times.
   * </p>
   */
  private MinimalDefineClassResult createMinimalDefineClass(LivenessPredicate livenessPredicate,
      LivenessPredicate alreadyLoadedPredicate, JsExprStmt defineClassStatement) {
    DefineClassMinimizerVisitor defineClassMinimizerVisitor =
        new DefineClassMinimizerVisitor(alreadyLoadedPredicate, livenessPredicate);
    JsExprStmt minimalDefineClassStatement = createDefineClassClone(defineClassStatement);
    defineClassMinimizerVisitor.accept(minimalDefineClassStatement);
    return new MinimalDefineClassResult(
        minimalDefineClassStatement, defineClassMinimizerVisitor.liveConstructorCount);
  }

  private boolean isLive(JsStatement stat, LivenessPredicate livenessPredicate) {
    JClassType type = map.typeForStatement(stat);
    if (type != null) {
      // This is part of the code only needed once a type is instantiable
      return livenessPredicate.isLive(type);
    }

    JMethod meth = methodFor(stat);

    if (meth != null) {
      /*
       * This statement either defines a method or installs it in a vtable.
       */
      if (!livenessPredicate.isLive(meth)) {
        // The method is not live. Skip it.
        return false;
      }
      // The method is live. Check that its enclosing type is instantiable.
      // TODO(spoon): this check should not be needed once the CFA is updated
      return !meth.needsVtable() || livenessPredicate.isLive(meth.getEnclosingType());
    }

    return livenessPredicate.miscellaneousStatementsAreLive();
  }

  /**
   * Check whether a variable is needed. If the variable is an intern variable,
   * then return whether the interned value is live. If the variable corresponds
   * to a Java field, then return whether the Java field is live. Otherwise,
   * assume the variable is needed and return <code>true</code>.
   *
   * Whenever this method is updated, also look at
   * {@link #containsRemovableVars(JsStatement)}.
   */
  private boolean isLive(JsVar var, LivenessPredicate livenessPredicate) {
    JField field = map.nameToField(var.getName());
    if (field != null) {
      // It's a field
      return livenessPredicate.isLive(field);
    }

    // It's not an intern variable at all
    return livenessPredicate.miscellaneousStatementsAreLive();
  }

  /**
   * Return the Java method corresponding to <code>stat</code>, or
   * <code>null</code> if there isn't one. It recognizes JavaScript of the form
   * <code>function foo(...) { ...}</code>, where <code>foo</code> is the name
   * of the JavaScript translation of a Java method.
   */
  private JMethod methodFor(JsStatement stat) {
    return methodFor(stat, map);
  }

  /**
   * If stat is a {@link JsVars} that initializes a bunch of intern vars, return
   * a modified statement that skips any vars are needed by
   * <code>currentLivenessPredicate</code> but not by
   * <code>alreadyLoadedPredicate</code>.
   */
  private JsStatement removeSomeVars(JsVars stat, LivenessPredicate currentLivenessPredicate,
      LivenessPredicate alreadyLoadedPredicate) {
    JsVars newVars = new JsVars(stat.getSourceInfo());

    for (JsVar var : stat) {
      if (isLive(var, currentLivenessPredicate) && !isLive(var, alreadyLoadedPredicate)) {
        newVars.add(var);
      }
    }

    if (newVars.getNumVars() == stat.getNumVars()) {
      // no change
      return stat;
    }

    if (newVars.iterator().hasNext()) {
      /*
       * The new variables are non-empty; return them.
       */
      return newVars;
    } else {
      /*
       * An empty JsVars seems possibly surprising; return a true empty
       * statement instead.
       */
      return new JsEmpty(stat.getSourceInfo());
    }
  }

  /**
   * If <code>state</code> is of the form <code>_ = String.prototype</code>,
   * then return <code>String</code>. If the form is
   * <code>defineClass(id, superId, cTM, ctor1, ctor2, ...)</code> return the type
   * corresponding to that id. Otherwise return <code>null</code>.
   */
  private JClassType vtableTypeAssigned(JsStatement stat) {
    if (!(stat instanceof JsExprStmt)) {
      return null;
    }
    JsExprStmt expr = (JsExprStmt) stat;
    if (expr.getExpression() instanceof JsInvocation) {
      // Handle a defineClass call.
      JsInvocation call = (JsInvocation) expr.getExpression();
      if (!(call.getQualifier() instanceof JsNameRef)) {
        return null;
      }
      JsNameRef func = (JsNameRef) call.getQualifier();
      JsFunction defineClassJsFunc =
          jsprogram.getIndexedFunction("JavaClassHierarchySetupUtil.defineClass");
      JsFunction defineClassJsProtoFunc =
          jsprogram.getIndexedFunction(
              "JavaClassHierarchySetupUtil.defineClassWithPrototype");
      if (func.getName() != defineClassJsFunc.getName() && func.getName() !=
          defineClassJsProtoFunc.getName()) {
        return null;
      }
      return map.typeForStatement(stat);
    }

    // Handle String.
    if (!(expr.getExpression() instanceof JsBinaryOperation)) {
      return null;
    }
    JsBinaryOperation binExpr = (JsBinaryOperation) expr.getExpression();
    if (binExpr.getOperator() != JsBinaryOperator.ASG) {
      return null;
    }
    if (!(binExpr.getArg1() instanceof JsNameRef)) {
      return null;
    }
    JsNameRef lhs = (JsNameRef) binExpr.getArg1();
    JsName underBar = jsprogram.getScope().findExistingName("_");
    assert underBar != null;
    if (lhs.getName() != underBar) {
      return null;
    }
    if (!(binExpr.getArg2() instanceof JsNameRef)) {
      return null;
    }

    JsNameRef rhsRef = (JsNameRef) binExpr.getArg2();
    if (!(rhsRef.getQualifier() instanceof JsNameRef)) {
      return null;
    }
    if (!((JsNameRef) rhsRef.getQualifier()).getShortIdent().equals("String")) {
      return null;
    }

    if (!rhsRef.getName().getShortIdent().equals("prototype")) {
      return null;
    }
    return map.typeForStatement(stat);
  }

  private JClassType vtableTypeNeeded(JsStatement stat) {
    JMethod meth = map.vtableInitToMethod(stat);
    if (meth != null) {
      if (meth.needsVtable()) {
        return (JClassType) meth.getEnclosingType();
      }
    }
    return null;
  }

  /**
   * Wrap an expression with a call to $entry.
   */
  private JsInvocation wrapWithEntry(JsExpression exp) {
    SourceInfo sourceInfo = exp.getSourceInfo();
    JsInvocation call = new JsInvocation(sourceInfo,
        jsprogram.getScope().findExistingName("$entry").makeRef(sourceInfo), exp);
    return call;
  }
}
