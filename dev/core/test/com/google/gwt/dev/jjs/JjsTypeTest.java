/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.dev.jjs;

import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JTypeOracle.StandardTypes;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Test basic operations on the types used by the JJS compiler. See
 * {@link com.google.gwt.dev.jjs.ast.JType}.
 */
public class JjsTypeTest extends TestCase {
  private JArrayType arrayOfA;
  private JArrayType arrayOfArrayOfB;
  private JReferenceType arrayOfArrayOfInt;
  private JArrayType arrayOfB;
  private JArrayType arrayOfBase;
  private JArrayType arrayOfBSub;
  private JArrayType arrayOfC;
  private JReferenceType arrayOfInt;
  private JArrayType arrayOfIntfI;
  private JArrayType arrayOfIntfIBase;
  private JArrayType arrayOfObject;
  private JClassType classA;
  private JClassType classArrayList;
  private JClassType classB;
  private JClassType classBase;
  private JNonNullType classBaseNn;
  private JNonNullType classBnn;
  private JClassType classBSub;
  private JClassType classC;
  private JClassType classJso;
  private JClassType classJso1;
  private JClassType classJso2;
  private JClassType classObject;
  private JClassType classString;
  private JReferenceType intfCloneable;
  private JInterfaceType intfCollection;
  private JInterfaceType intfI;
  private JInterfaceType intfIBase;
  private JInterfaceType intfIterable;
  private JInterfaceType intfJ;
  private JInterfaceType intfK;
  private JInterfaceType intfList;
  private JReferenceType intfSerializable;
  private JProgram program;
  private SourceInfo synthSource;
  private JReferenceType typeNull;
  private JTypeOracle typeOracle;
  private JArrayType arrayOfArrayOfBase;
  private JArrayType arrayOfArrayOfObject;
  private JArrayType arrayOfArrayOfIntfI;
  private JArrayType arrayOfArrayOfIntfIBase;

  public void testCanTheoreticallyCast() {
    assertFalse(typeOracle.canTheoreticallyCast(classBnn, typeNull));

    assertTrue(typeOracle.canTheoreticallyCast(classBSub, classB));
    assertTrue(typeOracle.canTheoreticallyCast(classB, classBSub));

    assertTrue(typeOracle.canTheoreticallyCast(classB, classBnn));
    assertTrue(typeOracle.canTheoreticallyCast(classBnn, classB));

    assertTrue(typeOracle.canTheoreticallyCast(classB, classB));

    assertTrue(typeOracle.canTheoreticallyCast(classObject, arrayOfB));
    assertFalse(typeOracle.canTheoreticallyCast(arrayOfA, arrayOfArrayOfB));

    assertTrue(typeOracle.canTheoreticallyCast(arrayOfObject, arrayOfArrayOfB));

    assertTrue(typeOracle.canTheoreticallyCast(arrayOfB, arrayOfBSub));

    assertTrue(typeOracle.canTheoreticallyCast(classBase, intfI));
    assertFalse(typeOracle.canTheoreticallyCast(classA, intfJ));

    assertTrue(typeOracle.canTheoreticallyCast(intfIBase, intfI));

    assertTrue(typeOracle.canTheoreticallyCast(intfIBase, classBase));
    assertFalse(typeOracle.canTheoreticallyCast(intfJ, classA));

    assertTrue(typeOracle.canTheoreticallyCast(arrayOfA, intfSerializable));
    assertTrue(typeOracle.canTheoreticallyCast(intfSerializable, arrayOfA));

    assertTrue(typeOracle.canTheoreticallyCast(arrayOfA, intfCloneable));
    assertTrue(typeOracle.canTheoreticallyCast(intfCloneable, arrayOfA));

    assertTrue(typeOracle.canTheoreticallyCast(intfList, intfIterable));
    assertTrue(typeOracle.canTheoreticallyCast(classArrayList, intfIterable));
  }

