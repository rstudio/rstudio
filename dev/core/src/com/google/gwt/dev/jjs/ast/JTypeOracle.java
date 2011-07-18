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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.impl.HasNameSort;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.IdentityHashSet;
import com.google.gwt.dev.util.collect.IdentitySets;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Oracle that can answer questions regarding the types in a program.
 */
public class JTypeOracle implements Serializable {

  /**
   * Checks a clinit method to find out a few things.
   * 
   * <ol>
   * <li>What other clinits it calls.</li>
   * <li>If it runs any code other than clinit calls.</li>
   * </ol>
   * 
   * This is used to remove "dead clinit cycles" where self-referential cycles
   * of empty clinits can keep each other alive.
   */
  private static final class CheckClinitVisitor extends JVisitor {

    private final Set<JDeclaredType> clinitTargets = new IdentityHashSet<JDeclaredType>();

    /**
     * Tracks whether any live code is run in this clinit. This is only reliable
     * because we explicitly visit all AST structures that might contain
     * non-clinit-calling code.
     * 
     * @see #mightBeDeadCode(JExpression)
     * @see #mightBeDeadCode(JStatement)
     */
    private boolean hasLiveCode = false;

    public JDeclaredType[] getClinitTargets() {
      return clinitTargets.toArray(new JDeclaredType[clinitTargets.size()]);
    }

    public boolean hasLiveCode() {
      return hasLiveCode;
    }

    @Override
    public boolean visit(JBlock x, Context ctx) {
      for (JStatement stmt : x.getStatements()) {
        if (mightBeDeadCode(stmt)) {
          accept(stmt);
        } else {
          hasLiveCode = true;
        }
      }
      return false;
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
      JVariable target = x.getVariableRef().getTarget();
      if (target instanceof JField) {
        JField field = (JField) target;
        if (field.getLiteralInitializer() != null) {
          // Top level initializations generate no code.
          return false;
        }
      }
      hasLiveCode = true;
      return false;
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      JExpression expr = x.getExpr();
      if (mightBeDeadCode(expr)) {
        accept(expr);
      } else {
        hasLiveCode = true;
      }
      return false;
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      JMethod target = x.getTarget();
      if (JProgram.isClinit(target)) {
        clinitTargets.add(target.getEnclosingType());
      } else {
        hasLiveCode = true;
      }
      return false;
    }

    @Override
    public boolean visit(JMultiExpression x, Context ctx) {
      for (JExpression expr : x.exprs) {
        // Only a JMultiExpression or JMethodCall can contain clinit calls.
        if (mightBeDeadCode(expr)) {
          accept(expr);
        } else {
          hasLiveCode = true;
        }
      }
      return false;
    }

    @Override
    public boolean visit(JNewInstance x, Context ctx) {
      if (x.hasSideEffects()) {
        hasLiveCode = true;
      }
      return false;
    }

    private boolean mightBeDeadCode(JExpression expr) {
      // Must have a visit method for every subtype that answers yes!
      return expr instanceof JMultiExpression || expr instanceof JMethodCall
          || expr instanceof JNewInstance;
    }

    private boolean mightBeDeadCode(JStatement stmt) {
      // Must have a visit method for every subtype that answers yes!
      return stmt instanceof JBlock || stmt instanceof JExpressionStatement
          || stmt instanceof JDeclarationStatement;
    }
  }

  /**
   * Compare two methods based on name and original argument types
   * {@link JMethod#getOriginalParamTypes()}. Note that nothing special is done
   * here regarding methods with type parameters in their argument lists. The
   * caller must be careful that this level of matching is sufficient.
   */
  public static boolean methodsDoMatch(JMethod method1, JMethod method2) {
    // static methods cannot match each other
    if (method1.isStatic() || method2.isStatic()) {
      return false;
    }

    // names must be identical
    if (!method1.getName().equals(method2.getName())) {
      return false;
    }

    // original return type must be identical
    if (method1.getOriginalReturnType() != method2.getOriginalReturnType()) {
      return false;
    }

    // original parameter types must be identical
    List<JType> params1 = method1.getOriginalParamTypes();
    List<JType> params2 = method2.getOriginalParamTypes();
    int params1size = params1.size();
    if (params1size != params2.size()) {
      return false;
    }

    for (int i = 0; i < params1size; ++i) {
      if (params1.get(i) != params2.get(i)) {
        return false;
      }
    }
    return true;
  }

