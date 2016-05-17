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
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
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
      return requiredTypes;
    }

    private String javaIoSerializable;

    private String javaLangCloneable;

    private String javaLangObject;
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

    private final Set<JDeclaredType> clinitTargets = Sets.newLinkedHashSet();

    /**
     * Tracks whether any live code is run in this clinit. This is only reliable
     * because we explicitly visit all AST structures that might contain
     * non-clinit-calling code.
     *
     * @see #mightContainOnlyClinitCalls(JExpression)
     * @see #mightContainOnlyClinitCallsOrDeclarationStatements(JStatement)
     */
    private boolean hasLiveCode = false;

    public Set<JDeclaredType> getClinitTargets() {
      return clinitTargets;
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
        assert field.isStatic();
        // {@See ControlFlowAnalizer.rescue(JVariable var)
        if (field.getLiteralInitializer() != null) {
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

  private boolean optimize = true;

  private ImmediateTypeRelations immediateTypeRelations;
  private ArrayTypeCreator arrayTypeCreator;
  private StandardTypes standardTypes;

  /**
   * Constructs a new JTypeOracle.
   */
  public JTypeOracle(ArrayTypeCreator arrayTypeCreator, MinimalRebuildCache minimalRebuildCache) {
    this.immediateTypeRelations = minimalRebuildCache.getImmediateTypeRelations();
    this.arrayTypeCreator = arrayTypeCreator;

    // Be ready to answer simple questions (type hierarchy) even before recompute...().
    computeExtendedTypeRelations();
  }

  /**
   * True if the type is a JSO or interface implemented by JSO or a JsType without
   * prototype.
   */
  public boolean canBeJavaScriptObject(JType type) {
    type = type.getUnderlyingType();
    return type.isJsoType() || isSingleJsoImpl(type);
  }

  public static boolean isNoOpCast(JType type) {
    return type instanceof JInterfaceType && type.isJsNative();
  }

  private boolean isJsInteropCrossCastTarget(JType type) {
    return type.isJsNative() || type.isJsFunction();
  }

  public boolean castFailsTrivially(JReferenceType fromType, JReferenceType toType) {
    if (!fromType.canBeNull() && toType.isNullType()) {
      // Cannot cast non-nullable to null
      return true;
    }

    if (isJsInteropCrossCastTarget(toType.getUnderlyingType())
        || isJsInteropCrossCastTarget(fromType.getUnderlyingType())) {
      return false;
    }

    if (!fromType.canBeSubclass() && fromType.getUnderlyingType() instanceof JClassType &&
        fromType.getUnderlyingType() != toType.getUnderlyingType() &&
        !isSuperClass(fromType, toType) && !implementsInterface(fromType, toType)) {
      // An exact type can only be cast to any of its supers or itself.
      return true;
    }

    // Compare the underlying types.
    fromType = fromType.getUnderlyingType();
    toType = toType.getUnderlyingType();

    if (fromType == toType || isJavaLangObject(fromType)) {
      return false;
    }

    /**
     * Cross-cast allowed in theory, prevents TypeTightener from turning
     * cross-casts into null-casts.
     */
    if (canBeJavaScriptObject(fromType) && canBeJavaScriptObject(toType)) {
      return false;
    }

    // TODO (cromwellian): handle case where types S and T have identical Js Prototypes
    if (castSucceedsTrivially(fromType, toType)) {
      return false;
    }

    if (fromType instanceof JArrayType) {

      JArrayType fromArrayType = (JArrayType) fromType;
      if (toType instanceof JArrayType) {
        JArrayType toArrayType = (JArrayType) toType;
        JType fromLeafType = fromArrayType.getLeafType();
        JType toLeafType = toArrayType.getLeafType();
        int fromDims = fromArrayType.getDims();
        int toDims = toArrayType.getDims();

        // null[] or Object[] -> int[][] might work, other combinations won't
        if (fromDims < toDims && !isJavaLangObject(fromLeafType)
            && !fromLeafType.isNullType()) {
          return true;
        }

        if (fromDims == toDims &&
          fromLeafType instanceof JReferenceType && toLeafType instanceof JReferenceType) {
          return castFailsTrivially((JReferenceType) fromLeafType, (JReferenceType) toLeafType);
        }
      }

      /*
       * Warning: If this code is ever updated to consider casts of array types
       * to interface types, then be sure to consider that casting an array to
       * Serializable and Cloneable succeeds. Currently all casts of an array to
       * an interface return true, which is overly conservative but is safe.
       */
    } else if (fromType instanceof JClassType) {

      JClassType cType = (JClassType) fromType;
      if (toType instanceof JClassType) {
        return !isSubClass(cType, (JClassType) toType);
      } else if (toType instanceof JInterfaceType) {
        return !potentialInterfaceByClass.containsEntry(cType.getName(), toType.getName());
      }
    } else if (fromType instanceof JInterfaceType) {
      JInterfaceType fromInterfaceType = (JInterfaceType) fromType;
      if (toType instanceof JClassType) {
        return !potentialInterfaceByClass.containsEntry(
            toType.getName(), fromInterfaceType.getName());
      }
    }

    return false;
  }

  public boolean castSucceedsTrivially(JReferenceType fromType, JReferenceType toType) {
    if (fromType.canBeNull() && !toType.canBeNull()) {
      // Cannot cast nullable to non-nullable
      return false;
    }

    if (fromType.isNullType()) {
      assert toType.canBeNull();
      // null can be cast to any nullable type.
      return true;
    }

    if (toType.weakenToNullable() == fromType.weakenToNullable()) {
      // These are either the same exact types or same inexact types.
      return true;
    }

    if (!toType.canBeSubclass()) {
      return false;
    }

    // Compare the underlying types.
    fromType = fromType.getUnderlyingType();
    toType = toType.getUnderlyingType();

    if (fromType == toType) {
      return true;
    }

    if (isJavaLangObject(toType)) {
      return true;
    }

    if (fromType instanceof JArrayType) {
      return castSucceedsTrivially((JArrayType) fromType, toType);
    }


    return isSuperClassOrInterface(fromType, toType);
  }

  private boolean castSucceedsTrivially(JArrayType fromArrayType, JReferenceType toType) {
    // Arrays can only be cast to object, serializable, clonable or some array type.

    // casting to objects is handled by the caller.
    assert !isJavaLangObject(toType);

    if (isArrayInterface(toType)) {
      return true;
    }

    if (!(toType instanceof JArrayType)) {
      return false;
    }

    JArrayType toArrayType = (JArrayType) toType;
    JType fromLeafType = fromArrayType.getLeafType();
    JType toLeafType = toArrayType.getLeafType();
    int fromDims = fromArrayType.getDims();
    int toDims = toArrayType.getDims();

    // int[][] -> Object[], Serializable[], Clonable[] or null[] trivially true
    if (fromDims > toDims
        && (isJavaLangObject(toLeafType)
        || isArrayInterface(toLeafType)
        || toLeafType.isNullType())) {
      return true;
    }

    if (fromDims != toDims) {
      return false;
    }

    // fromDims == toDims.
    if (fromLeafType instanceof JReferenceType && toLeafType instanceof JReferenceType) {
      return castSucceedsTrivially((JReferenceType) fromLeafType, (JReferenceType) toLeafType);
    }

    return false;
  }

  public boolean castSucceedsTrivially(JType fromType, JType toType) {
    if (fromType.isPrimitiveType() && toType.isPrimitiveType()) {
      return fromType == toType;
    }
    if (fromType instanceof JReferenceType && toType instanceof JReferenceType) {
      return castSucceedsTrivially((JReferenceType) fromType, (JReferenceType) toType);
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

  public JMethod getInstanceMethodBySignature(JClassType type, String signature) {
    return getOrCreateInstanceMethodsBySignatureForType(type).get(signature);
  }

  public JMethod findMostSpecificOverride(JClassType type, JMethod baseMethod) {
    JMethod foundMethod = getInstanceMethodBySignature(type, baseMethod.getSignature());
    if (foundMethod == baseMethod) {
      return foundMethod;
    }

    // A method with the same signature as the target method might NOT override if the original
    // method is package private and found method is defined in a different package.
    if (foundMethod != null && foundMethod.getOverriddenMethods().contains(baseMethod)) {
      return foundMethod;
    }

    // In the case that a method is found but is not an override (package private case), traverse
    // up in the hierarchy looking for the right override.
    if (foundMethod != null && baseMethod.isPackagePrivate() &&
        type.getSuperClass() != null) {
      return findMostSpecificOverride(type.getSuperClass(), baseMethod);
    }

    assert baseMethod.isAbstract();
    return baseMethod;
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

      if (arrayType.getLeafType().isPrimitiveType()) {
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
    // Do not add itself if it is a JavaScriptObject subclass, add JavaScriptObject.
    if (type.isJsoType()) {
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
   * True if either a JSO, or is an interface that is ONLY implemented by a JSO.
   */
  public boolean isEffectivelyJavaScriptObject(JType type) {
    return type.isJsoType() || (isSingleJsoImpl(type) && !isDualJsoInterface(type));
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

    if (type.isNullType()) {
      return true;
    } else if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      if (arrayType.getLeafType().isNullType()) {
        return true;
      }
    }

    return false;
  }

  private boolean isArrayInterface(JType type) {
    return type.getName().equals(standardTypes.javaIoSerializable)
        || type.getName().equals(standardTypes.javaLangCloneable);
  }

  private boolean isJavaLangObject(JType type) {
    if (!(type instanceof JClassType)) {
      return false;
    }
    JClassType classType = (JClassType) type;

    // java.lang.Object is the only class that does not have a superclass.
    assert classType.getSuperClass() == null ==
        classType.getName().equals(standardTypes.javaLangObject);

    return classType.getSuperClass() == null;
  }

  public boolean isSingleJsoImpl(JType type) {
    return type instanceof JReferenceType && getSingleJsoImpl((JReferenceType) type) != null;
  }

  /**
   * Returns true if possibleSubType is a subclass of type, directly or indirectly.
   */
  public boolean isSubClass(JClassType type, JClassType possibleSubType) {
    return subclassesByClass.containsEntry(type.getName(), possibleSubType.getName());
  }

  public boolean isSubType(JDeclaredType type, JDeclaredType possibleSubType) {
    return subclassesByClass.containsEntry(type.getName(), possibleSubType.getName())
        || classesByImplementingInterface.containsEntry(type.getName(), possibleSubType.getName())
        || subInterfacesByInterface.containsEntry(type.getName(), possibleSubType.getName());
  }

  public Iterable<String> getSubTypeNames(String typeName) {
    return Iterables.concat(classesByImplementingInterface.get(typeName),
        subclassesByClass.get(typeName), subInterfacesByInterface.get(typeName));
  }

  public Set<String> getSubClassNames(String typeName) {
    return (Set<String>) subclassesByClass.get(typeName);
  }

  public Set<String> getSubInterfaceNames(String typeName) {
    return (Set<String>) subInterfacesByInterface.get(typeName);
  }

  /**
   * Returns true if possibleSuperClass is a superclass of type, directly or indirectly.
   */
  public boolean isSuperClass(JReferenceType type, JReferenceType possibleSuperClass) {
    return isSuperClass(type.getName(), possibleSuperClass.getName());
  }

  public boolean isSuperClassOrInterface(JReferenceType fromType, JReferenceType toType) {
    return isSuperClass(fromType, toType) || implementsInterface(fromType, toType)
        || extendsInterface(fromType, toType);
  }

  /**
   * This method should be called after altering the types that are live in the
   * associated JProgram.
   */
  public void recomputeAfterOptimizations(Collection<JDeclaredType> declaredTypes) {
    Set<JDeclaredType> computed = Sets.newIdentityHashSet();
    assert optimize;

    // Optimizations that only make sense in whole world compiles:
    //   (1) minimize clinit()s.
    for (JDeclaredType type : declaredTypes) {
      computeClinitTarget(type, computed);
    }
    //   (2) make JSOs singleImpl when all the Java implementors are gone.
    nextDual:
    for (Iterator<String> it = dualImplInterfaces.iterator(); it.hasNext(); ) {
      String dualIntf = it.next();
      for (String implementorName : classesByImplementingInterface.get(dualIntf)) {
        JClassType implementor = (JClassType) referenceTypesByName.get(implementorName);
        assert implementor != null;
        if (isInstantiatedType(implementor) && !implementor.isJsoType()) {
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
        if (!isJavaScriptObject(implementor)) {
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
    Collection<JDeclaredType> clinitTargets = v.getClinitTargets();
    if (clinitTargets.size() == 1) {
      JDeclaredType singleTarget = clinitTargets.iterator().next();
      if (isSuperClass(type, singleTarget)) {
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
   * Returns true if type extends the interface represented by qType, either
   * directly or indirectly.
   */
  private boolean extendsInterface(JReferenceType type, JReferenceType qType) {
    return superInterfacesByInterface.containsEntry(type.getName(), qType.getName());
  }

  /**
   * Returns true if type implements the interface represented by interfaceType, either
   * directly or indirectly.
   */
  private boolean implementsInterface(JReferenceType type, JReferenceType interfaceType) {
    return implementedInterfacesByClass.containsEntry(type.getName(), interfaceType.getName());
  }

  private boolean isSuperClass(String type, String potentialSuperClass) {
    return subclassesByClass.containsEntry(potentialSuperClass, type);
  }
}
