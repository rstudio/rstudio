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

import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.util.arg.OptionJsInteropMode;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimaps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Oracle that can answer questions regarding the types in a program.
 * <p>
 * Since its entire responsibility is to be an index of type related information it should not
 * directly perform any optimizations.
 */
// TODO(stalcup): move the clinit() optimization out into a separate pass.
public class JTypeOracle implements Serializable {

  public static final Function<JType,String> TYPE_TO_NAME = new Function<JType, String>() {
    @Override
    public String apply(JType type) {
      return type.getName();
    }
  };

  /**
   * All authorative information about the current program.
   */
  public static class ImmediateTypeRelations implements Serializable {

    /**
     * A mapping from a class name to its immediate super class' name.
     */
    private Map<String, String> immediateSuperclassesByClass = Maps.newHashMap();

    /**
     * A mapping from an interface name to its super interface's name.
     */
    private Multimap<String, String> immediateSuperInterfacesByInterface = HashMultimap.create();

    /**
     * A mapping from a class name to its directly implemented interfaces' names..
     */
    private Multimap<String, String> immediateImplementedInterfacesByClass =
        HashMultimap.create();

    public void copyFrom(ImmediateTypeRelations that) {
      this.immediateImplementedInterfacesByClass.clear();
      this.immediateSuperclassesByClass.clear();
      this.immediateSuperInterfacesByInterface.clear();

      this.immediateImplementedInterfacesByClass.putAll(that.immediateImplementedInterfacesByClass);
      this.immediateSuperclassesByClass.putAll(that.immediateSuperclassesByClass);
      this.immediateSuperInterfacesByInterface.putAll(that.immediateSuperInterfacesByInterface);
    }

    @VisibleForTesting
    public boolean hasSameContent(ImmediateTypeRelations that) {
      return Objects.equal(this.immediateImplementedInterfacesByClass,
          that.immediateImplementedInterfacesByClass)
          && Objects.equal(this.immediateSuperclassesByClass, that.immediateSuperclassesByClass)
          && Objects.equal(this.immediateSuperInterfacesByInterface,
              that.immediateSuperInterfacesByInterface);
    }

    @VisibleForTesting
    public Map<String, String> getImmediateSuperclassesByClass() {
      return immediateSuperclassesByClass;
    }

    public boolean isEmpty() {
      return immediateSuperclassesByClass.isEmpty() && immediateSuperInterfacesByInterface.isEmpty()
          && immediateImplementedInterfacesByClass.isEmpty();
    }
  }

  /**
   * A collection of types that are required to correctly run JTypeOracle.
   */
  public static class StandardTypes implements Serializable {

    public static StandardTypes createFrom(JProgram program) {
      StandardTypes requiredTypes = new StandardTypes();
      requiredTypes.javaLangObject = program.getTypeJavaLangObject().getName();
      JDeclaredType javaIoSerializableType = program.getFromTypeMap(Serializable.class.getName());
      requiredTypes.javaIoSerializable =
          javaIoSerializableType == null ? null : javaIoSerializableType.getName();
      JDeclaredType javaLangConeableType = program.getFromTypeMap(Cloneable.class.getName());
      requiredTypes.javaLangCloneable =
          javaLangConeableType == null ? null : javaLangConeableType.getName();
      requiredTypes.nullType = program.getTypeNull().getName();
      return requiredTypes;
    }

    private String javaIoSerializable;

    private String javaLangCloneable;

    private String javaLangObject;

    private String nullType;
  }

  private Set<JMethod> exportedMethods = Sets.newLinkedHashSet();
  private Set<JField> exportedFields = Sets.newLinkedHashSet();

  private Set<JReferenceType> instantiatedJsoTypesViaCast = Sets.newHashSet();
  private OptionJsInteropMode.Mode jsInteropMode;

  public Set<JMethod> getExportedMethods() {
    return exportedMethods;
  }

  public Set<JField> getExportedFields() {
    return exportedFields;
  }

  public void setInstantiatedJsoTypesViaCast(Set<JReferenceType> instantiatedJsoTypesViaCast) {
    this.instantiatedJsoTypesViaCast = instantiatedJsoTypesViaCast;
  }

  public Set<JReferenceType> getInstantiatedJsoTypesViaCast() {
    return instantiatedJsoTypesViaCast;
  }

