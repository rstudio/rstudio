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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.impl.EnumOrdinalizer.Tracker;

/**
 * A set of tests for the conditions under which ordinalization is and is not allowed.  The
 * ordinalization is performed when allowed.
 * <p>
 * A complete test of the resulting ordinalization is not performed.  However, the
 * ImplementCastsAndTypeChecks and the EqualityNormalizer are run after the EnumOrdinalizer, to help
 * ensure the integrity of the AST, such that there are no partially mismatched type assignments or
 * comparisons, and that no binary operations between a primitive type and null have been added.
 * Typically, such errors introduced by the EnumOrdinalizer are caught by these normalizers, so it
 * makes sense to test the output in this way.  Thus, we provide confidence that the AST is left in
 * a coherent state, but it is not a complete test that ordinalization has completed correctly in
 * every respec.
 * <p>
 * NOTE: the tests in this test case are very fragile. On one hand EnumOrdinalizer requires other
 * passes to perform their cleanups to be effective; on the other some passes (DeadCodeElimination)
 * optimize enums in a way that the tests might become ineffective and some artificial constructs
 * are used to avoid this scenario. See {@link EnumOrdinalizerTest#setupNotInlineable()}.
 */
public class EnumOrdinalizerTest extends OptimizerTestBase {
  /*
   * Always run ImplementCastsAndTypeChecks and EqualityNormalizer, even in cases where we
   * are testing that ordinalization cannot occur, since there may be other
   * enums (such as DummyEnum) which do get ordinalized, and we want to test
   * that all is well regardless.
   */
  private final boolean performCastReplacement = true;
  private final boolean runEqualityNormalizer = true;
  // These are enabled as needed for a given test
  private boolean runMakeCallsStatic;
  private boolean runMethodInliner;
  private boolean runMethodCallTightener;
  private boolean runPruner;
  private boolean runTypeTightener;

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
    // Opportunities for ordinalization are only present after unused references to $VALUES are
    // pruned.
    // NOTE: because we are pruning, each test case needs to make sure that enums that are
    // considered for ordinalization are still live.
    runPruner = true;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testOrdinalizeBasicAssignment()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit apple = Fruit.APPLE;",
        "Fruit orange = Fruit.ORANGE;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeNewArrayAndAssignmentLocalRef()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result =
        optimize("void", "Fruit[] fruits = new Fruit[] {Fruit.APPLE, Fruit.ORANGE, Fruit.APPLE};",
            "if (fruits[0] == Fruit.APPLE) {",
            "  fruits[0] = Fruit.ORANGE;",
            "}");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeNewArrayOfArrayAndAssignmentLocalRef()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result =
        optimize("void", "Fruit[][] fruits = new Fruit[][] ",
            " {{Fruit.APPLE, Fruit.ORANGE},{Fruit.APPLE, Fruit.ORANGE}};",
            "if (fruits[0][1] == Fruit.APPLE) {",
            "  fruits[0][1] = Fruit.ORANGE;",
            "}");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeNewArrayAndAssignmentFieldRef()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("private final Fruit[] fruits = new Fruit[] ",
        "  {Fruit.APPLE, Fruit.ORANGE, Fruit.APPLE};");
    Result result = optimize("void", "EntryPoint ep = new EntryPoint();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizableFinalFieldUninitializedByDefault()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("private final Fruit uninitializedFinalFruit;",
        "public EntryPoint() {",
        "  uninitializedFinalFruit = Fruit.ORANGE;",
        "}");
    Result result = optimize("void", "EntryPoint ep = new EntryPoint();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeSwitchStatement()
      throws UnableToCompleteException {
    setupFruitEnum();
    setupFruitSwitchMethod();
    Result result = optimize("void", "String apple = fruitSwitch(Fruit.APPLE);",
        "String orange = fruitSwitch(Fruit.ORANGE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeIfStatement()
      throws UnableToCompleteException {
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
    Result result = optimize("void", "String apple = fruitIf(Fruit.APPLE);",
        "String orange = fruitIf(Fruit.ORANGE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeConditional()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void",
        "Fruit fruit = (new Integer(1)).toString().isEmpty() ? Fruit.APPLE : Fruit.ORANGE;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeFieldRefOrdinalMethodCall()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "int i = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeVariableRefOrdinalMethodCall()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "int i = fruit.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeUnusedEmptyEnum() throws UnableToCompleteException {
    setupEmptyEnum();

    Result result = optimize("void", "EmptyEnum myEnum;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$EmptyEnum") ||
        !tracker.isVisited("test.EntryPoint$EmptyEnum"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizeUnusedEnum() throws UnableToCompleteException {
    setupFruitEnum();

    Result result = optimize("void", "Fruit myEnum;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit") ||
        !tracker.isVisited("test.EntryPoint$Fruit"));

    // This enum is not referenced and it has gone away before ordinalizing, however make
    // sure there are no references anyway.
    tracker.addOrdinalized("test.EntryPoint$Fruit");
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
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
    Result result = optimize("void", "int i = switchMethodCall(Fruit.APPLE);",
        "Fruit fruit = Fruit.ORANGE;",
        "int j = switchMethodCall(fruit);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizableStaticFieldRef()
      throws UnableToCompleteException {
    // this will cause a static field ref in the enum clinit
    setupFruitEnumWithStaticField();
    Result result = optimize("void",
        "String y = Fruit.staticField + Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizableStaticMethod()
      throws UnableToCompleteException {
    // this will cause a static method enum class
    setupFruitEnumWithStaticMethod();
    Result result = optimize("void", "int y = Fruit.staticMethod() + Fruit.APPLE.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizableCallingValues()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "int l = Fruit.values().length;",
        "int ord = Fruit.APPLE.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizableStaticFieldRefToVALUES()
      throws UnableToCompleteException {
    // this ends up inlining the values() method call, and thus $VALUES is referenced external
    // to the Fruit enum class.
    setupFruitEnum();
    Result result = optimize("void", "Fruit[] fruits = Fruit.values();",
        "int ord = Fruit.APPLE.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testOrdinalizableStaticMethodThatRefsValuesLength()
      throws UnableToCompleteException {
    // this will cause a static method that references values().length
    setupFruitEnumWithStaticMethodThatRefsValuesLength();
    Result result = optimize("void", "Fruit y = Fruit.forInteger(0);",
        "int ord = Fruit.APPLE.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableInstanceStaticFieldRef()
      throws UnableToCompleteException {
    // this will cause a static field ref in the enum clinit
    setupFruitEnumWithStaticField();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "String y = fruit.staticField;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableInstanceStaticMethod()
      throws UnableToCompleteException {
    // this will cause a static method enum class
    setupFruitEnumWithStaticMethod();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "int y = fruit.staticMethod();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableClassLiteralReference()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Class clazz = Fruit.class;",
        "String clazzStr = clazz.toString() + Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableEnumValueOfWithClassLiteralArg()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Object Carrot = Enum.valueOf(Fruit.class, \"APPLE\");",
        "String carrot = Carrot.toString() + Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableGetClassMethodCall()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Class clazz = Fruit.APPLE.getClass();",
        "String clazzStr = clazz.toString() + Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableExplicitCastToEnumClass()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Object obj = new Object();",
        "Fruit fruit = (Fruit) obj;",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableExplicitCastToArrayOfEnumClass()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Enum[] enumArray = new Enum[10];",
        "Fruit[] fruitArray = (Fruit[]) enumArray;",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableExplicitCastFromArrayOfEnumClass()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit[] fruitArray = new Fruit[10];",
        "Enum[] enumArray = (Enum[]) fruitArray;",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableExplicitCastToArrayOfArrayOfEnumClass()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Enum[][] enumArray = new Enum[10][10];",
        "Fruit[][] fruitArray = (Fruit[][]) enumArray;",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableExplicitCastFromArrayOfArrayOfEnumClass()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit[][] fruitArray = new Fruit[10][10];",
        "Enum[][] enumArray = (Enum[][]) fruitArray;",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableExplicitCastFromEnumClass()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Enum Carrot = (Enum) Fruit.APPLE;",
        "String carrot = Carrot.toString();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableOrdinalMethodRefFromExplicitCastWithBlackListableSubExpression()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "int ord = " +
        "((Fruit) Enum.valueOf(Fruit.class,\"APPLE\")).ordinal();",
        "int ord2 = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableInstanceFieldRef()
      throws UnableToCompleteException {
    // this will cause an instance field ref in the enum constructor
    setupFruitEnumWithInstanceField();
    Result result = optimize("void", "String instanceField = Fruit.APPLE.instanceField;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableInstanceOfEnumExpression()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "if (fruit instanceof Enum) {",
        "  fruit = Fruit.ORANGE;",
        "}");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableInstanceOfEnumTestType()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Object fruitObj = new Object();",
        "if (fruitObj instanceof Fruit) {",
        "  fruitObj = null;",
        "}",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableStaticMethodCallValues()
      throws UnableToCompleteException {
    // make sure values() method call doesn't doesn't get inlined
    runMethodInliner = false;

    setupFruitEnum();
    Result result = optimize("void", "Fruit[] fruits = Fruit.values();",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableJsniFieldRef()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static Fruit instanceFruit;");
    addSnippetClassDecl("public static native void jsniMethod() /*-{",
        "  var x = @test.EntryPoint::instanceFruit",
        "}-*/");
    Result result = optimize("void", "instanceFruit = Fruit.APPLE;",
        "jsniMethod();",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableJsniFieldRefStatic()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static native void jsniMethod() /*-{",
        "  var x = @test.EntryPoint.Fruit::APPLE",
        "}-*/");
    Result result = optimize("void", "jsniMethod();",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableJsniFieldRefClassLiteral()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static native void jsniMethod() /*-{",
        "  var x = @test.EntryPoint.Fruit::class",
        "}-*/");
    Result result = optimize("void", "jsniMethod();",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpAssignment()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void",
        "Enum tomato;",
        "tomato = Fruit.APPLE;",
        "int ord = tomato.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastFieldInitializedWithNullByDefault()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("static private Fruit uninitializedFruitAsNull;");
    Result result = optimize("void", "if (uninitializedFruitAsNull != Fruit.APPLE) {",
        "  uninitializedFruitAsNull = Fruit.ORANGE;",
        "}",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpEquals()
      throws UnableToCompleteException {
    setupFruitAndVegetableEnums();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "Enum carrot = (Enum) Vegetable.CARROT;",
        "boolean test = (fruit == carrot);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpNotEquals()
      throws UnableToCompleteException {
    setupFruitAndVegetableEnums();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "Enum carrot = (Enum) Vegetable.CARROT;",
        // do in opposite order from OpEquals test
        "boolean test = (carrot != fruit);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpEqualsNull()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static boolean testIsNull(Fruit fruit) {",
        "  if (fruit == null) {",
        "    return true;",
        "    } else {",
        "    return false;",
        "    }",
        "}");
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "boolean isNull = testIsNull(fruit) || testIsNull(Fruit.ORANGE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpNotEqualsNull()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static boolean testIsNull(Fruit fruit) {",
        "  if (fruit != null) {",
        "    return true;",
        "  } else {",
        "    return false;",
        "  }",
        "}");
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "boolean isNull = testIsNull(fruit) || testIsNull(Fruit.ORANGE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpStringConcat()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "String str = \"A string followed by \" + fruit;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpStringConcat2()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "String str = fruit + \" followed by a string\";");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastBinaryOpStringConcatAssignment()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit = Fruit.APPLE;",
        "String str = \"A string concatenated with: \";",
        "str += fruit;");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastDeclarationToNull()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit = null;",
        "int ord = fruit == null ? Util.notInlineable(Fruit.APPLE).ordinal() : fruit.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastAssignmentToNull()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Fruit fruit;",
        "fruit = null;",
        "int ord = fruit == null ? Util.notInlineable(Fruit.APPLE).ordinal() : fruit.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastDeclaration()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Enum tomato = Fruit.APPLE;",
        "int ord = Fruit.APPLE.ordinal() + tomato.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastConditional()
      throws UnableToCompleteException {
    setupFruitAndVegetableEnums();
    Result result = optimize("void", "Enum tomato = null;",
        "tomato = (true) ? Fruit.APPLE : Vegetable.CARROT;",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal() +",
        "    Util.notInlineable(Vegetable.CARROT).ordinal() + tomato.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertTrue(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastOverriddenMethodReturnType()
      throws UnableToCompleteException {

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
    Result result = optimize("void", "EntryPoint ep = new EntryPoint();",
        "AbstractClass abstractClass1 = ep.new CustomClass1();",
        "AbstractClass abstractClass2 = ep.new CustomClass2();",
        "testEnumClass(abstractClass1);",
        "testEnumClass(abstractClass2);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$EnumClass1"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$EnumClass1"));
    assertTrue(tracker.isVisited("test.EntryPoint$EnumClass2"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$EnumClass2"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastMethodCallArgs()
      throws UnableToCompleteException {
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
    Result result = optimize("void", "String stringApple = getEnumString(Fruit.APPLE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastMethodCallArgsNewArray()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static String getEnumString(Enum[] myEnumArray) {",
        "  String retString = \"\";",
        "  for (Enum myEnum : myEnumArray) {",
        "    retString += myEnum.name();",
        "  }",
        "  return retString;",
        "}");
    Result result = optimize("void",
        "String stringFruits = getEnumString(new Enum[] {Fruit.APPLE, Fruit.ORANGE});");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastMethodCallVarArgs()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static String getEnumString(Enum...myEnumArray) {",
        "  String retString = \"\";",
        "  for (Enum myEnum : myEnumArray) {",
        "    retString += myEnum.name();",
        "  }",
        "  return retString;",
        "}");
    Result result = optimize("void",
        "String stringFruits = getEnumString(Fruit.APPLE, Fruit.ORANGE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastNewArrayElements()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void", "Enum[] enums = new Enum[] {Fruit.APPLE, Fruit.ORANGE};");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastNewArrayArrayElements()
      throws UnableToCompleteException {
    setupFruitEnum();
    Result result = optimize("void",
        "Enum[][] enums = new Enum[][] {{Fruit.APPLE, Fruit.ORANGE},{Fruit.ORANGE, Fruit.APPLE}};");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableJsniMethodBodyParams()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static native void passEnumToJsniMethod(Enum myEnum) /*-{",
        "  myEnum == null; }-*/");
    Result result = optimize("void", "passEnumToJsniMethod(Fruit.APPLE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastJsniMethodBodyParams()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static native void passEnumToJsniMethod(Fruit myEnum) /*-{",
        "   myEnum == null; }-*/;");
    Result result = optimize("void", "passEnumToJsniMethod(Fruit.APPLE);");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableJsniMethodBodyCall()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static native void consumeFruitViaJsni() /*-{",
        "  var myJso = @test.EntryPoint::calledFromJsni(*)();",
        "}-*/;",
        "public static Fruit calledFromJsni() {",
        "  return Fruit.APPLE;",
        "}");
    Result result = optimize("void", "consumeFruitViaJsni();",
        "int ord = Fruit.APPLE.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastJsniMethodRefParams()
      throws UnableToCompleteException {
    setupFruitEnum();
    setupFruitSwitchMethod();
    addSnippetClassDecl("public static native void fruitSwitchViaJsni() /*-{",
        "  var myJso;",
        "  var result = @test.EntryPoint::fruitSwitch(Ltest/EntryPoint$Fruit;)(myJso);",
        "}-*/");
    Result result = optimize("void", "fruitSwitchViaJsni();",
        "int ord = Util.notInlineable(Fruit.APPLE).ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastJsniMethodRefReturnType()
      throws UnableToCompleteException {
    setupFruitEnum();
    addSnippetClassDecl("public static Fruit returnSomeFruit() {",
        "  return Fruit.APPLE;",
        "}");
    addSnippetClassDecl("public static native void jsniMethodRefWithEnumReturn() /*-{",
        "  var result = @test.EntryPoint::returnSomeFruit()();",
        "}-*/");
    Result result = optimize("void", "jsniMethodRefWithEnumReturn();",
        "int ord = Fruit.APPLE.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  public void testNotOrdinalizableImplicitUpcastReturnStatement()
      throws UnableToCompleteException {
    setupFruitAndVegetableEnums();
    addSnippetClassDecl("public static Enum returnAsEnum(int mode) {",
        "  if (mode == 0) {",
        "    return Fruit.APPLE;",
        "  } else {",
        "    return Vegetable.CARROT;",
        "  }",
        "}");
    Result result = optimize("void", "Enum myEnum = returnAsEnum(0);",
        // do a second one, to prevent inlining
        "Enum myOtherEnum = returnAsEnum(1);",
        "int ord = myEnum.ordinal() + myOtherEnum.ordinal();");

    EnumOrdinalizer.Tracker tracker = EnumOrdinalizer.getTracker();
    assertTrue(tracker.isVisited("test.EntryPoint$Fruit"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Fruit"));
    assertTrue(tracker.isVisited("test.EntryPoint$Vegetable"));
    assertFalse(tracker.isOrdinalized("test.EntryPoint$Vegetable"));
    assertAllEnumOrdinalizedReferencesReplaced(result.getOptimizedProgram(), tracker);
  }

  private void setupEmptyEnum() {
    addSnippetClassDecl("public enum EmptyEnum {}");
  }

  private void setupFruitEnum() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE}");
    setupNotInlineable("Fruit");
  }

  private void setupFruitEnumWithInstanceField() {
    addSnippetClassDecl("public enum Fruit {APPLE(\"a\"), ORANGE(\"b\");",
        "  public final String instanceField;",
        "  private Fruit(String str) {",
        "    instanceField = str;",
        "  }",
        "}");
    setupNotInlineable("Fruit");
  }

  private void setupFruitEnumWithStaticField() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE;",
        "  public static String staticField = \"STATIC\";",
        "}");
    setupNotInlineable("Fruit");
  }

  private void setupFruitEnumWithStaticMethod() {
    addSnippetImport("javaemul.internal.annotations.DoNotInline");
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE;",
        "  @DoNotInline",
        "  public static final int staticMethod() {",
        "    int x = 0;",
        "    return x;",
        "  }",
        "}");
    setupNotInlineable("Fruit");
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
    setupNotInlineable("Fruit");
  }

  private void setupVegetableEnum() {
    addSnippetClassDecl("public enum Vegetable {CARROT, SPINACH}");
    setupNotInlineable("Vegetable");
  }

  private void setupFruitAndVegetableEnums() {
    addSnippetClassDecl("public enum Fruit {APPLE, ORANGE}");
    addSnippetClassDecl("public enum Vegetable {CARROT, SPINACH}");
    setupNotInlineable("Fruit", "Vegetable");
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
  private void setupNotInlineable(String... classes) {
    addSnippetClassDecl("public static class Util {");
    for (String clazz : classes) {
      addSnippetClassDecl(
          "  public static " + clazz + " notInlineable(" + clazz + " obj) {",
          "    if (new Integer(1).toString().isEmpty()) return obj;",
          "    return obj;",
          "  }");
    }
    addSnippetClassDecl("}");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
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
    program.addEntryMethod(findMainMethod(program));

    OptimizerContext optimizerContext = new FullOptimizerContext(program);
    if (runMakeCallsStatic) {
      didChange = MakeCallsStatic.exec(program, false, optimizerContext).didChange() || didChange;
    }
    if (runTypeTightener) {
      didChange = TypeTightener.exec(program, optimizerContext).didChange() || didChange;
    }
    if (runMethodCallTightener) {
      didChange = MethodCallTightener.exec(program, optimizerContext).didChange() || didChange;
    }
    if (runMethodInliner) {
      didChange = MethodInliner.exec(program, optimizerContext).didChange() || didChange;
    }
    if (runPruner) {
      didChange = Pruner.exec(program, true, optimizerContext).didChange() || didChange;
    }

    didChange = EnumOrdinalizer.exec(program, optimizerContext).didChange() || didChange;

    /*
     * Run these normalizers to sanity check the AST.  If there are any
     * dangling type substitutions, the ImplementCastsAndTypeChecks will generally find it.
     * If there are any introduced binary ops between an int and a null, the
     * EqualityNormalizer will find it.
     */
    if (performCastReplacement) {
      ComputeCastabilityInformation.exec(program, false);
      ImplementCastsAndTypeChecks.exec(program, false);
    }
    if (runEqualityNormalizer) {
      EqualityNormalizer.exec(program);
    }

    return didChange;
  }

  private void assertAllEnumOrdinalizedReferencesReplaced(JProgram program, final Tracker tracker) {
    new JVisitor() {
      @Override
      public void endVisit(JFieldRef x, Context ctx) {
        assertTrue(x.getField() + " was not replaced everywhere",
            ctx.isLvalue() || !tracker.isOrdinalized(x.getEnclosingType().getName()));
      }

      @Override
      public void endVisit(JDeclarationStatement x, Context ctx) {
        assertTrue(x.getVariableRef().getTarget() + " was not replaced everywhere",
            x.getInitializer() instanceof JValueLiteral ||
                !(x.getVariableRef() instanceof JFieldRef) ||
                !tracker.isOrdinalized(
                    ((JFieldRef) x.getVariableRef()).getField().getEnclosingType().getName()));
      }
    }.accept(program);
  }
}