  private JDeclaredType baseArrayType;

  /**
   * A map of all interfaces to the set of classes that could theoretically
   * implement them.
   */
  private final Map<JInterfaceType, Set<JClassType>> couldBeImplementedMap =
      new IdentityHashMap<JInterfaceType, Set<JClassType>>();

  /**
   * A map of all classes to the set of interfaces that they could theoretically
   * implement.
   */
  private final Map<JClassType, Set<JInterfaceType>> couldImplementMap =
      new IdentityHashMap<JClassType, Set<JInterfaceType>>();

  /**
   * The set of all interfaces that are initially implemented by both a Java and
   * Overlay type.
   */
  private final Set<JInterfaceType> dualImpls = new IdentityHashSet<JInterfaceType>();

  /**
   * A map of all classes to the set of interfaces they directly implement,
   * possibly through inheritance.
   */
  private final Map<JClassType, Set<JInterfaceType>> implementsMap =
      new IdentityHashMap<JClassType, Set<JInterfaceType>>();

  /**
   * The types in the program that are instantiable. All types in this set
   * should be run-time types as defined at
   * {@link JProgram#getRunTimeType(JReferenceType)}.
   */
  private Set<JReferenceType> instantiatedTypes = null;

  /**
   * A map of all interfaces to the set of classes that directly implement them,
   * possibly through inheritance.
   */
  private final Map<JInterfaceType, Set<JClassType>> isImplementedMap =
      new IdentityHashMap<JInterfaceType, Set<JClassType>>();

  private JDeclaredType javaIoSerializable;

  private JDeclaredType javaLangCloneable;

  /**
   * Caches the {@link Object} class.
   */
  private JClassType javaLangObject = null;

  /**
   * A map of all interfaces that are implemented by overlay types to the
   * overlay type that initially implements it.
   */
  private final Map<JInterfaceType, JClassType> jsoSingleImpls =
      new IdentityHashMap<JInterfaceType, JClassType>();

  /**
   * The associated {@link JProgram}.
   */
  private final JProgram program;

  /**
   * A map of all classes to the set of classes that extend them, directly or
   * indirectly.
   */
  private final Map<JClassType, Set<JClassType>> subClassMap =
      new IdentityHashMap<JClassType, Set<JClassType>>();

  /**
   * A map of all interfaces to the set of interfaces that extend them, directly
   * or indirectly.
   */
  private final Map<JInterfaceType, Set<JInterfaceType>> subInterfaceMap =
      new IdentityHashMap<JInterfaceType, Set<JInterfaceType>>();

  /**
   * A map of all classes to the set of classes they extend, directly or
   * indirectly.
   */
  private final Map<JClassType, Set<JClassType>> superClassMap =
      new IdentityHashMap<JClassType, Set<JClassType>>();

  /**
   * A map of all interfaces to the set of interfaces they extend, directly or
   * indirectly.
   */
  private final Map<JInterfaceType, Set<JInterfaceType>> superInterfaceMap =
      new IdentityHashMap<JInterfaceType, Set<JInterfaceType>>();
  /**
   * A map of all methods with virtual overrides, onto the collection of
   * overridden methods. Each key method's collections is a map of the set of
   * subclasses who inherit the key method mapped onto the set of interface
   * methods the key method virtually implements. For a definition of a virtual
   * override, see {@link #getAllVirtualOverrides(JMethod)}.
   */
  private final Map<JMethod, Map<JClassType, Set<JMethod>>> virtualUpRefMap =
      new IdentityHashMap<JMethod, Map<JClassType, Set<JMethod>>>();