  /**
   * A method needs a JsInterop bridge if any of the following are true:
   * 1) the method name conflicts with a method name of a non-JsType/JsExport method in a superclass
   * 2) the method returns or accepts Single-Abstract-Method types
   * 3) the method returns or accepts JsAware/JsConvert types.
   */
  public boolean needsJsInteropBridgeMethod(JMethod x) {
    if (!isJsInteropEnabled()) {
      return false;
    }

    /*
     * We need Javascript bridge methods for exports in this class
     * @JsType
     * interface A {
     *   X m();
     * }
     * Y is a subtype of X
     * interface B extends A {
     *   Y m();
     * }
     *
     * We now have an 'overload' situation, but there's only one concrete
     * implementor.
     *
     * class C implements B {
     *   Y m() { }
     * }
     *
     * JDT/GwtAstBuilder will insert a synthetic method to make sure A is
     * implemented.
     *
     * class C implements B {
     *   X m() { return this.m(); [targetd at Y] }
     *   Y m() { }
     * }
     *
     * Since both methods are part of JsType interfaces, both are considered
     * exportable, but they can't own the same JsName. It doesn't matter
     * which one is exported since they do the same thing.  Here we detect
     * that a covariant return situation exists and assert that a JS bridge
     * method is needed. That is, we will not let either of these methods
     * 'own' the JsName. If we don't do this, and the X m() get's exported,
     * you end up with an infinite loop and other oddities (because it's
     * an exported method and it invoked itself through it's own exported
     * name).
     *
     * This change lets both methods have their Java obfuscated name.
     */
    // covariant methods need JS bridges
    List<JParameter> xParams = x.getParams();
    if (isJsTypeMethod(x)) {
      for (JMethod other : x.getEnclosingType().getMethods()) {
         if (other == x) {
           continue;
         }
         if (isJsTypeMethod(other) && x.getName().equals(other.getName())) {
           List<JParameter> otherParams = other.getParams();
           if (otherParams.size() == xParams.size()) {
             for (int i = 0; i < otherParams.size(); i++) {
               if (otherParams.get(i).getType() != xParams.get(i).getType()) {
                 break;
               }
             }
             // found exact method match, covariant return
             return true;
           } else {
             break;
           }
         }
      }
    }

    if (x.needsVtable() && isJsTypeMethod(x)) {
      for (JMethod override : x.getOverriddenMethods()) {
        if (!isJsTypeMethod(override)) {
          return true;
        }
      }
    }

    // implicit builtin @JsConvert, longs are converted
    if (isJsTypeMethod(x) || isExportedMethod(x)) {
      if (x.getOriginalReturnType() == JPrimitiveType.LONG) {
        return true;
      }
      for (JParameter p : xParams) {
        if (p.getType() == JPrimitiveType.LONG) {
          return true;
        }
      }
    }
    // TODO (cromwellian): add SAM and JsAware/Convert cases in follow up
    return false;
  }

  public boolean isJsInteropEnabled() {
    return jsInteropMode != OptionJsInteropMode.Mode.NONE;
  }

  public void setJsInteropMode(OptionJsInteropMode.Mode jsInteropMode) {
    this.jsInteropMode = jsInteropMode;
  }

  public void setOptimize(boolean optimize) {
    this.optimize = optimize;
  }

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
   * <p>
   * IMPORTANT: do not optimize clinit visitor to do a better job in determining if the clinit
   * contains useful code (like by doing implicit DeadCodeEliminination). Passes like
   * ControlFlowAnalyzer and Pruner will produce inconsistent ASTs.
   *
   * @see ControlFlowAnalyzer.visit(JClassType class, Context ctx)
   */
  private static final class CheckClinitVisitor extends JVisitor {

    private final Set<JDeclaredType> clinitTargets = Sets.newIdentityHashSet();

    /**
     * Tracks whether any live code is run in this clinit. This is only reliable
     * because we explicitly visit all AST structures that might contain
     * non-clinit-calling code.
     *
     * @see #mightContainOnlyClinitCalls(JExpression)
     * @see #mightContainOnlyClinitCallsOrDeclarationStatements(JStatement)
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
        if (mightContainOnlyClinitCallsOrDeclarationStatements(stmt)) {
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
        // {@See ControlFlowAnalizer.rescue(JVariable var)
        if (field.getLiteralInitializer() != null && field.isStatic()) {
          // Literal initializers for static fields, even though they appear in the clinit they are
          // not considered part of it; instead they are normally considered part of the fields they
          // initialize.
          return false;
        }
      }
      hasLiveCode = true;
      return false;
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      JExpression expr = x.getExpr();
      if (mightContainOnlyClinitCalls(expr)) {
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
      for (JExpression expr : x.getExpressions()) {
        // Only a JMultiExpression or JMethodCall can contain clinit calls.
        if (mightContainOnlyClinitCalls(expr)) {
          accept(expr);
        } else {
          hasLiveCode = true;
        }
      }
      return false;
    }

    private boolean mightContainOnlyClinitCalls(JExpression expr) {
      // Must have a visit method for every subtype that might answer yes!
      return expr instanceof JMultiExpression || expr instanceof JMethodCall;
    }

    private boolean mightContainOnlyClinitCallsOrDeclarationStatements(JStatement stmt) {
      // Must have a visit method for every subtype that might answer yes!
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
   * A set of all classes in the current program.
   */
  private Set<String> allClasses = Sets.newHashSet();

  /**
   * A map of all classes to the set of interfaces that they could theoretically
   * implement.
   * <p>
   * C hasPotentialInterface I iff Exists C'. C' = C or C' subclassOf C and C implements I.
   */
  private Multimap<String, String> potentialInterfaceByClass;

  /**
   * The set of all interfaces that are initially implemented by both a Java and
   * Overlay type.
   */
  private final Set<String> dualImplInterfaces = Sets.newHashSet();

  /**
   * A map of all classes to the set of interfaces they implement,
   * possibly through inheritance.
   * <p>
   * C implements I iff Exists, C', I'. (C' = C or C' isSubclassOf C) and (I = I' or
   * I' isSuperInterfaceOf I) and C' immediateImplements I'.
   */
  private Multimap<String, String> implementedInterfacesByClass;

  /**
   * The types in the program that are instantiable. All types in this set
   * should be run-time types as defined at
   * {@link JProgram#getRunTimeType(JReferenceType)}.
   */
  private Set<JReferenceType> instantiatedTypes = null;

  /**
   * A map of all interfaces to the set of classes that directly implement them,
   * possibly through inheritance. {@code classesByImplementingInterface} is the relational
   * inverse of {@code implementedInterfacesByClass}.
   */
  private Multimap<String, String> classesByImplementingInterface;

  /**
   * A map of all interfaces that are implemented by overlay types to the
   * overlay type that initially implements it.
   */
  private final Map<String, String> jsoByInterface = Maps.newHashMap();

  /**
   * A mapping from the type name to the actual type instance.
   */
  private Map<String, JReferenceType> referenceTypesByName = Maps.newHashMap();

