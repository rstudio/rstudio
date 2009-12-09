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
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.IdentityHashSet;
import com.google.gwt.dev.util.collect.IdentitySets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

  /**
   * A map of all interfaces to the set of classes that could theoretically
   * implement them.
   */
  private final Map<JInterfaceType, Set<JClassType>> couldBeImplementedMap = new IdentityHashMap<JInterfaceType, Set<JClassType>>();

  /**
   * A map of all classes to the set of interfaces that they could theoretically
   * implement.
   */
  private final Map<JClassType, Set<JInterfaceType>> couldImplementMap = new IdentityHashMap<JClassType, Set<JInterfaceType>>();

  /**
   * The set of all interfaces that are initially implemented by both a Java and
   * Overlay type.
   */
  private final Set<JInterfaceType> dualImpls = new IdentityHashSet<JInterfaceType>();

  /**
   * A map of all classes to the set of interfaces they directly implement,
   * possibly through inheritance.
   */
  private final Map<JClassType, Set<JInterfaceType>> implementsMap = new IdentityHashMap<JClassType, Set<JInterfaceType>>();

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
  private final Map<JInterfaceType, Set<JClassType>> isImplementedMap = new IdentityHashMap<JInterfaceType, Set<JClassType>>();

  /**
   * Caches the {@link Object} class.
   */
  private JClassType javaLangObject = null;

  /**
   * A map of all interfaces that are implemented by overlay types to the
   * overlay type that initially implements it.
   */
  private final Map<JInterfaceType, JClassType> jsoSingleImpls = new IdentityHashMap<JInterfaceType, JClassType>();

  /**
   * The associated {@link JProgram}.
   */
  private final JProgram program;

  /**
   * A map of all classes to the set of classes that extend them, directly or
   * indirectly.
   */
  private final Map<JClassType, Set<JClassType>> subClassMap = new IdentityHashMap<JClassType, Set<JClassType>>();

  /**
   * A map of all interfaces to the set of interfaces that extend them, directly
   * or indirectly.
   */
  private final Map<JInterfaceType, Set<JInterfaceType>> subInterfaceMap = new IdentityHashMap<JInterfaceType, Set<JInterfaceType>>();

  /**
   * A map of all classes to the set of classes they extend, directly or
   * indirectly.
   */
  private final Map<JClassType, Set<JClassType>> superClassMap = new IdentityHashMap<JClassType, Set<JClassType>>();

  /**
   * A map of all interfaces to the set of interfaces they extend, directly or
   * indirectly.
   */
  private final Map<JInterfaceType, Set<JInterfaceType>> superInterfaceMap = new IdentityHashMap<JInterfaceType, Set<JInterfaceType>>();

  /**
   * A map of all methods with virtual overrides, onto the collection of
   * overridden methods. Each key method's collections is a map of the set of
   * subclasses who inherit the key method mapped onto the set of interface
   * methods the key method virtually implements. For a definition of a virtual
   * override, see {@link #getAllVirtualOverrides(JMethod)}.
   */
  private final Map<JMethod, Map<JClassType, Set<JMethod>>> virtualUpRefMap = new IdentityHashMap<JMethod, Map<JClassType, Set<JMethod>>>();

  public JTypeOracle(JProgram program) {
    this.program = program;
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
     * Now that the basic type hierarchy is computed, move all interfaces that
     * are implemented by overlay types onto JavaScriptObject itself before
     * building the full maps.
     */
    JClassType jsoType = program.getJavaScriptObject();
    Set<JClassType> jsoSubTypes = Collections.emptySet();
    if (jsoType != null) {
      assert jsoType.getImplements().size() == 0;
      jsoSubTypes = get(subClassMap, jsoType);
      for (JClassType jsoSubType : jsoSubTypes) {
        for (JInterfaceType intf : jsoSubType.getImplements()) {
          jsoType.addImplements(intf);
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
    int totalJsoTypes = jsoSubTypes.size() + 1;
    for (JInterfaceType jsoIntf : jsoSingleImpls.keySet()) {
      Set<JClassType> implementors = get(isImplementedMap, jsoIntf);
      if (implementors.size() == totalJsoTypes) {
        assert implementors.contains(jsoType);
      } else {
        assert implementors.size() > totalJsoTypes;
        dualImpls.add(jsoIntf);
      }
    }
  }

  public Set<JMethod> getAllOverrides(JMethod method) {
    return getAllOverrides(method, instantiatedTypes);
  }

  /**
   * References to any methods which this method implementation might override
   * or implement in any instantiable class.
   */
  public Set<JMethod> getAllOverrides(JMethod method,
      Set<JReferenceType> instantiatedTypes) {
    Set<JMethod> results = new IdentityHashSet<JMethod>();
    getAllRealOverrides(method, results);
    getAllVirtualOverrides(method, instantiatedTypes, results);
    return results;
  }

  /**
   * References to any methods which this method directly overrides. This should
   * be an EXHAUSTIVE list, that is, if C overrides B overrides A, then C's
   * overrides list will contain both A and B.
   */
  public Set<JMethod> getAllRealOverrides(JMethod method) {
    Set<JMethod> results = new IdentityHashSet<JMethod>();
    getAllRealOverrides(method, results);
    return results;
  }

  /**
   * Returns the set of methods the given method virtually overrides. A virtual
   * override is an association between a concrete method and an unrelated
   * interface method with the exact same name and signature. The association
   * occurs if and only if some subclass extends the concrete method's
   * containing class and implements the interface method's containing
   * interface. Example:
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
  public Set<JMethod> getAllVirtualOverrides(JMethod method) {
    Set<JMethod> results = new IdentityHashSet<JMethod>();
    getAllVirtualOverrides(method, instantiatedTypes, results);
    return results;
  }

  public JClassType getSingleJsoImpl(JReferenceType maybeSingleJsoIntf) {
    return jsoSingleImpls.get(maybeSingleJsoIntf.getUnderlyingType());
  }

  public boolean isDualJsoInterface(JReferenceType maybeDualImpl) {
    return dualImpls.contains(maybeDualImpl.getUnderlyingType());
  }

  public boolean isInstantiatedType(JReferenceType type) {
    return isInstantiatedType(type, instantiatedTypes);
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
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JDeclaredType type = program.getDeclaredTypes().get(i);
      if (type.hasClinit()) {
        computeHasClinit(type, computed);
      }
    }
  }

  public void setInstantiatedTypes(Set<JReferenceType> instantiatedTypes) {
    this.instantiatedTypes = new IdentityHashSet<JReferenceType>();
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

  private void computeHasClinit(JDeclaredType type, Set<JDeclaredType> computed) {
    if (computeHasClinitRecursive(type, computed,
        new IdentityHashSet<JDeclaredType>())) {
      computed.add(type);
    } else {
      type.removeClinit();
    }
  }

  private boolean computeHasClinitRecursive(JDeclaredType type,
      Set<JDeclaredType> computed, Set<JDeclaredType> alreadySeen) {
    // Track that we've been seen.
    alreadySeen.add(type);

    JMethod method = type.getMethods().get(0);
    assert (JProgram.isClinit(method));
    CheckClinitVisitor v = new CheckClinitVisitor();
    v.accept(method);
    if (v.hasLiveCode()) {
      return true;
    }
    for (JDeclaredType target : v.getClinitTargets()) {
      if (!target.hasClinit()) {
        // A false result is always accurate.
        continue;
      }

      /*
       * If target has a clinit, so do I; but only if target has already been
       * recomputed this run.
       */
      if (target.hasClinit() && computed.contains(target)) {
        return true;
      }

      /*
       * Prevent recursion sickness: ignore this call for now since this call is
       * being accounted for higher on the stack.
       */
      if (alreadySeen.contains(target)) {
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
      for (JClassType superType = type.getSuperClass(); superType != javaLangObject; superType = superType.getSuperClass()) {
        for (JMethod superMethod : superType.getMethods()) {
          if (methodsDoMatch(intfMethod, superMethod)) {
            // this super class directly implements the interface method
            // create a virtual up ref

            // System.out.println("Virtual upref from " + superType.getName()
            // + "." + superMethod.getName() + " to " + intf.getName() + "."
            // + intfMethod.getName() + " via " + type.getName());

            Map<JClassType, Set<JMethod>> classToMethodMap = getOrCreateMap(
                virtualUpRefMap, superMethod);
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

  private void getAllVirtualOverrides(JMethod method,
      Set<JReferenceType> instantiatedTypes, Set<JMethod> results) {
    Map<JClassType, Set<JMethod>> overrideMap = virtualUpRefMap.get(method);
    if (overrideMap != null) {
      for (Map.Entry<JClassType, Set<JMethod>> entry : overrideMap.entrySet()) {
        JClassType classType = entry.getKey();
        if (isInstantiatedType(classType, instantiatedTypes)) {
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

  /**
   * Returns true if type implements the interface represented by qType, either
   * directly or indirectly.
   */
  private boolean implementsInterface(JClassType type, JInterfaceType qType) {
    return get(implementsMap, type).contains(qType);
  }

  /**
   * Determine whether a type is instantiated, given an assumed list of
   * instantiated types.
   * 
   * @param type any type
   * @param instantiatedTypes a set of types assumed to be instantiated. If
   *          <code>null</code>, then there are no assumptions about which types
   *          are instantiated.
   * @return whether the type is instantiated
   */
  private boolean isInstantiatedType(JReferenceType type,
      Set<JReferenceType> instantiatedTypes) {
    type = program.getRunTimeType(type);

    if (instantiatedTypes == null) {
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

  private boolean isSameOrSuper(JClassType type, JClassType qType) {
    return (type == qType || isSuperClass(type, qType));
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
  private void recordSuperSubInfo(JInterfaceType base,
      Set<JInterfaceType> superSet, JInterfaceType cur) {
    for (JInterfaceType intf : cur.getImplements()) {
      superSet.add(intf);
      add(subInterfaceMap, intf, base);
      recordSuperSubInfo(base, superSet, intf);
    }
  }

}
