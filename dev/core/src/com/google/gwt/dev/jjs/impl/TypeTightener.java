/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The purpose of this pass is to record "type flow" information and then use
 * the information to infer places where "tighter" (that is, more specific)
 * types can be inferred for locals, fields, parameters, and method return
 * types.
 * 
 * Type flow occurs automatically in most JExpressions. But locals, fields,
 * parameters, and method return types serve as "way points" where type
 * information is fixed based on the declared type. Type tightening can be done
 * by analyzing the types "flowing" into each way point, and then updating the
 * declared type of the way point to be a more specific type than it had before.
 * 
 * Oddly, it's quite possible to tighten a variable to the Null type, which
 * means either the variable was never assigned, or it was only ever assigned
 * null. This is great for two reasons:
 * 
 * 1) Once a variable has been tightened to null, it will no longer impact the
 * variables that depend on it.
 * 
 * 2) It creates some very interesting opportunities to optimize later, since we
 * know statically that the value of the variable is always null.
 * 
 * Open issue: we don't handle recursion where a method passes (some of) its own
 * args to itself or returns its own call result. With our naive analysis, we
 * can't figure out that tightening might occur.
 * 
 * Type flow is not supported for primitive types, only reference types.
 * 
 * TODO(later): handle recursion, self-assignment, arrays
 */
public class TypeTightener {