  public void testCanTriviallyCast() {
    assertTrue(typeOracle.canTriviallyCast(classB, classB));

    assertTrue(typeOracle.canTriviallyCast(classBSub, classB));
    assertFalse(typeOracle.canTriviallyCast(classB, classBSub));

    assertFalse(typeOracle.canTriviallyCast(classC, classA));
    assertFalse(typeOracle.canTriviallyCast(classA, classC));

    assertTrue(typeOracle.canTriviallyCast(classB, intfI));
    assertFalse(typeOracle.canTriviallyCast(intfI, classB));

    assertTrue(typeOracle.canTriviallyCast(classB, classObject));
    assertFalse(typeOracle.canTriviallyCast(classObject, classB));

    assertTrue(typeOracle.canTriviallyCast(classB, intfI));
    assertFalse(typeOracle.canTriviallyCast(intfI, classB));

    assertTrue(typeOracle.canTriviallyCast(classBnn, classB));
    assertFalse(typeOracle.canTriviallyCast(classB, classBnn));

    assertTrue(typeOracle.canTriviallyCast(typeNull, classB));
    assertFalse(typeOracle.canTriviallyCast(classB, typeNull));

    assertTrue(typeOracle.canTriviallyCast(arrayOfBSub, arrayOfB));
    assertFalse(typeOracle.canTriviallyCast(arrayOfB, arrayOfBSub));

    assertFalse(typeOracle.canTriviallyCast(arrayOfA, arrayOfB));
    assertFalse(typeOracle.canTriviallyCast(arrayOfB, arrayOfA));

    assertFalse(typeOracle.canTriviallyCast(arrayOfArrayOfB, arrayOfB));
    assertFalse(typeOracle.canTriviallyCast(arrayOfB, arrayOfArrayOfB));

    assertTrue(typeOracle.canTriviallyCast(arrayOfArrayOfB, arrayOfObject));
    assertFalse(typeOracle.canTriviallyCast(arrayOfObject, arrayOfArrayOfB));

    assertTrue(typeOracle.canTheoreticallyCast(classJso1, classJso2));
    assertTrue(typeOracle.canTheoreticallyCast(classJso2, classJso1));

    assertTrue(typeOracle.canTheoreticallyCast(classJso1, intfK));
    assertTrue(typeOracle.canTheoreticallyCast(intfK, classJso1));

    assertTrue(typeOracle.canTheoreticallyCast(intfJ, intfK));
    assertTrue(typeOracle.canTheoreticallyCast(intfK, intfJ));

    assertTrue(typeOracle.canTriviallyCast(classJso1, classJso));

    assertTrue(typeOracle.canTriviallyCast(arrayOfA, intfSerializable));
    assertFalse(typeOracle.canTriviallyCast(intfSerializable, arrayOfA));

    assertTrue(typeOracle.canTriviallyCast(arrayOfA, intfCloneable));
    assertFalse(typeOracle.canTriviallyCast(intfCloneable, arrayOfA));

    /*
     * Test that two types cannot both be trivially castable to each other,
     * unless they are the same type.
     */
    for (JReferenceType type1 : severalTypes()) {
      for (JReferenceType type2 : severalTypes()) {
        if (type1 != type2) {
          assertFalse(typeOracle.canTriviallyCast(type1, type2)
              && typeOracle.canTriviallyCast(type2, type1));
        }
      }
    }
  }

  public void testGeneralizeTypes() {
    assertSame(classA, generalizeTypes(classA, classA));
    assertSame(classB, generalizeTypes(classB, classBnn));
    assertSame(classB, generalizeTypes(classBnn, classB));
    assertSame(classBaseNn, generalizeTypes(classBnn, classBaseNn));
    assertSame(classB, generalizeTypes(classB, typeNull));
    assertSame(classB, generalizeTypes(typeNull, classB));

    assertSame(intfIBase, generalizeTypes(intfI, intfIBase));
    assertSame(intfIBase, generalizeTypes(intfIBase, intfI));
    assertSame(classObject, generalizeTypes(intfJ, intfI));

    assertSame(classObject, generalizeTypes(arrayOfB, arrayOfInt));
    assertSame(classObject, generalizeTypes(arrayOfC, arrayOfArrayOfB));
    assertSame(arrayOfObject, generalizeTypes(arrayOfC, arrayOfB));
    assertSame(arrayOfObject, generalizeTypes(arrayOfObject, arrayOfArrayOfInt));

    assertSame(intfI, generalizeTypes(classB, intfI));
    assertSame(classObject, generalizeTypes(classB, intfJ));

    assertSame(classObject, generalizeTypes(intfI, arrayOfInt));

    assertSame(intfSerializable, generalizeTypes(intfSerializable, arrayOfA));
    assertSame(intfCloneable, generalizeTypes(intfCloneable, arrayOfA));

    for (JReferenceType type1 : severalTypes()) {
      for (JReferenceType type2 : severalTypes()) {
        JReferenceType generalized = generalizeTypes(type1, type2);
        assertTrue(typeOracle.canTriviallyCast(type1, generalized));
        assertTrue(typeOracle.canTriviallyCast(type2, generalized));
      }
    }
  }

