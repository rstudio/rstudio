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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Oracle that can answer questions regarding the types in a program.
 */
public class JTypeOracle {

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

    private final Set<JReferenceType> clinitTargets = new HashSet<JReferenceType>();

    /**
     * Tracks whether any live code is run in this clinit. This is only reliable
     * because we explicitly visit all AST structures that might contain
     * non-clinit-calling code.
     * 
     * @see #mightBeDeadCode(JExpression)
     * @see #mightBeDeadCode(JStatement)
     */
    private boolean hasLiveCode = false;

    public JReferenceType[] getClinitTargets() {
      return clinitTargets.toArray(new JReferenceType[clinitTargets.size()]);
    }

    public boolean hasLiveCode() {
      return hasLiveCode;
    }

    @Override
    public boolean visit(JBlock x, Context ctx) {
      for (JStatement stmt : x.statements) {
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

    private boolean mightBeDeadCode(JExpression expr) {
      // Must have a visit method for every subtype that answers yes!
      return expr instanceof JMultiExpression || expr instanceof JMethodCall;
    }

    private boolean mightBeDeadCode(JStatement stmt) {
      // Must have a visit method for every subtype that answers yes!
      return stmt instanceof JBlock || stmt instanceof JExpressionStatement
          || stmt instanceof JDeclarationStatement;
    }
  }

  private final Map<JInterfaceType, Set<JClassType>> couldBeImplementedMap = new IdentityHashMap<JInterfaceType, Set<JClassType>>();

  private final Map<JClassType, Set<JInterfaceType>> couldImplementMap = new IdentityHashMap<JClassType, Set<JInterfaceType>>();

  private final Set<JReferenceType> hasClinitSet = new HashSet<JReferenceType>();

  private final Map<JClassType, Set<JInterfaceType>> implementsMap = new IdentityHashMap<JClassType, Set<JInterfaceType>>();

  private Set<JReferenceType> instantiatedTypes = null;

  private final Map<JInterfaceType, Set<JClassType>> isImplementedMap = new IdentityHashMap<JInterfaceType, Set<JClassType>>();

  private JClassType javaLangObject = null;

  private final JProgram program;

  private final Map<JClassType, Set<JClassType>> subClassMap = new IdentityHashMap<JClassType, Set<JClassType>>();

  private final Map<JInterfaceType, Set<JInterfaceType>> subInterfaceMap = new IdentityHashMap<JInterfaceType, Set<JInterfaceType>>();

  private final Map<JClassType, Set<JClassType>> superClassMap = new IdentityHashMap<JClassType, Set<JClassType>>();

  private final Map<JInterfaceType, Set<JInterfaceType>> superInterfaceMap = new IdentityHashMap<JInterfaceType, Set<JInterfaceType>>();

  private final Map<JMethod, Map<JClassType, Set<JMethod>>> virtualUpRefMap = new IdentityHashMap<JMethod, Map<JClassType, Set<JMethod>>>();

  public JTypeOracle(JProgram program) {
    this.program = program;
  }

  public boolean canTheoreticallyCast(JReferenceType type, JReferenceType qType) {
    JClassType jlo = program.getTypeJavaLangObject();
    if (type == qType || type == jlo) {
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
          if (leafType instanceof JReferenceType
              && qLeafType instanceof JReferenceType) {
            return canTheoreticallyCast((JReferenceType) leafType,
                (JReferenceType) qLeafType);
          }
        }
      }

    } else if (type instanceof JClassType) {

      JClassType cType = (JClassType) type;
      if (qType instanceof JClassType) {
        return isSubClass(cType, (JClassType) qType);
      } else if (qType instanceof JInterfaceType) {
        return getOrCreate(couldImplementMap, cType).contains(qType);
      }
    } else if (type instanceof JInterfaceType) {

      JInterfaceType iType = (JInterfaceType) type;
      if (qType instanceof JClassType) {
        return getOrCreate(couldBeImplementedMap, iType).contains(qType);
      }
    } else if (type instanceof JNullType) {
    }

