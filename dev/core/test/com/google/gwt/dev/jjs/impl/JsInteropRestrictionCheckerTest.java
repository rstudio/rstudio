/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for the JsInteropRestrictionChecker.
 */
public class JsInteropRestrictionCheckerTest extends OptimizerTestBase {

  // TODO: eventually test this for default methods in Java 8.
  public void testCollidingAccidentalOverrideConcreteMethodFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "  void doIt(Bar bar);",
        "}",
        "public static class ParentBuggy {",
        "  public void doIt(Foo foo) {}",
        "  public void doIt(Bar bar) {}",
        "}",
        "public static class Buggy extends ParentBuggy implements Foo, Bar {",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.doIt(Ltest/EntryPoint$Bar;)V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the name 'doIt' is already taken.");
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "  void doIt(Bar bar);",
        "}",
        "public static abstract class Baz implements Foo, Bar {",
        "  public abstract void doIt(Foo foo);",
        "  public abstract void doIt(Bar bar);",
        "}",
        "public static class Buggy {}  // Unrelated class");

    assertBuggyFails(
        "'test.EntryPoint$Baz.doIt(Ltest/EntryPoint$Bar;)V' can't be exported in type "
            + "'test.EntryPoint$Baz' because the name 'doIt' is already taken.");
  }

  public void testCollidingAccidentalOverrideHalfAndHalfFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "   void doIt(Bar bar);",
        "}",
        "public static class ParentParent {",
        "  public void doIt(Bar x) {}",
        "}",
        "@JsType",
        "public static class Parent extends ParentParent {",
        "  public void doIt(Foo x) {}",
        "}",
        "public static class Buggy extends Parent implements Bar {}");

    assertBuggyFails(
        "'test.EntryPoint$Parent.doIt(Ltest/EntryPoint$Foo;)V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the name 'doIt' is already taken.");
  }

  public void testCollidingFieldExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsProperty",
        "  public static final int show = 0;",
        "  @JsProperty(name = \"show\")",
        "  public static final int display = 0;",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.display' can't be exported because the "
            + "global name 'test.EntryPoint.Buggy.show' is already taken.");
  }

  public void testJsPropertyGetterStyleSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty int getX();",
        "  @JsProperty void setX(int x);",
        "  @JsProperty boolean isY();",
        "  @JsProperty void setY(boolean y);",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyIncorrectGetterStyleFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty int isX();",
        "  @JsProperty int getY(int x);",
        "  @JsProperty void getZ();",
        "  @JsProperty void setX(int x, int y);",
        "  @JsProperty void setY();",
        "  @JsProperty int setZ(int z);",
        "}");

    assertBuggyFails(
        "There can't be non-boolean return for the JsProperty 'is' getter"
            + " 'test.EntryPoint$Buggy.isX()I'.",
        "There can't be void return type or any parameters for the JsProperty getter"
            + " 'test.EntryPoint$Buggy.getY(I)I'.",
        "There can't be void return type or any parameters for the JsProperty getter"
            + " 'test.EntryPoint$Buggy.getZ()V'.",
        "There needs to be single parameter and void return type for the JsProperty setter"
            + " 'test.EntryPoint$Buggy.setX(II)V'.",
        "There needs to be single parameter and void return type for the JsProperty setter"
            + " 'test.EntryPoint$Buggy.setY()V'.",
        "There needs to be single parameter and void return type for the JsProperty setter"
            + " 'test.EntryPoint$Buggy.setZ(I)I'.");
  }

  public void testJsPropertyNonGetterStyleFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty boolean hasX();",
        "  @JsProperty int x();",
        "  @JsProperty void x(int x);",
        "}");

    assertBuggyFails(
        "JsProperty 'test.EntryPoint$Buggy.hasX()Z' doesn't follow Java Bean naming conventions.",
        "JsProperty 'test.EntryPoint$Buggy.x()I' doesn't follow Java Bean naming conventions.",
        "JsProperty 'test.EntryPoint$Buggy.x(I)V' doesn't follow Java Bean naming conventions.");
  }

  public void testCollidingJsPropertiesTwoGettersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  boolean isX();",
        "  @JsProperty",
        "  boolean getX();",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean isX() {return false;}",
        "  public boolean getX() {return false;}",
        "}");

    assertBuggyFails(
        "There can't be more than one getter for JsProperty 'x' in type 'test.EntryPoint$IBuggy'.",
        "There can't be more than one getter for JsProperty 'x' in type 'test.EntryPoint$Buggy'.");
  }

  public void testCollidingJsPropertiesTwoSettersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  void setX(boolean x);",
        "  @JsProperty",
        "  void setX(int x);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public void setX(boolean x) {}",
        "  public void setX(int x) {}",
        "}");

    assertBuggyFails(
        "There can't be more than one setter for JsProperty 'x' in type 'test.EntryPoint$IBuggy'.",
        "There can't be more than one setter for JsProperty 'x' in type 'test.EntryPoint$Buggy'.");
  }

  // TODO: duplicate this check with two @JsType interfaces.
  public void testCollidingJsTypeAndJsPropertyGetterFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  int getX();",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public int getX() {return 0;}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$IBuggy.x(Z)Z' and 'test.EntryPoint$IBuggy.getX()I' "
        + "can't both be named 'x' in type 'test.EntryPoint$IBuggy'.",
        "'test.EntryPoint$Buggy.x(Z)Z' and 'test.EntryPoint$Buggy.getX()I' "
        + "can't both be named 'x' in type 'test.EntryPoint$Buggy'.");
  }

  public void testCollidingJsTypeAndJsPropertySetterFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  void setX(int a);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public void setX(int a) {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$IBuggy.x(Z)Z' and 'test.EntryPoint$IBuggy.setX(I)V' "
        + "can't both be named 'x' in type 'test.EntryPoint$IBuggy'.",
        "'test.EntryPoint$Buggy.x(Z)Z' and 'test.EntryPoint$Buggy.setX(I)V' "
        + "can't both be named 'x' in type 'test.EntryPoint$Buggy'.");
  }

  public void testCollidingMethodExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod",
        "  public static void show() {}",
        "  @JsMethod(name = \"show\")",
        "  public static void display() {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.display()V' can't be exported "
            + "because the global name 'test.EntryPoint.Buggy.show' is already taken.");
  }

  public void testCollidingMethodToFieldExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod",
        "  public static void show() {}",
        "  @JsProperty",
        "  public static final int show = 0;",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.show()V' can't be exported because the "
            + "global name 'test.EntryPoint.Buggy.show' is already taken.");
  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "  public final int show = 0;",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.show()V' can't be exported in type "
            + "'test.EntryPoint$Buggy' because the name 'show' is already taken.");
  }

  public void testCollidingMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show(int x) {}",
        "  public void show() {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.show()V' can't be exported in type "
            + "'test.EntryPoint$Buggy' because the name 'show' is already taken.");
  }

  public void testCollidingSubclassExportedFieldToFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassExportedFieldToMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassExportedMethodToMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToExportedFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$ParentBuggy.foo' can't be exported in type "
            + "'test.EntryPoint$Buggy' because the name 'foo' is already taken.");
  }

  public void testCollidingSubclassFieldToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$ParentBuggy.foo' can't be exported in type "
            + "'test.EntryPoint$Buggy' because the name 'foo' is already taken.");
  }

  public void testCollidingSubclassMethodToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassMethodToMethodInterfaceJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public interface IBuggy1 {",
        "  void show();",
        "}",
        "@JsType",
        "public interface IBuggy2 {",
        "  void show(boolean b);",
        "}",
        "public static class Buggy implements IBuggy1 {",
        "  public void show() {}",
        "}",
        "public static class Buggy2 extends Buggy implements IBuggy2 {",
        "  public void show(boolean b) {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.show()V' can't be exported in type "
            + "'test.EntryPoint$Buggy2' because the name 'show' is already taken.");
  }

  public void testCollidingSubclassMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$ParentBuggy.foo()V' can't be exported in type "
            + "'test.EntryPoint$Buggy' because the name 'foo' is already taken.");
  }

  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public interface IParentBuggy1 {",
        "  void show();",
        "}",
        "public interface IBuggy1 extends IParentBuggy1 {",
        "}",
        "@JsType",
        "public interface IParentBuggy2 {",
        "  void show(boolean b);",
        "}",
        "public interface IBuggy2 extends IParentBuggy2 {",
        "}",
        "public static class Buggy implements IBuggy1 {",
        "  public void show() {}",
        "}",
        "public static class Buggy2 extends Buggy implements IBuggy2 {",
        "  public void show(boolean b) {}",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy.show()V' can't be exported in type "
            + "'test.EntryPoint$Buggy2' because the name 'show' is already taken.");
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface Comparable<T> {",
        "  int compareTo(T other);",
        "}",
        "@JsType",
        "public static class Enum<E extends Enum<E>> implements Comparable<E> {",
        "  public int compareTo(E other) {return 0;}",
        "}",
        "public static class Buggy {}");

    assertBuggySucceeds();
  }

  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class ParentBuggy extends ParentParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$ParentParentBuggy.foo' can't be exported in type "
            + "'test.EntryPoint$Buggy' because the name 'foo' is already taken.");
  }

  public void testConsistentPropertyTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public int getFoo();",
        "  @JsProperty",
        "  public void setFoo(int value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int getFoo() {return 0;}",
        "  public void setFoo(int value) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testInconsistentGetSetPropertyTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public int getFoo();",
        "  @JsProperty",
        "  public void setFoo(Integer value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int getFoo() {return 0;}",
        "  public void setFoo(Integer value) {}",
        "}");

    assertBuggyFails(
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$IBuggy' "
            + "must have consistent types.",
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$Buggy' "
            + "must have consistent types.");
  }

  public void testInconsistentIsSetPropertyTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public boolean isFoo();",
        "  @JsProperty",
        "  public void setFoo(Object value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean isFoo() {return false;}",
        "  public void setFoo(Object value) {}",
        "}");

    assertBuggyFails(
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$IBuggy' "
            + "must have consistent types.",
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$Buggy' "
            + "must have consistent types.");
  }

  public void testJsPropertySuperCallFails()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public int m() { return super.getX(); }",
        "}");

    assertBuggyFails(
        "Cannot call property accessor 'test.EntryPoint$Super.getX()I' via super "
            + "(test/EntryPoint.java:9).");
  }

  public void testJsPropertyCallSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public int m() { return getX(); }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyAccidentalSuperCallSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public interface Interface {",
        "  @JsProperty int getX();",
        "}",

        "@JsType public static class Buggy extends Super implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testMultiplePrivateConstructorsExportSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  private Buggy() {}",
        "  private Buggy(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testMultiplePublicConstructorsAllDelegatesToJsConstructorSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  @JsIgnore",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testMultipleConstructorsNotAllDelegatedToJsConstructorFails()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  private Buggy(int a) {",
        "    new Buggy();",
        "  }",
        "}");

    assertBuggyFails(
        "Constructor 'test.EntryPoint$Buggy.EntryPoint$Buggy() <init>' can be a JsConstructor only "
            + "if all constructors in the class are delegating to it.");
  }

  public void testMultiplePublicConstructorsExportFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}");

    assertBuggyFails(
        "More than one JsConstructor exists for test.EntryPoint$Buggy.",
        "'test.EntryPoint$Buggy.EntryPoint$Buggy(I) <init>' can't be "
            + "exported because the global name 'test.EntryPoint.Buggy' is already taken.");
  }

  public void testNonCollidingAccidentalOverrideSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Foo {",
        "  void doIt(Object foo);",
        "}",
        "public static class ParentParent {",
        "  public void doIt(String x) {}",
        "}",
        "@JsType",
        "public static class Parent extends ParentParent {",
        "  public void doIt(Object x) {}",
        "}",
        "public static class Buggy extends Parent implements Foo {}");

    assertBuggySucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public static void show1() {}",
        "  public void show2() {}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionWithNoExtendsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction",
        "public interface Buggy {",
        "  void foo();",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionExtendsInterfaceFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "interface AnotherInterface {}",
        "@JsFunction",
        "public interface Buggy extends AnotherInterface {",
        "  void foo();",
        "}");

    assertBuggyFails("JsFunction 'test.EntryPoint$Buggy' cannot extend other interfaces.");
  }

  public void testJsFunctionExtendedByInterfaceFails() throws Exception {
    addAll(jsFunctionInterface);

    addSnippetClassDecl("public interface Buggy extends MyJsFunctionInterface {}");

    assertBuggyFails("JsFunction 'test.MyJsFunctionInterface' cannot be extended by other "
        + "interfaces:\n\ttest.EntryPoint$Buggy");
  }

  public void testJsFunctionMarkedAsJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction @JsType",
        "public interface Buggy {",
        "  void foo();",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy' cannot be both a JsFunction and a JsType at the same time.");
  }

  public void testJsFunctionImplementationWithSingleInterfaceSucceeds() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class Buggy implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionImplementationWithMultipleSuperInterfacesFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "interface AnotherInterface {}",
        "public static class Buggy implements MyJsFunctionInterface, AnotherInterface {",
        "  public int foo(int x) { return 0; }",
        "  public int bar(int x) { return 0; }",
        "}");

    assertBuggyFails("JsFunction implementation 'test.EntryPoint$Buggy' cannot implement more than "
        + "one interface.");
  }

  public void testJsFunctionImplementationWithSuperClassFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class BaseClass {}",
        "public static class Buggy extends BaseClass implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggyFails("JsFunction implementation 'test.EntryPoint$Buggy' cannot extend a class.");
  }

  public void testJsFunctionImplementationWithSubclassesFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class BaseClass implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}",
        "public static class Buggy extends BaseClass  {",
        "}");

    assertBuggyFails("Implementation of JsFunction 'test.EntryPoint$BaseClass' cannot be extended "
        + "by other classes:\n\ttest.EntryPoint$Buggy");
  }

  public void testJsFunctionImplementationMarkedAsJsTypeFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy' cannot be both a JsFunction implementation and a JsType at the "
            + "same time.");
  }

  public void testJsFunctionStaticInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "public static String someString() { return \"hello\"; }",
        "@JsFunction public interface Buggy {",
        "  static String s = someString();",
        "  void m();",
        "}");

    assertBuggyFails(
        "JsFunction 'test.EntryPoint$Buggy' cannot have static initializer.");
  }

  public void testNativeJsTypeStaticInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public static String s = \"hello\";",
        "  static {  s += \"hello\"; }",
        "}");

    assertBuggyFails(
        "Native JsType 'test.EntryPoint$Buggy' cannot have static initializer.");
  }

  public void testNativeJsTypeNonEmptyConstructorFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public Buggy(int n) {",
        "    n++;",
        "  }",
        "}");

    assertBuggyFails(
        "Native JsType constructor 'test.EntryPoint$Buggy.EntryPoint$Buggy(I) <init>' "
            + "cannot have non-empty method body.");
  }

  public void testNativeJsTypeInstanceInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public int x = 1;",
        "  public Buggy(int n) {",
        "  }",
        "}");

    assertBuggyFails(
        "Native JsType constructor 'test.EntryPoint$Buggy.EntryPoint$Buggy(I) <init>' "
            + "cannot have non-empty method body.");
  }

  public void testNativeJsTypeImplicitSuperSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super() {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super(int x) {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "    super(n);",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super(int x) {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "    super(n++);",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeInlineStaticInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public static final String s = new String(\"hello\");",
        "}");

    assertBuggyFails(
        "Native JsType 'test.EntryPoint$Buggy' cannot have static initializer.");
  }

  public void testNativeJsTypeInterfaceInlineInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Buggy {",
        "  static final String s = new String(\"hello\");",
        "}");

    assertBuggyFails(
        "Native JsType 'test.EntryPoint$Buggy' cannot have static initializer.");
  }

  public void testNativeJsTypeCompileTimeConstantSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public static final String s = \"hello\";",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeInterfaceInInstanceofFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface IBuggy {}",
        "@JsType public static class Buggy {",
        "  public Buggy() { if (new Object() instanceof IBuggy) {} }",
        "}");

    assertBuggyFails("Cannot do instanceof against native JsType interface test.EntryPoint$IBuggy "
        + "(test/EntryPoint.java:6).");
  }

  public void testNativeJsTypeEnumFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public enum Buggy { A, B }");

    assertBuggyFails(
        "Enum 'test.EntryPoint$Buggy' cannot be a native JsType.");
  }

  public void testNativeJsTypeInterfaceCompileTimeConstantSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Buggy {",
        "  static final String s = \"hello\";",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Super {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public final void m() { }",
        "  @JsOverlay private final void n() { }",
        "  @JsOverlay final void o() { }",
        "  @JsOverlay protected final void p() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnStaticFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public static final void m() { }",
        "}");

    assertBuggyFails(
        "JsOverlay method 'test.EntryPoint$Buggy.m()V' cannot be non-final, static, nor native.");
  }

  public void testJsOverlayImplementingInterfaceMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface IBuggy {",
        "  void m();",
        "}",
        "@JsType(isNative=true) public static class Buggy implements IBuggy {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails(
        "JsOverlay method 'test.EntryPoint$Buggy.m()V' cannot override a supertype method.");
  }

  public void testJsOverlayOverridingSuperclassMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m();",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails(
        "JsOverlay method 'test.EntryPoint$Buggy.m()V' cannot override a supertype method.");
  }

  public void testJsOverlayOnNonFinalFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails(
        "JsOverlay method 'test.EntryPoint$Buggy.m()V' cannot be non-final, static, nor native.");
  }

  public void testJsOverlayOnNativeMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public final native void m();",
        "}");

    assertBuggyFails(
        "JsOverlay method 'test.EntryPoint$Buggy.m()V' cannot be non-final, static, nor native.");
  }

  public void testJsOverlayOnJsoMethodSucceeds() throws UnableToCompleteException {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy extends JavaScriptObject {",
        "  protected Buggy() { }",
        "  @JsOverlay public final void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testImplicitJsOverlayOnJsoMethodSucceeds() throws UnableToCompleteException {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy extends JavaScriptObject {",
        "  protected Buggy() { }",
        "  public final void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnNonNativeJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType public static class Buggy {",
        "  @JsOverlay public final void m() { };",
        "}");

    assertBuggyFails(
        "Method 'test.EntryPoint$Buggy.m()V' in non-native type cannot be @JsOverlay.");
  }

  public void testJsOverlayOnNonJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsOverlay public final void m() { };",
        "}");

    assertBuggyFails(
        "Method 'test.EntryPoint$Buggy.m()V' in non-native type cannot be @JsOverlay.");
  }

  public void testJsTypeExtendsNativeJsTypeSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeExtendsNonJsTypeSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class Super {",
        "}",
        "@JsType public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "@JsType public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Interface {",
        "}",
        "@JsType public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeIntefaceExtendsNativeJsTypeInterfaceSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "@JsType public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Interface {",
        "}",
        "@JsType public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExtendsNaiveJsTypeSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeNonPublicFieldFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  int f;",
        "}");

    assertBuggyFails(
        "Native JsType member 'test.EntryPoint$Buggy.f' is not public or has @JsIgnore.");
  }

  public void testNativeJsTypeJsIgnoredFieldFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  @JsIgnore public int x;",
        "}");

    assertBuggyFails(
        "Native JsType member 'test.EntryPoint$Buggy.x' is not public or has @JsIgnore.");
  }

  public void testNativeJsTypeNonPublicMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  native void m();",
        "}");

    assertBuggyFails(
        "Native JsType member 'test.EntryPoint$Buggy.m()V' is not public or has @JsIgnore.");
  }

  public void testNativeJsTypeJsIgnoredMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  @JsIgnore public native void m();",
        "}");

    assertBuggyFails(
        "Native JsType member 'test.EntryPoint$Buggy.m()V' is not public or has @JsIgnore.");
  }

  public void testNativeJsTypeJsIgnoredConstructorFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  @JsIgnore public Buggy() { }",
        "}");

    assertBuggyFails(
        "Native JsType member 'test.EntryPoint$Buggy.EntryPoint$Buggy() <init>' "
          + "is not public or has @JsIgnore.");
  }

  public void testNativeJsTypeNonPublicConstructorSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  Buggy() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendsJsTypeSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "}",
        "public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendsNativeJsTypeSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds()
      throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testUnusableByJsSuppressionSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl("public static class A {}");
    addSnippetClassDecl(
        "@JsType @SuppressWarnings(\"unusable-by-js\")", // SuppressWarnings on the class.
        "public static class B {",
        "  public A field;",
        "  public A t0(A a, A b) { return null; }",
        "}");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  @SuppressWarnings(\"unusable-by-js\") public A field;", // add SuppressWarnings to field.
        "  @SuppressWarnings({\"unusable-by-js\", \"unused\"})", // test multiple warnings.
        "  public A t0(A a, A b) { return null; }", // add SuppressWarnings to the method.
        "  public void t1(",
        "    @SuppressWarnings(\"unusable-by-js\")A a,",
        "    @SuppressWarnings(\"unusable-by-js\")A b",
        "  ) {}", // add SuppressWarnings to parameters.
        "}");

    assertBuggySucceeds();
  }

  public void testUsableByJsTypesSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsExport");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "@JsType public static class A {}",
        "@JsType public static interface I {}",
        "@JsFunction public static interface FI {void foo();}",
        "public static class C extends JavaScriptObject {protected C(){}}",
        "@JsType public static class Buggy {",
        "  public void f1(boolean a, int b, double c) {}", // primitive types work fine.
        "  public void f2(Boolean a, Double b, String c) {}", // unboxed types work fine.
        "  public void f3(A a) {}", // JsType works fine.
        "  public void f4(I a) {}", // JsFunction works fine.
        "  public void f5(FI a) {}", // JsFunction works fine.
        "  public void f6(C a) {}", // JavaScriptObject works fine.
        "  public void f7(Object a) {}", // Java Object works fine.
        "  public void f8(boolean[] a) {}", // array of primitive types work fine.
        "  public void f9(Boolean[] a, Double[] b, String[] c) {}", // array of unboxed types.
        "  public void f10(A[] a) {}", // array of JsType works fine.
        "  public void f11(FI[] a) {}", // array of JsFunction works fine.
        "  public void f12(C[][] a) {}", // array of JavaScriptObject works fine.
        "}");
    assertBuggySucceeds();
  }

  public void testUnusableByJsNotExportedMembersSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class A {}",
        "@JsType public static class Buggy {",
        "  private A field;", // private field.
        "  private A f1(A a) { return null; }", // private method.
        "}");
    assertBuggySucceeds();
  }

  public void testUnusuableByJsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class A {}",
        "@JsType public static interface I {}",
        "public static class B implements I {}",
        "public static class C {", // non-jstype class with JsMethod
        "  @JsMethod",
        "  public static void fc1(A a) {}", // JsMethod
        "}",
        "public static class D {", // non-jstype class with JsProperty
        "  @JsProperty",
        "  public static A a;", // JsProperty
        "}",
        "@JsFunction public static interface FI  { void f(A a); }", // JsFunction method is checked.
        "@JsType public static class Buggy {",
        "  public A field;", // exported field
        "  public A f1(A a) { return null; }", // regular class fails.
        "  public A[] f2(A[] a) { return null; }", // array of regular class fails.
        "  public long f3(long a) { return 1l; }", // long fails.
        // non-JsType class that implements a JsType interface fails.
        "  public B f4(B a) { return null; }",
        "  public void f5(Object[][] a) {}", // Object[][] fails.
        "  public void f6(Object[] a) {}", // Object[] also fails.
        "}");

    assertBuggySucceeds(
        "[unusable-by-js] Return type of "
            + "'test.EntryPoint$Buggy.f1(Ltest/EntryPoint$A;)Ltest/EntryPoint$A;' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Return type of "
            + "'test.EntryPoint$Buggy.f2([Ltest/EntryPoint$A;)[Ltest/EntryPoint$A;' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Return type of "
            + "'test.EntryPoint$Buggy.f3(J)J' is not usable by but exposed to JavaScript",
        "[unusable-by-js] Return type of "
            + "'test.EntryPoint$Buggy.f4(Ltest/EntryPoint$B;)Ltest/EntryPoint$B;' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of field 'field' in type 'test.EntryPoint$Buggy' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method "
            + "'test.EntryPoint$Buggy.f1(Ltest/EntryPoint$A;)Ltest/EntryPoint$A;' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method "
            + "'test.EntryPoint$Buggy.f2([Ltest/EntryPoint$A;)[Ltest/EntryPoint$A;' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method 'test.EntryPoint$Buggy.f3(J)J' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method "
            + "'test.EntryPoint$Buggy.f4(Ltest/EntryPoint$B;)Ltest/EntryPoint$B;' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method "
            + "'test.EntryPoint$Buggy.f5([[Ljava/lang/Object;)V' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method "
            + "'test.EntryPoint$Buggy.f6([Ljava/lang/Object;)V' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method " // JsFunction method
            + "'test.EntryPoint$FI.f(Ltest/EntryPoint$A;)V' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of parameter 'a' in method " // JsMethod in non-jstype class.
            + "'test.EntryPoint$C.fc1(Ltest/EntryPoint$A;)V' "
            + "is not usable by but exposed to JavaScript",
        "[unusable-by-js] Type of field 'a' in type " // JsProperty in non-jstype class.
            + "'test.EntryPoint$D' is not usable by but exposed to JavaScript");
  }

  private static final MockJavaResource jsFunctionInterface = new MockJavaResource(
      "test.MyJsFunctionInterface") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package test;\n");
      code.append("import jsinterop.annotations.JsFunction;\n");
      code.append("@JsFunction public interface MyJsFunctionInterface {\n");
      code.append("int foo(int x);\n");
      code.append("}\n");
      return code;
    }
  };

  public final void assertBuggySucceeds(String... expectedWarnings)
      throws UnableToCompleteException {
    Result result = assertCompileSucceeds("Buggy buggy = null;", expectedWarnings);
    assertNotNull(result.findClass("test.EntryPoint$Buggy"));
  }

  public final void assertBuggyFails(String... expectedErrors) {
    assertTrue(expectedErrors.length > 0);
    assertCompileFails("Buggy buggy = null;", expectedErrors);
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    try {
      JsInteropRestrictionChecker.exec(logger, program, new MinimalRebuildCache());
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