  /**
   * An index of all polymorphic methods for each class.
   */
  private final Map<JClassType, Map<String, JMethod>> polyClassMethodMap =
      new IdentityHashMap<JClassType, Map<String, JMethod>>();

  public JTypeOracle(JProgram program) {
    this.program = program;
  }

  /**
   * True if the type is a JSO or interface implemented by JSO..
   * 
   * @param type
   * @return
   */
  public boolean canBeJavaScriptObject(JType type) {
    if (type instanceof JNonNullType) {
      type = ((JNonNullType) type).getUnderlyingType();
    }
    return program.isJavaScriptObject(type) || program.typeOracle.isSingleJsoImpl(type);
  }

  public boolean canTheoreticallyCast(JReferenceType type, JReferenceType qType) {
    if (!type.canBeNull() && qType == program.getTypeNull()) {
      // Cannot cast non-nullable to null
      return false;
    }

    // Compare the underlying types.
    type = type.getUnderlyingType();
    qType = qType.getUnderlyingType();

    JClassType jlo = program.getTypeJavaLangObject();
    if (type == qType || type == jlo) {
      return true;
    }

    /**
     * Cross-cast allowed in theory, prevents TypeTightener from turning
     * cross-casts into null-casts.
     */
    if (canBeJavaScriptObject(type) && canBeJavaScriptObject(qType)) {
      return true;
    }

    if (canTriviallyCast(type, qType)) {
      return true;
    }

    if (type instanceof JArrayType) {

      JArrayType aType = (JArrayType) type;
      if (qType instanceof JArrayType) {
        JArrayType qaType = (JArrayType) qType;
        JType leafType = aType.getLeafType();
        JType qLeafType = qaType.getLeafType();
        int dims = aType.getDims();
        int qDims = qaType.getDims();

        // null[] or Object[] -> int[][] might work, other combinations won't
        if (dims < qDims && leafType != program.getTypeJavaLangObject()
            && !(leafType instanceof JNullType)) {
          return false;
        }

        if (dims == qDims) {
          if (leafType instanceof JReferenceType && qLeafType instanceof JReferenceType) {
            return canTheoreticallyCast((JReferenceType) leafType, (JReferenceType) qLeafType);
          }
        }
      }

      /*
       * Warning: If this code is ever updated to consider casts of array types
       * to interface types, then be sure to consider that casting an array to
       * Serializable and Cloneable succeeds. Currently all casts of an array to
       * an interface return true, which is overly conservative but is safe.
       */
    } else if (type instanceof JClassType) {

      JClassType cType = (JClassType) type;
      if (qType instanceof JClassType) {
        return isSubClass(cType, (JClassType) qType);
      } else if (qType instanceof JInterfaceType) {
        return get(couldImplementMap, cType).contains(qType);
      }
    } else if (type instanceof JInterfaceType) {

      JInterfaceType iType = (JInterfaceType) type;
      if (qType instanceof JClassType) {
        return get(couldBeImplementedMap, iType).contains(qType);
      }
    } else if (type instanceof JNullType) {
    }

    return true;
  }