  public void testGetSuperHierarchy() {
    assertSuperHierarchy(classObject, classObject);
    assertSuperHierarchy(classString, classString, classObject);
    assertSuperHierarchy(classJso, classJso, classObject);
    assertSuperHierarchy(intfSerializable, intfSerializable, classObject);
    assertSuperHierarchy(intfCloneable, intfCloneable, classObject);
    assertSuperHierarchy(intfIBase, intfIBase, classObject);
    assertSuperHierarchy(intfI, intfI, intfIBase, classObject);
    assertSuperHierarchy(intfJ, intfJ, classObject);
    assertSuperHierarchy(intfK, intfK, classObject);
    assertSuperHierarchy(classBase, classBase, classObject);
    assertSuperHierarchy(classA, classA, classObject, classBase);
    assertSuperHierarchy(classB, classB, classObject, classBase, intfIBase, intfI);
    assertSuperHierarchy(classC, classC, classObject, intfIBase, intfI);
    assertSuperHierarchy(classBSub, classBSub, classObject, classBase, classB, intfIBase, intfI);
    assertSuperHierarchy(classJso1, classJso1, classObject, classJso, intfJ);
    assertSuperHierarchy(classJso2, classJso2, classObject, classJso, intfK);
    assertSuperHierarchy(intfIterable, intfIterable, classObject);
    assertSuperHierarchy(intfCollection, intfCollection, intfIterable, classObject);
    assertSuperHierarchy(intfList, intfList, intfIterable, intfCollection, classObject);
    assertSuperHierarchy(classArrayList, classArrayList, intfList, classObject, intfIterable,
        intfCollection);
    assertSuperHierarchy(arrayOfB, arrayOfB, arrayOfBase, arrayOfObject, arrayOfIntfI,
        arrayOfIntfIBase, classObject, arrayOfObject, intfCloneable, intfSerializable);
    assertSuperHierarchy(arrayOfArrayOfB, arrayOfArrayOfB, arrayOfArrayOfBase, arrayOfArrayOfObject,
        arrayOfArrayOfIntfI, arrayOfArrayOfIntfIBase, classObject, arrayOfObject, intfCloneable,
        intfSerializable);
    assertSuperHierarchy(arrayOfInt, arrayOfInt, classObject, intfCloneable, intfSerializable);

    intfCollection = createInterface("java.util.Collection");
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(classArrayList, intfList, intfCollection, classObject),
        Sets.<JDeclaredType> newHashSet(intfIterable));

    assertSuperHierarchy(intfCollection, intfCollection, classObject);
    assertSuperHierarchy(intfList, intfList, intfCollection, classObject);
    assertSuperHierarchy(classArrayList, classArrayList, intfList, classObject,
        intfCollection);

    classA = createClass("A", classObject, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(classA, classObject),
        Sets.<JDeclaredType> newHashSet(classBase));
    assertSuperHierarchy(classA, classA, classObject);