  /**
   * A map of all classes to the set of classes that extend them, directly or
   * indirectly. {@code subclassesByClass} is the inverse of
   * {@code superclassesByClass}.
   * <p>
   * NOTE: {@code subclassesByClass} is NOT reflexive.
   */
  private Multimap<String, String> subclassesByClass;

  /**
   * A map of all interfaces to the set of interfaces that extend them, directly or indirectly
   * {@code subInterfacesByInterface} is the inverse of {@code superInterfacesByInterface}.
   * <p>
   * NOTE: {@code subInterfacesByInterface} is NOT reflexive.
   */
  private Multimap<String, String> subInterfacesByInterface;

  /**
   * A map of all classes to the set of classes they extend, directly or
   * indirectly. (not reflexive)
   * <p>
   * {@code superclassesByClass} is the transitive closure of
   * {@code immediateSuperclassesByClass}.
   * <p>
   * NOTE: {@code superclassesByClass} is NOT reflexive.
   */
  private Multimap<String, String> superclassesByClass;

  /**
   * A map of all interfaces to the set of interfaces they extend, directly or
   * indirectly.
   * <p>
   * {@code superInterfacesByInterface} is the transitive closure of
   * {@code immediateSuperInterfacesByInterface}.
   * <p>
   * NOTE: {@code superInterfacesByInterface} is NOT reflexive.
   */
  private Multimap<String, String> superInterfacesByInterface;

  /**
   * An index of all polymorphic methods for each class.
   */
  private final Map<JClassType, Map<String, JMethod>> methodsBySignatureForType =
      Maps.newIdentityHashMap();

  private final boolean hasWholeWorldKnowledge;
  private boolean optimize = true;

  private ImmediateTypeRelations immediateTypeRelations;
  private ArrayTypeCreator arrayTypeCreator;
  private StandardTypes standardTypes;

  /**
   * Constructs a new JTypeOracle.
   */
  public JTypeOracle(ArrayTypeCreator arrayTypeCreator, MinimalRebuildCache minimalRebuildCache,
      boolean hasWholeWorldKnowledge) {
    this.immediateTypeRelations = minimalRebuildCache.getImmediateTypeRelations();
    this.arrayTypeCreator = arrayTypeCreator;
    this.hasWholeWorldKnowledge = hasWholeWorldKnowledge;

    // Be ready to answer simple questions (type hierarchy) even before recompute...().
    computeExtendedTypeRelations();
  }

  /**
   * True if the type is a JSO or interface implemented by JSO or a JsType without
   * prototype.
   */
  public boolean canBeJavaScriptObject(JType type) {
    type = type.getUnderlyingType();
    return isJavaScriptObject(type) || isSingleJsoImpl(type);
  }

  /**
   * True if the type is a JSO or interface implemented by JSO or a JsType without prototype.
   */
  public boolean canCrossCastLikeJso(JType type) {
    JDeclaredType dtype = getNearestJsType(type, false);
    return canBeJavaScriptObject(type) || (dtype instanceof JInterfaceType
        && isOrExtendsJsType(type, false) && !isOrExtendsJsType(type, true));
  }

  /**
   * True if the type is a JSO or JSO Interface that is not dually implemented, or is a JsType
   * without the prototype that is not implemented by a Java class.
   */
  public boolean willCrossCastLikeJso(JType type) {
    return isEffectivelyJavaScriptObject(type) || canCrossCastLikeJso(type)
        && type instanceof JInterfaceType && !hasLiveImplementors(type);
  }