  public boolean canTriviallyCast(JReferenceType type, JReferenceType qType) {
    if (type.canBeNull() && !qType.canBeNull()) {
      // Cannot reliably cast nullable to non-nullable
      return false;
    }

    // Compare the underlying types.
    type = type.getUnderlyingType();
    qType = qType.getUnderlyingType();

    JClassType jlo = program.getTypeJavaLangObject();
    if (type == qType || qType == jlo) {
      return true;
    }

    if (type instanceof JArrayType) {

      JArrayType aType = (JArrayType) type;
      if (qType instanceof JArrayType) {
        JArrayType qaType = (JArrayType) qType;
        JType leafType = aType.getLeafType();
        JType qLeafType = qaType.getLeafType();
        int dims = aType.getDims();
        int qDims = qaType.getDims();

        // int[][] -> Object[] or null[] trivially true
        if (dims > qDims && (qLeafType == jlo || qLeafType instanceof JNullType)) {
          return true;
        }

        if (dims == qDims) {
          if (leafType instanceof JReferenceType && qLeafType instanceof JReferenceType) {
            return canTriviallyCast((JReferenceType) leafType, (JReferenceType) qLeafType);
          }
        }
      }

      if (qType == javaIoSerializable || qType == javaLangCloneable || qType == baseArrayType) {
        return true;
      }
    } else if (type instanceof JClassType) {

      JClassType cType = (JClassType) type;
      if (qType instanceof JClassType) {
        JClassType qcType = (JClassType) qType;
        if (isSuperClass(cType, qcType)) {
          return true;
        }
      } else if (qType instanceof JInterfaceType) {
        return implementsInterface(cType, (JInterfaceType) qType);
      }
    } else if (type instanceof JInterfaceType) {

      JInterfaceType iType = (JInterfaceType) type;
      if (qType instanceof JInterfaceType) {
        return extendsInterface(iType, (JInterfaceType) qType);
      }
    } else if (type instanceof JNullType) {

      return true;
    }

    return false;
  }

  public boolean canTriviallyCast(JType type, JType qType) {
    if (type instanceof JPrimitiveType && qType instanceof JPrimitiveType) {
      return type == qType;
    } else if (type instanceof JReferenceType && qType instanceof JReferenceType) {
      return canTriviallyCast((JReferenceType) type, (JReferenceType) qType);
    }
    return false;
  }

  public void computeBeforeAST() {
    baseArrayType = program.getIndexedType("Array");
    javaLangObject = program.getTypeJavaLangObject();
    javaIoSerializable = program.getFromTypeMap(Serializable.class.getName());
    javaLangCloneable = program.getFromTypeMap(Cloneable.class.getName());

    superClassMap.clear();
    subClassMap.clear();
    superInterfaceMap.clear();
    subInterfaceMap.clear();
    implementsMap.clear();
    couldImplementMap.clear();
    isImplementedMap.clear();
    couldBeImplementedMap.clear();
    jsoSingleImpls.clear();
    dualImpls.clear();

    for (JDeclaredType type : program.getDeclaredTypes()) {
      if (type instanceof JClassType) {
        recordSuperSubInfo((JClassType) type);
      } else {
        recordSuperSubInfo((JInterfaceType) type);
      }
    }

    /*
     * Now that the basic type hierarchy is computed, compute which JSOs
     * implement interfaces singlely or dually.
     */
    JClassType jsoType = program.getJavaScriptObject();
    List<JClassType> jsoSubTypes = Lists.create();
    if (jsoType != null) {
      jsoSubTypes = new ArrayList<JClassType>(get(subClassMap, jsoType));
      Collections.sort(jsoSubTypes, new HasNameSort());
      for (JClassType jsoSubType : jsoSubTypes) {
        for (JInterfaceType intf : jsoSubType.getImplements()) {
          jsoSingleImpls.put(intf, jsoSubType);
          for (JInterfaceType superIntf : get(superInterfaceMap, intf)) {
            if (!jsoSingleImpls.containsKey(superIntf)) {
              jsoSingleImpls.put(superIntf, jsoSubType);
            }
          }
        }
      }
    }

    for (JDeclaredType type : program.getDeclaredTypes()) {
      if (type instanceof JClassType) {
        computeImplements((JClassType) type);
      }
    }
    for (JDeclaredType type : program.getDeclaredTypes()) {
      if (type instanceof JClassType) {
        computeCouldImplement((JClassType) type);
      }
    }
    for (JDeclaredType type : program.getDeclaredTypes()) {
      if (type instanceof JClassType) {
        computeVirtualUpRefs((JClassType) type);
      }
    }

    // Create dual mappings for any jso interface with a Java implementor.
    for (JInterfaceType jsoIntf : jsoSingleImpls.keySet()) {
      Set<JClassType> implementors = get(isImplementedMap, jsoIntf);
      for (JClassType implementor : implementors) {
        if (!program.isJavaScriptObject(implementor)) {
          dualImpls.add(jsoIntf);
          break;
        }
      }
    }
  }