    JClassType classASub = createClass("ASub", classA, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(classASub, classA, classObject),
        Sets.<JDeclaredType> newHashSet());
    assertSuperHierarchy(classASub, classASub, classA, classObject);
  }

  public void testJavahSignatures() {
    for (JReferenceType type : severalTypes()) {
      if (!(type instanceof JNullType)) {
        assertEquals(type.getJavahSignatureName(), type.getNonNull().getJavahSignatureName());
      }
    }
  }

  public void testJsniSignatures() {
    for (JReferenceType type : severalTypes()) {
      if (!(type instanceof JNullType)) {
        assertEquals(type.getJsniSignatureName(), type.getNonNull().getJsniSignatureName());
      }
    }
  }

  public void testStrongerType() {
    assertSame(classA, program.strongerType(classA, classA));
    assertSame(classBnn, program.strongerType(classB, classBnn));
    assertSame(classB, program.strongerType(classB, classBase));
    assertSame(classB, program.strongerType(classBase, classB));
    assertSame(intfI, program.strongerType(intfI, intfJ));
    assertSame(arrayOfA, program.strongerType(intfSerializable, arrayOfA));
    assertSame(arrayOfA, program.strongerType(intfCloneable, arrayOfA));
  }

  public void testUpdateTypeInformation_isJavaScriptObject() {
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes());

    JClassType jso = createClass("SomeJSO", classJso, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(jso),
        Collections.<JDeclaredType> emptySet());
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jso));
    program.typeOracle.updateImmediateTypeRelations(
        Collections.<JDeclaredType> emptySet(),
        Sets.<JDeclaredType> newHashSet(jso));
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jso));

    jso = createClass("SomeJSO", classJso, false, false);
    JClassType jsoChild = createClass("SomeJSOChild", jso, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(jso, jsoChild),
        Collections.<JDeclaredType> emptySet());
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jsoChild));
    jsoChild = createClass("SomeJSOChild", classObject, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(jsoChild),
        Collections.<JDeclaredType> emptySet());
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jsoChild));

    jso = createClass("SomeJSO", classJso, false, false);
    jsoChild = createClass("SomeJSOChild", jso, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(jsoChild),
        Collections.<JDeclaredType> emptySet());
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jsoChild));
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jso));
    jso = createClass("SomeJSO", classObject, false, false);
    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType> newHashSet(jso),
        Collections.<JDeclaredType> emptySet());
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jsoChild));
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jso));
  }

  public void testUpdateTypeInformation_JSODualImpl() {
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes());

    Assert.assertFalse(program.typeOracle.isDualJsoInterface(intfJ));

    JClassType javaIntfImplementor = createClass("JavaImplementor", classObject, false, false);
    javaIntfImplementor.addImplements(intfJ);

    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType>newHashSet(javaIntfImplementor),
        Collections.<JDeclaredType>emptySet());
    Assert.assertTrue(program.typeOracle.isDualJsoInterface(intfJ));
    program.typeOracle.updateImmediateTypeRelations(
        Collections.<JDeclaredType>emptySet(),
        Sets.<JDeclaredType>newHashSet(javaIntfImplementor));
    Assert.assertFalse(program.typeOracle.isDualJsoInterface(intfJ));
  }

  public void testUpdateTypeInformation_SubClasses() {
    JClassType baseAll = createClass("IncrementalBaseAll", classObject, false, false);
    JClassType sub1 = createClass("IncrementalSub1", baseAll, false, false);
    JClassType sub2 = createClass("IncrementalSub2", baseAll, false, false);
    JClassType sub1_2 = createClass("IncrementalSub1_2", sub1, false, false);

    Assert.assertFalse(program.typeOracle.isSubClass(baseAll, sub1));

    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType>newHashSet(baseAll, sub1, sub2, sub1_2),
        Collections.<JDeclaredType>emptySet());

    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub1));
    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub2));
    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub1_2));
    Assert.assertTrue(program.typeOracle.isSubClass(sub1, sub1_2));

    sub1_2 = createClass("IncrementalSub1_2", baseAll, false, false);

    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType>newHashSet(baseAll, sub2, sub1_2),
        Sets.<JDeclaredType>newHashSet(sub1));

    Assert.assertFalse(program.typeOracle.isSubClass(baseAll, sub1));
    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub2));
    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub1_2));
  }

  public void testUpdateTypeInformation_SuperClasses() {
    JClassType baseAll = createClass("IncrementalBaseAll", classObject, false, false);
    JClassType sub1 = createClass("IncrementalSub1", baseAll, false, false);
    JClassType sub2 = createClass("IncrementalSub2", baseAll, false, false);
    JClassType sub1_2 = createClass("IncrementalSub1_2", sub1, false, false);

    Assert.assertFalse(program.typeOracle.isSuperClass(sub1, baseAll));

    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType>newHashSet(baseAll, sub1, sub2, sub1_2),
        Collections.<JDeclaredType>emptySet());

    Assert.assertTrue(program.typeOracle.isSuperClass(sub1, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub2, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub1_2, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub1_2, sub1));

    sub1_2 = createClass("IncrementalSub1_2", baseAll, false, false);

    program.typeOracle.updateImmediateTypeRelations(
        Sets.<JDeclaredType>newHashSet(baseAll, sub2, sub1_2),
        Sets.<JDeclaredType>newHashSet(sub1));

    Assert.assertFalse(program.typeOracle.isSuperClass(sub1, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub2, baseAll));
    Assert.assertFalse(program.typeOracle.isSuperClass(sub1_2, sub1));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub1_2, baseAll));
  }

  @Override
  protected void setUp() {
    createSampleProgram(new MinimalRebuildCache());
  }

  private void assertSuperHierarchy(JReferenceType type, JReferenceType... superHierarchyTypes) {
    assertEquals(Sets.<JReferenceType> newHashSet(superHierarchyTypes),
        typeOracle.getSuperHierarchyTypes(type));
  }

  private JClassType createClass(String className, JClassType superClass, boolean isAbstract,
      boolean isFinal) {
    JClassType x = new JClassType(synthSource, className, isAbstract, isFinal);
    program.addType(x);
    x.setSuperClass(superClass);
    return x;
  }

  private JInterfaceType createInterface(String className) {
    JInterfaceType x = new JInterfaceType(synthSource, className);
    program.addType(x);
    return x;
  }

  private void createSampleProgram(MinimalRebuildCache minimalRebuildCache) {
    // Make the program itself
    program = new JProgram(minimalRebuildCache);
    typeOracle = program.typeOracle;
    synthSource = SourceOrigin.UNKNOWN;

    classObject = createClass("java.lang.Object", null, false, false);
    classString = createClass("java.lang.String", classObject, false, true);
    createClass("com.google.gwt.lang.Array", classObject, false, true);
    classJso =
        createClass("com.google.gwt.core.client.JavaScriptObject", classObject, false, false);

    intfSerializable = createInterface("java.io.Serializable");
    intfCloneable = createInterface("java.lang.Cloneable");

    intfIBase = createInterface("IBase");

    intfI = createInterface("I");
    intfI.addImplements(intfIBase);

    intfJ = createInterface("J");
    intfK = createInterface("K");

    classBase = createClass("Base", classObject, false, false);

    classA = createClass("A", classBase, false, false);

    classB = createClass("B", classBase, false, false);
    classB.addImplements(intfI);

    classC = createClass("C", classObject, false, false);
    classC.addImplements(intfI);

    classBSub = createClass("BSub", classB, false, false);

    classJso1 = createClass("Jso1", classJso, false, false);
    classJso1.addImplements(intfJ);
    classJso2 = createClass("Jso2", classJso, false, false);
    classJso2.addImplements(intfK);

    intfIterable = createInterface("java.util.Iterable");
    intfCollection = createInterface("java.util.Collection");
    intfCollection.addImplements(intfIterable);
    intfList = createInterface("java.util.List");
    intfList.addImplements(intfCollection);
    classArrayList = createClass("java.util.ArrayList", classObject, false, false);
    classArrayList.addImplements(intfList);

    classBnn = classB.getNonNull();
    classBaseNn = classBase.getNonNull();

    // 1 dimensional
    arrayOfObject = program.getTypeArray(classObject);
    arrayOfBase = program.getTypeArray(classBase);
    arrayOfA = program.getTypeArray(classA);
    arrayOfB = program.getTypeArray(classB);
    arrayOfBSub = program.getTypeArray(classBSub);
    arrayOfIntfIBase = program.getTypeArray(intfIBase);
    arrayOfIntfI = program.getTypeArray(intfI);
    arrayOfC = program.getTypeArray(classC);
    arrayOfInt = program.getTypeArray(program.getTypePrimitiveInt());

    // 2 dimensional
    arrayOfArrayOfBase = program.getOrCreateArrayType(classBase, 2);
    arrayOfArrayOfObject = program.getOrCreateArrayType(classObject, 2);
    arrayOfArrayOfIntfI = program.getOrCreateArrayType(intfI, 2);
    arrayOfArrayOfIntfIBase = program.getOrCreateArrayType(intfIBase, 2);
    arrayOfArrayOfInt = program.getOrCreateArrayType(program.getTypePrimitiveInt(), 2);
    arrayOfArrayOfB = program.getOrCreateArrayType(classB, 2);

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes());

    // Save off some miscellaneous types to test against
    typeNull = program.getTypeNull();
  }

  private JReferenceType generalizeTypes(JReferenceType type1, JReferenceType type2) {
    List<JReferenceType> types = new ArrayList<JReferenceType>(2);
    types.add(type1);
    types.add(type2);
    return program.generalizeTypes(types);
  }

  /**
   * Return several types, for exhaustively testing basic properties.
   */
  private Collection<JReferenceType> severalTypes() {
    List<JReferenceType> types = new ArrayList<JReferenceType>();
    types.add(arrayOfA);
    types.add(arrayOfB);
    types.add(arrayOfArrayOfB);
    types.add(arrayOfBSub);
    types.add(arrayOfObject);
    types.add(classA);
    types.add(classB);
    types.add(classBSub);
    types.add(classBnn);
    types.add(classBase);
    types.add(classC);
    types.add(classObject);
    types.add(classString);
    types.add(intfI);
    types.add(intfJ);
    types.add(intfK);
    types.add(intfIBase);
    types.add(classJso1);
    types.add(classJso2);
    types.add(classJso);
    types.add(typeNull);

    return types;
  }
}