    return true;
  }

  public boolean canTriviallyCast(JReferenceType type, JReferenceType qType) {
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
        if (dims > qDims
            && (qLeafType == jlo || qLeafType instanceof JNullType)) {
          return true;
        }

        if (dims == qDims) {
          if (leafType instanceof JReferenceType
              && qLeafType instanceof JReferenceType) {
            return canTriviallyCast((JReferenceType) leafType,
                (JReferenceType) qLeafType);
          }
        }
      }
    } else if (type instanceof JClassType) {

      JClassType cType = (JClassType) type;
      if (qType instanceof JClassType) {
        JClassType qcType = (JClassType) qType;
        if (isSuperClass(cType, qcType)) {
          return true;
        }
        // All JavaScriptObject types can be freely cast to each other.
        JClassType jsoType = program.getJavaScriptObject();
        if (jsoType != null) {
          return isSameOrSuper(cType, jsoType)
              && isSameOrSuper(qcType, jsoType);
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

  /**
   * Returns <code>true</code> if a static field access of <code>toType</code>
   * from within <code>fromType</code> should generate a clinit call. This
   * will be true in cases where <code>toType</code> has a live clinit method
   * which we cannot statically know has already run. We can statically know the
   * clinit method has already run when:
   * <ol>
   * <li><code>fromType == toType</code></li>
   * <li><code>toType</code> is a superclass of <code>fromType</code>
   * (because <code>toType</code>'s clinit would have already run
   * <code>fromType</code>'s clinit; see JLS 12.4)</li>
   * </ol>
   */
  public boolean checkClinit(JReferenceType fromType, JReferenceType toType) {
    if (fromType == toType) {
      return false;
    }
    if (!hasClinit(toType)) {
      return false;
    }
    if (fromType instanceof JClassType && toType instanceof JClassType
        && isSuperClass((JClassType) fromType, (JClassType) toType)) {
      return false;
    }
    return true;
  }

  public void computeBeforeAST() {
    javaLangObject = program.getTypeJavaLangObject();
    superClassMap.clear();
    subClassMap.clear();
    superInterfaceMap.clear();
    subInterfaceMap.clear();
    implementsMap.clear();
    couldImplementMap.clear();
    isImplementedMap.clear();
    couldBeImplementedMap.clear();

    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        recordSuperSubInfo((JClassType) type);
      } else {
        recordSuperSubInfo((JInterfaceType) type);
      }
    }
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        computeImplements((JClassType) type);
      }
    }
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        computeCouldImplement((JClassType) type);
      }
    }
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        computeVirtualUpRefs((JClassType) type);
      }
    }
  }

  /**
   * Returns true if qType is a superinterface of type, directly or indirectly.
   */
  public boolean extendsInterface(JInterfaceType type, JInterfaceType qType) {
    return getOrCreate(superInterfaceMap, type).contains(qType);
  }

  /**
   * References to any methods which this method implementation might override
   * or implement in any instantiable class.
   */
  public Set<JMethod> getAllOverrides(JMethod method) {
    Set<JMethod> results = new HashSet<JMethod>();
    getAllRealOverrides(method, results);
    getAllVirtualOverrides(method, results);
    return results;
  }

  /**
   * References to any methods which this method directly overrides. This should
   * be an EXHAUSTIVE list, that is, if C overrides B overrides A, then C's
   * overrides list will contain both A and B.
   */
  public Set<JMethod> getAllRealOverrides(JMethod method) {
    Set<JMethod> results = new HashSet<JMethod>();
    getAllRealOverrides(method, results);
    return results;
  }

  /**
   * References to any methods which this method does not directly override
   * within the class in which it is declared; however, some instantiable
   * subclass will cause the implementation of this method to effectively
   * override methods with identical signatures declared in unrelated classes.
   */
  public Set<JMethod> getAllVirtualOverrides(JMethod method) {
    Set<JMethod> results = new HashSet<JMethod>();
    getAllVirtualOverrides(method, results);
    return results;
  }

  public boolean hasClinit(JReferenceType type) {
    return hasClinitSet.contains(type);
  }

  /**
   * Returns true if qType is an implemented interface of type, directly or
   * indirectly.
   */
  public boolean implementsInterface(JClassType type, JInterfaceType qType) {
    return getOrCreate(implementsMap, type).contains(qType);
  }

  public boolean isInstantiatedType(JReferenceType type) {
    if (instantiatedTypes == null) {
      // The instantiated types have not yet been computed.
      return true;
    }

    if (type instanceof JNullType) {
      return true;
    }

    if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      if (arrayType.getLeafType() instanceof JNullType) {
        return true;
      }
    }
    return instantiatedTypes.contains(type);
  }

  /**
   * Returns true if qType is a subclass of type, directly or indirectly.
   */
  public boolean isSubClass(JClassType type, JClassType qType) {
    return getOrCreate(subClassMap, type).contains(qType);
  }

  /**
   * Returns true if qType is a superclass of type, directly or indirectly.
   */
  public boolean isSuperClass(JClassType type, JClassType qType) {
    return getOrCreate(superClassMap, type).contains(qType);
  }

  public void recomputeClinits() {
    hasClinitSet.clear();
    Set<JReferenceType> computed = new HashSet<JReferenceType>();
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = program.getDeclaredTypes().get(i);
      computeHasClinit(type, computed);
    }
  }

  public void setInstantiatedTypes(Set<JReferenceType> instantiatedTypes) {
    this.instantiatedTypes = new HashSet<JReferenceType>();
    this.instantiatedTypes.addAll(instantiatedTypes);
  }

  private <K, V> void add(Map<K, Set<V>> map, K key, V value) {
    getOrCreate(map, key).add(value);
  }

  /**
   * Compute all of the things I might conceivably implement, either through
   * super types or sub types.
   */
  private void computeCouldImplement(JClassType type) {
    Set<JInterfaceType> couldImplementSet = getOrCreate(couldImplementMap, type);
    // all of my direct implements are trivially true
    couldImplementSet.addAll(getOrCreate(implementsMap, type));
    List<JClassType> subclasses = new ArrayList<JClassType>();
    subclasses.addAll(getOrCreate(subClassMap, type));
    for (JClassType subclass : subclasses) {
      for (JInterfaceType intf : subclass.implments) {
        couldImplementSet.add(intf);
        for (JInterfaceType isup : getOrCreate(superInterfaceMap, intf)) {
          couldImplementSet.add(isup);
        }
      }
    }
    for (JInterfaceType couldImpl : couldImplementSet) {
      add(couldBeImplementedMap, couldImpl, type);
    }
  }

  private void computeHasClinit(JReferenceType type,
      Set<JReferenceType> computed) {
    if (computeHasClinitRecursive(type, computed, new HashSet<JReferenceType>())) {
      hasClinitSet.add(type);
    }
    computed.add(type);
  }

  private boolean computeHasClinitRecursive(JReferenceType type,
      Set<JReferenceType> computed, Set<JReferenceType> alreadySeen) {
    // Track that we've been seen.
    alreadySeen.add(type);

    // If we've been computed, hasClinitSet is accurate for me.
    if (computed.contains(type)) {
      return hasClinitSet.contains(type);
    }

    JMethod method = type.methods.get(0);
    assert (JProgram.isClinit(method));
    CheckClinitVisitor v = new CheckClinitVisitor();
    v.accept(method);
    if (v.hasLiveCode()) {
      return true;
    }
    for (JReferenceType target : v.getClinitTargets()) {
      if (alreadySeen.contains(target)) {
        // Ignore this call for now.
        continue;
      }

      if (computeHasClinitRecursive(target, computed, alreadySeen)) {
        // Calling a non-empty clinit means I am a real clinit.
        return true;
      } else {
        // This clinit is okay, keep going.
        continue;
      }
    }
    return false;
  }

  /**
   * Compute all of the things I implement directly, through super types.
   */
  private void computeImplements(JClassType type) {
    Set<JInterfaceType> implementsSet = getOrCreate(implementsMap, type);
    List<JClassType> list = new ArrayList<JClassType>();
    list.add(type);
    list.addAll(getOrCreate(superClassMap, type));
    for (JClassType superclass : list) {
      for (JInterfaceType intf : superclass.implments) {
        implementsSet.add(intf);
        for (JInterfaceType isup : getOrCreate(superInterfaceMap, intf)) {
          implementsSet.add(isup);
        }
      }
    }
    for (JInterfaceType impl : implementsSet) {
      add(isImplementedMap, impl, type);
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
    if (type.extnds == null || type.extnds == javaLangObject) {
      return;
    }

    /*
     * For each interface I directly implement, check all methods and make sure
     * I define implementations for them. If I don't, then check all my super
     * classes to find virtual overrides.
     */
    for (JInterfaceType intf : type.implments) {
      computeVirtualUpRefs(type, intf);
      Set<JInterfaceType> superIntfs = getOrCreate(superInterfaceMap, intf);
      for (JInterfaceType superIntf : superIntfs) {
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
    outer : for (JMethod intfMethod : intf.methods) {
      for (JMethod classMethod : type.methods) {
        if (JProgram.methodsDoMatch(intfMethod, classMethod)) {
          // this class directly implements the interface method
          continue outer;
        }
      }

      // this class does not directly implement the interface method
      // if any super classes do, create a virtual up ref
      for (JClassType superType = type.extnds; superType != javaLangObject; superType = superType.extnds) {
        for (JMethod superMethod : superType.methods) {
          if (JProgram.methodsDoMatch(intfMethod, superMethod)) {
            // this super class directly implements the interface method
            // create a virtual up ref

            // System.out.println("Virtual upref from " + superType.getName()
            // + "." + superMethod.getName() + " to " + intf.getName() + "."
            // + intfMethod.getName() + " via " + type.getName());

            Map<JClassType, Set<JMethod>> classToMethodMap = getOrCreateMap(
                virtualUpRefMap, superMethod);
            Set<JMethod> methodSet = getOrCreate(classToMethodMap, type);
            methodSet.add(intfMethod);

            // do not search additional super types
            continue outer;
          }
        }
      }
    }
  }

  private void getAllRealOverrides(JMethod method, Set<JMethod> results) {
    for (JMethod possibleOverride : method.overrides) {
      // if (instantiatedTypes.contains(possibleOverride.getEnclosingType())) {
      results.add(possibleOverride);
      // }
    }
  }

  private void getAllVirtualOverrides(JMethod method, Set<JMethod> results) {
    Map<JClassType, Set<JMethod>> overrideMap = getOrCreateMap(virtualUpRefMap,
        method);
    for (JClassType classType : overrideMap.keySet()) {
      if (isInstantiatedType(classType)) {
        Set<JMethod> set = overrideMap.get(classType);
        results.addAll(set);
      }
    }
  }

  private <K, V> Set<V> getOrCreate(Map<K, Set<V>> map, K key) {
    Set<V> set = map.get(key);
    if (set == null) {
      set = new HashSet<V>();
      map.put(key, set);
    }
    return set;
  }

  private <K, K2, V> Map<K2, V> getOrCreateMap(Map<K, Map<K2, V>> map, K key) {
    Map<K2, V> map2 = map.get(key);
    if (map2 == null) {
      map2 = new HashMap<K2, V>();
      map.put(key, map2);
    }
    return map2;
  }

  private boolean isSameOrSuper(JClassType type, JClassType qType) {
    return (type == qType || isSuperClass(type, qType));
  }

  /**
   * Record the all of my super classes (and myself as a subclass of them).
   */
  private void recordSuperSubInfo(JClassType type) {
    Set<JClassType> superSet = getOrCreate(superClassMap, type);
    for (JClassType t = type.extnds; t != null; t = t.extnds) {
      superSet.add(t);
      add(subClassMap, t, type);
    }
  }

  /**
   * Record the all of my super interfaces (and myself as a sub interface of
   * them).
   */
  private void recordSuperSubInfo(JInterfaceType type) {
    Set<JInterfaceType> superSet = getOrCreate(superInterfaceMap, type);
    recordSuperSubInfo(type, superSet, type);
  }

  /**
   * Recursively record all of my super interfaces.
   */
  private void recordSuperSubInfo(JInterfaceType base,
      Set<JInterfaceType> superSet, JInterfaceType cur) {
    for (JInterfaceType intf : cur.implments) {
      superSet.add(intf);
      add(subInterfaceMap, intf, base);
      recordSuperSubInfo(base, superSet, intf);
    }
  }

}
