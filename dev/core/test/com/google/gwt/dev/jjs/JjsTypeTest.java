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
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JTypeOracle.ImmediateTypeRelations;
import com.google.gwt.dev.jjs.ast.JTypeOracle.StandardTypes;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
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
  private JArrayType arrayOfJso1;
  private JArrayType arrayOfJso2;
  private JArrayType arrayOfIntfJ;
  private JArrayType arrayOfIntfK;
  private JArrayType arrayOfJso;
  private JArrayType arrayOfC;
  private JArrayType arrayOfIntfSerializable;
  private JArrayType arrayOfIntfClonable;
  private JReferenceType arrayOfInt;
  private JArrayType arrayOfIntfI;
  private JArrayType arrayOfIntfIBase;
  private JArrayType arrayOfObject;
  private JClassType classA;
  private JClassType classArrayList;
  private JClassType classB;
  private JClassType classBase;
  private JReferenceType classBaseNn;
  private JReferenceType classBnn;
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
  private JInterfaceType intfL;
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

  private static final Collection<String> EMPTY_LIST = Collections.<String>emptySet();

  public void testCastFailsTrivially() {
    assertTrue(typeOracle.castFailsTrivially(classBnn, typeNull));

    assertFalse(typeOracle.castFailsTrivially(classBSub, classB));
    assertFalse(typeOracle.castFailsTrivially(classB, classBSub));

    assertFalse(typeOracle.castFailsTrivially(classB, classBnn));
    assertFalse(typeOracle.castFailsTrivially(classBnn, classB));

    assertFalse(typeOracle.castFailsTrivially(classB, classB));

    assertFalse(typeOracle.castFailsTrivially(classObject, arrayOfB));
    assertTrue(typeOracle.castFailsTrivially(arrayOfA, arrayOfArrayOfB));

    assertFalse(typeOracle.castFailsTrivially(arrayOfObject, arrayOfArrayOfB));

    assertFalse(typeOracle.castFailsTrivially(arrayOfB, arrayOfBSub));

    assertFalse(typeOracle.castFailsTrivially(classBase, intfI));
    assertTrue(typeOracle.castFailsTrivially(classA, intfJ));

    assertFalse(typeOracle.castFailsTrivially(intfIBase, intfI));

    assertFalse(typeOracle.castFailsTrivially(intfIBase, classBase));
    assertTrue(typeOracle.castFailsTrivially(intfJ, classA));

    assertFalse(typeOracle.castFailsTrivially(arrayOfA, intfSerializable));
    assertFalse(typeOracle.castFailsTrivially(intfSerializable, arrayOfA));

    assertFalse(typeOracle.castFailsTrivially(arrayOfA, intfCloneable));
    assertFalse(typeOracle.castFailsTrivially(intfCloneable, arrayOfA));

    assertFalse(typeOracle.castFailsTrivially(intfList, intfIterable));
    assertFalse(typeOracle.castFailsTrivially(classArrayList, intfIterable));

    assertFalse(typeOracle.castFailsTrivially(classJso1, classJso2));
    assertFalse(typeOracle.castFailsTrivially(classJso2, classJso1));

    assertFalse(typeOracle.castFailsTrivially(classJso1, intfK));
    assertFalse(typeOracle.castFailsTrivially(intfK, classJso1));

    assertFalse(typeOracle.castFailsTrivially(intfJ, intfK));
    assertFalse(typeOracle.castFailsTrivially(intfK, intfJ));

    assertFalse(typeOracle.castFailsTrivially(classB, intfL));
    assertTrue(typeOracle.castFailsTrivially(classB.strengthenToExact(), intfL));
  }

  public void testCastSucceedsTrivially() {
    assertTrue(typeOracle.castSucceedsTrivially(classB, classB));

    assertTrue(typeOracle.castSucceedsTrivially(classBSub, classB));
    assertFalse(typeOracle.castSucceedsTrivially(classB, classBSub));

    assertFalse(typeOracle.castSucceedsTrivially(classC, classA));
    assertFalse(typeOracle.castSucceedsTrivially(classA, classC));

    assertTrue(typeOracle.castSucceedsTrivially(classB, intfI));
    assertFalse(typeOracle.castSucceedsTrivially(intfI, classB));

    assertTrue(typeOracle.castSucceedsTrivially(classB, classObject));
    assertFalse(typeOracle.castSucceedsTrivially(classObject, classB));

    assertTrue(typeOracle.castSucceedsTrivially(classB, intfI));
    assertFalse(typeOracle.castSucceedsTrivially(intfI, classB));

    assertTrue(typeOracle.castSucceedsTrivially(classBnn, classB));
    assertFalse(typeOracle.castSucceedsTrivially(classB, classBnn));

    assertTrue(typeOracle.castSucceedsTrivially(typeNull, classB));
    assertFalse(typeOracle.castSucceedsTrivially(classB, typeNull));

    assertTrue(typeOracle.castSucceedsTrivially(arrayOfA, classObject));

    assertTrue(typeOracle.castSucceedsTrivially(arrayOfBSub, arrayOfB));
    assertFalse(typeOracle.castSucceedsTrivially(arrayOfB, arrayOfBSub));

    assertFalse(typeOracle.castSucceedsTrivially(arrayOfA, arrayOfB));
    assertFalse(typeOracle.castSucceedsTrivially(arrayOfB, arrayOfA));

    assertFalse(typeOracle.castSucceedsTrivially(arrayOfArrayOfB, arrayOfB));
    assertFalse(typeOracle.castSucceedsTrivially(arrayOfB, arrayOfArrayOfB));

    assertTrue(typeOracle.castSucceedsTrivially(arrayOfArrayOfB, arrayOfObject));
    assertFalse(typeOracle.castSucceedsTrivially(arrayOfObject, arrayOfArrayOfB));

    assertTrue(typeOracle.castSucceedsTrivially(classJso1, classJso));

    assertTrue(typeOracle.castSucceedsTrivially(arrayOfA, intfSerializable));
    assertFalse(typeOracle.castSucceedsTrivially(intfSerializable, arrayOfA));

    assertTrue(typeOracle.castSucceedsTrivially(arrayOfA, intfCloneable));
    assertFalse(typeOracle.castSucceedsTrivially(intfCloneable, arrayOfA));

    /*
     * Test that two types cannot both be trivially castable to each other,
     * unless they are the same type.
     */
    for (JReferenceType type1 : severalTypes()) {
      for (JReferenceType type2 : severalTypes()) {
        if (type1 != type2) {
          assertFalse(typeOracle.castSucceedsTrivially(type1, type2)
              && typeOracle.castSucceedsTrivially(type2, type1));
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
        assertTrue(typeOracle.castSucceedsTrivially(type1, generalized));
        assertTrue(typeOracle.castSucceedsTrivially(type2, generalized));
      }
    }
  }

  public void testCastableDestinationTypes() {
    assertCastableDestinationTypes(classObject, classObject);
    assertCastableDestinationTypes(classString, classString, classObject);
    assertCastableDestinationTypes(classJso, classJso, classObject);
    assertCastableDestinationTypes(intfSerializable, intfSerializable, classObject);
    assertCastableDestinationTypes(intfCloneable, intfCloneable, classObject);
    assertCastableDestinationTypes(intfIBase, intfIBase, classObject);
    assertCastableDestinationTypes(intfI, intfI, intfIBase, classObject);
    assertCastableDestinationTypes(intfJ, intfJ, classJso, classObject);
    assertCastableDestinationTypes(intfK, intfK, classJso, classObject);
    assertCastableDestinationTypes(classBase, classBase, classObject);
    assertCastableDestinationTypes(classA, classA, classObject, classBase);
    assertCastableDestinationTypes(classB, classB, classObject, classBase, intfIBase, intfI);
    assertCastableDestinationTypes(classC, classC, classObject, intfIBase, intfI);
    assertCastableDestinationTypes(classBSub, classBSub, classObject, classBase, classB, intfIBase,
        intfI, intfL);
    assertCastableDestinationTypes(classJso1, classObject, classJso, intfJ);
    assertCastableDestinationTypes(classJso2, classObject, classJso, intfK);
    assertCastableDestinationTypes(arrayOfJso, classObject, intfCloneable, intfSerializable,
        arrayOfJso, arrayOfObject);
    assertCastableDestinationTypes(arrayOfJso1, classObject, intfCloneable, intfSerializable,
        arrayOfJso, arrayOfObject, arrayOfIntfJ);
    assertCastableDestinationTypes(arrayOfJso2, classObject, intfCloneable, intfSerializable,
        arrayOfJso, arrayOfObject, arrayOfIntfK);

    assertCastableDestinationTypes(intfIterable, intfIterable, classObject);
    assertCastableDestinationTypes(intfCollection, intfCollection, intfIterable, classObject);
    assertCastableDestinationTypes(intfList, intfList, intfIterable, intfCollection, classObject);
    assertCastableDestinationTypes(classArrayList, classArrayList, intfList, classObject,
        intfIterable,
        intfCollection);
    assertCastableDestinationTypes(arrayOfB, arrayOfB, arrayOfBase, arrayOfObject, arrayOfIntfI,
        arrayOfIntfIBase, classObject, arrayOfObject, intfCloneable, intfSerializable);
    assertCastableDestinationTypes(arrayOfArrayOfB, arrayOfArrayOfB, arrayOfArrayOfBase,
        arrayOfArrayOfObject, arrayOfArrayOfIntfI, arrayOfArrayOfIntfIBase, classObject,
        arrayOfObject, intfCloneable, arrayOfIntfClonable, intfSerializable,
        arrayOfIntfSerializable);
    assertCastableDestinationTypes(arrayOfInt, arrayOfInt, classObject, intfCloneable,
        intfSerializable);

    intfCollection = createInterface("java.util.Collection");
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(),
        Sets.<JDeclaredType> newHashSet(classArrayList, intfList, intfCollection, classObject),
        Lists.newArrayList(intfIterable.getName()));

    assertCastableDestinationTypes(intfCollection, intfCollection, classObject);
    assertCastableDestinationTypes(intfList, intfList, intfCollection, classObject);
    assertCastableDestinationTypes(classArrayList, classArrayList, intfList, classObject,
        intfCollection);

    classA = createClass("A", classObject, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(),
        Sets.<JDeclaredType> newHashSet(classA, classObject),
        Lists.newArrayList(classBase.getName()));
    assertCastableDestinationTypes(classA, classA, classObject);

    JClassType classASub = createClass("ASub", classA, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(),
        Sets.<JDeclaredType> newHashSet(classASub, classA, classObject),
        EMPTY_LIST);
    assertCastableDestinationTypes(classASub, classASub, classA, classObject);
  }

  public void testJavahSignatures() {
    for (JReferenceType type : severalTypes()) {
      assertSignaturesMatch(type,
          new Function<JReferenceType, String>() {
            @Override
            public String apply(JReferenceType type) {
              return type.getJavahSignatureName();
            }
          });
    }
  }

  public void testJsniSignatures() {
    for (JReferenceType type : severalTypes()) {
      assertSignaturesMatch(type,
          new Function<JReferenceType, String>() {
            @Override
            public String apply(JReferenceType type) {
              return type.getJsniSignatureName();
            }
          });
    }
  }

  public void testStrongerType() {
    assertSame(classA, strongerType(classA, classA));
    assertSame(classBnn, strongerType(classB, classBnn));
    assertSame(classB, strongerType(classB, classBase));
    assertSame(classB, strongerType(classBase, classB));
    assertSame(intfI, strongerType(intfI, intfJ));
    assertSame(arrayOfA, strongerType(intfSerializable, arrayOfA));
    assertSame(arrayOfA, strongerType(intfCloneable, arrayOfA));
  }

  public void testUpdateTypeInformation_isJavaScriptObject() {
    List<String> emptyList = Lists.<String> newArrayList();
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), program.getModuleDeclaredTypes(), emptyList);

    JClassType jso = createClass("SomeJSO", classJso, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(jso), emptyList);
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jso));
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Collections.<JDeclaredType> emptySet(),
        Lists.newArrayList(jso.getName()));
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jso));

    jso = createClass("SomeJSO", classJso, false, false);
    JClassType jsoChild = createClass("SomeJSOChild", jso, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(jso, jsoChild), emptyList);
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jsoChild));
    jsoChild = createClass("SomeJSOChild", classObject, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(jsoChild), emptyList);
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jsoChild));

    jso = createClass("SomeJSO", classJso, false, false);
    jsoChild = createClass("SomeJSOChild", jso, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(jsoChild), emptyList);
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jsoChild));
    Assert.assertTrue(program.typeOracle.isJavaScriptObject(jso));
    jso = createClass("SomeJSO", classObject, false, false);
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(jso), emptyList);
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jsoChild));
    Assert.assertFalse(program.typeOracle.isJavaScriptObject(jso));
  }

  public void testUpdateTypeInformation_JSODualImpl() {
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), program.getModuleDeclaredTypes(), EMPTY_LIST);

    Assert.assertFalse(program.typeOracle.isDualJsoInterface(intfJ));

    JClassType javaIntfImplementor = createClass("JavaImplementor", classObject, false, false);
    javaIntfImplementor.addImplements(intfJ);

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(javaIntfImplementor),
        EMPTY_LIST);
    Assert.assertTrue(program.typeOracle.isDualJsoInterface(intfJ));
    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Collections.<JDeclaredType> emptySet(),
        Lists.newArrayList(javaIntfImplementor.getName()));
    Assert.assertFalse(program.typeOracle.isDualJsoInterface(intfJ));
  }

  public void testUpdateTypeInformation_SubClasses() {
    JClassType baseAll = createClass("IncrementalBaseAll", classObject, false, false);
    JClassType sub1 = createClass("IncrementalSub1", baseAll, false, false);
    JClassType sub2 = createClass("IncrementalSub2", baseAll, false, false);
    JClassType sub1_2 = createClass("IncrementalSub1_2", sub1, false, false);

    Assert.assertFalse(program.typeOracle.isSubClass(baseAll, sub1));

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(baseAll, sub1, sub2, sub1_2),
        EMPTY_LIST);

    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub1));
    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub2));
    Assert.assertTrue(program.typeOracle.isSubClass(baseAll, sub1_2));
    Assert.assertTrue(program.typeOracle.isSubClass(sub1, sub1_2));

    sub1_2 = createClass("IncrementalSub1_2", baseAll, false, false);

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), Sets.<JDeclaredType> newHashSet(baseAll, sub2, sub1_2),
        Lists.newArrayList(sub1.getName()));

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

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(),
        Sets.<JDeclaredType>newHashSet(baseAll, sub1, sub2, sub1_2),
        EMPTY_LIST);

    Assert.assertTrue(program.typeOracle.isSuperClass(sub1, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub2, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub1_2, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub1_2, sub1));

    sub1_2 = createClass("IncrementalSub1_2", baseAll, false, false);

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(),
        Sets.<JDeclaredType>newHashSet(baseAll, sub2, sub1_2),
        Lists.newArrayList(sub1.getName()));

    Assert.assertFalse(program.typeOracle.isSuperClass(sub1, baseAll));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub2, baseAll));
    Assert.assertFalse(program.typeOracle.isSuperClass(sub1_2, sub1));
    Assert.assertTrue(program.typeOracle.isSuperClass(sub1_2, baseAll));
  }

  public void testTypeRelationsBeforeRecompute() {
    // Construct a JTypeOracle with some previously cached type relations, but do not recompute();
    MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
    ImmediateTypeRelations immediateTypeRelations = minimalRebuildCache.getImmediateTypeRelations();
    immediateTypeRelations.getImmediateSuperclassesByClass().put("Foo", "java.lang.Object");
    JTypeOracle typeOracle = new JTypeOracle(null, minimalRebuildCache, true);

    // Show that the typeOracle can already answer basic type relation questions.
    assertEquals("java.lang.Object", typeOracle.getSuperTypeName("Foo"));
  }

  @Override
  protected void setUp() {
    createSampleProgram(new MinimalRebuildCache());
  }

  private void assertCastableDestinationTypes(JReferenceType type,
      JReferenceType... superHierarchyTypes) {
    assertEquals(Sets.<JReferenceType> newHashSet(superHierarchyTypes),
        typeOracle.getCastableDestinationTypes(type));
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
    intfL = createInterface("L");

    classBase = createClass("Base", classObject, false, false);

    classA = createClass("A", classBase, false, false);

    classB = createClass("B", classBase, false, false);
    classB.addImplements(intfI);

    classC = createClass("C", classObject, false, false);
    classC.addImplements(intfI);

    classBSub = createClass("BSub", classB, false, false);
    classBSub.addImplements(intfL);

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

    classBnn = classB.strengthenToNonNull();
    classBaseNn = classBase.strengthenToNonNull();

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
    arrayOfJso = program.getTypeArray(classJso);
    arrayOfJso1 = program.getTypeArray(classJso1);
    arrayOfJso2 = program.getTypeArray(classJso2);
    arrayOfIntfJ = program.getTypeArray(intfJ);
    arrayOfIntfK = program.getTypeArray(intfK);
    arrayOfIntfClonable = program.getTypeArray(intfCloneable);
    arrayOfIntfSerializable = program.getTypeArray(intfSerializable);

    // 2 dimensional
    arrayOfArrayOfBase = program.getOrCreateArrayType(classBase, 2);
    arrayOfArrayOfObject = program.getOrCreateArrayType(classObject, 2);
    arrayOfArrayOfIntfI = program.getOrCreateArrayType(intfI, 2);
    arrayOfArrayOfIntfIBase = program.getOrCreateArrayType(intfIBase, 2);
    arrayOfArrayOfInt = program.getOrCreateArrayType(program.getTypePrimitiveInt(), 2);
    arrayOfArrayOfB = program.getOrCreateArrayType(classB, 2);

    program.typeOracle.computeBeforeAST(StandardTypes.createFrom(program),
        program.getDeclaredTypes(), program.getModuleDeclaredTypes(), EMPTY_LIST);

    // Save off some miscellaneous types to test against
    typeNull = JReferenceType.NULL_TYPE;
  }

  private JReferenceType strongerType(JReferenceType type1, JReferenceType type2) {
    return program.strengthenType(type1, program.generalizeTypes(Arrays.asList(type2)));
  }

  private JReferenceType generalizeTypes(JReferenceType type1, JReferenceType type2) {
    return program.strengthenType(program.getTypeJavaLangObject(),
        program.generalizeTypes(Arrays.asList(type1, type2)));
  }

  private void assertSignaturesMatch(
      JReferenceType type, Function<JReferenceType, String> signatureForType) {
    if (type.isNullType()) {
      return;
    }
    assertEquals(signatureForType.apply(type), signatureForType.apply(type.strengthenToNonNull()));
    assertEquals(signatureForType.apply(type), signatureForType.apply(type.weakenToNullable()));
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
