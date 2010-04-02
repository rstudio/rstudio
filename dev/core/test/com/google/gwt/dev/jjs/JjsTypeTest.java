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

import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.CharOperation;

import java.util.ArrayList;
import java.util.Collection;
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
  private JArrayType arrayOfBSub;
  private JArrayType arrayOfC;
  private JReferenceType arrayOfInt;
  private JArrayType arrayOfObject;
  private JClassType classA;
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
  private JInterfaceType intfI;
  private JInterfaceType intfIBase;
  private JInterfaceType intfJ;
  private JProgram program;
  private SourceInfo synthSource;
  private JReferenceType typeNull;
  private JTypeOracle typeOracle;
  private JReferenceType intfSerializable;
  private JReferenceType intfCloneable;

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

    assertTrue(typeOracle.canTriviallyCast(classJso1, classJso2));
    assertTrue(typeOracle.canTriviallyCast(classJso2, classJso1));

    assertTrue(typeOracle.canTriviallyCast(classJso, classJso1));
    assertTrue(typeOracle.canTriviallyCast(classJso, classJso1));

    assertTrue(typeOracle.canTriviallyCast(arrayOfA, intfSerializable));
    assertFalse(typeOracle.canTriviallyCast(intfSerializable, arrayOfA));

    assertTrue(typeOracle.canTriviallyCast(arrayOfA, intfCloneable));
    assertFalse(typeOracle.canTriviallyCast(intfCloneable, arrayOfA));

    /*
     * Test that two types cannot both be trivially castable to each other,
     * unless they are the same type. Or, unless they are both JSOs.
     */
    for (JReferenceType type1 : severalTypes()) {
      for (JReferenceType type2 : severalTypes()) {
        if (type1 != type2) {
          if (!isJso(type1) || !isJso(type2)) {
            assertFalse(typeOracle.canTriviallyCast(type1, type2)
                && typeOracle.canTriviallyCast(type2, type1));
          }
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

  public void testStrongerType() {
    assertSame(classA, program.strongerType(classA, classA));
    assertSame(classBnn, program.strongerType(classB, classBnn));
    assertSame(classB, program.strongerType(classB, classBase));
    assertSame(classB, program.strongerType(classBase, classB));
    assertSame(intfI, program.strongerType(intfI, intfJ));
    assertSame(arrayOfA, program.strongerType(intfSerializable, arrayOfA));
    assertSame(arrayOfA, program.strongerType(intfCloneable, arrayOfA));
  }

  @Override
  protected void setUp() {
    createSampleProgram();
  }

  private JClassType createClass(String className, JClassType superClass,
      boolean isAbstract, boolean isFinal) {
    JClassType clazz = program.createClass(synthSource, className, isAbstract,
        isFinal);
    clazz.setSuperClass(superClass);
    return clazz;
  }

  private JInterfaceType createInterface(String className) {
    JInterfaceType intf = program.createInterface(synthSource, className);
    return intf;
  }

  private void createSampleProgram() {
    // Make the program itself
    program = new JProgram();
    typeOracle = program.typeOracle;
    synthSource = program.createSourceInfoSynthetic(JjsTypeTest.class,
        "synthetic node used for testing");

    classObject = createClass("java.lang.Object", null, false, false);
    classString = createClass("java.lang.String", classObject, false, true);
    classJso = createClass("com.google.gwt.core.client.JavaScriptObject",
        classObject, false, false);

    intfSerializable = createInterface("java.io.Serializable");
    intfCloneable = createInterface("java.lang.Cloneable");

    intfIBase = createInterface("IBase");

    intfI = createInterface("I");
    intfI.addImplements(intfIBase);

    intfJ = createInterface("J");

    classBase = createClass("Base", classObject, false, false);

    classA = createClass("A", classBase, false, false);

    classB = createClass("B", classBase, false, false);
    classB.addImplements(intfI);

    classC = createClass("C", classObject, false, false);
    classC.addImplements(intfI);

    classBSub = createClass("BSub", classB, false, false);

    classJso1 = createClass("Jso1", classJso, false, false);
    classJso2 = createClass("Jso2", classJso, false, false);

    program.typeOracle.computeBeforeAST();

    // Save off some miscellaneous types to test against
    typeNull = program.getTypeNull();

    classBnn = program.getNonNullType(classB);
    classBaseNn = program.getNonNullType(classBase);

    arrayOfA = program.getTypeArray(classA, 1);
    arrayOfB = program.getTypeArray(classB, 1);
    arrayOfBSub = program.getTypeArray(classBSub, 1);
    arrayOfC = program.getTypeArray(classC, 1);
    arrayOfObject = program.getTypeArray(classObject, 1);
    arrayOfInt = program.getTypeArray(program.getTypePrimitiveInt(), 1);
    arrayOfArrayOfInt = program.getTypeArray(program.getTypePrimitiveInt(), 2);

    arrayOfArrayOfB = program.getTypeArray(classB, 2);
  }

  private JReferenceType generalizeTypes(JReferenceType type1,
      JReferenceType type2) {
    List<JReferenceType> types = new ArrayList<JReferenceType>(2);
    types.add(type1);
    types.add(type2);
    return program.generalizeTypes(types);
  }

  private boolean isJso(JReferenceType type) {
    return typeOracle.canTriviallyCast(type, classJso);
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
    types.add(intfIBase);
    types.add(classJso1);
    types.add(classJso2);
    types.add(classJso);
    types.add(typeNull);

    return types;
  }
}