  public boolean hasLiveImplementors(JType type) {
    // If our knowledge is limited or we're not optimizing.
    if (!hasWholeWorldKnowledge || !optimize) {
      // Assume the worst case, that the provided type does have live implementors.
      return true;
    }
    if (type instanceof JInterfaceType) {
      for (JReferenceType impl : getTypes(classesByImplementingInterface.get(type.getName()))) {
        if (isInstantiatedType((JClassType) impl)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * True if the type is a JSO or interface implemented by a JSO, or a JsType.
   */
  public boolean canBeInstantiatedInJavascript(JType type) {
    return canBeJavaScriptObject(type) || isOrExtendsJsType(type, false);
  }

  public boolean canTheoreticallyCast(JReferenceType type, JReferenceType qType) {
    if (!type.canBeNull() && qType.getName().equals(standardTypes.nullType)) {
      // Cannot cast non-nullable to null
      return false;
    }

    // Compare the underlying types.
    type = type.getUnderlyingType();
    qType = qType.getUnderlyingType();

    if (type == qType || type.getName().equals(standardTypes.javaLangObject)) {
      return true;
    }

    /**
     * Cross-cast allowed in theory, prevents TypeTightener from turning
     * cross-casts into null-casts.
     */
    if (canCrossCastLikeJso(type) && canCrossCastLikeJso(qType)) {
      return true;
    }

    // TODO (cromwellian): handle case where types S and T have identical Js Prototypes
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
        if (dims < qDims && !leafType.getName().equals(standardTypes.javaLangObject)
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
        return potentialInterfaceByClass.containsEntry(cType.getName(), qType.getName());
      }
    } else if (type instanceof JInterfaceType) {

      JInterfaceType iType = (JInterfaceType) type;
      if (qType instanceof JClassType) {
        return potentialInterfaceByClass.containsEntry(qType.getName(), iType.getName());
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

    if (type == qType || qType.getName().equals(standardTypes.javaLangObject)) {
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

        // int[][] -> Object[], Serializable[], Clonable[] or null[] trivially true
        if (dims > qDims
            && (qLeafType.getName().equals(standardTypes.javaLangObject)
                || qLeafType.getName().equals(standardTypes.javaIoSerializable)
                || qLeafType.getName().equals(standardTypes.javaLangCloneable)
                || qLeafType instanceof JNullType)) {
          return true;
        }

        if (dims == qDims) {
          if (leafType instanceof JReferenceType && qLeafType instanceof JReferenceType) {
            return canTriviallyCast((JReferenceType) leafType, (JReferenceType) qLeafType);
          }
        }
      }

      if (qType.getName().equals(standardTypes.javaIoSerializable)
          || qType.getName().equals(standardTypes.javaLangCloneable)) {
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

  public void computeBeforeAST(StandardTypes standardTypes, Collection<JDeclaredType> declaredTypes,
      List<JDeclaredType> moduleDeclaredTypes) {
    computeBeforeAST(standardTypes, declaredTypes, moduleDeclaredTypes,
        ImmutableList.<String> of());
  }

  public void computeBeforeAST(StandardTypes standardTypes, Collection<JDeclaredType> declaredTypes,
      Collection<JDeclaredType> moduleDeclaredTypes, Collection<String> deletedTypeNames) {
    this.standardTypes = standardTypes;
    recordReferenceTypeByName(declaredTypes);
    deleteImmediateTypeRelations(deletedTypeNames);
    deleteImmediateTypeRelations(getNamesOf(moduleDeclaredTypes));
    recordImmediateTypeRelations(moduleDeclaredTypes);
    computeExtendedTypeRelations();

    for (JDeclaredType type : declaredTypes) {

      // first time through, record all exported methods
      for (JMethod method : type.getMethods()) {
        if (isExportedMethod(method)) {
          exportedMethods.add(method);
        }
      }
      for (JField field : type.getFields()) {
        if (isExportedField(field)) {
          exportedFields.add(field);
        }
      }
    }
  }

  private static Collection<String> getNamesOf(Collection<JDeclaredType> types) {
    List<String> typeNames = Lists.newArrayList();
    for (JDeclaredType type : types) {
      typeNames.add(type.getName());
    }
    return typeNames;
  }

  private void recordReferenceTypeByName(Collection<JDeclaredType> types) {
    referenceTypesByName.clear();
    for (JReferenceType type : types) {
      referenceTypesByName.put(type.getName(), type);
    }
  }

  /**
   * Get the nearest JS type.
   */
  public JDeclaredType getNearestJsType(JType type, boolean mustHavePrototype) {
    if (!isJsInteropEnabled()) {
      return null;
    }

    type = type.getUnderlyingType();

    if (!(type instanceof JDeclaredType)) {
      return null;
    }

    JDeclaredType dtype = (JDeclaredType) type;
    if (isJsType(dtype) && (!mustHavePrototype || !Strings.isNullOrEmpty(dtype.getJsPrototype()))) {
      return dtype;
    }

    for (JInterfaceType superIntf : dtype.getImplements()) {
      JDeclaredType jsIntf = getNearestJsType(superIntf, mustHavePrototype);
      if (jsIntf != null) {
        return jsIntf;
      }
    }

    return null;
  }

  /**
   * Get the JsFunction method of {@code type}.
   */
  public JMethod getJsFunctionMethod(JClassType type) {
    for (JMethod method : type.getMethods()) {
      if (isJsFunctionMethod(method)) {
        return method;
      }
    }
    return (type.getSuperClass() != null) ? getJsFunctionMethod(type.getSuperClass()) : null;
  }

  /**
   * Get all implemented interfaces of {@code type}.
   */
  public Collection<JInterfaceType> getImplementedInterfaces(JDeclaredType type) {
    Multimap<String, String> implementedInterfaces =
        (type instanceof JClassType) ? implementedInterfacesByClass : superInterfacesByInterface;
    return Collections2.transform(implementedInterfaces.get((type.getName())),
        new Function<String, JInterfaceType>() {
          @Override
          public JInterfaceType apply(String typeName) {
            JReferenceType referenceType = referenceTypesByName.get(typeName);
            assert (referenceType instanceof JInterfaceType);
            return (JInterfaceType) referenceType;
          }
        }
    );
  }

  /**
   * Get all implemented JsFunction interfaces of {@code type}.
   * After JsInteropRestrictionChecker, jsFunctions.size() <= 1 would always be true.
   */
  public Collection<JInterfaceType> getImplementedJsFunctions(JDeclaredType type) {
    Collection<JInterfaceType> jsFunctions =
        Collections2.filter(getImplementedInterfaces(type), new Predicate<JInterfaceType>() {
          @Override
          public boolean apply(JInterfaceType implementedInterface) {
            return implementedInterface.isJsFunction();
          }
        });
    return jsFunctions;
  }

  public JMethod getInstanceMethodBySignature(JClassType type, String signature) {
    return getOrCreateInstanceMethodsBySignatureForType(type).get(signature);
  }

  public JClassType getSingleJsoImpl(JReferenceType maybeSingleJsoIntf) {
    String className = jsoByInterface.get(maybeSingleJsoIntf.getName());
    if (className == null) {
      return null;
    }
    return (JClassType) referenceTypesByName.get(className);
  }

  public String getSuperTypeName(String className) {
    return immediateTypeRelations.immediateSuperclassesByClass.get(className);
  }

  public Set<JReferenceType> getCastableDestinationTypes(JReferenceType type) {
    // For arrays we build up their castable destination types on the fly
    if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      List<JReferenceType> castableDestinationTypes = Lists.newArrayList();

      // All arrays cast to Object, Serializable and Cloneable.
      ImmutableList<JReferenceType> arrayBaseTypes = ImmutableList.of(
          ensureTypeExistsAndAppend(standardTypes.javaLangObject, castableDestinationTypes),
          ensureTypeExistsAndAppend(standardTypes.javaIoSerializable, castableDestinationTypes),
          ensureTypeExistsAndAppend(standardTypes.javaLangCloneable, castableDestinationTypes));

      // Foo[][][] can cast to <ArrayBaseType>[][].
      for (int lowerDimension = 1; lowerDimension < arrayType.getDims(); lowerDimension++) {
        for (JReferenceType arrayBaseType : arrayBaseTypes) {
          castableDestinationTypes.add(
              arrayTypeCreator.getOrCreateArrayType(arrayBaseType, lowerDimension));
        }
      }

      if (arrayType.getLeafType() instanceof JPrimitiveType) {
        castableDestinationTypes.add(arrayType);
      } else {
        // Class arrays reuse their leaf type castable destination types.
        JDeclaredType leafType = (JDeclaredType) arrayType.getLeafType();
        for (JReferenceType castableDestinationType : getCastableDestinationTypes(leafType)) {
          JArrayType superArrayType =
              arrayTypeCreator.getOrCreateArrayType(castableDestinationType, arrayType.getDims());
          castableDestinationTypes.add(superArrayType);
        }
      }
      Collections.sort(castableDestinationTypes, HasName.BY_NAME_COMPARATOR);
      return Sets.newLinkedHashSet(castableDestinationTypes);
    }

    List<JReferenceType> castableDestinationTypes = Lists.newArrayList();
    if (superclassesByClass.containsKey(type.getName())) {
      Iterables.addAll(castableDestinationTypes,
          getTypes(superclassesByClass.get(type.getName())));
    }
    if (superInterfacesByInterface.containsKey(type.getName())) {
      Iterables.addAll(castableDestinationTypes,
          getTypes(superInterfacesByInterface.get(type.getName())));
    }
    if (implementedInterfacesByClass.containsKey(type.getName())) {
      Iterables.addAll(castableDestinationTypes,
          getTypes(implementedInterfacesByClass.get(type.getName())));
    }
    if (willCrossCastLikeJso(type)) {
      ensureTypeExistsAndAppend(JProgram.JAVASCRIPTOBJECT, castableDestinationTypes);
    }
    // Do not add itself if it is a JavaScriptObject subclass, add JavaScriptObject.
    if (isJavaScriptObject(type)) {
      ensureTypeExistsAndAppend(JProgram.JAVASCRIPTOBJECT, castableDestinationTypes);
    } else {
      castableDestinationTypes.add(type);
    }

    // Even though the AST representation of interfaces do not claim to inherit from Object, they
    // can cast to Object.
    JReferenceType javaLangObjectType = referenceTypesByName.get(standardTypes.javaLangObject);
    // Make sure that the type is really available
    assert javaLangObjectType != null;
    castableDestinationTypes.add(javaLangObjectType);

    Collections.sort(castableDestinationTypes, HasName.BY_NAME_COMPARATOR);
    return Sets.newLinkedHashSet(castableDestinationTypes);
  }

  public boolean isDualJsoInterface(JType maybeDualImpl) {
    return dualImplInterfaces.contains(maybeDualImpl.getName());
  }

  /**
   * Returns the method definition where {@code method} is first defined in a class.
   */
  public JMethod getTopMostDefinition(JMethod method) {
    if (method.getEnclosingType() instanceof JInterfaceType) {
      return null;
    }
    JMethod currentMethod = method;
    for (JMethod overriddenMethod : method.getOverriddenMethods()) {
      if (overriddenMethod.getEnclosingType() instanceof JInterfaceType) {
        continue;
      }
      if (isSuperClass((JClassType) currentMethod.getEnclosingType(),
          (JClassType) overriddenMethod.getEnclosingType())) {
        currentMethod = overriddenMethod;
      }
    }
    return currentMethod;
  }

  /**
   * Whether this type oracle has whole world knowledge or not. Monolithic compiles have whole
   * world knowledge but separate compiles know only about their immediate source and the
   * immediately referenced types.
   */
  public boolean hasWholeWorldKnowledge() {
    return hasWholeWorldKnowledge;
  }

  /**
   * True if either a JSO, or is an interface that is ONLY implemented by a JSO.
   */
  public boolean isEffectivelyJavaScriptObject(JType type) {
    if (type instanceof JReferenceType) {
      JReferenceType refType = (JReferenceType) type;
      return isJavaScriptObject(refType)
          || (isSingleJsoImpl(refType) && !isDualJsoInterface(refType));
    } else {
      return false;
    }
  }

  public boolean isJavaScriptObject(JType type) {
    if (!(type instanceof JReferenceType)) {
      return false;
    }

    type = type.getUnderlyingType();

    // TODO(dankurka): Null should not be recognized as a possible JSO.
    // Take a look on how to refactor this inside of the compiler
    if (type instanceof JNullType) {
      return true;
    }
    return isJavaScriptObject(type.getName());
  }

  // Note: This method does not account for null types and only relies on static
  // class inheritance and does not account for any changes due to optimizations.
  // Therefore this method should be kept private since callers need to be aware
  // of this semantic difference.
  private boolean isJavaScriptObject(String typeName) {
    if (typeName.equals(JProgram.JAVASCRIPTOBJECT)) {
      return true;
    }
    return isSuperClass(typeName, JProgram.JAVASCRIPTOBJECT);
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
    // any type that can be JS or exported to JS is considered instantiated
    if (isJsType(type) || hasAnyExports(type) || isJsFunction(type)) {
      return true;
    }
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

  private boolean hasAnyExports(JReferenceType type) {
    return type instanceof JDeclaredType ? ((JDeclaredType) type).hasAnyExports() : false;
  }

  public boolean isExportedField(JField field) {
    return isJsInteropEnabled() && field.isExported();
  }

  public boolean isExportedMethod(JMethod method) {
    return isJsInteropEnabled() && method.isExported();
  }

  /**
   * Returns whether the given method is exported by an @JsType annotation.
   * <p>
   * A method is a JsType method if it is a public instance method that has not been marked NoExport
   * and is in a concrete class that has been annotated @JsType or overrides some other JsType
   * method.
   */
  public boolean isJsTypeMethod(JMethod x) {
    return isJsInteropEnabled() && x.isOrOverridesJsTypeMethod();
  }

  /**
   * Returns whether the given method is directly marked with an @JsProperty annotation.
   */
  public boolean isJsPropertyMethod(JMethod x) {
    return isJsInteropEnabled() && x.isJsProperty();
  }

  /**
   * Returns whether the given field is exported by an @JsType annotation.
   * <p>
   * A field is a JsType field if it is a public instance field on a concrete class that has been
   * annotated @JsType.
   */
  public boolean isJsTypeField(JField x) {
    return isJsInteropEnabled() && x.isJsTypeMember();
  }

  public boolean isSingleJsoImpl(JType type) {
    return type instanceof JReferenceType && getSingleJsoImpl((JReferenceType) type) != null;
  }

  /**
   * Returns whether the given method may be implicitly called by a instance of a class that
   * is exported by an @JsFunction annotation.
   * <p>
   * A method is a JsFunction method if it is or overrides a SAM function of a @JsFunction annotated
   * functional interface.
   */
  public boolean isJsFunctionMethod(JMethod x) {
    return isJsInteropEnabled() && x.isOrOverridesJsFunctionMethod();
  }

  /**
   * Whether the type is a JS interface (does not check supertypes).
   */
  public boolean isJsType(JType type) {
    return isJsInteropEnabled()
        && (type instanceof JDeclaredType && ((JDeclaredType) type).isJsType());
  }

  /**
   * Whether the type or any supertypes is a JS type, optionally, only return true if
   * one of the types has a js prototype.
   */
  public boolean isOrExtendsJsType(JType type, boolean mustHavePrototype) {
    if (isJsInteropEnabled()) {
      JDeclaredType dtype = getNearestJsType(type, mustHavePrototype);
      return dtype != null;
    } else {
      return false;
    }
  }

  /**
   * Whether the type is a JsFunction interface.
   */
  public boolean isJsFunction(JType type) {
    return isJsInteropEnabled()
        && (type instanceof JInterfaceType && ((JInterfaceType) type).isJsFunction());
  }

  /**
   * Whether the type or any supertypes is a JsFunction type.
   */
  public boolean isOrExtendsJsFunction(JDeclaredType type) {
    return isJsInteropEnabled() && !getImplementedJsFunctions(type).isEmpty();
  }

  /**
   * Returns true if possibleSubType is a subclass of type, directly or indirectly.
   */
  public boolean isSubClass(JClassType type, JClassType possibleSubType) {
    return subclassesByClass.containsEntry(type.getName(), possibleSubType.getName());
  }

  public Set<String> getSubTypeNames(String typeName) {
    return Sets.union((Set<String>) subclassesByClass.get(typeName),
        (Set<String>) subInterfacesByInterface.get(typeName));
  }

  /**
   * Returns true if possibleSuperClass is a superclass of type, directly or indirectly.
   */
  public boolean isSuperClass(JClassType type, JClassType possibleSuperClass) {
    return isSuperClass(type.getName(), possibleSuperClass.getName());
  }

  /**
   * This method should be called after altering the types that are live in the
   * associated JProgram.
   */
  public void recomputeAfterOptimizations(Collection<JDeclaredType> declaredTypes) {
    Set<JDeclaredType> computed = Sets.newIdentityHashSet();

    if (hasWholeWorldKnowledge) {
      if (optimize) {
        // Optimizations that only make sense in whole world compiles:
        //   (1) minimize clinit()s.
        for (JDeclaredType type : declaredTypes) {
          computeClinitTarget(type, computed);
        }
      }

      //   (2) make JSOs singleImpl when all the Java implementors are gone.
      nextDual:
      for (Iterator<String> it = dualImplInterfaces.iterator(); it.hasNext(); ) {
        String dualIntf = it.next();
        for (String implementorName : classesByImplementingInterface.get(dualIntf)) {
          JClassType implementor = (JClassType) referenceTypesByName.get(implementorName);
          if (isInstantiatedType(implementor) && !isJavaScriptObject(implementor)) {
            // This dual is still implemented by a Java class.
            continue nextDual;
          }
        }
        // No Java implementors.
        it.remove();
      }

      //   (3) prune JSOs from jsoByInterface and dualImplInterfaces when JSO isn't live hence the
      //       interface is no longer considered to be implemented by a JSO.
      Iterator<Entry<String, String>> jit = jsoByInterface.entrySet().iterator();
      while (jit.hasNext()) {
        Entry<String, String> jsoSingleImplEntry = jit.next();
        JClassType clazz = (JClassType) referenceTypesByName.get(jsoSingleImplEntry.getValue());
        if (isInstantiatedType(clazz)) {
          continue;
        }
        dualImplInterfaces.remove(jsoSingleImplEntry.getKey());
        jit.remove();
      }
    }
  }

  public void setInstantiatedTypes(Set<JReferenceType> instantiatedTypes) {
    this.instantiatedTypes = instantiatedTypes;
    methodsBySignatureForType.keySet().retainAll(instantiatedTypes);
  }

  private void deleteImmediateTypeRelations(final Collection<String> typeNames) {
    Predicate<Entry<String, String>> inToDeleteSet =
        new Predicate<Entry<String, String>>() {
          @Override
          public boolean apply(Entry<String, String> typeTypeEntry) {
            // Only remove data from the index that can be regenerated by processing this type.
            return typeNames.contains(typeTypeEntry.getKey());
          }
        };

    Maps.filterEntries(immediateTypeRelations.immediateSuperclassesByClass, inToDeleteSet).clear();
    Multimaps.filterEntries(immediateTypeRelations.immediateImplementedInterfacesByClass,
        inToDeleteSet).clear();
    Multimaps.filterEntries(immediateTypeRelations.immediateSuperInterfacesByInterface,
        inToDeleteSet).clear();
  }

  private void recordImmediateTypeRelations(Iterable<JDeclaredType> types) {
    for (JReferenceType type : types) {
      if (type instanceof JClassType) {
        JClassType jClassType = (JClassType) type;
        // Record immediate super class
        JClassType superClass = jClassType.getSuperClass();
        if (superClass != null) {
          immediateTypeRelations.immediateSuperclassesByClass.put(jClassType.getName(),
              superClass.getName());
        }

        // Record immediately implemented interfaces.
        immediateTypeRelations.immediateImplementedInterfacesByClass
            .putAll(type.getName(), Iterables.transform(jClassType.getImplements(), TYPE_TO_NAME));
      } else if (type instanceof JInterfaceType) {

        JInterfaceType currentIntf = (JInterfaceType) type;
        // Record immediate super interfaces.
        immediateTypeRelations.immediateSuperInterfacesByInterface
            .putAll(type.getName(), Iterables.transform(currentIntf.getImplements(), TYPE_TO_NAME));
      }
    }
  }

  private void computeExtendedTypeRelations() {
    computeAllClasses();
    computeClassMaps();
    computeInterfaceMaps();
    computeImplementsMaps();
    computePotentialImplementMap();
    computeSingleJSO();
    computeDualJSO();
  }

  private void computeAllClasses() {
    allClasses.clear();
    allClasses.addAll(immediateTypeRelations.immediateSuperclassesByClass.values());
    allClasses.addAll(immediateTypeRelations.immediateSuperclassesByClass.keySet());
  }

  private void computePotentialImplementMap() {
    // Compute the reflexive subclass closure.
    Multimap<String, String> reflexiveSubtypes = HashMultimap.create();
    reflexiveSubtypes.putAll(subclassesByClass);
    reflexiveClosure(reflexiveSubtypes, allClasses);

    potentialInterfaceByClass =
        ImmutableSetMultimap.copyOf(compose(reflexiveSubtypes, implementedInterfacesByClass));
  }

  private void computeDualJSO() {
    dualImplInterfaces.clear();
    // Create dual mappings for any jso interface with a Java implementor.
    for (String jsoIntfName : jsoByInterface.keySet()) {
      for (String implementor : classesByImplementingInterface.get(jsoIntfName)) {
        if (!hasWholeWorldKnowledge || !isJavaScriptObject(implementor)) {
          // Assume always dualImpl for separate compilation. Due to the nature of separate
          // compilation, the compiler can not know if a specific interface is implemented in a
          // different module unless it is a monolithic whole world compile.
          // TODO(rluble): Jso devirtualization should be an normalization pass before optimization
          // JTypeOracle should be mostly unaware of JSOs.
          dualImplInterfaces.add(jsoIntfName);
          break;
        }
      }
    }
  }

  private void computeImplementsMaps() {
    // Construct the immediate supertype relation.
    Multimap<String, String> superTypesByType = HashMultimap.create();
    superTypesByType.putAll(immediateTypeRelations.immediateImplementedInterfacesByClass);
    superTypesByType.putAll(Multimaps.forMap(immediateTypeRelations.immediateSuperclassesByClass));
    superTypesByType.putAll(immediateTypeRelations.immediateSuperInterfacesByInterface);

    Multimap<String, String> superTypesByTypeClosure = transitiveClosure(superTypesByType);

    // Remove interfaces from keys and classes from values.
    implementedInterfacesByClass = ImmutableSetMultimap.copyOf(
        Multimaps.filterEntries(superTypesByTypeClosure,
            new Predicate<Entry<String, String>>() {
              @Override
              public boolean apply(Entry<String, String> typeTypeEntry) {
                // Only keep classes as keys and interfaces as values.
                return allClasses.contains(typeTypeEntry.getKey()) &&
                    !allClasses.contains(typeTypeEntry.getValue());
              }
            }));

    classesByImplementingInterface =
        ImmutableSetMultimap.copyOf(inverse(implementedInterfacesByClass));
  }

  private void computeSingleJSO() {
    jsoByInterface.clear();

    for (String jsoSubType : subclassesByClass.get(JProgram.JAVASCRIPTOBJECT)) {
      for (String intf :
          immediateTypeRelations.immediateImplementedInterfacesByClass.get(jsoSubType)) {
        jsoByInterface.put(intf, jsoSubType);
        for (String superIntf : superInterfacesByInterface.get(intf)) {
          if (!jsoByInterface.containsKey(superIntf)) {
            jsoByInterface.put(superIntf, jsoSubType);
          }
        }
      }
    }
  }

  private void computeClassMaps() {
    superclassesByClass = ImmutableSetMultimap.copyOf(
        transitiveClosure(Multimaps.forMap(immediateTypeRelations.immediateSuperclassesByClass)));
    subclassesByClass = ImmutableSetMultimap.copyOf(inverse(superclassesByClass));
  }

  private void computeInterfaceMaps() {
    superInterfacesByInterface = ImmutableSetMultimap.copyOf(
        transitiveClosure(immediateTypeRelations.immediateSuperInterfacesByInterface));
    subInterfacesByInterface  = ImmutableSetMultimap.copyOf(inverse(superInterfacesByInterface));
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
          computeClinitTargetRecursive(type, computed, Sets.<JDeclaredType>newIdentityHashSet());
      type.setClinitTarget(target);
    }
    computed.add(type);
  }

  private JDeclaredType computeClinitTargetRecursive(JDeclaredType type,
      Set<JDeclaredType> computed, Set<JDeclaredType> alreadySeen) {
    // Track that we've been seen.
    alreadySeen.add(type);

    JMethod method = type.getClinitMethod();
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

  private JReferenceType ensureTypeExistsAndAppend(String typeName, List<JReferenceType> types) {
    JReferenceType type = referenceTypesByName.get(typeName);
    assert type != null;
    types.add(type);
    return type;
  }

  /**
   * Returns true if type extends the interface represented by qType, either
   * directly or indirectly.
   */
  private boolean extendsInterface(JInterfaceType type, JInterfaceType qType) {
    return superInterfacesByInterface.containsEntry(type.getName(), qType.getName());
  }

  /**
   * Returns an iterable set of types for the given iterable set of type names.
   * <p>
   * Incremental builds will not have all type instances available, so users of this function should
   * be careful to only use it when they know that their expected types will be loaded.
   */
  private Iterable<JReferenceType> getTypes(Iterable<String> typeNameSet) {
    return Iterables.transform(typeNameSet,
        new Function<String, JReferenceType>() {
          @Override
          public JReferenceType apply(String typeName) {
            JReferenceType referenceType = referenceTypesByName.get(typeName);
            assert referenceType != null;
            return referenceType;
          }
        });
  }

  private <K, K2, V> Multimap<K2, V> getOrCreateMultimap(Map<K, Multimap<K2, V>> map, K key) {
    Multimap<K2, V> multimap = map.get(key);
    if (multimap == null) {
      multimap = HashMultimap.create();
      map.put(key, multimap);
    }
    return multimap;
  }

  private Map<String, JMethod> getOrCreateInstanceMethodsBySignatureForType(JClassType type) {
    Map<String, JMethod> methodsBySignature = methodsBySignatureForType.get(type);
    if (methodsBySignature == null) {
      methodsBySignature = Maps.newHashMap();
      JClassType superClass = type.getSuperClass();
      Map<String, JMethod> parentMethods = superClass == null
          ? Collections.<String, JMethod>emptyMap()
          : getOrCreateInstanceMethodsBySignatureForType(type.getSuperClass());

      // Add inherited methods.
      for (JMethod method : parentMethods.values()) {
        if (method.canBePolymorphic()) {
          methodsBySignature.put(method.getSignature(), method);
        }
      }

      // Add all of our own non-static methods.
      for (JMethod method : type.getMethods()) {
        if (!method.isStatic()) {
          methodsBySignature.put(method.getSignature(), method);
        }
      }

      methodsBySignatureForType.put(type, methodsBySignature);
    }
    return methodsBySignature;
  }

  /**
   * Computes the reflexive closure of a relation.
   */
  private void reflexiveClosure(Multimap<String, String> relation, Iterable<String> domain) {
    for (String element : domain) {
      relation.put(element, element);
    }
  }

  /**
   * Computes the transitive closure of a relation.
   */
  private Multimap<String, String> transitiveClosure(Multimap<String, String> relation) {
    Multimap<String, String> transitiveClosure = HashMultimap.create();
    Set<String> domain = Sets.newHashSet(relation.keySet());
    domain.addAll(relation.values());
    for (String element : domain) {
      expandTransitiveClosureForElement(relation, element, transitiveClosure);
    }
    return transitiveClosure;
  }

  /**
   * Expands {@code transitiveClosure} to contain the transitive closure of {@code relation}
   * restricted to an element.
   */
  private Collection<String> expandTransitiveClosureForElement(Multimap<String, String> relation,
      String element, Multimap<String, String> transitiveClosure) {
    // This algorithm computes the transitive closure of an relation via
    // dynamic programming.

    Collection<String> preComputedExpansion = transitiveClosure.get(element);

    if (!preComputedExpansion.isEmpty()) {
      // already computed.
      return preComputedExpansion;
    }

    Set<String> transitiveExpansion = Sets.newHashSet();
    Collection<String> immediateSuccessors = relation.get(element);
    transitiveExpansion.addAll(immediateSuccessors);

    for (String child : immediateSuccessors) {
      transitiveExpansion.addAll(expandTransitiveClosureForElement(relation, child,
          transitiveClosure));
    }
    transitiveClosure.putAll(element, transitiveExpansion);
    return transitiveExpansion;
  }

  /**
   * Given two binary relations {@code f} and {@code g} represented as multimaps computes the
   * relational composition, i.e. (a,c) is in (f.g) iif (a,b) is in f and (b,c) is in g.
   */
  private <A, B, C> Multimap<A, C> compose(Multimap<A, B> f, Multimap<B, C> g) {
    Multimap<A, C> composition = HashMultimap.create();
    for (A a : f.keySet()) {
      for (B b : f.get(a)) {
        composition.putAll(a, g.get(b));
      }
    }
    return composition;
  }

  /**
   * Given a binary relation {@code relation} represented as a multimap computes the relational
   * inverse; i.e. (a,b) is in inverse(relation) iff (b,a) is in relation.
   */
  private <K, V> Multimap<V, K> inverse(Multimap<K, V> relation) {
    Multimap<V, K> inverse = HashMultimap.create();
    Multimaps.invertFrom(relation, inverse);
    return inverse;
  }

  /**
   * Returns true if type implements the interface represented by qType, either
   * directly or indirectly.
   */
  private boolean implementsInterface(JClassType type, JInterfaceType qType) {
    return implementedInterfacesByClass.containsEntry(type.getName(), qType.getName());
  }

  private boolean isSuperClass(String type, String qType) {
    return subclassesByClass.containsEntry(qType, type);
  }
}