  /**
   * References to any methods which this method implementation might override
   * or implement in any instantiable class, including strange cases where there
   * is no direct relationship between the methods except in a subclass that
   * inherits one and implements the other. Example:
   * 
   * <pre>
   * interface IFoo {
   *   foo();
   * }
   * class Unrelated {
   *   foo() { ... }
   * }
   * class Foo extends Unrelated implements IFoo {
   * }
   * </pre>
   * 
   * In this case, <code>Unrelated.foo()</code> virtually implements
   * <code>IFoo.foo()</code> in subclass <code>Foo</code>.
   */
  public Set<JMethod> getAllOverrides(JMethod method) {
    Set<JMethod> results = new IdentityHashSet<JMethod>();
    getAllRealOverrides(method, results);
    getAllVirtualOverrides(method, results);
    return results;
  }

  public Set<JReferenceType> getInstantiatedTypes() {
    return instantiatedTypes;
  }

  public JMethod getPolyMethod(JClassType type, String signature) {
    return getOrCreatePolyMap(type).get(signature);
  }

  public JClassType getSingleJsoImpl(JReferenceType maybeSingleJsoIntf) {
    return jsoSingleImpls.get(maybeSingleJsoIntf.getUnderlyingType());
  }

  public boolean isDualJsoInterface(JReferenceType maybeDualImpl) {
    return dualImpls.contains(maybeDualImpl.getUnderlyingType());
  }

  /**
   * True if either a JSO, or is an interface that is ONLY implemented by a JSO.
   */
  public boolean isEffectivelyJavaScriptObject(JType type) {
    if (type instanceof JReferenceType) {
      JReferenceType refType = (JReferenceType) type;
      return program.isJavaScriptObject(refType)
          || (isSingleJsoImpl(refType) && !isDualJsoInterface(refType));
    } else {
      return false;
    }
  }

  /**
   * Determine whether a type is instantiated.
   */
  public boolean isInstantiatedType(JDeclaredType type) {
    return instantiatedTypes == null || instantiatedTypes.contains(type);
  }

