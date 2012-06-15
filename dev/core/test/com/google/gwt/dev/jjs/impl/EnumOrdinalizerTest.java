/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * A set of tests for the conditions under which ordinalization is and is not 
 * allowed.  The ordinalization is performed when allowed.
 * 
 * A complete test of the resulting ordinalization is not performed.  However,
 * the CastNormalizer and the EqualityNormalizer are run after the EnumOrdinalizer, 
 * to help ensure the integrity of the AST, such that there are no partially 
 * mismatched type assignments or comparisons, and that no binary operations 
 * between a primitive type and null have been added.  Typically, such errors 
 * introduced by the EnumOrdinalizer are caught by these normalizers, so it 
 * makes sense to test the output in this way.  Thus, we provide confidence
 * that the AST is left in a coherent state, but it is not a complete test that
 * ordinalization has completed correctly in every respec.
 */
public class EnumOrdinalizerTest extends OptimizerTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    EnumOrdinalizer.enableTracker();
    EnumOrdinalizer.resetTracker();
    
    // defaults, can be overridden by individual test cases
    runTypeTightener = false;
    runMethodCallTightener = false;
    runMethodInliner = true;
    runMakeCallsStatic = true;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testOrdinalizeBasicAssignment() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit apple = Fruit.APPLE;",
                    "Fruit orange = Fruit.ORANGE;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeNewArrayAndAssignmentLocalRef() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit[] fruits = new Fruit[] {Fruit.APPLE, Fruit.ORANGE, Fruit.APPLE};",
                     "if (fruits[0] == Fruit.APPLE) {",
                     "  fruits[0] = Fruit.ORANGE;",
                     "}");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeNewArrayOfArrayAndAssignmentLocalRef() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit[][] fruits = new Fruit[][] ",
                     " {{Fruit.APPLE, Fruit.ORANGE},{Fruit.APPLE, Fruit.ORANGE}};",
                     "if (fruits[0][1] == Fruit.APPLE) {",
                     "  fruits[0][1] = Fruit.ORANGE;",
                     "}");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeNewArrayAndAssignmentFieldRef() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("private final Fruit[] fruits = new Fruit[] ",
                        "  {Fruit.APPLE, Fruit.ORANGE, Fruit.APPLE};");
    optimize("void", "EntryPoint ep = new EntryPoint();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizableFinalFieldUninitializedByDefault() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("private final Fruit uninitializedFinalFruit;",
                        "public EntryPoint() {",
                        "  uninitializedFinalFruit = Fruit.ORANGE;",
                        "}");
    optimize("void", "EntryPoint ep = new EntryPoint();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testOrdinalizeSwitchStatement() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    setupFruitSwitchMethod();
    optimize("void", "String apple = fruitSwitch(Fruit.APPLE);",
                    "String orange = fruitSwitch(Fruit.ORANGE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeIfStatement() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl(
        "public static String fruitIf(Fruit fruit) {",
        " if (fruit == Fruit.APPLE) {",
        "   return \"Apple\";",
        " } else if (fruit == Fruit.ORANGE) {",
        "   return \"Orange\";",
        " } else {",
        "   return \"Unknown\";",
        " }",
        "}");
    optimize("void", "String apple = fruitIf(Fruit.APPLE);",
                    "String orange = fruitIf(Fruit.ORANGE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testOrdinalizeConditional() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = (true) ? Fruit.APPLE : Fruit.ORANGE;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeFieldRefOrdinalMethodCall() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "int i = Fruit.APPLE.ordinal();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testOrdinalizeVariableRefOrdinalMethodCall() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "int i = fruit.ordinal();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeUnusedEmptyEnum() throws UnableToCompleteException {
    setupEmptyEnum();

    optimize("void", "EmptyEnum myEnum;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$EmptyEnum"));
  }

  public void testOrdinalizeUnusedEnum() throws UnableToCompleteException {
    setupFruitEnum();

    optimize("void", "Fruit myEnum;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testOrdinalizeMethodCallExpressionOrdinalFieldRef() 
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static Fruit getResolvedFruit(Fruit fruit) {",
                        "  if (fruit == Fruit.APPLE) {",
                        "    return Fruit.ORANGE;",
                        "  } else { ",
                        "    return Fruit.APPLE;",
                        "  }",
                        "}");
    addSnippetClassDecl("public static int switchMethodCall(Fruit fruit) {",
                        "  int retVal = 0;",
                        "  switch (getResolvedFruit(fruit)) {",
                        "    case APPLE: retVal = 12; break;",
                        "    case ORANGE:retVal = 73; break;",
                        "  }",
                        "  return retVal;",
                        "}");
    optimize("void", "int i = switchMethodCall(Fruit.APPLE);",
                    "Fruit fruit = Fruit.ORANGE;",
                    "int j = switchMethodCall(fruit);");
                        
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testOrdinalizableStaticFieldRef() 
      throws UnableToCompleteException  {
    // this will cause a static field ref in the enum clinit
    setupFruitEnumWithStaticField();
    optimize("void", "String y = Fruit.staticField;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  } 
  
  public void testOrdinalizableStaticMethod() 
      throws UnableToCompleteException  {
    // this will cause a static method enum class
    setupFruitEnumWithStaticMethod();
    optimize("void", "int y = Fruit.staticMethod();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  } 
  
  public void testNotOrdinalizableStaticMethodThatRefsValuesArray() 
      throws UnableToCompleteException  {
    // this will cause a static method that references an element
    // of the values() array
    setupFruitEnumWithStaticMethodThatRefsValuesArray();
    optimize("void", "Fruit y = Fruit.forInteger(0);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  } 
  
  public void testNotOrdinalizableStaticMethodThatRefsValuesLength() 
      throws UnableToCompleteException  {
    // this will cause a static method that references values().length
    setupFruitEnumWithStaticMethodThatRefsValuesLength();
    optimize("void", "Fruit y = Fruit.forInteger(0);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  } 
  
  public void testNotOrdinalizableInstanceStaticFieldRef() 
      throws UnableToCompleteException  {
    // this will cause a static field ref in the enum clinit
    setupFruitEnumWithStaticField();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                     "String y = fruit.staticField;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  } 
  
  public void testNotOrdinalizableInstanceStaticMethod() 
      throws UnableToCompleteException  {
    // this will cause a static method enum class
    setupFruitEnumWithStaticMethod();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                     "int y = fruit.staticMethod();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  } 
  
  public void testNotOrdinalizableClassLiteralReference() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Class clazz = Fruit.class;",
                    "String clazzStr = clazz.toString();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableEnumValueOfWithClassLiteralArg() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Object Carrot = Enum.valueOf(Fruit.class, \"APPLE\");",
                    "String carrot = Carrot.toString();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableGetClassMethodCall() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Class clazz = Fruit.APPLE.getClass();",
                    "String clazzStr = clazz.toString();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableExplicitCastToEnumClass() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Object obj = new Object();",
                    "Fruit fruit = (Fruit) obj;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableExplicitCastToArrayOfEnumClass() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum[] enumArray = new Enum[10];",
                    "Fruit[] fruitArray = (Fruit[]) enumArray;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableExplicitCastFromArrayOfEnumClass() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit[] fruitArray = new Fruit[10];",
                    "Enum[] enumArray = (Enum[]) fruitArray;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableExplicitCastToArrayOfArrayOfEnumClass() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum[][] enumArray = new Enum[10][10];",
                    "Fruit[][] fruitArray = (Fruit[][]) enumArray;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableExplicitCastFromArrayOfArrayOfEnumClass() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit[][] fruitArray = new Fruit[10][10];",
                    "Enum[][] enumArray = (Enum[][]) fruitArray;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableExplicitCastFromEnumClass() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum Carrot = (Enum) Fruit.APPLE;",
                    "String carrot = Carrot.toString();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableOrdinalMethodRefFromExplicitCastWithBlackListableSubExpression() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "int ord = " +
        "((Fruit) Enum.valueOf(Fruit.class,\"APPLE\")).ordinal();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableInstanceFieldRef() 
      throws UnableToCompleteException  {
    // this will cause an instance field ref in the enum constructor
    setupFruitEnumWithInstanceField();
    optimize("void");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testNotOrdinalizableInstanceOfEnumExpression() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                     "if (fruit instanceof Enum) {",
                     "  fruit = Fruit.ORANGE;",
                     "}");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testNotOrdinalizableInstanceOfEnumTestType() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Object fruitObj = new Object();",
                     "if (fruitObj instanceof Fruit) {",
                     "  fruitObj = null;",
                     "}");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testNotOrdinalizableStaticFieldRefToVALUES() 
      throws UnableToCompleteException  {
    // this ends up inlining the values() method call, and thus $VALUES is referenced external
    // to the Fruit enum class.
    setupFruitEnum();
    optimize("void", "Fruit[] fruits = Fruit.values();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableStaticMethodCallValues() 
      throws UnableToCompleteException  {
    // make sure values() method call doesn't doesn't get inlined
    runMethodInliner = false;
    
    setupFruitEnum();
    optimize("void", "Fruit[] fruits = Fruit.values();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableJsniFieldRef() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static Fruit instanceFruit;");
    addSnippetClassDecl("public static native void jsniMethod() /*-{",
                        "  var x = @test.EntryPoint::instanceFruit",
                        "}-*/");
    optimize("void", "instanceFruit = Fruit.APPLE;",
                    "jsniMethod();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableJsniFieldRefStatic() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static native void jsniMethod() /*-{",
                        "  var x = @test.EntryPoint.Fruit::APPLE",
                        "}-*/");
    optimize("void", "jsniMethod();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableJsniFieldRefClassLiteral() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static native void jsniMethod() /*-{",
                        "  var x = @test.EntryPoint.Fruit::class",
                        "}-*/");
    optimize("void", "jsniMethod();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpAssignment() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum tomato;",
                    "tomato = Fruit.APPLE;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastFieldInitializedWithNullByDefault() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("static private Fruit uninitializedFruitAsNull;");
    optimize("void", "if (uninitializedFruitAsNull != Fruit.APPLE) {",
                     "  uninitializedFruitAsNull = Fruit.ORANGE;",
                     "}");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpEquals() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    setupVegetableEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "Enum carrot = (Enum) Vegetable.CARROT;",
                    "boolean test = (fruit == carrot);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpNotEquals() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    setupVegetableEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "Enum carrot = (Enum) Vegetable.CARROT;",
                    // do in opposite order from OpEquals test
                    "boolean test = (carrot != fruit);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpEqualsNull() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static boolean testIsNull(Fruit fruit) {",
                        "  if (fruit == null) {",
                        "    return true;",
                        "  } else {",
                        "    return false;",
                        "  }",
                        "}");
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "boolean isNull = testIsNull(fruit) || testIsNull(Fruit.ORANGE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpNotEqualsNull() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static boolean testIsNull(Fruit fruit) {",
                        "  if (fruit != null) {",
                        "    return true;",
                        "  } else {",
                        "    return false;",
                        "  }",
                        "}");
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "boolean isNull = testIsNull(fruit) || testIsNull(Fruit.ORANGE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpStringConcat() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "String str = \"A string followed by \" + fruit;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpStringConcat2() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "String str = fruit + \" followed by a string\";");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastBinaryOpStringConcatAssignment() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = Fruit.APPLE;",
                    "String str = \"A string concatenated with: \";",
                    "str += fruit;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastDeclarationToNull() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit = null;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastAssignmentToNull() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Fruit fruit;",
                    "fruit = null;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastDeclaration() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum tomato = Fruit.APPLE;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastConditional() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    setupVegetableEnum();
    optimize("void", "Enum tomato = null;",
                    "tomato = (true) ? Fruit.APPLE : Vegetable.CARROT;");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
  }
  
  public void testNotOrdinalizableImplicitUpcastOverriddenMethodReturnType() 
      throws UnableToCompleteException  {
    
    // this test depends on the tighteners running
    runTypeTightener = true;
    runMethodCallTightener = true;
    
    /*
     * Create methods with covariant return type, which the MethodTypeTightener
     * will optimize to no longer be covariant, so make sure we check original
     * overridden method type.
     */
    addSnippetClassDecl("public interface EnumInterface {",
                        "  String name();",
                        "}");
    addSnippetClassDecl("public abstract class AbstractClass<T extends EnumInterface> {",
                        "  public abstract T getEnumClass();",
                        "}");
    addSnippetClassDecl("public class CustomClass1 extends AbstractClass<EnumClass1> {",
                        "  public EnumClass1 getEnumClass() { return EnumClass1.CONST1; }",
                        "}");
    addSnippetClassDecl("public class CustomClass2 extends AbstractClass<EnumClass2> {",
                        "  public EnumClass2 getEnumClass() { return EnumClass2.CONST2; }",
                        "}");
    addSnippetClassDecl("public enum EnumClass1 implements EnumInterface {",
                        "  CONST1;",
                        "}");
    addSnippetClassDecl("public enum EnumClass2 implements EnumInterface {",
                        "  CONST2;",
                        "}");
    addSnippetClassDecl("public static void testEnumClass(AbstractClass abstractClass) {",
                        "  EnumInterface enumClass = abstractClass.getEnumClass();",
                        "}");
    optimize("void", "EntryPoint ep = new EntryPoint();",
                    "AbstractClass abstractClass1 = ep.new CustomClass1();",
                    "AbstractClass abstractClass2 = ep.new CustomClass2();",
                    "testEnumClass(abstractClass1);",
                    "testEnumClass(abstractClass2);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$EnumClass1"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$EnumClass1"));
    assertTrue(tracker.isVisited("test.EntryPoint$EnumClass2"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$EnumClass2"));
  }
  
  public void testNotOrdinalizableImplicitUpcastMethodCallArgs() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static String getEnumString(Enum myEnum) {",
                        // make sure this method does something so not inlined
                        "  int ord = myEnum.ordinal();",
                        "  String retString = \"\";",
                        "  for (int i = 0;i<ord;i++) {",
                        "    retString += \"-\";",
                        "  }",
                        "  retString += myEnum.name();",
                        "  return retString;",
                        "}");
    optimize("void", "String stringApple = getEnumString(Fruit.APPLE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastMethodCallArgsNewArray() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static String getEnumString(Enum[] myEnumArray) {",
                        "  String retString = \"\";",
                        "  for (Enum myEnum : myEnumArray) {",
                        "    retString += myEnum.name();",
                        "  }",
                        "  return retString;",
                        "}");
    optimize("void", "String stringFruits = getEnumString(new Enum[] {Fruit.APPLE, Fruit.ORANGE});");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testNotOrdinalizableImplicitUpcastMethodCallVarArgs() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static String getEnumString(Enum...myEnumArray) {",
                        "  String retString = \"\";",
                        "  for (Enum myEnum : myEnumArray) {",
                        "    retString += myEnum.name();",
                        "  }",
                        "  return retString;",
                        "}");
    optimize("void", "String stringFruits = getEnumString(Fruit.APPLE, Fruit.ORANGE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }

  public void testNotOrdinalizableImplicitUpcastNewArrayElements() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum[] enums = new Enum[] {Fruit.APPLE, Fruit.ORANGE};");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastNewArrayArrayElements() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    optimize("void", "Enum[][] enums = new Enum[][] {{Fruit.APPLE, Fruit.ORANGE},{Fruit.ORANGE, Fruit.APPLE}};");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastJsniMethodBodyParams() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static native void passEnumToJsniMethod(Fruit myEnum) /*-{",
                        "}-*/");
    optimize("void", "passEnumToJsniMethod(Fruit.APPLE);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastJsniMethodBodyReturnType() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static native Fruit returnFruitViaJsni() /*-{",
                        "  var myJso;",
                        "  return myJso;",
                        "}-*/");
    optimize("void", "Fruit fruit = returnFruitViaJsni();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastJsniMethodRefParams() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    setupFruitSwitchMethod();
    addSnippetClassDecl("public static native void fruitSwitchViaJsni() /*-{",
                        "  var myJso;",
                        "  var result = @test.EntryPoint::fruitSwitch(Ltest/EntryPoint$Fruit;)(myJso);",
                        "}-*/");
    optimize("void", "fruitSwitchViaJsni();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastJsniMethodRefReturnType() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    addSnippetClassDecl("public static Fruit returnSomeFruit() {",
                        "  return Fruit.APPLE;",
                        "}");
    addSnippetClassDecl("public static native void jsniMethodRefWithEnumReturn() /*-{",
                        "  var result = @test.EntryPoint::returnSomeFruit()();",
                        "}-*/");
    optimize("void", "jsniMethodRefWithEnumReturn();");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
  }
  
  public void testNotOrdinalizableImplicitUpcastReturnStatement() 
      throws UnableToCompleteException  {
    setupFruitEnum();
    setupVegetableEnum();
    addSnippetClassDecl("public static Enum returnAsEnum(int mode) {",
                        "  if (mode == 0) {",
                        "    return Fruit.APPLE;",
                        "  } else {",
                        "    return Vegetable.CARROT;",
                        "  }",
                        "}");
    optimize("void", "Enum myEnum = returnAsEnum(0);",
                    // do a second one, to prevent inlining
                    "Enum myOtherEnum = returnAsEnum(1);");
    
    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
  }

  private void setupEmptyEnum() {
    addSnippetClassDecl("public enum EmptyEnum {}");
  }

  private void setupFruitEnum() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE}");
  }

  private void setupFruitEnumWithInstanceField() {
    addSnippetClassDecl("public enum Fruit {APPLE(\"a\"), ORANGE(\"b\");",
                        "  public final String instanceField;",
                        "  private Fruit(String str) {",
                        "    instanceField = str;",
                        "  }",
                        "}");
  }
    
  private void setupFruitEnumWithStaticField() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE;",
                        "  public static final String staticField = \"STATIC\";",
                        "}");
  }
    
  private void setupFruitEnumWithStaticMethod() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE;",
                        "  public static final int staticMethod() {",
                        "    int x = 0;",
                        "    return x;",
                        "  }",
                        "}");
  }
    
  private void setupFruitEnumWithStaticMethodThatRefsValuesArray() {
    // add a little extra logic here, to prevent inlining
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE;",
                        "  public static Fruit forInteger(int value) {",
                        "    if (value < 0 || value >= 2) {",
                        "      return ORANGE;",
                        "    }",
                        "    return Fruit.values()[value];",
                        "  }",
                        "}");
  }
    
  private void setupFruitEnumWithStaticMethodThatRefsValuesLength() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE;",
                        "  public static Fruit forInteger(int value) {",
                        "    if (value < 0 || value >= Fruit.values().length) {",
                        "      return ORANGE;",
                        "    }",
                        "    return APPLE;",
                        "  }",
                        "}");
  }
    
  private void setupVegetableEnum() {
    addSnippetClassDecl("public enum Vegetable {CARROT, SPINACH}");
  }
    
  private void setupFruitSwitchMethod() {
    addSnippetClassDecl("public static String fruitSwitch(Fruit fruit) {",
                        " switch(fruit) {",
                        "   case APPLE: return \"Apple\";",
                        "   case ORANGE: return \"Orange\";",
                        "   default: return \"Unknown\";",
                        " }",
                        "}"); 
  }
  
  /*
   * Always run CastNormalizer and EqualityNormalizer, even in cases where we 
   * are testing that ordinalization cannot occur, since there may be other 
   * enums (such as DummyEnum) which do get ordinalized, and we want to test 
   * that all is well regardless.
   */
  private final boolean runCastNormalizer = true;
  private final boolean runEqualityNormalizer = true;
  
  // These are enabled as needed for a given test
  private boolean runMakeCallsStatic;
  private boolean runMethodInliner;
  private boolean runMethodCallTightener;
  private boolean runTypeTightener;
  
  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    /*
     * EnumOrdinalizer depends MakeCallsStatic and MethodInliner running before
     * it runs, since they cleanup the internal structure of an enum class to
     * inline instance methods like $init.
     * 
     * TypeTightener and methodCallTightener are necessary to test some cases 
     * involving implicit casts on overridden method call return types.
     * 
     * These are a subset of the actual optimizers run in JJS.optimizeLoop().
     */
    boolean didChange = false;
    AstDumper.maybeDumpAST(program, "EnumOrdinalizerTest_start");
    
    if (runMakeCallsStatic) {
      didChange = MakeCallsStatic.exec(program).didChange() || didChange;
      AstDumper.maybeDumpAST(program,
          "EnumOrdinalizerTest_after_makeCallsStatic");
    }
    if (runTypeTightener) {
      didChange = TypeTightener.exec(program).didChange() || didChange;
      AstDumper.maybeDumpAST(program,
          "EnumOrdinalizerTest_after_typeTightener");
    }
    if (runMethodCallTightener) {
      didChange = MethodCallTightener.exec(program).didChange() || didChange;
      AstDumper.maybeDumpAST(program,
          "EnumOrdinalizerTest_after_methodCallTightener");
    }
    if (runMethodInliner) {
      didChange = MethodInliner.exec(program).didChange() || didChange;
      AstDumper.maybeDumpAST(program,
          "EnumOrdinalizerTest_after_methodInliner");
    }
    
    didChange = EnumOrdinalizer.exec(program).didChange() || didChange;
    AstDumper.maybeDumpAST(program,
        "EnumOrdinalizerTest_after_EnumOrdinalizer");
    
    /*
     * Run these normalizers to sanity check the AST.  If there are any
     * dangling type substitutions, the CastNormalizer will generally find it.
     * If there are any introduced binary ops between an int and a null, the
     * EqualityNormalizer will find it.
     */
    if (runCastNormalizer) {
      CastNormalizer.exec(program, false);
    }
    if (runEqualityNormalizer) {
      EqualityNormalizer.exec(program);
    }
    
    return didChange;
  }
}