  /**
   * Replaces dangling null references with dummy calls.
   */
  public class FixDanglingRefsVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
        "Replace dangling null references with dummy calls.");

    // @Override
    public void endVisit(JArrayRef x, Mutator m) {
      JExpression instance = x.getInstance();
      if (instance.getType() == typeNull) {
        if (!instance.hasSideEffects()) {
          instance = program.getLiteralNull();
        }
        JArrayRef arrayRef = new JArrayRef(program, instance,
            program.getLiteralInt(0));
        changeList.replaceExpression(m, arrayRef);
      }
    }

    // @Override
    public void endVisit(JFieldRef x, Mutator m) {
      JExpression instance = x.getInstance();
      boolean isStatic = x.getField().isStatic();
      if (isStatic && instance != null) {
        // this doesn't really belong here, but while we're here let's remove
        // non-side-effect qualifiers to statics
        if (!instance.hasSideEffects()) {
          changeList.replaceExpression(x.instance, (JExpression) null);
        }
      } else if (!isStatic && instance.getType() == typeNull) {
        if (!instance.hasSideEffects()) {
          instance = program.getLiteralNull();
        }
        JFieldRef fieldRef = new JFieldRef(program, instance,
            program.getNullField(), null);
        changeList.replaceExpression(m, fieldRef);
      }
    }

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      JExpression instance = x.getInstance();
      JMethod method = x.getTarget();
      boolean isStatic = method.isStatic();
      boolean isStaticImpl = program.isStaticImpl(method);
      if (isStatic && !isStaticImpl && instance != null) {
        // this doesn't really belong here, but while we're here let's remove
        // non-side-effect qualifiers to statics
        if (!instance.hasSideEffects()) {
          changeList.replaceExpression(x.instance, (JExpression) null);
        }
      } else if (!isStatic && instance.getType() == typeNull) {
        if (!instance.hasSideEffects()) {
          instance = program.getLiteralNull();
        }
        JMethodCall call = new JMethodCall(program, instance,
            program.getNullMethod());
        changeList.replaceExpression(m, call);
      } else if (isStaticImpl && x.args.size() > 0
          && x.args.getExpr(0).getType() == typeNull) {
        instance = x.args.getExpr(0);
        if (!instance.hasSideEffects()) {
          instance = program.getLiteralNull();
        }
        JMethodCall call = new JMethodCall(program, instance,
            program.getNullMethod());
        changeList.replaceExpression(m, call);
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }
  }
  /**
   * Record "type flow" information. Variables receive type flow via assignment.
   * As a special case, Parameters also receive type flow based on the types of
   * arguments used when calling the containing method (think of this as a kind
   * of assignment). Method return types receive type flow from their contained
   * return statements, plus the return type of any methods that
   * override/implement them.
   * 
   * Note that we only have to run this pass ONCE to record the relationships,
   * because type tightening never changes any relationships, only the types of
   * the things related. In my original implementation, I had naively mapped
   * nodes onto sets of JReferenceType directly, which meant I had to rerun this
   * visitor each time.
   */
  public class RecordVisitor extends JVisitor {

    private JMethod currentMethod;

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      if (x.isAssignment() && (x.getType() instanceof JReferenceType)) {
        JExpression lhs = x.getLhs();
        if (lhs instanceof JVariableRef) {
          addAssignment(((JVariableRef) lhs).getTarget(), x.getRhs());
        }
      }
    }

    // @Override
    public void endVisit(JClassType x) {
      for (JClassType cur = x; cur != null; cur = cur.extnds) {
        addImplementor(cur, x);
        for (Iterator it = cur.implments.iterator(); it.hasNext();) {
          JInterfaceType implment = (JInterfaceType) it.next();
          addImplementor(implment, x);
        }
      }
    }

    // @Override
    public void endVisit(JField x) {
      if (x.constInitializer != null) {
        addAssignment(x, x.constInitializer);
      }
      currentMethod = null;
    }

    // @Override
    public void endVisit(JLocalDeclarationStatement x) {
      JExpression initializer = x.getInitializer();
      if (initializer != null) {
        addAssignment(x.getLocalRef().getTarget(), initializer);
      }
    }

    // @Override
    public void endVisit(JMethod x) {
      for (int i = 0; i < x.overrides.size(); ++i) {
        JMethod method = (JMethod) x.overrides.get(i);
        addOverrider(method, x);
      }
      JMethod[] allVirtualOverrides = program.typeOracle.getAllVirtualOverrides(x);
      for (int i = 0; i < allVirtualOverrides.length; ++i) {
        JMethod method = allVirtualOverrides[i];
        addOverrider(method, x);
      }
      currentMethod = null;
    }

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      // All of the params in the target method are considered to be assigned by
      // the arguments from the caller
      Iterator/* <JExpression> */argIt = x.args.iterator();
      ArrayList params = x.getTarget().params;
      for (int i = 0; i < params.size(); ++i) {
        JParameter param = (JParameter) params.get(i);
        JExpression arg = (JExpression) argIt.next();
        if (param.getType() instanceof JReferenceType) {
          addAssignment(param, arg);
        }
      }
    }

    // @Override
    public void endVisit(JReturnStatement x) {
      if (currentMethod.getType() instanceof JReferenceType) {
        addReturn(currentMethod, x.getExpression());
      }
    }

    // @Override
    public void endVisit(JsniFieldRef x) {
      // If this happens in JSNI, we can't make any type-tightening assumptions
      // Fake an assignment-to-self to prevent tightening
      addAssignment(x.getTarget(), x);
    }

    // @Override
    public void endVisit(JsniMethod x) {
      endVisit((JMethod) x);
    }

    // @Override
    public void endVisit(JsniMethodRef x) {
      // If this happens in JSNI, we can't make any type-tightening assumptions
      // Fake an assignment-to-self on all args to prevent tightening

      JMethod method = x.getTarget();

      for (int i = 0; i < method.params.size(); ++i) {
        JParameter param = (JParameter) method.params.get(i);
        addAssignment(param, new JParameterRef(program, param));
      }
    }

    // @Override
    public void endVisit(JTryStatement x) {
      // Never tighten args to catch blocks
      // Fake an assignment-to-self to prevent tightening
      for (int i = 0; i < x.catchArgs.size(); ++i) {
        JLocalRef arg = (JLocalRef) x.catchArgs.get(i);
        addAssignment(arg.getTarget(), arg);
      }
    }

    /**
     * Merge param call args across overriders/implementors. We can't tighten a
     * param type in an overriding method if the declaring method is looser.
     */
    // @Override
    public boolean visit(JMethod x) {
      currentMethod = x;

      List/* <JMethod> */overrides = x.overrides;
      JMethod[] virtualOverrides = program.typeOracle.getAllVirtualOverrides(x);

      /*
       * Special case: also add upRefs from a staticImpl's params to the params
       * of the instance method it is implementing. Most of the time, this would
       * happen naturally since the instance method delegates to the static.
       * However, in cases where the static has been inlined into the instance
       * method, future optimization could tighten an instance call into a
       * static call at some other call site, and fail to inline. If we allowed
       * a staticImpl param to be tighter than its instance param, badness would
       * ensue.
       */
      JMethod staticImplFor = program.staticImplFor(x);
      // Unless the instance method has already been pruned, of course.
      if (staticImplFor != null
          && !staticImplFor.getEnclosingType().methods.contains(staticImplFor)) {
        staticImplFor = null;
      }

      if (overrides.isEmpty() && virtualOverrides.length == 0
          && staticImplFor == null) {
        return true;
      }

      for (int j = 0, c = x.params.size(); j < c; ++j) {
        JParameter param = (JParameter) x.params.get(j);
        Set/* <JParameter> */set = (Set) paramUpRefs.get(param);
        if (set == null) {
          set = new HashSet/* <JParameter> */();
          paramUpRefs.put(param, set);
        }
        for (int i = 0; i < overrides.size(); ++i) {
          JMethod baseMethod = (JMethod) overrides.get(i);
          JParameter baseParam = (JParameter) baseMethod.params.get(j);
          set.add(baseParam);
        }
        for (int i = 0; i < virtualOverrides.length; ++i) {
          JMethod baseMethod = virtualOverrides[i];
          JParameter baseParam = (JParameter) baseMethod.params.get(j);
          set.add(baseParam);
        }
        if (staticImplFor != null && j > 1) {
          // static impls have an extra first "this" arg
          JParameter baseParam = (JParameter) staticImplFor.params.get(j - 1);
          set.add(baseParam);
        }
      }

      return true;
    }

    private void addAssignment(JVariable target, JExpression rhs) {
      add(target, rhs, assignments);
    }

    private void addImplementor(JReferenceType target, JClassType implementor) {
      add(target, implementor, implementors);
    }

    private void addOverrider(JMethod target, JMethod overrider) {
      add(target, overrider, overriders);
    }

    private void addReturn(JMethod target, JExpression expr) {
      add(target, expr, returns);
    }
  }

  /**
   * Wherever possible, use the type flow information recorded by RecordVisitor
   * to change the declared type of a field, local, parameter, or method to a
   * more specific type.
   * 
   * We must iterate mutiple times because each way point we tighten creates
   * more opportunities to do additional tightening for the things that depend
   * on it.
   */
  public class TightenTypesVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
        "Tighten types on fields, locals, params, methods.");

    // @Override
    public void endVisit(JField x) {
      tighten(x);
    }

    // @Override
    public void endVisit(JLocal x) {
      tighten(x);
    }

    /**
     * Tighten based on return types and overrides.
     */
    // @Override
    public void endVisit(JMethod x) {

      if (!(x.getType() instanceof JReferenceType)) {
        return;
      }
      JReferenceType refType = (JReferenceType) x.getType();

      if (refType == typeNull) {
        return;
      }

      // tighten based on non-instantiability
      if (!program.typeOracle.isInstantiatedType(refType)) {
        changeList.changeType(x, typeNull);
        return;
      }

      // tighten based on both returned types and possible overrides
      List/* <JReferenceType> */typeList = new ArrayList/* <JReferenceType> */();

      /*
       * Always assume at least one null assignment; if there really aren't any
       * other assignments, then this variable will get the null type. If there
       * are, it won't hurt anything because null type will always lose.
       */
      typeList.add(typeNull);

      Set/* <JExpression> */myReturns = (Set) returns.get(x);
      if (myReturns != null) {
        for (Iterator iter = myReturns.iterator(); iter.hasNext();) {
          JExpression expr = (JExpression) iter.next();
          typeList.add(expr.getType());
        }
      }
      Set/* <JMethod> */myOverriders = (Set) overriders.get(x);
      if (myOverriders != null) {
        for (Iterator iter = myOverriders.iterator(); iter.hasNext();) {
          JMethod method = (JMethod) iter.next();
          typeList.add(method.getType());
        }
      }

      JReferenceType resultType = program.generalizeTypes(typeList);
      resultType = program.strongerType(refType, resultType);
      if (refType != resultType) {
        changeList.changeType(x, resultType);
      }
    }

    // @Override
    public void endVisit(JNewArray x, Mutator m) {
      // tighten leaf type based on non-instantiability
      JArrayType arrayType = x.getArrayType();
      JType leafType = arrayType.getLeafType();
      if (leafType instanceof JReferenceType) {
        if (!program.typeOracle.isInstantiatedType((JReferenceType) leafType)) {
          arrayType = program.getTypeArray(typeNull, arrayType.getDims());
          changeList.changeType(x, arrayType);
        }
      }
    }

    // @Override
    public void endVisit(JParameter x) {
      tighten(x);
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JClassType x) {
      // don't mess with classes used in code gen
      if (program.specialTypes.contains(x)) {
        return false;
      }
      return true;
    }

    public boolean visit(JsniMethod x) {
      /*
       * Explicitly NOT visiting native methods since we can't infer type
       * information.
       * 
       * TODO(later): can we figure out simple pass-through info?
       */
      return false;
    }

    /**
     * Tighten based on assignment, and for parameters, callArgs as well.
     */
    private void tighten(JVariable x) {
      if (!(x.getType() instanceof JReferenceType)) {
        return;
      }
      JReferenceType refType = (JReferenceType) x.getType();

      if (refType == typeNull) {
        return;
      }

      // tighten based on non-instantiability
      if (!program.typeOracle.isInstantiatedType(refType)) {
        changeList.changeType(x, typeNull);
        return;
      }

      // tighten based on assignment
      List/* <JReferenceType> */typeList = new ArrayList/* <JReferenceType> */();

      /*
       * For non-parameters, always assume at least one null assignment; if
       * there really aren't any other assignments, then this variable will get
       * the null type. If there are, it won't hurt anything because null type
       * will always lose.
       * 
       * For parameters, don't perform any tightening if we can't find any
       * actual assignments. The method should eventually get pruned.
       */
      if (!(x instanceof JParameter)) {
        typeList.add(typeNull);
      }

      Set/* <JExpression> */myAssignments = (Set) assignments.get(x);
      if (myAssignments != null) {
        for (Iterator iter = myAssignments.iterator(); iter.hasNext();) {
          JExpression expr = (JExpression) iter.next();
          JType type = expr.getType();
          if (!(type instanceof JReferenceType)) {
            return; // something fishy is going on, just abort
          }
          typeList.add(type);
        }
      }

      if (x instanceof JParameter) {
        Set/* <JParameter> */myParams = (Set) paramUpRefs.get(x);
        if (myParams != null) {
          for (Iterator iter = myParams.iterator(); iter.hasNext();) {
            JParameter param = (JParameter) iter.next();
            typeList.add(param.getType());
          }
        }
      }

      if (typeList.isEmpty()) {
        return;
      }

      JReferenceType resultType = program.generalizeTypes(typeList);
      resultType = program.strongerType(refType, resultType);
      if (refType != resultType) {
        changeList.changeType(x, resultType);
      }
    }
  }

  public static boolean exec(JProgram program) {
    return new TypeTightener(program).execImpl();
  }

  private static/* <T, V> */void add(Object target, Object value,
      Map/* <T, Set<V>> */map) {
    Set/* <V> */set = (Set) map.get(target);
    if (set == null) {
      set = new HashSet/* <V> */();
      map.put(target, set);
    }
    set.add(value);
  }

  private final Map/* <JVariable, Set<JExpression>> */assignments = new IdentityHashMap();
  private final Map/* <JReferenceType, Set<JClassType>> */implementors = new IdentityHashMap();
  private final Map/* <JMethod, Set<JMethod>> */overriders = new IdentityHashMap();
  private final Map/* <JParameter, Set<JParameter>> */paramUpRefs = new IdentityHashMap();
  private final JProgram program;
  private final Map/* <JMethod, Set<JExpression>> */returns = new IdentityHashMap();
  private final JNullType typeNull;

  private TypeTightener(JProgram program) {
    this.program = program;
    typeNull = program.getTypeNull();
  }

  private boolean execImpl() {
    boolean madeChanges = false;
    RecordVisitor recorder = new RecordVisitor();
    program.traverse(recorder);
    while (true) {
      TightenTypesVisitor tightener = new TightenTypesVisitor();
      program.traverse(tightener);
      ChangeList changes = tightener.getChangeList();
      if (changes.empty()) {
        return madeChanges;
      }
      changes.apply();
      madeChanges = true;

      FixDanglingRefsVisitor fixer = new FixDanglingRefsVisitor();
      program.traverse(fixer);
      changes = fixer.getChangeList();
      if (!changes.empty()) {
        changes.apply();
      }
    }
  }

}