  /**
   * Determine whether a type is instantiated.
   */
  public boolean isInstantiatedType(JReferenceType type) {
    type = type.getUnderlyingType();
    if (instantiatedTypes == null || instantiatedTypes.contains(type)) {
      return true;
    }

    if (type.isExternal()) {
      // TODO(tobyr) I don't know under what situations it is safe to assume
      // that an external type won't be instantiated. For example, if we
      // assumed that an external exception weren't instantiated, because we
      // didn't see it constructed in our code, dead code elimination would
      // incorrectly elide any catch blocks for that exception.
      //
      // We should see how this effects optimization and if we can limit its
      // impact if necessary.
      return true;
    }

    if (type instanceof JNullType) {
      return true;
    } else if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      if (arrayType.getLeafType() instanceof JNullType) {
        return true;
      }
    }
    return false;
  }

  public boolean isSameOrSuper(JClassType type, JClassType qType) {
    return (type == qType || isSuperClass(type, qType));
  }

  public boolean isSingleJsoImpl(JType type) {
    return type instanceof JReferenceType && getSingleJsoImpl((JReferenceType) type) != null;
  }

  /**
   * Returns true if qType is a subclass of type, directly or indirectly.
   */
  public boolean isSubClass(JClassType type, JClassType qType) {
    return get(subClassMap, type).contains(qType);
  }

  /**
   * Returns true if qType is a superclass of type, directly or indirectly.
   */
  public boolean isSuperClass(JClassType type, JClassType qType) {
    return get(superClassMap, type).contains(qType);
  }

  /**
   * This method should be called after altering the types that are live in the
   * associated JProgram.
   */
  public void recomputeAfterOptimizations() {
    Set<JDeclaredType> computed = new IdentityHashSet<JDeclaredType>();

    for (JDeclaredType type : program.getDeclaredTypes()) {
      computeClinitTarget(type, computed);
    }
    nextDual : for (Iterator<JInterfaceType> it = dualImpls.iterator(); it.hasNext();) {
      JInterfaceType dualIntf = it.next();
      Set<JClassType> implementors = get(isImplementedMap, dualIntf);
      for (JClassType implementor : implementors) {
        if (isInstantiatedType(implementor) && !program.isJavaScriptObject(implementor)) {
          // This dual is still implemented by a Java class.
          continue nextDual;
        }
      }
      // No Java implementors.
      it.remove();
    }

    // Prune jsoSingleImpls when implementor isn't live
    Iterator<JClassType> jit = jsoSingleImpls.values().iterator();
    while (jit.hasNext()) {
      if (!isInstantiatedType(jit.next())) {
        jit.remove();
      }
    }
  }

  public void setInstantiatedTypes(Set<JReferenceType> instantiatedTypes) {
    this.instantiatedTypes = instantiatedTypes;
    polyClassMethodMap.keySet().retainAll(instantiatedTypes);
  }

  private <K, V> void add(Map<K, Set<V>> map, K key, V value) {
    getOrCreate(map, key).add(value);
  }

  private void computeClinitTarget(JDeclaredType type, Set<JDeclaredType> computed) {
    if (type.isExternal() || !type.hasClinit() || computed.contains(type)) {
      return;
    }
    JClassType superClass = null;
    if (type instanceof JClassType) {
      superClass = ((JClassType) type).getSuperClass();
    }
    if (superClass != null) {
      /*
       * Compute super first so that it's already been tightened to the tightest
       * possible target; this ensures if we're tightened as well it's to the
       * transitively tightest target.
       */
      computeClinitTarget(superClass, computed);
    }
    if (type.getClinitTarget() != type) {
      // I already have a trivial clinit, just follow my super chain.
      type.setClinitTarget(superClass.getClinitTarget());
    } else {
      // I still have a real clinit, actually compute.
      JDeclaredType target =
          computeClinitTargetRecursive(type, computed, new IdentityHashSet<JDeclaredType>());
      type.setClinitTarget(target);
    }
    computed.add(type);
  }

  private JDeclaredType computeClinitTargetRecursive(JDeclaredType type,
      Set<JDeclaredType> computed, Set<JDeclaredType> alreadySeen) {
    // Track that we've been seen.
    alreadySeen.add(type);

    JMethod method = type.getMethods().get(0);
    assert (JProgram.isClinit(method));
    CheckClinitVisitor v = new CheckClinitVisitor();
    v.accept(method);
    if (v.hasLiveCode()) {
      return type;
    }
    // Check for trivial super clinit.
    JDeclaredType[] clinitTargets = v.getClinitTargets();
    if (clinitTargets.length == 1) {
      JDeclaredType singleTarget = clinitTargets[0];
      if (type instanceof JClassType && singleTarget instanceof JClassType
          && isSuperClass((JClassType) type, (JClassType) singleTarget)) {
        return singleTarget.getClinitTarget();
      }
    }
    for (JDeclaredType target : clinitTargets) {
      if (!target.hasClinit()) {
        // A false result is always accurate.
        continue;
      }

      /*
       * If target has a clinit, so do I; but only if target has already been
       * recomputed this run.
       */
      if (target.hasClinit() && computed.contains(target)) {
        return type;
      }

      /*
       * Prevent recursion sickness: ignore this call for now since this call is
       * being accounted for higher on the stack.
       */
      if (alreadySeen.contains(target)) {
        continue;
      }

      if (computeClinitTargetRecursive(target, computed, alreadySeen) != null) {
        // Calling a non-empty clinit means I am a real clinit.
        return type;
      } else {
        // This clinit is okay, keep going.
        continue;
      }
    }
    return null;
  }

  /**
   * Compute all of the things I might conceivably implement, either through
   * super types or sub types.
   */
  private void computeCouldImplement(JClassType type) {
    Set<JInterfaceType> couldImplementSet = new IdentityHashSet<JInterfaceType>();
    // all of my direct implements are trivially true
    couldImplementSet.addAll(get(implementsMap, type));
    List<JClassType> subclasses = new ArrayList<JClassType>();
    subclasses.addAll(get(subClassMap, type));
    for (JClassType subclass : subclasses) {
      for (JInterfaceType intf : subclass.getImplements()) {
        couldImplementSet.add(intf);
        for (JInterfaceType isup : get(superInterfaceMap, intf)) {
          couldImplementSet.add(isup);
        }
      }
    }
    if (!couldImplementSet.isEmpty()) {
      couldImplementMap.put(type, IdentitySets.normalize(couldImplementSet));
      for (JInterfaceType couldImpl : couldImplementSet) {
        add(couldBeImplementedMap, couldImpl, type);
      }
    }
  }

  /**
   * Compute all of the things I implement directly, through super types.
   */
  private void computeImplements(JClassType type) {
    Set<JInterfaceType> implementsSet = new IdentityHashSet<JInterfaceType>();
    List<JClassType> list = new ArrayList<JClassType>();
    list.add(type);
    list.addAll(get(superClassMap, type));
    for (JClassType superclass : list) {
      for (JInterfaceType intf : superclass.getImplements()) {
        implementsSet.add(intf);
        for (JInterfaceType isup : get(superInterfaceMap, intf)) {
          implementsSet.add(isup);
        }
      }
    }
    if (!implementsSet.isEmpty()) {
      implementsMap.put(type, IdentitySets.normalize(implementsSet));
      for (JInterfaceType impl : implementsSet) {
        add(isImplementedMap, impl, type);
      }
    }
  }

  /**
   * WEIRD: Suppose class Foo declares void f(){} and unrelated interface I also
   * declares void f(). Then suppose Bar extends Foo implements I and doesn't
   * override f(). We need to record a "virtual" upref from Foo.f() to I.f() so
   * that if I.f() is rescued AND Bar is instantiable, Foo.f() does not get
   * pruned.
   */
  private void computeVirtualUpRefs(JClassType type) {
    if (type.getSuperClass() == null || type.getSuperClass() == javaLangObject) {
      return;
    }

    /*
     * For each interface I directly implement, check all methods and make sure
     * I define implementations for them. If I don't, then check all my super
     * classes to find virtual overrides.
     */
    for (JInterfaceType intf : type.getImplements()) {
      computeVirtualUpRefs(type, intf);
      for (JInterfaceType superIntf : get(superInterfaceMap, intf)) {
        computeVirtualUpRefs(type, superIntf);
      }
    }
  }

  /**
   * For each interface I directly implement, check all methods and make sure I
   * define implementations for them. If I don't, then check all my super
   * classes to find virtual overrides.
   */
  private void computeVirtualUpRefs(JClassType type, JInterfaceType intf) {
    outer : for (JMethod intfMethod : intf.getMethods()) {
      for (JMethod classMethod : type.getMethods()) {
        if (methodsDoMatch(intfMethod, classMethod)) {
          // this class directly implements the interface method
          continue outer;
        }
      }

      // this class does not directly implement the interface method
      // if any super classes do, create a virtual up ref
      for (JClassType superType = type.getSuperClass(); superType != javaLangObject; superType =
          superType.getSuperClass()) {
        for (JMethod superMethod : superType.getMethods()) {
          if (methodsDoMatch(intfMethod, superMethod)) {
            // this super class directly implements the interface method
            // create a virtual up ref

            // System.out.println("Virtual upref from " + superType.getName()
            // + "." + superMethod.getName() + " to " + intf.getName() + "."
            // + intfMethod.getName() + " via " + type.getName());

            Map<JClassType, Set<JMethod>> classToMethodMap =
                getOrCreateMap(virtualUpRefMap, superMethod);
            add(classToMethodMap, type, intfMethod);

            // do not search additional super types
            continue outer;
          }
        }
      }
    }
  }

  /**
   * Returns true if type extends the interface represented by qType, either
   * directly or indirectly.
   */
  private boolean extendsInterface(JInterfaceType type, JInterfaceType qType) {
    return get(superInterfaceMap, type).contains(qType);
  }

  private <K, V> Set<V> get(Map<K, Set<V>> map, K key) {
    Set<V> set = map.get(key);
    if (set == null) {
      return Collections.emptySet();
    }
    return set;
  }

  private void getAllRealOverrides(JMethod method, Set<JMethod> results) {
    for (JMethod possibleOverride : method.getOverrides()) {
      results.add(possibleOverride);
    }
  }

  private void getAllVirtualOverrides(JMethod method, Set<JMethod> results) {
    Map<JClassType, Set<JMethod>> overrideMap = virtualUpRefMap.get(method);
    if (overrideMap != null) {
      for (Map.Entry<JClassType, Set<JMethod>> entry : overrideMap.entrySet()) {
        JClassType classType = entry.getKey();
        if (isInstantiatedType(classType)) {
          results.addAll(entry.getValue());
        }
      }
    }
  }

  private <K, V> Set<V> getOrCreate(Map<K, Set<V>> map, K key) {
    Set<V> set = map.get(key);
    if (set == null) {
      set = new IdentityHashSet<V>();
      map.put(key, set);
    }
    return set;
  }

  private <K, K2, V> Map<K2, V> getOrCreateMap(Map<K, Map<K2, V>> map, K key) {
    Map<K2, V> map2 = map.get(key);
    if (map2 == null) {
      map2 = new IdentityHashMap<K2, V>();
      map.put(key, map2);
    }
    return map2;
  }

  private Map<String, JMethod> getOrCreatePolyMap(JClassType type) {
    Map<String, JMethod> polyMap = polyClassMethodMap.get(type);
    if (polyMap == null) {
      JClassType superClass = type.getSuperClass();
      if (superClass == null) {
        polyMap = new HashMap<String, JMethod>();
      } else {
        Map<String, JMethod> superPolyMap = getOrCreatePolyMap(type.getSuperClass());
        polyMap = new HashMap<String, JMethod>(superPolyMap);
      }
      for (JMethod method : type.getMethods()) {
        if (method.canBePolymorphic()) {
          polyMap.put(method.getSignature(), method);
        }
      }
      polyMap = Maps.normalize(polyMap);
      polyClassMethodMap.put(type, polyMap);
    }
    return polyMap;
  }

  /**
   * Returns true if type implements the interface represented by qType, either
   * directly or indirectly.
   */
  private boolean implementsInterface(JClassType type, JInterfaceType qType) {
    return get(implementsMap, type).contains(qType);
  }

  /**
   * Record the all of my super classes (and myself as a subclass of them).
   */
  private void recordSuperSubInfo(JClassType type) {
    Set<JClassType> superSet = new IdentityHashSet<JClassType>();
    for (JClassType t = type.getSuperClass(); t != null; t = t.getSuperClass()) {
      superSet.add(t);
      add(subClassMap, t, type);
    }
    if (!superSet.isEmpty()) {
      superClassMap.put(type, IdentitySets.normalize(superSet));
    }
  }

  /**
   * Record the all of my super interfaces (and myself as a sub interface of
   * them).
   */
  private void recordSuperSubInfo(JInterfaceType type) {
    if (!type.getImplements().isEmpty()) {
      Set<JInterfaceType> superSet = new IdentityHashSet<JInterfaceType>();
      recordSuperSubInfo(type, superSet, type);
      superInterfaceMap.put(type, IdentitySets.normalize(superSet));
    }
  }

  /**
   * Recursively record all of my super interfaces.
   */
  private void recordSuperSubInfo(JInterfaceType base, Set<JInterfaceType> superSet,
      JInterfaceType cur) {
    for (JInterfaceType intf : cur.getImplements()) {
      superSet.add(intf);
      add(subInterfaceMap, intf, base);
      recordSuperSubInfo(base, superSet, intf);
    }
  }

}
